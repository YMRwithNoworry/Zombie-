package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.UUID;

public class ZombieSnowball extends Snowball {
    
    private static final float DAMAGE = 1.0f;
    private static final int FREEZE_HITS_REQUIRED = 5;
    private static final int FREEZE_DURATION = 100;
    private static final String FROST_HIT_TAG = "FrostHitCount";
    private static final String FROZEN_TAG = "IsFrozenByFrost";
    
    private static final UUID FROZEN_SPEED_UUID = UUID.fromString("d5d5d5d5-2222-0000-0000-000000000001");
    
    public ZombieSnowball(Level level, LivingEntity shooter) {
        super(level, shooter);
    }
    
    public ZombieSnowball(EntityType<? extends Snowball> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();
        
        if (entity instanceof LivingEntity target) {
            target.hurt(this.damageSources().thrown(this, owner), DAMAGE);
            
            applyFrostEffect(target);
            
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1);
                
                serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    5, 0.3, 0.3, 0.3, 0.05);
            }
        }
        
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
            SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
    
    private void applyFrostEffect(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        
        int hitCount = data.getInt(FROST_HIT_TAG);
        hitCount++;
        data.putInt(FROST_HIT_TAG, hitCount);
        
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, target, SoundEvents.GLASS_BREAK, 
                SoundSource.HOSTILE, 0.5F, 1.5F);
        }
        
        if (hitCount >= FREEZE_HITS_REQUIRED) {
            freezeTarget(target);
            data.putInt(FROST_HIT_TAG, 0);
        }
    }
    
    private void freezeTarget(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        CompoundTag data = target.getPersistentData();
        data.putBoolean(FROZEN_TAG, true);
        
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_DURATION, 10, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FREEZE_DURATION, 10, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, FREEZE_DURATION, 10, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FREEZE_DURATION, 0, false, true));
        
        if (target.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            target.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                new AttributeModifier(FROZEN_SPEED_UUID, "frozen_speed", -1.0, 
                    AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
        
        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
            30, 0.8, 0.8, 0.8, 0.2);
        
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
            target.getX(), target.getY(), target.getZ(),
            50, 1.0, 1.0, 1.0, 0.3);
        
        serverLevel.playSound(null, target, SoundEvents.GLASS_BREAK, 
            SoundSource.HOSTILE, 2.0F, 0.5F);
        
        scheduleUnfreeze(target);
    }
    
    private void scheduleUnfreeze(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                serverLevel.getServer().getTickCount() + FREEZE_DURATION,
                () -> {
                    if (target.isAlive()) {
                        unfreezeTarget(target);
                    }
                }
            ));
        }
    }
    
    private void unfreezeTarget(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.putBoolean(FROZEN_TAG, false);
        
        if (target.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            target.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(FROZEN_SPEED_UUID);
        }
        
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
            
            serverLevel.playSound(null, target, SoundEvents.GLASS_BREAK, 
                SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }
    
    public static boolean isFrozen(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(FROZEN_TAG);
    }
}
