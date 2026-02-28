package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ZombieAdaptationSystem {
    
    private static final String ADAPTATION_TAG = "ZombieAdaptation";
    private static final int MAX_ADAPTATION_HITS = 15;
    private static final double MAX_DAMAGE_REDUCTION = 0.9;
    private static final int EFFECT_DURATION = 600;
    
    private static final Map<UUID, Map<String, Integer>> adaptationCache = new HashMap<>();
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onZombieHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 3) return;
        
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        
        if (!(attacker instanceof LivingEntity livingAttacker)) return;
        
        if (attacker instanceof Zombie) return;
        
        String entityType = EntityType.getKey(attacker.getType()).toString();
        
        increaseAdaptation(zombie, entityType);
        
        double reduction = getDamageReduction(zombie, entityType);
        if (reduction > 0) {
            float newDamage = event.getAmount() * (float)(1.0 - reduction);
            event.setAmount(newDamage);
        }
    }
    
    public static void increaseAdaptation(Zombie zombie, String entityType) {
        UUID zombieId = zombie.getUUID();
        
        Map<String, Integer> adaptations = adaptationCache.computeIfAbsent(zombieId, k -> new HashMap<>());
        
        int currentHits = adaptations.getOrDefault(entityType, 0);
        if (currentHits < MAX_ADAPTATION_HITS) {
            adaptations.put(entityType, currentHits + 1);
            saveAdaptationToNBT(zombie, adaptations);
            
            applyAdaptationEffect(zombie, entityType, currentHits + 1);
        }
    }
    
    private static void applyAdaptationEffect(Zombie zombie, String entityType, int hitCount) {
        int amplifier = Math.min(hitCount - 1, 14);
        
        MobEffectInstance effect = new MobEffectInstance(
            ModEffects.ADAPTATION.get(),
            EFFECT_DURATION,
            amplifier,
            false,
            true
        );
        
        zombie.addEffect(effect);
        
        if (zombie.level() instanceof ServerLevel serverLevel) {
            spawnAdaptationParticles(serverLevel, zombie);
        }
    }
    
    private static void spawnAdaptationParticles(ServerLevel level, Zombie zombie) {
        for (int i = 0; i < 10; i++) {
            double x = zombie.getX() + (level.random.nextDouble() - 0.5) * zombie.getBbWidth();
            double y = zombie.getY() + level.random.nextDouble() * zombie.getBbHeight();
            double z = zombie.getZ() + (level.random.nextDouble() - 0.5) * zombie.getBbWidth();
            
            level.sendParticles(ParticleTypes.EFFECT, x, y, z, 1, 0, 0, 0, 0.1);
        }
    }
    
    public static int getAdaptationHits(Zombie zombie, String entityType) {
        Map<String, Integer> adaptations = loadAdaptationFromNBT(zombie);
        return adaptations.getOrDefault(entityType, 0);
    }
    
    public static double getDamageReduction(Zombie zombie, String entityType) {
        int hits = getAdaptationHits(zombie, entityType);
        if (hits <= 0) return 0;
        
        double progress = (double) hits / MAX_ADAPTATION_HITS;
        return progress * MAX_DAMAGE_REDUCTION;
    }
    
    private static void saveAdaptationToNBT(Zombie zombie, Map<String, Integer> adaptations) {
        CompoundTag data = zombie.getPersistentData();
        CompoundTag adaptationTag = new CompoundTag();
        
        for (Map.Entry<String, Integer> entry : adaptations.entrySet()) {
            adaptationTag.putInt(entry.getKey(), entry.getValue());
        }
        
        data.put(ADAPTATION_TAG, adaptationTag);
    }
    
    private static Map<String, Integer> loadAdaptationFromNBT(Zombie zombie) {
        UUID zombieId = zombie.getUUID();
        
        if (adaptationCache.containsKey(zombieId)) {
            return adaptationCache.get(zombieId);
        }
        
        Map<String, Integer> adaptations = new HashMap<>();
        
        CompoundTag data = zombie.getPersistentData();
        if (data.contains(ADAPTATION_TAG)) {
            CompoundTag adaptationTag = data.getCompound(ADAPTATION_TAG);
            for (String key : adaptationTag.getAllKeys()) {
                adaptations.put(key, adaptationTag.getInt(key));
            }
        }
        
        adaptationCache.put(zombieId, adaptations);
        return adaptations;
    }
    
    public static void clearCache(UUID zombieId) {
        adaptationCache.remove(zombieId);
    }
}
