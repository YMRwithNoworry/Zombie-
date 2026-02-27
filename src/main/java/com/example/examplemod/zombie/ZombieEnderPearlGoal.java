package com.example.examplemod.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ZombieEnderPearlGoal extends Goal {
    
    private final Zombie zombie;
    private int cooldown;
    private static final int COOLDOWN_TICKS = 100;
    private static final double MIN_TELEPORT_DISTANCE = 8.0;
    private static final double MAX_TELEPORT_DISTANCE = 30.0;
    
    public ZombieEnderPearlGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 5) {
            return false;
        }
        
        if (zombie.getTarget() == null || !zombie.getTarget().isAlive()) {
            return false;
        }
        
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        
        double distance = zombie.distanceToSqr(zombie.getTarget());
        if (distance < MIN_TELEPORT_DISTANCE * MIN_TELEPORT_DISTANCE) {
            return false;
        }
        
        if (distance > MAX_TELEPORT_DISTANCE * MAX_TELEPORT_DISTANCE) {
            return false;
        }
        
        return canThrowPearlToTarget();
    }
    
    @Override
    public void start() {
        throwEnderPearl();
        cooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public boolean canContinueToUse() {
        return false;
    }
    
    private boolean canThrowPearlToTarget() {
        if (zombie.getTarget() == null) return false;
        
        Vec3 eyePos = zombie.getEyePosition();
        Vec3 targetPos = zombie.getTarget().position().add(0, zombie.getTarget().getEyeHeight() * 0.5, 0);
        
        ClipContext context = new ClipContext(eyePos, targetPos, 
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, zombie);
        HitResult result = zombie.level().clip(context);
        
        return result.getType() == HitResult.Type.MISS;
    }
    
    private void throwEnderPearl() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (zombie.getTarget() == null) return;
        
        Vec3 targetPos = zombie.getTarget().position();
        Vec3 eyePos = zombie.getEyePosition();
        
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y + zombie.getTarget().getEyeHeight() * 0.5 - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        ThrownEnderpearl pearl = new ThrownEnderpearl(serverLevel, zombie);
        pearl.shoot(dx, dy + distance * 0.1, dz, 1.5F, 5.0F);
        serverLevel.addFreshEntity(pearl);
        
        serverLevel.playSound(null, zombie, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        serverLevel.sendParticles(ParticleTypes.PORTAL,
            zombie.getX(), zombie.getY() + zombie.getEyeHeight(), zombie.getZ(),
            20, 0.5, 0.5, 0.5, 0.1);
    }
}
