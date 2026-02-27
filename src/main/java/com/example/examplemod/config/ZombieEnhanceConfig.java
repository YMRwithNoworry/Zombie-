package com.example.examplemod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ZombieEnhanceConfig {
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER = BUILDER
            .comment("每次增强的生命值倍率 (默认30%)")
            .defineInRange("healthMultiplier", 0.3, 0.0, 10.0);
    
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER = BUILDER
            .comment("每次增强的攻击力倍率 (默认30%)")
            .defineInRange("attackMultiplier", 0.3, 0.0, 10.0);
    
    public static final ForgeConfigSpec.DoubleValue SPEED_MULTIPLIER = BUILDER
            .comment("每次增强的速度倍率 (默认30%)")
            .defineInRange("speedMultiplier", 0.3, 0.0, 10.0);
    
    public static final ForgeConfigSpec.DoubleValue ARMOR_MULTIPLIER = BUILDER
            .comment("每次增强的护甲倍率 (默认30%)")
            .defineInRange("armorMultiplier", 0.3, 0.0, 10.0);
    
    public static final ForgeConfigSpec.IntValue ENHANCE_INTERVAL = BUILDER
            .comment("增强间隔天数 (默认10天)")
            .defineInRange("enhanceInterval", 10, 1, 100);
    
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLOCK_BREAKING = BUILDER
            .comment("是否启用僵尸破坏方块 (第一次增强后)")
            .define("enableBlockBreaking", true);
    
    public static final ForgeConfigSpec.DoubleValue BLOCK_BREAK_SPEED = BUILDER
            .comment("僵尸破坏方块的速度倍率 (相对于玩家)")
            .defineInRange("blockBreakSpeed", 1.0, 0.1, 5.0);
    
    public static final ForgeConfigSpec SPEC = BUILDER.build();
    
    public static double healthMultiplier = 0.3;
    public static double attackMultiplier = 0.3;
    public static double speedMultiplier = 0.3;
    public static double armorMultiplier = 0.3;
    public static int enhanceInterval = 10;
    public static boolean enableBlockBreaking = true;
    public static double blockBreakSpeed = 1.0;
    
    public static void bake() {
        healthMultiplier = HEALTH_MULTIPLIER.get();
        attackMultiplier = ATTACK_MULTIPLIER.get();
        speedMultiplier = SPEED_MULTIPLIER.get();
        armorMultiplier = ARMOR_MULTIPLIER.get();
        enhanceInterval = ENHANCE_INTERVAL.get();
        enableBlockBreaking = ENABLE_BLOCK_BREAKING.get();
        blockBreakSpeed = BLOCK_BREAK_SPEED.get();
    }
}
