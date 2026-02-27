package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Random;

public class ZombieCrossbowGoal extends Goal {
    
    private final Zombie zombie;
    private final Random random = new Random();
    private int cooldown;
    private int fishingRodCooldown;
    private int tntCooldown;
    
    private static final int COOLDOWN_TICKS = 40;
    private static final double ATTACK_RANGE = 30.0;
    private static final double MELEE_RANGE = 6.0;
    private static final double FISHING_ROD_RANGE = 15.0;
    private static final double TNT_RANGE = 25.0;
    private static final int FISHING_ROD_COOLDOWN = 40;
    private static final int TNT_COOLDOWN = 100;
    
    private boolean isCharging = false;
    private int chargeTime = 0;
    private static final int CHARGE_DURATION = 20;
    
    public ZombieCrossbowGoal(Zombie zombie) {
        this.zombie = zombie;
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
        
        double distance = zombie.distanceToSqr(zombie.getTarget());
        
        if (distance < MELEE_RANGE * MELEE_RANGE) {
            switchToMeleeWeapon();
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (zombie.getTarget() == null || !zombie.getTarget().isAlive()) {
            return false;
        }
        
        double distance = zombie.distanceToSqr(zombie.getTarget());
        
        if (distance < MELEE_RANGE * MELEE_RANGE) {
            switchToMeleeWeapon();
            return false;
        }
        
        return true;
    }
    
    @Override
    public void start() {
        cooldown = 0;
        fishingRodCooldown = 0;
        tntCooldown = 0;
        isCharging = false;
        chargeTime = 0;
        switchToCrossbow();
    }
    
    @Override
    public void stop() {
        isCharging = false;
        chargeTime = 0;
    }
    
    @Override
    public void tick() {
        if (zombie.getTarget() == null) return;
        
        LivingEntity target = zombie.getTarget();
        zombie.getLookControl().setLookAt(target);
        
        double distance = zombie.distanceToSqr(target);
        
        if (cooldown > 0) {
            cooldown--;
        }
        if (fishingRodCooldown > 0) {
            fishingRodCooldown--;
        }
        if (tntCooldown > 0) {
            tntCooldown--;
        }
        
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        
        if (level >= 6 && distance < FISHING_ROD_RANGE * FISHING_ROD_RANGE && fishingRodCooldown <= 0) {
            useFishingRod();
        }
        
        if (level >= 6 && distance < TNT_RANGE * TNT_RANGE && tntCooldown <= 0) {
            throwTNT();
        }
        
        if (distance < ATTACK_RANGE * ATTACK_RANGE) {
            if (isCharging) {
                chargeTime++;
                if (chargeTime >= CHARGE_DURATION) {
                    shootCrossbow(target);
                    isCharging = false;
                    chargeTime = 0;
                    cooldown = COOLDOWN_TICKS;
                }
            } else if (cooldown <= 0) {
                startCharging();
            }
        }
    }
    
    private void startCharging() {
        isCharging = true;
        chargeTime = 0;
        
        if (zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, zombie, SoundEvents.CROSSBOW_LOADING_START, 
                SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }
    
    private void shootCrossbow(LivingEntity target) {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (target == null) return;
        
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        boolean useFireworks = level >= 6;
        
        Vec3 zombiePos = zombie.position();
        Vec3 targetPos = target.position();
        
        if (useFireworks) {
            shootFireworkRocket(serverLevel, target, zombiePos, targetPos);
        } else {
            shootArrow(serverLevel, target, zombiePos, targetPos);
        }
        
        serverLevel.playSound(null, zombie, SoundEvents.CROSSBOW_SHOOT, 
            SoundSource.HOSTILE, 1.0F, 1.0F);
        
        serverLevel.sendParticles(ParticleTypes.SMOKE,
            zombiePos.x, zombiePos.y + 1, zombiePos.z,
            5, 0.2, 0.2, 0.2, 0.02);
    }
    
    private void shootFireworkRocket(ServerLevel serverLevel, LivingEntity target, 
                                      Vec3 zombiePos, Vec3 targetPos) {
        ItemStack fireworkStack = createFireworkRocket();
        
        FireworkRocketEntity firework = new FireworkRocketEntity(
            serverLevel, 
            zombie, 
            zombiePos.x, 
            zombiePos.y + zombie.getEyeHeight(), 
            zombiePos.z, 
            fireworkStack
        );
        
        double dx = targetPos.x - zombiePos.x;
        double dy = targetPos.y + target.getEyeHeight() * 0.5 - zombiePos.y - zombie.getEyeHeight();
        double dz = targetPos.z - zombiePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        double speed = 1.5;
        firework.shoot(dx, dy + distance * 0.1, dz, (float)speed, 5.0F);
        
        serverLevel.addFreshEntity(firework);
    }
    
    private void shootArrow(ServerLevel serverLevel, LivingEntity target,
                            Vec3 zombiePos, Vec3 targetPos) {
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(
            serverLevel, zombie
        );
        
        arrow.setPos(zombiePos.x, zombiePos.y + zombie.getEyeHeight(), zombiePos.z);
        
        double dx = targetPos.x - zombiePos.x;
        double dy = targetPos.y + target.getEyeHeight() * 0.5 - zombiePos.y - zombie.getEyeHeight();
        double dz = targetPos.z - zombiePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        arrow.shoot(dx, dy + distance * 0.1, dz, 1.5F, 5.0F);
        
        serverLevel.addFreshEntity(arrow);
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
    
    private void throwTNT() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (zombie.getTarget() == null) return;
        
        tntCooldown = TNT_COOLDOWN;
        
        Vec3 targetPos = zombie.getTarget().position();
        Vec3 zombiePos = zombie.position();
        
        ZombieInstantTNT tnt = new ZombieInstantTNT(serverLevel, 
            zombiePos.x, zombiePos.y + 1, zombiePos.z, zombie);
        
        double dx = targetPos.x - zombiePos.x;
        double dy = targetPos.y - zombiePos.y;
        double dz = targetPos.z - zombiePos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        double velocityX = dx / distance * 0.8;
        double velocityY = Math.max(0.3, distance * 0.03);
        double velocityZ = dz / distance * 0.8;
        
        tnt.setDeltaMovement(velocityX, velocityY, velocityZ);
        
        serverLevel.addFreshEntity(tnt);
        
        serverLevel.playSound(null, zombie, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        serverLevel.sendParticles(ParticleTypes.FLAME,
            zombiePos.x, zombiePos.y + 1, zombiePos.z,
            10, 0.3, 0.3, 0.3, 0.05);
    }
    
    private ItemStack createFireworkRocket() {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fireworkTag = fireworkStack.getOrCreateTag();
        
        CompoundTag fireworksTag = new CompoundTag();
        ListTag explosionsList = new ListTag();
        
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 1);
        explosion.putBoolean("Flicker", true);
        explosion.putBoolean("Trail", true);
        explosion.putIntArray("Colors", new int[]{0xFF0000, 0xFF7F00, 0xFFFF00});
        explosion.putIntArray("FadeColors", new int[]{0x00FF00, 0x0000FF});
        
        explosionsList.add(explosion);
        fireworksTag.put("Explosions", explosionsList);
        fireworksTag.putByte("Flight", (byte) 1);
        
        fireworkTag.put("Fireworks", fireworksTag);
        
        return fireworkStack;
    }
    
    private void switchToCrossbow() {
        ItemStack mainhandItem = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainhandItem.getItem() != Items.CROSSBOW) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }
    
    private void switchToMeleeWeapon() {
        ItemStack mainhandItem = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainhandItem.getItem() == Items.CROSSBOW) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }
}
