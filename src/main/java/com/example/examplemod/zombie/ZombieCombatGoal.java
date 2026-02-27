package com.example.examplemod.zombie;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ZombieCombatGoal extends Goal {
    
    private final Zombie zombie;
    private final Random random = new Random();
    private int snowballCooldown;
    private int shieldCheckCooldown;
    private static final int SNOWBALL_COOLDOWN = 8;
    private static final double SNOWBALL_MIN_RANGE = 4.0;
    private static final double SNOWBALL_MAX_RANGE = 40.0;
    
    public ZombieCombatGoal(Zombie zombie) {
        this.zombie = zombie;
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 4) {
            return false;
        }
        
        return zombie.getTarget() != null && zombie.getTarget().isAlive();
    }
    
    @Override
    public boolean canContinueToUse() {
        return zombie.getTarget() != null && zombie.getTarget().isAlive();
    }
    
    @Override
    public void start() {
        snowballCooldown = 5;
        shieldCheckCooldown = 0;
    }
    
    @Override
    public void stop() {
        ZombieShieldHandler.deactivateShield(zombie);
    }
    
    @Override
    public void tick() {
        if (zombie.getTarget() == null) return;
        
        zombie.getLookControl().setLookAt(zombie.getTarget());
        
        if (snowballCooldown > 0) {
            snowballCooldown--;
        }
        
        if (shieldCheckCooldown > 0) {
            shieldCheckCooldown--;
        }
        
        double distanceToTarget = Math.sqrt(zombie.distanceToSqr(zombie.getTarget()));
        
        if (distanceToTarget > SNOWBALL_MIN_RANGE && distanceToTarget < SNOWBALL_MAX_RANGE) {
            throwSnowball();
        }
        
        if (shieldCheckCooldown <= 0) {
            checkShieldUsage();
            shieldCheckCooldown = 3;
        }
    }
    
    private void throwSnowball() {
        if (snowballCooldown > 0) return;
        if (zombie.getTarget() == null) return;
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        
        snowballCooldown = SNOWBALL_COOLDOWN;
        
        Vec3 targetPos = zombie.getTarget().position();
        Vec3 zombiePos = zombie.position();
        
        double dx = targetPos.x - zombiePos.x;
        double dy = targetPos.y + zombie.getTarget().getEyeHeight() * 0.5 - zombiePos.y - zombie.getEyeHeight();
        double dz = targetPos.z - zombiePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        ZombieSnowball snowball = new ZombieSnowball(serverLevel, zombie);
        
        float velocity = 1.5F;
        if (distance > 20) {
            velocity = 2.0F;
        }
        if (distance > 30) {
            velocity = 2.5F;
        }
        
        snowball.shoot(dx, dy + distance * 0.15, dz, velocity, 5.0F);
        serverLevel.addFreshEntity(snowball);
        
        serverLevel.playSound(null, zombie, SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
    
    private void checkShieldUsage() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 4) return;
        
        if (ZombieShieldHandler.isShieldActive(zombie)) return;
        
        if (!ZombieShieldHandler.canRaiseShield(zombie)) return;
        
        if (shouldRaiseShield()) {
            ZombieShieldHandler.activateShield(zombie);
        }
    }
    
    private boolean shouldRaiseShield() {
        if (!(zombie.getTarget() instanceof Player player)) return false;
        
        double distance = zombie.distanceToSqr(player);
        if (distance > 25) return false;
        
        Vec3 lookVec = player.getLookAngle();
        Vec3 toZombie = zombie.position().subtract(player.position()).normalize();
        double dot = lookVec.dot(toZombie);
        
        if (dot > 0.7) {
            return true;
        }
        
        if (player.swinging) {
            return true;
        }
        
        return false;
    }
}
