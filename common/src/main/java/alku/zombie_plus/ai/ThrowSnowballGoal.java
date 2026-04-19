package alku.zombie_plus.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ThrowSnowballGoal extends Goal {
    private final PathfinderMob mob;
    private int cooldown;
    private static final int COOLDOWN_TICKS = 40;

    public ThrowSnowballGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || cooldown > 0) return false;
        
        double distance = mob.distanceTo(mob.getTarget());
        return distance >= 5 && distance <= 20;
    }

    @Override
    public void start() {
        cooldown = 0;
    }

    @Override
    public void tick() {
        if (mob.getTarget() == null) return;
        
        mob.getLookControl().setLookAt(mob.getTarget(), 30, 30);
        
        if (cooldown <= 0) {
            throwSnowball();
            cooldown = COOLDOWN_TICKS;
        } else {
            cooldown--;
        }
    }

    private void throwSnowball() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        
        Vec3 targetPos = mob.getTarget().position();
        Vec3 mobPos = mob.position().add(0, mob.getEyeHeight(), 0);
        
        double dx = targetPos.x - mobPos.x;
        double dy = targetPos.y + mob.getTarget().getEyeHeight() * 0.5 - mobPos.y;
        double dz = targetPos.z - mobPos.z;
        
        double distance = Math.sqrt(dx * dx + dz * dz);
        double velocity = 1.5;
        
        Snowball snowball = new Snowball(serverLevel, mob);
        snowball.shoot(dx, dy + distance * 0.2, dz, (float)velocity, 1.0f);
        serverLevel.addFreshEntity(snowball);
        
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getTarget() != null && mob.isAlive() && cooldown > 0;
    }

    @Override
    public void stop() {
        cooldown = 0;
    }
}
