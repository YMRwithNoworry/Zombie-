package alku.zombie_plus.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ElytraFlightGoal extends Goal {
    private final PathfinderMob mob;
    private boolean isFlying;
    private int flightTime;
    private int cooldown;
    private static final int MAX_FLIGHT_TIME = 200;
    private static final int COOLDOWN_TICKS = 300;
    private BlockPos targetPos;

    public ElytraFlightGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || cooldown > 0) return false;
        
        if (mob.onGround()) {
            double targetY = mob.getTarget().getY();
            double currentY = mob.getY();
            
            if (targetY > currentY + 5 || mob.distanceTo(mob.getTarget()) > 15) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void start() {
        isFlying = true;
        flightTime = 0;
        cooldown = 0;
        
        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        
        mob.setDeltaMovement(mob.getDeltaMovement().add(0, 1.5, 0));
        mob.hasImpulse = true;
    }

    @Override
    public void tick() {
        if (mob.getTarget() == null) {
            stop();
            return;
        }
        
        flightTime++;
        
        Vec3 targetVec = mob.getTarget().position();
        Vec3 mobVec = mob.position();
        
        Vec3 direction = targetVec.subtract(mobVec).normalize();
        
        double speed = 0.5;
        mob.setDeltaMovement(
            direction.x * speed,
            Math.max(direction.y * speed, -0.1),
            direction.z * speed
        );
        
        mob.getLookControl().setLookAt(mob.getTarget(), 30, 30);
        
        if (mob.level() instanceof ServerLevel serverLevel && flightTime % 20 == 0) {
            ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
            serverLevel.levelEvent(2004, mob.blockPosition(), 0);
        }
        
        if (mob.onGround() && flightTime > 10) {
            stop();
        }
        
        if (flightTime >= MAX_FLIGHT_TIME) {
            stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return isFlying && mob.getTarget() != null && mob.isAlive() && flightTime < MAX_FLIGHT_TIME;
    }

    @Override
    public void stop() {
        isFlying = false;
        flightTime = 0;
        cooldown = COOLDOWN_TICKS;
        
        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
    }
}
