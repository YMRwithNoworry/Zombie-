package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.config.ZombieEnhanceConfig;
import com.example.examplemod.core.DayTracker;
import com.example.examplemod.zombie.ZombieEnhancer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ZombieEnhanceCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zombieenhance")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status")
                .executes(ZombieEnhanceCommand::showStatus))
            .then(Commands.literal("setday")
                .then(Commands.argument("day", IntegerArgumentType.integer(0))
                    .executes(ZombieEnhanceCommand::setDay)))
            .then(Commands.literal("setlevel")
                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                    .executes(ZombieEnhanceCommand::setLevel)))
            .then(Commands.literal("help")
                .executes(ZombieEnhanceCommand::showHelp))
            .then(Commands.literal("abilities")
                .executes(ZombieEnhanceCommand::showAbilities))
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Level level = source.getLevel();
        
        DayTracker tracker = DayTracker.get(level);
        if (tracker == null) {
            source.sendFailure(Component.literal("无法获取天数追踪器"));
            return 0;
        }
        
        long currentDay = tracker.getCurrentDay();
        long totalTicks = tracker.getTotalTicks();
        int enhanceLevel = calculateEnhanceLevel(currentDay);
        
        source.sendSuccess(() -> Component.literal("§6========== 僵尸强化状态 =========="), false);
        source.sendSuccess(() -> Component.literal("§e当前天数: §f" + (currentDay + 1)), false);
        source.sendSuccess(() -> Component.literal("§e总Tick数: §f" + totalTicks), false);
        source.sendSuccess(() -> Component.literal("§e强化阶段: §c" + enhanceLevel), false);
        
        if (enhanceLevel > 0) {
            double healthBonus = enhanceLevel * ZombieEnhanceConfig.healthMultiplier * 100;
            double attackBonus = enhanceLevel * ZombieEnhanceConfig.attackMultiplier * 100;
            double speedBonus = enhanceLevel * ZombieEnhanceConfig.speedMultiplier * 100;
            double armorBonus = enhanceLevel * ZombieEnhanceConfig.armorMultiplier * 100;
            
            source.sendSuccess(() -> Component.literal("§e--- 属性加成 ---"), false);
            source.sendSuccess(() -> Component.literal("§a生命值: §f+" + String.format("%.0f%%", healthBonus)), false);
            source.sendSuccess(() -> Component.literal("§a攻击力: §f+" + String.format("%.0f%%", attackBonus)), false);
            source.sendSuccess(() -> Component.literal("§a移动速度: §f+" + String.format("%.0f%%", speedBonus)), false);
            source.sendSuccess(() -> Component.literal("§a护甲值: §f+" + String.format("%.0f%%", armorBonus)), false);
            
            source.sendSuccess(() -> Component.literal("§e--- 已解锁能力 ---"), false);
            List<String> abilities = getAbilitiesForLevel(enhanceLevel);
            for (String ability : abilities) {
                source.sendSuccess(() -> Component.literal(ability), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("§7僵尸尚未强化"), false);
        }
        
        int nextEnhanceDay = (enhanceLevel + 1) * ZombieEnhanceConfig.enhanceInterval;
        int daysUntilNext = nextEnhanceDay - (int)currentDay;
        if (daysUntilNext > 0) {
            source.sendSuccess(() -> Component.literal("§e距离下一强化: §f" + daysUntilNext + " 天"), false);
        }
        
        return 1;
    }
    
    private static int showAbilities(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6========== 僵尸能力一览 =========="), false);
        source.sendSuccess(() -> Component.literal("§e等级1: §f破坏方块 (可被斧破盾)"), false);
        source.sendSuccess(() -> Component.literal("§e等级2: §f搭方块 (原石/苔石)"), false);
        source.sendSuccess(() -> Component.literal("§e等级3: §fTNT炸阻碍 + 远离TNT"), false);
        source.sendSuccess(() -> Component.literal("§e等级4: §f盾牌防御 + 雪球控距"), false);
        source.sendSuccess(() -> Component.literal("§e等级5: §f末影珍珠瞬移"), false);
        source.sendSuccess(() -> Component.literal("§e等级6: §f投掷点燃TNT + 钓鱼竿"), false);
        source.sendSuccess(() -> Component.literal("§e等级8: §f瞬间伤害药水回血"), false);
        source.sendSuccess(() -> Component.literal("§e等级9: §f瞬间破坏方块 + TNT概率提升"), false);
        source.sendSuccess(() -> Component.literal("§e等级10 (99天): §f鞘翅飞行 + 隐身 + 钻石/下界合金套"), false);
        
        return 1;
    }
    
    private static List<String> getAbilitiesForLevel(int level) {
        List<String> abilities = new ArrayList<>();
        
        if (level >= 1) {
            abilities.add("§c✓ 破坏方块");
        }
        if (level >= 2) {
            abilities.add("§c✓ 搭方块");
        }
        if (level >= 3) {
            abilities.add("§c✓ TNT炸阻碍");
        }
        if (level >= 4) {
            abilities.add("§c✓ 盾牌 + 雪球");
        }
        if (level >= 5) {
            abilities.add("§c✓ 末影珍珠瞬移");
        }
        if (level >= 6) {
            abilities.add("§c✓ 投掷TNT + 钓鱼竿");
        }
        if (level >= 8) {
            abilities.add("§c✓ 药水回血");
        }
        if (level >= 9) {
            abilities.add("§c✓ 瞬间破坏");
        }
        if (level >= 10) {
            abilities.add("§c✓ 鞘翅飞行 + 隐身 + 神装");
        }
        
        return abilities;
    }
    
    private static int setDay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int day = IntegerArgumentType.getInteger(context, "day");
        
        Level level = source.getLevel();
        if (!(level instanceof ServerLevel)) {
            source.sendFailure(Component.literal("此命令只能在服务器端执行"));
            return 0;
        }
        
        DayTracker tracker = DayTracker.get(level);
        if (tracker == null) {
            source.sendFailure(Component.literal("无法获取天数追踪器"));
            return 0;
        }
        
        long newTicks = (long) day * 24000L;
        
        tracker.setTotalTicks(newTicks);
        tracker.setDirty();
        
        int newLevel = calculateEnhanceLevel(day);
        
        source.sendSuccess(() -> Component.literal("§a已将天数设置为: §f" + (day + 1) + " §a(强化阶段: §c" + newLevel + "§a)"), true);
        
        return 1;
    }
    
    private static int setLevel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int levelValue = IntegerArgumentType.getInteger(context, "level");
        
        ZombieEnhancer.setOverrideLevel(levelValue);
        
        Level level = source.getLevel();
        DayTracker tracker = DayTracker.get(level);
        long currentDay = tracker != null ? tracker.getCurrentDay() : 0;
        
        source.sendSuccess(() -> Component.literal("§a已将强化等级设置为：§c" + levelValue + " §a(当前天数：§f" + (currentDay + 1) + "§a)"), true);
        
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6========== 僵尸强化指令帮助 =========="), false);
        source.sendSuccess(() -> Component.literal("§e/zombieenhance status §f- 查看当前强化状态"), false);
        source.sendSuccess(() -> Component.literal("§e/zombieenhance setday <天数> §f- 设置当前天数"), false);
        source.sendSuccess(() -> Component.literal("§e/zombieenhance setlevel <等级> §f- 设置强化等级"), false);
        source.sendSuccess(() -> Component.literal("§e/zombieenhance abilities §f- 查看所有能力"), false);
        source.sendSuccess(() -> Component.literal("§e/zombieenhance help §f- 显示此帮助"), false);
        source.sendSuccess(() -> Component.literal("§7--------------------------------"), false);
        source.sendSuccess(() -> Component.literal("§7每 " + ZombieEnhanceConfig.enhanceInterval + " 天僵尸强化一次"), false);
        source.sendSuccess(() -> Component.literal("§7每次强化属性提升: 生命/攻击/速度/护甲 +" + (int)(ZombieEnhanceConfig.healthMultiplier * 100) + "%"), false);
        
        return 1;
    }
    
    private static int calculateEnhanceLevel(long day) {
        int interval = ZombieEnhanceConfig.enhanceInterval;
        if (interval <= 0) return 0;
        return (int) (day / interval);
    }
}
