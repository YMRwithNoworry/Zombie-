package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class ZombieSnowball extends Snowball {
    
    private static final float DAMAGE = 1.0f;
    
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
            
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    5, 0.3, 0.3, 0.3, 0.1);
            }
        }
        
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
            SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
