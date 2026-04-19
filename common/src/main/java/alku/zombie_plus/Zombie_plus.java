package alku.zombie_plus;

import alku.zombie_plus.client.ModConfigScreen;
import alku.zombie_plus.client.ZombiePlusKeys;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LootEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.IntegerArgumentType;

public final class Zombie_plus {
    public static final String MOD_ID = "zombie_plus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        ModEffects.init();
        ModConfig.get();
        ModNetworking.register();

        registerServerTick();
        registerEntityEvents();
        registerLootEvents();
        registerPlayerEvents();
        registerCommands();

        LOGGER.info("Zombie Plus initialized!");
    }

    public static void initClient() {
        registerClientEvents();
    }

    private static void registerServerTick() {
        TickEvent.SERVER_POST.register(server -> {
            ServerLevel overworld = server.overworld();
            DayTracker tracker = DayTracker.get(overworld);
            tracker.tick();

            if (tracker.shouldSync()) {
                ModNetworking.syncToAll(server);
            }
        });
    }

    private static void registerEntityEvents() {
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, world, x, y, z, type, spawner) -> {
            if (type == net.minecraft.world.entity.MobSpawnType.NATURAL
                    || type == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION
                    || type == net.minecraft.world.entity.MobSpawnType.PATROL
                    || type == net.minecraft.world.entity.MobSpawnType.STRUCTURE) {
                if (entity instanceof Zombie) {
                    return dev.architectury.event.EventResult.pass();
                }
                if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
                    return dev.architectury.event.EventResult.pass();
                }
                return dev.architectury.event.EventResult.interrupt(false);
            }
            return dev.architectury.event.EventResult.pass();
        });
    }

    private static void registerLootEvents() {
        LootEvent.MODIFY_LOOT_TABLE.register((id, context, builtin) -> {
            if (!builtin) return;
            ResourceLocation lootId = id.location();
            String path = lootId.getPath();
            if (path.startsWith("entities/") && isZombieLootTable(path)) {
                addZombieDrops(context);
            }
        });
    }

    private static boolean isZombieLootTable(String path) {
        return path.equals("entities/zombie")
                || path.equals("entities/zombie_villager")
                || path.equals("entities/husk")
                || path.equals("entities/drowned")
                || path.equals("entities/zombified_piglin");
    }

    private static void addZombieDrops(LootEvent.LootTableModificationContext context) {
        Item[] drops = {Items.GUNPOWDER, Items.ENDER_PEARL, Items.SLIME_BALL, Items.BLAZE_ROD};
        for (Item item : drops) {
            LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(item)
                            .when(LootItemRandomChanceCondition.randomChance(0.3F)));
            context.addPool(pool);
        }
    }

    private static void registerPlayerEvents() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            ServerLevel overworld = player.server.overworld();
            DayTracker tracker = DayTracker.get(overworld);
            ModNetworking.syncToPlayer(player, tracker.getElapsedTicks(), ModConfig.get().getTotalDays());
        });
    }

    private static void registerCommands() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("zombieplus")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("setday")
                            .then(Commands.argument("day", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        int day = IntegerArgumentType.getInteger(context, "day");
                                        DayTracker tracker = DayTracker.get(context.getSource().getLevel());
                                        tracker.setCurrentDay(day);
                                        ModNetworking.syncToAll(context.getSource().getServer());
                                        context.getSource().sendSuccess(
                                                () -> net.minecraft.network.chat.Component.translatable(
                                                        "command.zombie_plus.set_current_day", day), true);
                                        return 1;
                                    })
                            )
                    )
                    .then(Commands.literal("settotaldays")
                            .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        int days = IntegerArgumentType.getInteger(context, "days");
                                        ModConfig.get().setTotalDays(days, context.getSource().getServer());
                                        context.getSource().sendSuccess(
                                                () -> net.minecraft.network.chat.Component.translatable(
                                                        "command.zombie_plus.set_total_days", days), true);
                                        return 1;
                                    })
                            )
                    )
                    .executes(context -> {
                        int currentDay = DayTracker.get(context.getSource().getLevel()).getCurrentDay();
                        int totalDays = ModConfig.get().getTotalDays();
                        int stage = ZombieEnhancer.getEvolutionStage(currentDay, totalDays);
                        context.getSource().sendSuccess(
                                () -> net.minecraft.network.chat.Component.translatable(
                                        "command.zombie_plus.status", currentDay, totalDays, stage), false);
                        return 0;
                    })
            );
        });
    }

    private static void registerClientEvents() {
        ClientGuiEvent.RENDER_HUD.register((GuiGraphics guiGraphics, DeltaTracker deltaTracker) -> {
            alku.zombie_plus.client.DayHudRenderer.renderHud(guiGraphics, deltaTracker);
        });

        ClientTickEvent.CLIENT_POST.register(client -> {
            while (ZombiePlusKeys.CONFIG_KEY.consumeClick()) {
                client.setScreen(new ModConfigScreen());
            }
        });
    }
}
