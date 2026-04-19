package alku.zombie_plus;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {
    public static final ResourceLocation DAY_SYNC = ResourceLocation.fromNamespaceAndPath(Zombie_plus.MOD_ID, "day_sync");
    public static final ResourceLocation CONFIG_UPDATE = ResourceLocation.fromNamespaceAndPath(Zombie_plus.MOD_ID, "config_update");

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.s2c(), DAY_SYNC, (buf, context) -> {
            long elapsedTicks = buf.readLong();
            int totalDays = buf.readInt();
            context.queue(() -> {
                alku.zombie_plus.client.ClientData.update(elapsedTicks, totalDays);
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CONFIG_UPDATE, (buf, context) -> {
            int totalDays = buf.readInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    if (serverPlayer.hasPermissions(2)) {
                        ModConfig.get().setTotalDays(totalDays, serverPlayer.server);
                    }
                }
            });
        });
    }

    public static void syncToPlayer(ServerPlayer player, long elapsedTicks, int totalDays) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeLong(elapsedTicks);
        buf.writeInt(totalDays);
        NetworkManager.sendToPlayer(player, DAY_SYNC, buf);
    }

    public static void syncToAll(net.minecraft.server.MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DayTracker tracker = DayTracker.get(overworld);
        long ticks = tracker.getElapsedTicks();
        int days = ModConfig.get().getTotalDays();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player, ticks, days);
        }
    }

    public static void sendConfigUpdate(int totalDays) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                net.minecraft.client.Minecraft.getInstance().getConnection().registryAccess());
        buf.writeInt(totalDays);
        NetworkManager.sendToServer(CONFIG_UPDATE, buf);
    }
}
