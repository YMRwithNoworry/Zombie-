package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ExampleMod.MODID, "day_sync"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        int id = 0;
        
        INSTANCE.messageBuilder(DaySyncPacket.class, id++)
            .encoder(DaySyncPacket::encode)
            .decoder(DaySyncPacket::new)
            .consumerMainThread(DaySyncPacket::handle)
            .add();
    }
}
