package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class ZombieHealGoal extends Goal {
    
    private final Zombie zombie;
    private int cooldown;
    private static final int COOLDOWN_TICKS = 80;
    private static final double HEAL_RANGE = 8.0;
    private static final float HEAL_THRESHOLD = 0.5f;
    
    public ZombieHealGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 8) {
            return false;
        }
        
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        
        if (zombie.getHealth() < zombie.getMaxHealth() * HEAL_THRESHOLD) {
            return true;
        }
        
        return hasNearbyInjuredZombie();
    }
    
    @Override
    public void start() {
        useHealingPotion();
        cooldown = COOLDOWN_TICKS;
    }
    
    @Override
    public boolean canContinueToUse() {
        return false;
    }
    
    private boolean hasNearbyInjuredZombie() {
        Level level = zombie.level();
        
        AABB searchBox = zombie.getBoundingBox().inflate(HEAL_RANGE);
        List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, searchBox);
        
        for (Zombie nearbyZombie : nearbyZombies) {
            if (nearbyZombie != zombie && nearbyZombie.getHealth() < nearbyZombie.getMaxHealth() * HEAL_THRESHOLD) {
                return true;
            }
        }
        
        return false;
    }
    
    private void useHealingPotion() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        
        healSelf();
        
        healNearbyZombies(serverLevel);
        
        serverLevel.playSound(null, zombie, SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT,
            zombie.getX(), zombie.getY() + zombie.getEyeHeight(), zombie.getZ(),
            20, 0.5, 0.5, 0.5, 0.0);
    }
    
    private void healSelf() {
        float healAmount = zombie.getMaxHealth() * 0.3f;
        zombie.heal(healAmount);
        
        if (zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                zombie.getX(), zombie.getY() + zombie.getBbHeight() + 0.5, zombie.getZ(),
                5, 0.5, 0.3, 0.5, 0.0);
        }
    }
    
    private void healNearbyZombies(ServerLevel serverLevel) {
        AABB searchBox = zombie.getBoundingBox().inflate(HEAL_RANGE);
        List<Zombie> nearbyZombies = serverLevel.getEntitiesOfClass(Zombie.class, searchBox);
        
        for (Zombie nearbyZombie : nearbyZombies) {
            if (nearbyZombie != zombie && nearbyZombie.getHealth() < nearbyZombie.getMaxHealth()) {
                float healAmount = nearbyZombie.getMaxHealth() * 0.2f;
                nearbyZombie.heal(healAmount);
                
                serverLevel.sendParticles(ParticleTypes.HEART,
                    nearbyZombie.getX(), nearbyZombie.getY() + nearbyZombie.getBbHeight() + 0.5, nearbyZombie.getZ(),
                    3, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }
}
