package com.example.examplemod.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Random;

public class ZombieRangedAttackGoal extends Goal {
    
    private final Zombie zombie;
    private final Random random = new Random();
    private int tntCooldown;
    private int fishingRodCooldown;
    private static final int TNT_COOLDOWN = 60;
    private static final int FISHING_ROD_COOLDOWN = 40;
    private static final double TNT_RANGE = 25.0;
    private static final double FISHING_ROD_RANGE = 15.0;
    
    public ZombieRangedAttackGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 6) {
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
        tntCooldown = 60;
        fishingRodCooldown = 30;
    }
    
    @Override
    public void tick() {
        if (zombie.getTarget() == null) return;
        
        zombie.getLookControl().setLookAt(zombie.getTarget());
        
        if (tntCooldown > 0) {
            tntCooldown--;
        }
        
        if (fishingRodCooldown > 0) {
            fishingRodCooldown--;
        }
        
        double distance = zombie.distanceToSqr(zombie.getTarget());
        
        if (distance < FISHING_ROD_RANGE * FISHING_ROD_RANGE && fishingRodCooldown <= 0) {
            useFishingRod();
        }
        
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        double tntChance = level >= 9 ? 0.3 : 0.1;
        
        if (distance < TNT_RANGE * TNT_RANGE && tntCooldown <= 0) {
            if (random.nextDouble() < tntChance) {
                throwTNT();
            } else {
                tntCooldown = TNT_COOLDOWN / 2;
            }
        }
    }
    
    private void throwTNT() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (zombie.getTarget() == null) return;
        
        tntCooldown = TNT_COOLDOWN;
        
        Vec3 targetPos = zombie.getTarget().position();
        Vec3 zombiePos = zombie.position();
        
        PrimedTnt tnt = new PrimedTnt(serverLevel, 
            zombiePos.x, zombiePos.y + 1, zombiePos.z, zombie);
        
        double dx = targetPos.x - zombiePos.x;
        double dy = targetPos.y - zombiePos.y;
        double dz = targetPos.z - zombiePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        double velocityX = dx / distance * 0.8;
        double velocityY = Math.max(0.3, distance * 0.03);
        double velocityZ = dz / distance * 0.8;
        
        tnt.setDeltaMovement(velocityX, velocityY, velocityZ);
        tnt.setFuse(40);
        
        serverLevel.addFreshEntity(tnt);
        
        serverLevel.playSound(null, zombie, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        serverLevel.sendParticles(ParticleTypes.FLAME,
            zombiePos.x, zombiePos.y + 1, zombiePos.z,
            10, 0.3, 0.3, 0.3, 0.05);
    }
    
    private void useFishingRod() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (zombie.getTarget() == null) return;
        
        fishingRodCooldown = FISHING_ROD_COOLDOWN;
        
        Vec3 targetPos = zombie.getTarget().position();
        Vec3 zombiePos = zombie.position();
        
        double dx = targetPos.x - zombiePos.x;
        double dy = targetPos.y + zombie.getTarget().getEyeHeight() * 0.5 - zombiePos.y - zombie.getEyeHeight();
        double dz = targetPos.z - zombiePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        ZombieFishingHook hook = new ZombieFishingHook(serverLevel, zombie);
        hook.setPos(zombiePos.x, zombiePos.y + zombie.getEyeHeight(), zombiePos.z);
        
        hook.shoot(dx, dy + distance * 0.1, dz, 1.5F, 5.0F);
        
        serverLevel.addFreshEntity(hook);
        
        serverLevel.playSound(null, zombie, SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
