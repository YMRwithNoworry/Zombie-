package com.example.examplemod.zombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Random;

public class ZombieUltimateGoal extends Goal {
    
    private final Zombie zombie;
    private final Random random = new Random();
    private int invisibilityCooldown;
    private int fireworkCooldown;
    private int flightTimer;
    private static final int INVISIBILITY_COOLDOWN = 600;
    private static final int INVISIBILITY_DURATION = 200;
    private static final int FIREWORK_COOLDOWN = 40;
    private static final double FIREWORK_BOOST = 1.5;
    private static final double GLIDE_SPEED = 0.3;
    
    private static Method setSharedFlagMethod;
    
    static {
        try {
            setSharedFlagMethod = net.minecraft.world.entity.Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
            setSharedFlagMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public ZombieUltimateGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 10) {
            return false;
        }
        
        if (zombie.getTarget() == null) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void start() {
        equipUltimateGear();
        
        if (invisibilityCooldown <= 0) {
            applyInvisibility();
            invisibilityCooldown = INVISIBILITY_COOLDOWN;
        }
    }
    
    @Override
    public void tick() {
        if (zombie.getTarget() == null) return;
        
        if (invisibilityCooldown > 0) {
            invisibilityCooldown--;
        }
        
        if (fireworkCooldown > 0) {
            fireworkCooldown--;
        }
        
        handleElytraFlight();
        
        if (invisibilityCooldown <= 0 && !zombie.hasEffect(MobEffects.INVISIBILITY)) {
            applyInvisibility();
            invisibilityCooldown = INVISIBILITY_COOLDOWN;
        }
    }
    
    private void equipUltimateGear() {
        ItemStack helmet = createEnchantedArmor(Items.DIAMOND_HELMET);
        ItemStack chestplate = createEnchantedArmor(Items.DIAMOND_CHESTPLATE);
        ItemStack leggings = createEnchantedArmor(Items.DIAMOND_LEGGINGS);
        ItemStack boots = createEnchantedArmor(Items.DIAMOND_BOOTS);
        
        if (random.nextDouble() < 0.3) {
            helmet = createEnchantedArmor(Items.NETHERITE_HELMET);
            chestplate = createEnchantedArmor(Items.NETHERITE_CHESTPLATE);
            leggings = createEnchantedArmor(Items.NETHERITE_LEGGINGS);
            boots = createEnchantedArmor(Items.NETHERITE_BOOTS);
        }
        
        zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
        zombie.setItemSlot(EquipmentSlot.CHEST, chestplate);
        zombie.setItemSlot(EquipmentSlot.LEGS, leggings);
        zombie.setItemSlot(EquipmentSlot.FEET, boots);
        
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        elytra.enchant(Enchantments.BINDING_CURSE, 1);
        zombie.setItemSlot(EquipmentSlot.CHEST, elytra);
        
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0f);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.0f);
        zombie.setDropChance(EquipmentSlot.LEGS, 0.0f);
        zombie.setDropChance(EquipmentSlot.FEET, 0.0f);
    }
    
    private ItemStack createEnchantedArmor(Item item) {
        ItemStack armor = new ItemStack(item);
        
        armor.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        armor.enchant(Enchantments.UNBREAKING, 3);
        
        if (item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) {
            armor.enchant(Enchantments.FALL_PROTECTION, 4);
        }
        
        return armor;
    }
    
    private void applyInvisibility() {
        zombie.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION, 0, false, false));
        
        Level level = zombie.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                zombie.getX(), zombie.getY() + zombie.getBbHeight() / 2, zombie.getZ(),
                30, 0.5, 0.5, 0.5, 0.05);
        }
        
        applyInvisibilityToNearbyZombies();
    }
    
    private void applyInvisibilityToNearbyZombies() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        
        serverLevel.getNearbyEntities(Zombie.class, TargetingConditions.DEFAULT, zombie,
            zombie.getBoundingBox().inflate(10)).forEach(nearbyZombie -> {
                if (nearbyZombie != zombie) {
                    nearbyZombie.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION, 0, false, false));
                }
            });
    }
    
    private void handleElytraFlight() {
        if (zombie.getTarget() == null) return;
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        
        Vec3 targetPos = zombie.getTarget().position();
        Vec3 zombiePos = zombie.position();
        
        double horizontalDistance = Math.sqrt(
            Math.pow(targetPos.x - zombiePos.x, 2) + 
            Math.pow(targetPos.z - zombiePos.z, 2)
        );
        
        double heightDifference = targetPos.y - zombiePos.y;
        
        boolean shouldFly = horizontalDistance > 8 || heightDifference > 3;
        
        if (shouldFly && !zombie.isFallFlying()) {
            startFlying();
        }
        
        if (zombie.isFallFlying()) {
            performGlide(targetPos);
            
            if (fireworkCooldown <= 0 && random.nextDouble() < 0.3) {
                useFireworkBoost();
                fireworkCooldown = FIREWORK_COOLDOWN;
            }
            
            if (horizontalDistance < 5 && zombie.onGround()) {
                stopFlying();
            }
        }
    }
    
    private void startFlying() {
        Vec3 currentPos = zombie.position();
        Vec3 targetPos = zombie.getTarget() != null ? zombie.getTarget().position() : currentPos;
        
        Vec3 launchDirection = targetPos.subtract(currentPos).normalize();
        
        zombie.setDeltaMovement(launchDirection.x * 0.8, 1.0, launchDirection.z * 0.8);
        zombie.hurtMarked = true;
        zombie.setOnGround(false);
        
        try {
            setSharedFlagMethod.invoke(zombie, 7, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        zombie.level().playSound(null, zombie, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
    
    private void stopFlying() {
        try {
            setSharedFlagMethod.invoke(zombie, 7, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void performGlide(Vec3 targetPos) {
        Vec3 currentPos = zombie.position();
        Vec3 currentVel = zombie.getDeltaMovement();
        
        Vec3 direction = targetPos.subtract(currentPos).normalize();
        
        double speed = GLIDE_SPEED;
        
        double horizontalSpeed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
        if (horizontalSpeed < 0.1) {
            speed = 0.5;
        }
        
        Vec3 newVel = new Vec3(
            direction.x * speed,
            currentVel.y * 0.98,
            direction.z * speed
        );
        
        if (currentPos.y > targetPos.y + 5) {
            newVel = new Vec3(newVel.x, -0.3, newVel.z);
        } else if (currentPos.y < targetPos.y) {
            newVel = new Vec3(newVel.x, 0.2, newVel.z);
        }
        
        zombie.setDeltaMovement(newVel);
        zombie.hurtMarked = true;
        
        zombie.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z);
        
        zombie.fallDistance = 0;
    }
    
    private void useFireworkBoost() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (!zombie.isFallFlying()) return;
        
        Vec3 currentPos = zombie.position();
        Vec3 currentVel = zombie.getDeltaMovement();
        
        Vec3 boostDirection;
        if (currentVel.length() > 0.1) {
            boostDirection = currentVel.normalize();
        } else {
            Vec3 targetPos = zombie.getTarget().position();
            boostDirection = targetPos.subtract(currentPos).normalize();
        }
        
        Vec3 boostVelocity = boostDirection.scale(FIREWORK_BOOST);
        
        zombie.setDeltaMovement(currentVel.add(boostVelocity));
        zombie.hurtMarked = true;
        
        serverLevel.sendParticles(ParticleTypes.FLAME,
            currentPos.x, currentPos.y + 0.5, currentPos.z,
            20, 0.3, 0.3, 0.3, 0.1);
        
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
            currentPos.x, currentPos.y + 0.5, currentPos.z,
            10, 0.2, 0.2, 0.2, 0.1);
        
        serverLevel.playSound(null, zombie, SoundEvents.FIREWORK_ROCKET_LAUNCH, 
            SoundSource.HOSTILE, 1.5F, 1.2F);
    }
}
