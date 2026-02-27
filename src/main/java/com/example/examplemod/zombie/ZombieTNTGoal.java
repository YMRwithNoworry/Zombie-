package com.example.examplemod.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ZombieTNTGoal extends Goal {
    
    private final Zombie zombie;
    private BlockPos tntPos;
    private int placeCooldown;
    private int fleeCooldown;
    private static final int PLACE_COOLDOWN = 60;
    private static final int FLEE_DURATION = 30;
    private static final double FLEE_DISTANCE = 10.0;
    private BlockPos fleeTarget;
    
    public ZombieTNTGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 3) {
            return false;
        }
        
        if (zombie.getTarget() == null) {
            return false;
        }
        
        if (fleeCooldown > 0) {
            fleeCooldown--;
            return false;
        }
        
        if (placeCooldown > 0) {
            placeCooldown--;
            return false;
        }
        
        tntPos = findObstruction();
        return tntPos != null;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (fleeCooldown > 0) {
            return true;
        }
        
        if (tntPos == null) return false;
        if (zombie.getTarget() == null) return false;
        
        BlockState state = zombie.level().getBlockState(tntPos);
        return !state.isAir() && !(state.getBlock() instanceof TntBlock);
    }
    
    @Override
    public void start() {
        if (tntPos != null) {
            placeTNT();
            startFleeing();
        }
    }
    
    @Override
    public void stop() {
        tntPos = null;
        fleeTarget = null;
    }
    
    @Override
    public void tick() {
        if (fleeCooldown > 0 && fleeTarget != null) {
            zombie.getNavigation().moveTo(fleeTarget.getX(), fleeTarget.getY(), fleeTarget.getZ(), 1.5);
            fleeCooldown--;
            
            if (fleeCooldown <= 0) {
                placeCooldown = PLACE_COOLDOWN;
            }
            return;
        }
    }
    
    private BlockPos findObstruction() {
        if (zombie.getTarget() == null) return null;
        
        Vec3 targetPos = zombie.getTarget().position();
        BlockPos zombiePos = zombie.blockPosition();
        
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = zombiePos.offset(dx, dy, dz);
                    BlockState state = zombie.level().getBlockState(checkPos);
                    
                    if (!state.isAir() && !state.canBeReplaced() && 
                        !(state.getBlock() instanceof TntBlock) &&
                        state.getDestroySpeed(zombie.level(), checkPos) > 3.0f) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    @SuppressWarnings("deprecation")
    private void placeTNT() {
        Level level = zombie.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        level.setBlock(tntPos, Blocks.TNT.defaultBlockState(), 3);
        
        level.playSound(null, tntPos, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        TntBlock.explode(level, tntPos);
        
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, 
                tntPos.getX() + 0.5, tntPos.getY() + 0.5, tntPos.getZ() + 0.5, 
                10, 0.5, 0.5, 0.5, 0.0);
        }
    }
    
    private void startFleeing() {
        fleeCooldown = FLEE_DURATION;
        
        Vec3 zombiePos = zombie.position();
        Vec3 fleeDirection = zombiePos.subtract(tntPos.getX(), tntPos.getY(), tntPos.getZ()).normalize();
        
        fleeTarget = new BlockPos(
            (int)(zombiePos.x + fleeDirection.x * FLEE_DISTANCE),
            (int)zombiePos.y,
            (int)(zombiePos.z + fleeDirection.z * FLEE_DISTANCE)
        );
        
        alertNearbyZombies();
    }
    
    private void alertNearbyZombies() {
        Level level = zombie.level();
        
        level.getNearbyEntities(Zombie.class, TargetingConditions.DEFAULT, zombie, 
            zombie.getBoundingBox().inflate(FLEE_DISTANCE)).forEach(nearbyZombie -> {
                if (nearbyZombie != zombie) {
                    Vec3 nearbyPos = nearbyZombie.position();
                    Vec3 fleeDir = nearbyPos.subtract(tntPos.getX(), tntPos.getY(), tntPos.getZ()).normalize();
                    
                    BlockPos fleePos = new BlockPos(
                        (int)(nearbyPos.x + fleeDir.x * FLEE_DISTANCE),
                        (int)nearbyPos.y,
                        (int)(nearbyPos.z + fleeDir.z * FLEE_DISTANCE)
                    );
                    
                    nearbyZombie.getNavigation().moveTo(fleePos.getX(), fleePos.getY(), fleePos.getZ(), 1.5);
                }
            });
    }
}
