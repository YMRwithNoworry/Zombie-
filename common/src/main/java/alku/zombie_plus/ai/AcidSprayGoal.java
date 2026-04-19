package alku.zombie_plus.ai;

import alku.zombie_plus.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class AcidSprayGoal extends Goal {
    private final PathfinderMob mob;
    private int cooldown;
    private int chargeTime;
    private boolean isCharging;
    private static final int COOLDOWN_TICKS = 100;
    private static final int CHARGE_TICKS = 20;
    private static final double RANGE = 8.0;

    public AcidSprayGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || cooldown > 0) return false;
        return mob.distanceTo(mob.getTarget()) <= RANGE;
    }

    @Override
    public void start() {
        cooldown = 0;
        chargeTime = 0;
        isCharging = true;
    }

    @Override
    public void tick() {
        if (mob.getTarget() == null) return;
        
        mob.getLookControl().setLookAt(mob.getTarget(), 30, 30);
        
        if (isCharging) {
            chargeTime++;
            
            if (mob.level() instanceof ServerLevel serverLevel) {
                Vec3 lookVec = mob.getLookAngle();
                for (int i = 0; i < 3; i++) {
                    double offsetX = (mob.getRandom().nextDouble() - 0.5) * 0.5;
                    double offsetY = (mob.getRandom().nextDouble() - 0.5) * 0.5;
                    double offsetZ = (mob.getRandom().nextDouble() - 0.5) * 0.5;
                    
                    serverLevel.sendParticles(
                        ParticleTypes.DRIPPING_HONEY,
                        mob.getX() + lookVec.x + offsetX,
                        mob.getY() + mob.getEyeHeight() + offsetY,
                        mob.getZ() + lookVec.z + offsetZ,
                        1, 0, 0, 0, 0
                    );
                }
            }
            
            if (chargeTime >= CHARGE_TICKS) {
                sprayAcid();
                isCharging = false;
                cooldown = COOLDOWN_TICKS;
            }
        } else {
            cooldown--;
            if (cooldown <= 0 && mob.distanceTo(mob.getTarget()) <= RANGE) {
                isCharging = true;
                chargeTime = 0;
            }
        }
    }

    private void sprayAcid() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        
        Vec3 mobPos = mob.position().add(0, mob.getEyeHeight(), 0);
        Vec3 lookVec = mob.getLookAngle();
        
        for (int i = 0; i < 30; i++) {
            double spread = 0.3;
            double offsetX = (mob.getRandom().nextDouble() - 0.5) * spread;
            double offsetY = (mob.getRandom().nextDouble() - 0.5) * spread;
            double offsetZ = (mob.getRandom().nextDouble() - 0.5) * spread;
            
            serverLevel.sendParticles(
                ParticleTypes.ITEM_SLIME,
                mobPos.x + lookVec.x * 0.5,
                mobPos.y,
                mobPos.z + lookVec.z * 0.5,
                1,
                lookVec.x * 0.5 + offsetX,
                lookVec.y * 0.5 + offsetY,
                lookVec.z * 0.5 + offsetZ,
                0.1
            );
        }
        
        AABB area = new AABB(
            mob.getX() - RANGE, mob.getY() - 2, mob.getZ() - RANGE,
            mob.getX() + RANGE, mob.getY() + 3, mob.getZ() + RANGE
        );
        
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
            e -> e != mob && e.distanceTo(mob) <= RANGE);
        
        Registry<MobEffect> effectRegistry = serverLevel.registryAccess()
            .registryOrThrow(Registries.MOB_EFFECT);
        Holder<MobEffect> corrosionHolder = effectRegistry.wrapAsHolder(ModEffects.CORROSION.get());
        
        for (LivingEntity target : entities) {
            Vec3 toTarget = target.position().subtract(mob.position()).normalize();
            double dot = lookVec.dot(toTarget);
            
            if (dot > 0.5) {
                target.hurt(serverLevel.damageSources().indirectMagic(mob, mob), 4.0f);
                target.addEffect(new MobEffectInstance(corrosionHolder, 200, 1));
            }
        }
        
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getTarget() != null && mob.isAlive();
    }

    @Override
    public void stop() {
        cooldown = 0;
        chargeTime = 0;
        isCharging = false;
    }
}
