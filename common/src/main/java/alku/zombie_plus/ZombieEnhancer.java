package alku.zombie_plus;

import alku.zombie_plus.ai.*;
import alku.zombie_plus.mixin.ZombieAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieEnhancer {

    public static int getEvolutionStage(int currentDay, int totalDays) {
        if (totalDays <= 0) return 0;
        int stage = (currentDay * 9) / totalDays;
        return Math.min(stage, 9);
    }

    public static void applyEnhancements(Zombie zombie, int stage) {
        if (stage <= 0) return;

        applyBaseStats(zombie, stage);
        
        ZombieAccessor accessor = (ZombieAccessor) zombie;
        
        for (int i = 1; i <= stage; i++) {
            switch (i) {
                case 1 -> applyStage1(zombie, accessor);
                case 2 -> applyStage2(zombie, accessor);
                case 3 -> applyStage3(zombie, accessor);
                case 4 -> applyStage4(zombie);
                case 5 -> applyStage5(zombie);
                case 6 -> applyStage6(zombie);
                case 7 -> applyStage7(zombie, accessor);
                case 8 -> applyStage8(zombie, accessor);
                case 9 -> applyStage9(zombie);
            }
        }
        
        zombie.setHealth(zombie.getMaxHealth());
    }

    private static void applyBaseStats(Zombie zombie, int stage) {
        applyModifier(zombie, Attributes.MAX_HEALTH, "health_boost",
                stage * 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyModifier(zombie, Attributes.MOVEMENT_SPEED, "speed_boost",
                stage * 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyModifier(zombie, Attributes.ATTACK_DAMAGE, "attack_boost",
                stage * 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        applyModifier(zombie, Attributes.KNOCKBACK_RESISTANCE, "knockback_boost",
                Math.min(stage * 0.03, 0.5), AttributeModifier.Operation.ADD_VALUE);

        applyModifier(zombie, Attributes.FOLLOW_RANGE, "follow_boost",
                stage * 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void applyStage1(Zombie zombie, ZombieAccessor accessor) {
        accessor.getGoalSelector().addGoal(2, new DigBlockGoal(zombie));
        accessor.getGoalSelector().addGoal(3, new ThrowSnowballGoal(zombie));
    }

    private static void applyStage2(Zombie zombie, ZombieAccessor accessor) {
        accessor.getGoalSelector().addGoal(2, new CrossbowAttackGoal(zombie));
        accessor.getGoalSelector().addGoal(1, new ShieldBlockGoal(zombie));
    }

    private static void applyStage3(Zombie zombie, ZombieAccessor accessor) {
        accessor.getGoalSelector().addGoal(2, new BuildBlockGoal(zombie));
    }

    private static void applyStage4(Zombie zombie) {
        EquipGearGoal equipGoal = new EquipGearGoal(zombie);
        equipGoal.start();
    }

    private static void applyStage5(Zombie zombie) {
        applyModifier(zombie, Attributes.FOLLOW_RANGE, "mega_follow",
                256.0 / 40.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void applyStage6(Zombie zombie) {
        applyModifier(zombie, Attributes.MOVEMENT_SPEED, "turbo_speed",
                0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void applyStage7(Zombie zombie, ZombieAccessor accessor) {
        accessor.getGoalSelector().addGoal(1, new ElytraFlightGoal(zombie));
    }

    private static void applyStage8(Zombie zombie, ZombieAccessor accessor) {
        accessor.getGoalSelector().addGoal(2, new AcidSprayGoal(zombie));
    }

    private static void applyStage9(Zombie zombie) {
    }

    private static void applyModifier(Zombie zombie, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                      String name, double value, AttributeModifier.Operation operation) {
        var attrInstance = zombie.getAttribute(attribute);
        if (attrInstance == null) return;

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Zombie_plus.MOD_ID, name);
        attrInstance.removeModifier(id);
        if (value > 0) {
            attrInstance.addTransientModifier(new AttributeModifier(id, value, operation));
        }
    }
}
