package com.example.examplemod.zombie;

import com.example.examplemod.config.ZombieEnhanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

import java.util.*;

public class ZombieBreakBlockGoal extends Goal {
    
    private final Zombie zombie;
    private BlockPos targetBlock;
    private int breakProgress;
    private int breakTime;
    private ItemStack originalMainHand;
    private boolean hasStoredOriginalItem;
    private int recheckCooldown;
    
    public ZombieBreakBlockGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        if (!ZombieEnhanceConfig.enableBlockBreaking) {
            return false;
        }
        
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 1) {
            return false;
        }
        
        if (zombie.getTarget() == null || !zombie.getTarget().isAlive()) {
            return false;
        }
        
        if (canReachTargetDirectly()) {
            return false;
        }
        
        targetBlock = findBlockingBlock();
        return targetBlock != null;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (zombie.getTarget() == null || !zombie.getTarget().isAlive()) {
            return false;
        }
        
        if (targetBlock == null) {
            return false;
        }
        
        if (canReachTargetDirectly()) {
            return false;
        }
        
        BlockState state = zombie.level().getBlockState(targetBlock);
        if (state.isAir()) {
            return false;
        }
        
        return true;
    }
    
    private boolean canReachTargetDirectly() {
        if (zombie.getTarget() == null) return true;
        
        Vec3 targetPos = zombie.getTarget().position();
        Path path = zombie.getNavigation().createPath(targetPos.x, targetPos.y, targetPos.z, 0);
        
        if (path == null) {
            return false;
        }
        
        if (path.isDone()) {
            return true;
        }
        
        int pathLength = path.getNodeCount();
        if (pathLength > 100) {
            return false;
        }
        
        double distToTarget = zombie.distanceToSqr(zombie.getTarget());
        if (distToTarget < 9.0) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void start() {
        breakProgress = 0;
        hasStoredOriginalItem = false;
        recheckCooldown = 0;
        
        if (targetBlock != null) {
            BlockState state = zombie.level().getBlockState(targetBlock);
            breakTime = calculateBreakTime(state);
            storeAndEquipPickaxe();
            zombie.getNavigation().stop();
        }
    }
    
    @Override
    public void stop() {
        restoreOriginalItem();
        targetBlock = null;
        breakProgress = 0;
    }
    
    @Override
    public void tick() {
        if (targetBlock == null) {
            targetBlock = findBlockingBlock();
            if (targetBlock != null) {
                BlockState state = zombie.level().getBlockState(targetBlock);
                breakTime = calculateBreakTime(state);
                storeAndEquipPickaxe();
            }
            return;
        }
        
        recheckCooldown++;
        if (recheckCooldown >= 30) {
            recheckCooldown = 0;
            if (canReachTargetDirectly()) {
                return;
            }
        }
        
        zombie.getLookControl().setLookAt(
            targetBlock.getX() + 0.5, 
            targetBlock.getY() + 0.5, 
            targetBlock.getZ() + 0.5
        );
        
        Level level = zombie.level();
        BlockState state = level.getBlockState(targetBlock);
        
        if (state.isAir()) {
            targetBlock = findBlockingBlock();
            if (targetBlock != null) {
                state = level.getBlockState(targetBlock);
                breakTime = calculateBreakTime(state);
                storeAndEquipPickaxe();
            }
            return;
        }
        
        int enhanceLevel = ZombieEnhancer.getEnhanceLevel(zombie);
        boolean instantBreak = enhanceLevel >= 9;
        
        if (instantBreak) {
            breakBlock(level, targetBlock, state);
            targetBlock = findBlockingBlock();
            if (targetBlock != null) {
                state = level.getBlockState(targetBlock);
                breakTime = calculateBreakTime(state);
                storeAndEquipPickaxe();
            }
            return;
        }
        
        breakProgress++;
        
        if (level instanceof ServerLevel) {
            level.destroyBlockProgress(zombie.getId(), targetBlock, 
                (int)((float)breakProgress / breakTime * 10));
        }
        
        if (breakProgress % 4 == 0) {
            level.playSound(null, targetBlock, state.getSoundType().getHitSound(), 
                SoundSource.HOSTILE, 0.5F, 0.75F);
        }
        
        if (breakProgress >= breakTime) {
            breakBlock(level, targetBlock, state);
            targetBlock = findBlockingBlock();
            if (targetBlock != null) {
                state = level.getBlockState(targetBlock);
                breakTime = calculateBreakTime(state);
                storeAndEquipPickaxe();
            }
        }
    }
    
    private BlockPos findBlockingBlock() {
        if (zombie.getTarget() == null) return null;
        
        Vec3 zombiePos = zombie.position();
        Vec3 targetPos = zombie.getTarget().position();
        
        BlockPos blockingOnPath = findBlockOnPath(zombiePos, targetPos);
        if (blockingOnPath != null) {
            return blockingOnPath;
        }
        
        BlockPos blockingLOS = findBlockBlockingLineOfSight(zombiePos, targetPos);
        if (blockingLOS != null) {
            return blockingLOS;
        }
        
        return null;
    }
    
    private BlockPos findBlockOnPath(Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        
        for (double d = 1.0; d < Math.min(distance, 8.0); d += 0.5) {
            Vec3 checkPoint = start.add(direction.scale(d));
            BlockPos checkPos = new BlockPos(
                (int)Math.floor(checkPoint.x), 
                (int)Math.floor(checkPoint.y), 
                (int)Math.floor(checkPoint.z)
            );
            
            BlockState state = zombie.level().getBlockState(checkPos);
            if (isBreakableBlock(state, checkPos)) {
                return checkPos;
            }
            
            BlockPos abovePos = checkPos.above();
            state = zombie.level().getBlockState(abovePos);
            if (isBreakableBlock(state, abovePos)) {
                return abovePos;
            }
        }
        
        return null;
    }
    
    private BlockPos findBlockBlockingLineOfSight(Vec3 start, Vec3 end) {
        Vec3 eyeStart = zombie.getEyePosition();
        Vec3 eyeEnd = zombie.getTarget().getEyePosition();
        
        HitResult hit = zombie.level().clip(new ClipContext(
            eyeStart, eyeEnd, 
            ClipContext.Block.COLLIDER, 
            ClipContext.Fluid.NONE, 
            zombie
        ));
        
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = BlockPos.containing(hit.getLocation());
            BlockState state = zombie.level().getBlockState(hitPos);
            if (isBreakableBlock(state, hitPos)) {
                return hitPos;
            }
        }
        
        return null;
    }
    
    private boolean isBreakableBlock(BlockState state, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getFluidState().isSource()) return false;
        
        float hardness = state.getDestroySpeed(zombie.level(), pos);
        if (hardness < 0) return false;
        if (hardness > 50) return false;
        
        return true;
    }
    
    private int calculateBreakTime(BlockState state) {
        float hardness = state.getDestroySpeed(zombie.level(), targetBlock);
        
        int baseTime = (int)(hardness * 5);
        
        baseTime = (int)(baseTime / ZombieEnhanceConfig.blockBreakSpeed);
        
        int enhanceLevel = ZombieEnhancer.getEnhanceLevel(zombie);
        float levelMultiplier = 1.0f + enhanceLevel * 0.5f;
        baseTime = (int)(baseTime / levelMultiplier);
        
        return Math.max(baseTime, 1);
    }
    
    private void storeAndEquipPickaxe() {
        if (!hasStoredOriginalItem) {
            originalMainHand = zombie.getItemBySlot(EquipmentSlot.MAINHAND).copy();
            hasStoredOriginalItem = true;
        }
        
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
    }
    
    private void restoreOriginalItem() {
        if (hasStoredOriginalItem) {
            if (originalMainHand.isEmpty()) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            } else {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, originalMainHand.copy());
            }
            hasStoredOriginalItem = false;
        }
    }
    
    private void breakBlock(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel) {
            level.playSound(null, pos, state.getSoundType().getBreakSound(), 
                SoundSource.HOSTILE, 1.0F, 1.0F);
            
            level.destroyBlock(pos, false);
            
            level.destroyBlockProgress(zombie.getId(), pos, -1);
        }
    }
}
