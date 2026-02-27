package com.example.examplemod.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ZombieBuildBlockGoal extends Goal {
    
    private final Zombie zombie;
    private final Random random = new Random();
    private int buildCooldown;
    private int buildProgress;
    private static final int COOLDOWN_TICKS = 3;
    private static final int BUILD_TIME = 2;
    private static final double HEIGHT_THRESHOLD = 2.0;
    
    public ZombieBuildBlockGoal(Zombie zombie) {
        this.zombie = zombie;
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 2) {
            return false;
        }
        
        if (zombie.getTarget() == null || !zombie.getTarget().isAlive()) {
            return false;
        }
        
        if (buildCooldown > 0) {
            buildCooldown--;
            return false;
        }
        
        double targetY = zombie.getTarget().getY();
        double zombieY = zombie.getY();
        double heightDiff = targetY - zombieY;
        
        if (heightDiff < HEIGHT_THRESHOLD) {
            return false;
        }
        
        if (heightDiff < 0) {
            return false;
        }
        
        BlockPos belowFeet = zombie.blockPosition().below();
        BlockState belowState = zombie.level().getBlockState(belowFeet);
        if (belowState.isAir() || belowState.canBeReplaced()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (zombie.getTarget() == null || !zombie.getTarget().isAlive()) return false;
        
        double targetY = zombie.getTarget().getY();
        double zombieY = zombie.getY();
        double heightDiff = targetY - zombieY;
        
        if (heightDiff < 1.0) {
            return false;
        }
        
        if (heightDiff < 0) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void start() {
        buildProgress = 0;
    }
    
    @Override
    public void stop() {
        buildCooldown = COOLDOWN_TICKS;
        buildProgress = 0;
    }
    
    @Override
    public void tick() {
        if (zombie.getTarget() == null) return;
        
        zombie.getLookControl().setLookAt(zombie.getTarget());
        
        buildProgress++;
        
        if (buildProgress >= BUILD_TIME) {
            placeBlockAndJump();
            buildCooldown = COOLDOWN_TICKS;
            buildProgress = 0;
        }
    }
    
    private void placeBlockAndJump() {
        Level level = zombie.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        BlockPos feetPos = zombie.blockPosition();
        
        BlockState blockToPlace = random.nextBoolean() ? 
            Blocks.COBBLESTONE.defaultBlockState() : 
            Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        
        level.setBlock(feetPos, blockToPlace, 3);
        level.playSound(null, feetPos, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        Vec3 currentVec = zombie.position();
        double newY = currentVec.y + 1.0;
        zombie.setPos(currentVec.x, newY, currentVec.z);
        
        zombie.setDeltaMovement(new Vec3(0, 0.42, 0));
        zombie.hurtMarked = true;
        zombie.setOnGround(false);
        
        zombie.getNavigation().stop();
    }
}
