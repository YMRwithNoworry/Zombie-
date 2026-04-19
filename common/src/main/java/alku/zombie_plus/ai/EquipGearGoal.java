package alku.zombie_plus.ai;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.EnumSet;
import java.util.Random;

public class EquipGearGoal extends Goal {
    private final PathfinderMob mob;
    private boolean equipped;
    private final Random random = new Random();

    public EquipGearGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return !equipped && mob.getTarget() != null;
    }

    @Override
    public void start() {
        equipRandomGear();
        equipped = true;
    }

    private void equipRandomGear() {
        if (random.nextFloat() < 0.7f) {
            ItemStack helmet = createRandomArmor(EquipmentSlot.HEAD);
            mob.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
        
        if (random.nextFloat() < 0.7f) {
            ItemStack chestplate = createRandomArmor(EquipmentSlot.CHEST);
            mob.setItemSlot(EquipmentSlot.CHEST, chestplate);
        }
        
        if (random.nextFloat() < 0.6f) {
            ItemStack leggings = createRandomArmor(EquipmentSlot.LEGS);
            mob.setItemSlot(EquipmentSlot.LEGS, leggings);
        }
        
        if (random.nextFloat() < 0.5f) {
            ItemStack boots = createRandomArmor(EquipmentSlot.FEET);
            mob.setItemSlot(EquipmentSlot.FEET, boots);
        }
        
        if (random.nextFloat() < 0.6f) {
            ItemStack weapon = createRandomWeapon();
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
    }

    private ItemStack createRandomArmor(EquipmentSlot slot) {
        ItemStack armor;
        int tier = random.nextInt(5);
        
        switch (slot) {
            case HEAD -> {
                armor = switch (tier) {
                    case 0 -> new ItemStack(Items.LEATHER_HELMET);
                    case 1 -> new ItemStack(Items.CHAINMAIL_HELMET);
                    case 2 -> new ItemStack(Items.IRON_HELMET);
                    case 3 -> new ItemStack(Items.DIAMOND_HELMET);
                    default -> new ItemStack(Items.NETHERITE_HELMET);
                };
            }
            case CHEST -> {
                armor = switch (tier) {
                    case 0 -> new ItemStack(Items.LEATHER_CHESTPLATE);
                    case 1 -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                    case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
                    case 3 -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                    default -> new ItemStack(Items.NETHERITE_CHESTPLATE);
                };
            }
            case LEGS -> {
                armor = switch (tier) {
                    case 0 -> new ItemStack(Items.LEATHER_LEGGINGS);
                    case 1 -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                    case 2 -> new ItemStack(Items.IRON_LEGGINGS);
                    case 3 -> new ItemStack(Items.DIAMOND_LEGGINGS);
                    default -> new ItemStack(Items.NETHERITE_LEGGINGS);
                };
            }
            case FEET -> {
                armor = switch (tier) {
                    case 0 -> new ItemStack(Items.LEATHER_BOOTS);
                    case 1 -> new ItemStack(Items.CHAINMAIL_BOOTS);
                    case 2 -> new ItemStack(Items.IRON_BOOTS);
                    case 3 -> new ItemStack(Items.DIAMOND_BOOTS);
                    default -> new ItemStack(Items.NETHERITE_BOOTS);
                };
            }
            default -> armor = ItemStack.EMPTY;
        }
        
        if (random.nextFloat() < 0.3f && armor.isEnchantable()) {
            int enchantLevel = random.nextInt(3) + 1;
            armor.enchant(mob.level().registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION), 
                enchantLevel);
        }
        
        return armor;
    }

    private ItemStack createRandomWeapon() {
        ItemStack weapon;
        int type = random.nextInt(4);
        int tier = random.nextInt(5);
        
        weapon = switch (type) {
            case 0 -> switch (tier) {
                case 0 -> new ItemStack(Items.WOODEN_SWORD);
                case 1 -> new ItemStack(Items.STONE_SWORD);
                case 2 -> new ItemStack(Items.IRON_SWORD);
                case 3 -> new ItemStack(Items.DIAMOND_SWORD);
                default -> new ItemStack(Items.NETHERITE_SWORD);
            };
            case 1 -> switch (tier) {
                case 0 -> new ItemStack(Items.WOODEN_AXE);
                case 1 -> new ItemStack(Items.STONE_AXE);
                case 2 -> new ItemStack(Items.IRON_AXE);
                case 3 -> new ItemStack(Items.DIAMOND_AXE);
                default -> new ItemStack(Items.NETHERITE_AXE);
            };
            case 2 -> new ItemStack(Items.BOW);
            default -> new ItemStack(Items.CROSSBOW);
        };
        
        if (random.nextFloat() < 0.3f && weapon.isEnchantable()) {
            int enchantLevel = random.nextInt(3) + 1;
            if (weapon.getItem() == Items.BOW || weapon.getItem() == Items.CROSSBOW) {
                weapon.enchant(mob.level().registryAccess().registryOrThrow(
                    net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER),
                    enchantLevel);
            } else {
                weapon.enchant(mob.level().registryAccess().registryOrThrow(
                    net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),
                    enchantLevel);
            }
        }
        
        return weapon;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    public void reset() {
        equipped = false;
    }
}
