package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ExampleMod.MODID);
    
    public static final RegistryObject<AdaptationEffect> ADAPTATION = 
        MOB_EFFECTS.register("adaptation", () -> new AdaptationEffect("", 0));
    
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
