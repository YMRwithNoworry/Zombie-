package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.config.ZombieEnhanceConfig;
import com.example.examplemod.core.DayTracker;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ZombieEnhancer {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final String ENHANCED_TAG = "ZombieEnhanced";
    private static final String ENHANCE_LEVEL_TAG = "EnhanceLevel";
    
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("d5d5d5d5-0000-0000-0000-000000000001");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("d5d5d5d5-0000-0000-0000-000000000002");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d5d5d5d5-0000-0000-0000-000000000003");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("d5d5d5d5-0000-0000-0000-000000000004");
    private static final UUID FOLLOW_RANGE_UUID = UUID.fromString("d5d5d5d5-0000-0000-0000-000000000005");
    
    private static final double TARGET_FOLLOW_RANGE = 512.0;
    private static final int DEFAULT_ENHANCE_INTERVAL = 10;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onZombieSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Zombie zombie && !event.getLevel().isClientSide) {
            enhanceZombie(zombie, event.getLevel());
        }
    }
    
    public static void enhanceZombie(Zombie zombie, Level level) {
        CompoundTag data = zombie.getPersistentData();
        
        if (data.getBoolean(ENHANCED_TAG)) {
            return;
        }
        
        data.putBoolean(ENHANCED_TAG, true);
        
        increaseFollowRange(zombie);
        
        ZombieInfectionHandler.addTargetGoals(zombie);
        
        int enhanceLevel = calculateEnhanceLevel(level);
        data.putInt(ENHANCE_LEVEL_TAG, enhanceLevel);
        
        LOGGER.info("Zombie enhanced with level: {} (config interval: {})", 
            enhanceLevel, ZombieEnhanceConfig.enhanceInterval);
        
        if (enhanceLevel <= 0) {
            return;
        }
        
        applyAttributeEnhancements(zombie, enhanceLevel);
        applyAbilityEnhancements(zombie, enhanceLevel);
    }
    
    private static void increaseFollowRange(Zombie zombie) {
        if (zombie.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            double currentRange = zombie.getAttributeValue(Attributes.FOLLOW_RANGE);
            double additionalRange = TARGET_FOLLOW_RANGE - currentRange;
            
            if (additionalRange > 0) {
                zombie.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(
                    new AttributeModifier(FOLLOW_RANGE_UUID, "zombie_follow_range_boost", 
                        additionalRange, AttributeModifier.Operation.ADDITION)
                );
            }
        }
    }
    
    private static int calculateEnhanceLevel(Level level) {
        DayTracker tracker = DayTracker.get(level);
        
        if (tracker == null) {
            LOGGER.warn("DayTracker is null, using default enhance level 0");
            return 0;
        }
        
        long currentDay = tracker.getCurrentDay();
        int interval = ZombieEnhanceConfig.enhanceInterval;
        
        if (interval <= 0) {
            interval = DEFAULT_ENHANCE_INTERVAL;
        }
        
        int level_result = (int) (currentDay / interval);
        LOGGER.info("Current day: {}, interval: {}, enhance level: {}", currentDay, interval, level_result);
        
        return level_result;
    }
    
    private static void applyAttributeEnhancements(Zombie zombie, int level) {
        double healthBonus = level * ZombieEnhanceConfig.healthMultiplier;
        double attackBonus = level * ZombieEnhanceConfig.attackMultiplier;
        double speedBonus = level * ZombieEnhanceConfig.speedMultiplier;
        double armorBonus = level * ZombieEnhanceConfig.armorMultiplier;
        
        if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            zombie.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                new AttributeModifier(HEALTH_MODIFIER_UUID, "zombie_health_boost", healthBonus, 
                    AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
            zombie.setHealth(zombie.getMaxHealth());
        }
        
        if (zombie.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            zombie.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                new AttributeModifier(ATTACK_MODIFIER_UUID, "zombie_attack_boost", attackBonus, 
                    AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
        
        if (zombie.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            zombie.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                new AttributeModifier(SPEED_MODIFIER_UUID, "zombie_speed_boost", speedBonus, 
                    AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
        
        if (zombie.getAttribute(Attributes.ARMOR) != null) {
            zombie.getAttribute(Attributes.ARMOR).addPermanentModifier(
                new AttributeModifier(ARMOR_MODIFIER_UUID, "zombie_armor_boost", armorBonus, 
                    AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
    }
    
    private static void applyAbilityEnhancements(Zombie zombie, int level) {
        LOGGER.info("Applying abilities for level: {}", level);
        
        if (level >= 1 && ZombieEnhanceConfig.enableBlockBreaking) {
            zombie.goalSelector.addGoal(2, new ZombieBreakBlockGoal(zombie));
            LOGGER.info("Added ZombieBreakBlockGoal");
        }
        
        if (level >= 2) {
            zombie.goalSelector.addGoal(3, new ZombieBuildBlockGoal(zombie));
            LOGGER.info("Added ZombieBuildBlockGoal");
        }
        
        if (level >= 3) {
            zombie.goalSelector.addGoal(2, new ZombieTNTGoal(zombie));
            LOGGER.info("Added ZombieTNTGoal");
        }
        
        if (level >= 4) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            zombie.goalSelector.addGoal(1, new ZombieCombatGoal(zombie));
            LOGGER.info("Equipped shield and added ZombieCombatGoal");
        }
        
        if (level >= 5) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            zombie.goalSelector.addGoal(2, new ZombieCrossbowGoal(zombie));
            LOGGER.info("Equipped crossbow and added ZombieCrossbowGoal");
        }
        
        if (level >= 6) {
            zombie.goalSelector.addGoal(2, new ZombieEnderPearlGoal(zombie));
            LOGGER.info("Added ZombieEnderPearlGoal (crossbow now uses fireworks)");
        }
        
        if (level >= 8) {
            zombie.goalSelector.addGoal(1, new ZombieHealGoal(zombie));
            LOGGER.info("Added ZombieHealGoal");
        }
        
        if (level >= 10) {
            zombie.goalSelector.addGoal(0, new ZombieUltimateGoal(zombie));
            LOGGER.info("Added ZombieUltimateGoal");
        }
    }
    
    public static int getEnhanceLevel(Zombie zombie) {
        return zombie.getPersistentData().getInt(ENHANCE_LEVEL_TAG);
    }
    
    public static boolean canBreakBlocks(Zombie zombie) {
        return getEnhanceLevel(zombie) >= 1 && ZombieEnhanceConfig.enableBlockBreaking;
    }
}
