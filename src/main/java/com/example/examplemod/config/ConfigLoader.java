package com.example.examplemod.config;

import com.example.examplemod.ExampleMod;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigLoader {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        LOGGER.info("Config loading: {}", event.getConfig().getFileName());
        if (event.getConfig().getSpec() == ZombieEnhanceConfig.SPEC) {
            ZombieEnhanceConfig.bake();
            LOGGER.info("ZombieEnhanceConfig baked - interval: {}, healthMultiplier: {}", 
                ZombieEnhanceConfig.enhanceInterval, ZombieEnhanceConfig.healthMultiplier);
        }
    }
    
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        LOGGER.info("Config reloading: {}", event.getConfig().getFileName());
        if (event.getConfig().getSpec() == ZombieEnhanceConfig.SPEC) {
            ZombieEnhanceConfig.bake();
            LOGGER.info("ZombieEnhanceConfig re-baked - interval: {}, healthMultiplier: {}", 
                ZombieEnhanceConfig.enhanceInterval, ZombieEnhanceConfig.healthMultiplier);
        }
    }
}
