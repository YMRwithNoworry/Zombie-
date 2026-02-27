package com.example.examplemod.core;

import com.example.examplemod.ExampleMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class DayTracker extends SavedData {
    
    private static final String DATA_NAME = "daytracker_data";
    private static final long TICKS_PER_DAY = 24000L;
    
    private long totalTicks = 0;
    private long currentDay = 0;
    
    private static final Map<UUID, Long> playerJoinTime = new HashMap<>();
    
    public DayTracker() {
    }
    
    public DayTracker(CompoundTag tag) {
        this.totalTicks = tag.getLong("TotalTicks");
        this.currentDay = tag.getLong("CurrentDay");
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("TotalTicks", totalTicks);
        tag.putLong("CurrentDay", currentDay);
        return tag;
    }
    
    public long getTotalTicks() {
        return totalTicks;
    }
    
    public long getCurrentDay() {
        return currentDay;
    }
    
    public void addTick() {
        totalTicks++;
        currentDay = totalTicks / TICKS_PER_DAY;
        setDirty();
    }
    
    public void setTotalTicks(long ticks) {
        this.totalTicks = ticks;
        this.currentDay = ticks / TICKS_PER_DAY;
    }
    
    public static DayTracker get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            DimensionDataStorage storage = serverLevel.getDataStorage();
            return storage.computeIfAbsent(DayTracker::new, DayTracker::new, DATA_NAME);
        }
        return null;
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerLifecycleHooks.getCurrentServer().getAllLevels().forEach(level -> {
                DayTracker tracker = get(level);
                if (tracker != null) {
                    tracker.addTick();
                    if (tracker.getTotalTicks() % TICKS_PER_DAY == 0) {
                        syncToAllPlayers(level);
                    }
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DayTracker tracker = get(player.level());
            if (tracker != null) {
                syncToPlayer(player, tracker.getCurrentDay());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DayTracker tracker = get(player.level());
            if (tracker != null) {
                syncToPlayer(player, tracker.getCurrentDay());
            }
        }
    }
    
    private static void syncToAllPlayers(ServerLevel level) {
        DayTracker tracker = get(level);
        if (tracker != null) {
            long day = tracker.getCurrentDay();
            for (ServerPlayer player : level.players()) {
                syncToPlayer(player, day);
            }
        }
    }
    
    private static void syncToPlayer(ServerPlayer player, long day) {
        com.example.examplemod.network.NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), 
            new com.example.examplemod.network.DaySyncPacket(day));
    }
}
