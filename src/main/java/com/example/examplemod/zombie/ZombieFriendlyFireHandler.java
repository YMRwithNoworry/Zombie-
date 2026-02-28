package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ZombieFriendlyFireHandler {
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        
        if (victim instanceof Zombie && attacker instanceof Zombie) {
            event.setCanceled(true);
        }
    }
}
