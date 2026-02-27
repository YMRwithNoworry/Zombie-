package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ZombieFishingHook extends FishingHook {
    
    private static final double PULL_STRENGTH = 0.8;
    private static final double PULL_HEIGHT = 0.4;
    
    public ZombieFishingHook(EntityType<? extends FishingHook> type, Level level) {
        super(type, level);
    }
    
    public ZombieFishingHook(Level level, LivingEntity shooter) {
        super(EntityType.FISHING_BOBBER, level);
        this.setOwner(shooter);
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();
        
        if (entity instanceof LivingEntity target && owner instanceof LivingEntity shooter) {
            if (target != shooter) {
                pullTarget(target, shooter);
                
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FISHING,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        10, 0.5, 0.5, 0.5, 0.1);
                }
                
                this.level().playSound(null, target, SoundEvents.FISHING_BOBBER_SPLASH, 
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
        
        this.discard();
    }
    
    private void pullTarget(LivingEntity target, LivingEntity shooter) {
        Vec3 targetPos = target.position();
        Vec3 shooterPos = shooter.position();
        
        Vec3 pullDirection = shooterPos.subtract(targetPos).normalize();
        
        double distance = targetPos.distanceTo(shooterPos);
        double pullMultiplier = Math.min(1.5, distance / 10.0);
        
        Vec3 currentMotion = target.getDeltaMovement();
        Vec3 pullMotion = new Vec3(
            pullDirection.x * PULL_STRENGTH * pullMultiplier,
            PULL_HEIGHT,
            pullDirection.z * PULL_STRENGTH * pullMultiplier
        );
        
        target.setDeltaMovement(currentMotion.add(pullMotion));
        target.hurtMarked = true;
        target.setOnGround(false);
    }
}
