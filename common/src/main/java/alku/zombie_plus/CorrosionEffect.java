package alku.zombie_plus;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class CorrosionEffect extends MobEffect {

    public CorrosionEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A7C2E);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            for (ItemStack armor : entity.getArmorSlots()) {
                if (!armor.isEmpty() && armor.isDamageableItem()) {
                    int damage = 1 + amplifier;
                    armor.hurtAndBreak(damage, entity, EquipmentSlot.HEAD);
                }
            }
            ItemStack mainHand = entity.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
                int damage = 1 + amplifier;
                mainHand.hurtAndBreak(damage, entity, EquipmentSlot.MAINHAND);
            }
            ItemStack offHand = entity.getOffhandItem();
            if (!offHand.isEmpty() && offHand.isDamageableItem()) {
                int damage = 1 + amplifier;
                offHand.hurtAndBreak(damage, entity, EquipmentSlot.OFFHAND);
            }
        }
        return true;
    }
}
