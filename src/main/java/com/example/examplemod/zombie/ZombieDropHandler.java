package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ZombieDropHandler {
    
    private static final Random random = new Random();
    
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onZombieDeath(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (event.getEntity().level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) event.getEntity().level();
        int enhanceLevel = ZombieEnhancer.getEnhanceLevel(zombie);
        
        if (enhanceLevel <= 0) return;
        
        double dropChance = Math.min(0.1 + enhanceLevel * 0.05, 0.8);
        
        if (random.nextDouble() < dropChance) {
            dropBlazeRod(level, zombie);
        }
        
        if (random.nextDouble() < dropChance) {
            dropGunpowder(level, zombie);
        }
        
        if (random.nextDouble() < dropChance) {
            dropBone(level, zombie);
        }
        
        if (enhanceLevel >= 5 && random.nextDouble() < dropChance * 0.3) {
            dropWitherSkeletonSkull(level, zombie);
        }
        
        if (enhanceLevel >= 6 && random.nextDouble() < dropChance * 0.5) {
            dropEnderPearl(level, zombie);
        }
    }
    
    private static void dropBlazeRod(ServerLevel level, Zombie zombie) {
        int count = 1 + random.nextInt(2);
        ItemStack drop = new ItemStack(Items.BLAZE_ROD, count);
        spawnItem(level, zombie, drop);
    }
    
    private static void dropGunpowder(ServerLevel level, Zombie zombie) {
        int count = 1 + random.nextInt(3);
        ItemStack drop = new ItemStack(Items.GUNPOWDER, count);
        spawnItem(level, zombie, drop);
    }
    
    private static void dropBone(ServerLevel level, Zombie zombie) {
        int count = 1 + random.nextInt(3);
        ItemStack drop = new ItemStack(Items.BONE, count);
        spawnItem(level, zombie, drop);
    }
    
    private static void dropWitherSkeletonSkull(ServerLevel level, Zombie zombie) {
        ItemStack drop = new ItemStack(Items.WITHER_SKELETON_SKULL, 1);
        spawnItem(level, zombie, drop);
    }
    
    private static void dropEnderPearl(ServerLevel level, Zombie zombie) {
        int count = 1 + random.nextInt(2);
        ItemStack drop = new ItemStack(Items.ENDER_PEARL, count);
        spawnItem(level, zombie, drop);
    }
    
    private static void spawnItem(ServerLevel level, Zombie zombie, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
            level,
            zombie.getX(),
            zombie.getY() + 0.5,
            zombie.getZ(),
            stack
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
