package alku.zombie_plus.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CrossbowAttackGoal extends Goal {
    private final PathfinderMob mob;
    private int cooldown;
    private int chargeTime;
    private boolean isCharging;
    private static final int COOLDOWN_TICKS = 60;
    private static final int CHARGE_TICKS = 25;
    private static final double RANGE = 15.0;

    public CrossbowAttackGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || cooldown > 0) return false;
        
        double distance = mob.distanceTo(mob.getTarget());
        return distance >= 5 && distance <= RANGE;
    }

    @Override
    public void start() {
        cooldown = 0;
        chargeTime = 0;
        isCharging = false;
        
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        if (mob.getRandom().nextFloat() < 0.3f) {
            crossbow.enchant(mob.level().registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.MULTISHOT), 1);
        }
        if (mob.getRandom().nextFloat() < 0.3f) {
            crossbow.enchant(mob.level().registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.PIERCING), 1);
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
    }

    @Override
    public void tick() {
        if (mob.getTarget() == null) return;
        
        mob.getLookControl().setLookAt(mob.getTarget(), 30, 30);
        
        double distance = mob.distanceTo(mob.getTarget());
        
        if (!isCharging && cooldown <= 0 && distance <= RANGE) {
            isCharging = true;
            chargeTime = 0;
        }
        
        if (isCharging) {
            chargeTime++;
            mob.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
            
            if (chargeTime >= CHARGE_TICKS) {
                shootCrossbow();
                isCharging = false;
                cooldown = COOLDOWN_TICKS;
                mob.stopUsingItem();
            }
        } else {
            cooldown--;
        }
    }

    private void shootCrossbow() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        
        ItemStack crossbow = mob.getMainHandItem();
        if (crossbow.getItem() == Items.CROSSBOW) {
            ItemStack arrow = new ItemStack(Items.ARROW);
            
            net.minecraft.world.entity.projectile.AbstractArrow arrowEntity = 
                new net.minecraft.world.entity.projectile.Arrow(serverLevel, mob, arrow, null);
            
            Vec3 targetPos = mob.getTarget().position();
            Vec3 mobPos = mob.position().add(0, mob.getEyeHeight(), 0);
            
            double dx = targetPos.x - mobPos.x;
            double dy = targetPos.y + mob.getTarget().getEyeHeight() * 0.5 - mobPos.y;
            double dz = targetPos.z - mobPos.z;
            
            arrowEntity.shoot(dx, dy, dz, 1.6f, 1.0f);
            serverLevel.addFreshEntity(arrowEntity);
            
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getTarget() != null && mob.isAlive();
    }

    @Override
    public void stop() {
        cooldown = 0;
        chargeTime = 0;
        isCharging = false;
        mob.stopUsingItem();
    }
}
