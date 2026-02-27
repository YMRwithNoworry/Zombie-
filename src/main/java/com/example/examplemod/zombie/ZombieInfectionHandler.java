package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ZombieInfectionHandler {
    
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("d5d5d5d5-1111-0000-0000-000000000001");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("d5d5d5d5-1111-0000-0000-000000000002");
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        LivingEntity victim = event.getEntity();
        
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Zombie zombie)) return;
        
        if (victim instanceof Zombie) return;
        
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;
        
        if (victim instanceof Player) return;
        
        spawnInfectedZombie(serverLevel, victim, zombie);
    }
    
    private static void spawnInfectedZombie(ServerLevel level, LivingEntity victim, Zombie killer) {
        BlockPos pos = victim.blockPosition();
        
        EntityType<Zombie> zombieType = EntityType.ZOMBIE;
        Zombie newZombie = zombieType.create(level);
        
        if (newZombie == null) return;
        
        newZombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 
            victim.getYRot(), victim.getXRot());
        
        double killerHealth = killer.getMaxHealth();
        double killerAttack = killer.getAttributeValue(Attributes.ATTACK_DAMAGE);
        
        double baseHealth = newZombie.getAttributeValue(Attributes.MAX_HEALTH);
        double baseAttack = newZombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
        
        if (newZombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            double healthBonus = (killerHealth - baseHealth) / baseHealth;
            if (healthBonus > 0) {
                newZombie.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                    new AttributeModifier(HEALTH_MODIFIER_UUID, "infected_health", 
                        healthBonus, AttributeModifier.Operation.MULTIPLY_TOTAL)
                );
            }
        }
        
        if (newZombie.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double attackBonus = (killerAttack - baseAttack) / baseAttack;
            if (attackBonus > 0) {
                newZombie.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                    new AttributeModifier(ATTACK_MODIFIER_UUID, "infected_attack", 
                        attackBonus, AttributeModifier.Operation.MULTIPLY_TOTAL)
                );
            }
        }
        
        newZombie.setHealth(newZombie.getMaxHealth());
        
        level.addFreshEntity(newZombie);
    }
    
    public static void addTargetGoals(Zombie zombie) {
        zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, Player.class, false));
        zombie.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(zombie, net.minecraft.world.entity.animal.IronGolem.class, false));
        zombie.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(zombie, net.minecraft.world.entity.animal.SnowGolem.class, false));
        zombie.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(zombie, Villager.class, false));
        zombie.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(zombie, AbstractVillager.class, false));
        zombie.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(zombie, Animal.class, false));
        zombie.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(zombie, WaterAnimal.class, false));
        zombie.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(zombie, net.minecraft.world.entity.monster.Monster.class, false, false));
    }
}
