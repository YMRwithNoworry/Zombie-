package alku.zombie_plus.ai;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

public class ShieldBlockGoal extends Goal {
    private final PathfinderMob mob;
    private boolean isBlocking;
    private int blockTime;
    private int cooldown;
    private static final int MAX_BLOCK_TIME = 60;
    private static final int COOLDOWN_TICKS = 100;

    public ShieldBlockGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || cooldown > 0) return false;
        
        if (mob.getOffhandItem().isEmpty()) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        
        return mob.distanceTo(mob.getTarget()) <= 5;
    }

    @Override
    public void start() {
        isBlocking = true;
        blockTime = 0;
        cooldown = 0;
        
        mob.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
    }

    @Override
    public void tick() {
        if (mob.getTarget() == null) {
            stop();
            return;
        }
        
        blockTime++;
        
        if (blockTime >= MAX_BLOCK_TIME) {
            stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return isBlocking && mob.getTarget() != null && mob.isAlive() && blockTime < MAX_BLOCK_TIME;
    }

    @Override
    public void stop() {
        isBlocking = false;
        blockTime = 0;
        cooldown = COOLDOWN_TICKS;
        mob.stopUsingItem();
    }

    public boolean isBlocking() {
        return isBlocking;
    }
}
