package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ZombieShieldHandler {
    
    private static final Map<UUID, Long> shieldActiveTime = new HashMap<>();
    private static final int SHIELD_COOLDOWN = 50;
    private static final Map<UUID, Long> shieldCooldownEnd = new HashMap<>();
    
    public static void activateShield(Zombie zombie) {
        UUID id = zombie.getUUID();
        shieldActiveTime.put(id, System.currentTimeMillis());
    }
    
    public static void deactivateShield(Zombie zombie) {
        UUID id = zombie.getUUID();
        shieldActiveTime.remove(id);
        shieldCooldownEnd.put(id, System.currentTimeMillis() + SHIELD_COOLDOWN * 50);
    }
    
    public static boolean isShieldActive(Zombie zombie) {
        Long activeTime = shieldActiveTime.get(zombie.getUUID());
        if (activeTime == null) return false;
        
        return System.currentTimeMillis() - activeTime < 3000;
    }
    
    public static boolean isShieldOnCooldown(Zombie zombie) {
        Long cooldownEnd = shieldCooldownEnd.get(zombie.getUUID());
        if (cooldownEnd == null) return false;
        
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    public static boolean canRaiseShield(Zombie zombie) {
        return !isShieldActive(zombie) && !isShieldOnCooldown(zombie);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onZombieAttacked(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        
        int level = ZombieEnhancer.getEnhanceLevel(zombie);
        if (level < 4) return;
        
        ItemStack offhand = zombie.getItemBySlot(EquipmentSlot.OFFHAND);
        if (offhand.getItem() != Items.SHIELD) return;
        
        DamageSource source = event.getSource();
        
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.EXPLOSION) || 
            source.is(DamageTypes.FALL)) {
            return;
        }
        
        if (isShieldActive(zombie) && canBlockDamage(zombie, source)) {
            event.setCanceled(true);
            
            zombie.level().playSound(null, zombie, SoundEvents.SHIELD_BLOCK, 
                SoundSource.HOSTILE, 1.0F, 1.0F);
            
            if (source.getEntity() instanceof Player player) {
                ItemStack weapon = player.getMainHandItem();
                if (isAxe(weapon)) {
                    zombie.level().playSound(null, zombie, SoundEvents.SHIELD_BREAK, 
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                    
                    deactivateShield(zombie);
                    shieldCooldownEnd.put(zombie.getUUID(), 
                        System.currentTimeMillis() + SHIELD_COOLDOWN * 100);
                }
            }
        }
    }
    
    private static boolean canBlockDamage(Zombie zombie, DamageSource source) {
        if (source.getEntity() == null) return true;
        
        LivingEntity attacker = null;
        if (source.getEntity() instanceof LivingEntity le) {
            attacker = le;
        }
        
        if (attacker == null) return true;
        
        Vec3 attackerPos = attacker.position();
        Vec3 zombiePos = zombie.position();
        
        Vec3 toZombie = zombiePos.subtract(attackerPos).normalize();
        Vec3 zombieLook = zombie.getLookAngle();
        
        Vec3 reversedToZombie = new Vec3(-toZombie.x, -toZombie.y, -toZombie.z);
        double dot = zombieLook.dot(reversedToZombie);
        
        return dot > 0.0;
    }
    
    private static boolean isAxe(ItemStack stack) {
        return stack.getItem() == Items.WOODEN_AXE ||
               stack.getItem() == Items.STONE_AXE ||
               stack.getItem() == Items.IRON_AXE ||
               stack.getItem() == Items.GOLDEN_AXE ||
               stack.getItem() == Items.DIAMOND_AXE ||
               stack.getItem() == Items.NETHERITE_AXE;
    }
}
