package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class CommandRegister {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ZombieEnhanceCommand.register(event.getDispatcher());
    }
}
