package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ZombieInstantTNT extends PrimedTnt {
    
    private boolean hasExploded = false;
    
    public ZombieInstantTNT(Level level, double x, double y, double z, net.minecraft.world.entity.LivingEntity owner) {
        super(level, x, y, z, owner);
        this.setFuse(100);
    }
    
    @Override
    public void tick() {
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
        }
        
        if (!this.level().isClientSide && this.onGround() && !hasExploded) {
            explode();
            return;
        }
        
        super.tick();
    }
    
    protected void explode() {
        if (hasExploded) return;
        hasExploded = true;
        
        this.discard();
        
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(this, this.getX(), this.getY(0.0625), this.getZ(), 4.0F, 
                net.minecraft.world.level.Level.ExplosionInteraction.MOB);
            
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 
                (1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F) * 0.7F);
            
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
