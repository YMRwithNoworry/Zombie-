package com.example.examplemod.zombie;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AdaptationEffect extends MobEffect {
    
    private final String entityType;
    private final int hitCount;
    
    public AdaptationEffect(String entityType, int hitCount) {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        this.entityType = entityType;
        this.hitCount = hitCount;
    }
    
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            spawnParticles(entity);
        }
    }
    
    private void spawnParticles(LivingEntity entity) {
        if (entity.level().random.nextInt(3) != 0) {
            return;
        }
        
        double x = entity.getX() + (entity.level().random.nextDouble() - 0.5) * entity.getBbWidth();
        double y = entity.getY() + entity.level().random.nextDouble() * entity.getBbHeight();
        double z = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * entity.getBbWidth();
        
        entity.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.EFFECT,
            x, y, z,
            0, 0, 0
        );
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public int getHitCount() {
        return hitCount;
    }
    
    public int getDamageReductionPercentage() {
        return Math.min(90, hitCount * 6);
    }
}
