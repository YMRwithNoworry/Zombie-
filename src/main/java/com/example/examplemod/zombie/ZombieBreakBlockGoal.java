package com.example.examplemod.zombie;

import com.example.examplemod.config.ZombieEnhanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ZombieBreakBlockGoal extends Goal {
    
    private final Zombie zombie;
    private BlockPos targetBlock;
    private int breakProgress;
    private int breakTime;
    private ItemStack originalMainHand;
    private boolean hasStoredOriginalItem;
    private int checkPathCooldown;
    
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
        
        if (zombie.getTarget() == null) {
            return false;
        }
        
        if (hasPathToTarget()) {
            return false;
        }
        
        targetBlock = findBlockToBreak();
        return targetBlock != null;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (targetBlock == null) {
            return false;
        }
        
        if (zombie.getTarget() == null) {
            return false;
        }
        
        if (hasPathToTarget()) {
            return false;
        }
        
        BlockState state = zombie.level().getBlockState(targetBlock);
        if (state.isAir()) {
            return false;
        }
        
        return zombie.distanceToSqr(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()) < 9.0;
    }
    
    private boolean hasPathToTarget() {
        if (zombie.getTarget() == null) return false;
        
        Path path = zombie.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            return true;
        }
        
        Vec3 targetPos = zombie.getTarget().position();
        Path newPath = zombie.getNavigation().createPath(targetPos.x, targetPos.y, targetPos.z, 0);
        return newPath != null;
    }
    
    @Override
    public void start() {
        breakProgress = 0;
        hasStoredOriginalItem = false;
        checkPathCooldown = 0;
        
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
        breakProgress = 0;
        targetBlock = null;
    }
    
    @Override
    public void tick() {
        if (targetBlock == null) {
            return;
        }
        
        checkPathCooldown++;
        if (checkPathCooldown >= 20 && hasPathToTarget()) {
            stop();
            return;
        }
        
        zombie.getLookControl().setLookAt(
            targetBlock.getX() + 0.5, 
            targetBlock.getY() + 0.5, 
            targetBlock.getZ() + 0.5
        );
        
        Level level = zombie.level();
        BlockState state = level.getBlockState(targetBlock);
        
        if (state.isAir()) {
            restoreOriginalItem();
            if (!hasPathToTarget()) {
                targetBlock = findBlockToBreak();
            } else {
                targetBlock = null;
            }
            breakProgress = 0;
            return;
        }
        
        int enhanceLevel = ZombieEnhancer.getEnhanceLevel(zombie);
        boolean instantBreak = enhanceLevel >= 9;
        
        if (instantBreak) {
            breakBlock(level, targetBlock, state);
            restoreOriginalItem();
            breakProgress = 0;
            if (!hasPathToTarget()) {
                targetBlock = findBlockToBreak();
            } else {
                targetBlock = null;
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
            restoreOriginalItem();
            breakProgress = 0;
            if (!hasPathToTarget()) {
                targetBlock = findBlockToBreak();
            } else {
                targetBlock = null;
            }
        }
    }
    
    private BlockPos findBlockToBreak() {
        if (zombie.getTarget() == null) {
            return null;
        }
        
        Vec3 targetPos = zombie.getTarget().position();
        BlockPos zombiePos = zombie.blockPosition();
        BlockPos targetBlockPos = new BlockPos((int)targetPos.x, (int)targetPos.y, (int)targetPos.z);
        
        BlockPos bestPos = null;
        double bestScore = Double.MAX_VALUE;
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    BlockPos checkPos = zombiePos.offset(dx, dy, dz);
                    BlockState state = zombie.level().getBlockState(checkPos);
                    
                    if (!state.isAir() && !state.getFluidState().isSource()) {
                        float hardness = state.getDestroySpeed(zombie.level(), checkPos);
                        if (hardness >= 0 && hardness < 50) {
                            double distToTarget = checkPos.distSqr(targetBlockPos);
                            if (distToTarget < bestScore) {
                                bestScore = distToTarget;
                                bestPos = checkPos;
                            }
                        }
                    }
                }
            }
        }
        
        return bestPos;
    }
    
    private int calculateBreakTime(BlockState state) {
        float hardness = state.getDestroySpeed(zombie.level(), targetBlock);
        
        int baseTime = (int)(hardness * 30);
        
        baseTime = (int)(baseTime / ZombieEnhanceConfig.blockBreakSpeed);
        
        return Math.max(baseTime, 10);
    }
    
    private void storeAndEquipPickaxe() {
        originalMainHand = zombie.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        hasStoredOriginalItem = true;
        
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
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
            
            Block.dropResources(state, level, pos, null, zombie, zombie.getMainHandItem());
            
            level.destroyBlock(pos, false);
            
            level.destroyBlockProgress(zombie.getId(), pos, -1);
        }
    }
}
