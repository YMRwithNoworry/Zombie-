package com.example.examplemod.network;

import com.example.examplemod.client.ClientDayData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DaySyncPacket {
    private final long day;
    
    public DaySyncPacket(long day) {
        this.day = day;
    }
    
    public DaySyncPacket(FriendlyByteBuf buffer) {
        this.day = buffer.readLong();
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(day);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientDayData.setDay(day);
            });
        });
        context.get().setPacketHandled(true);
    }
}
