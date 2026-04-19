package alku.zombie_plus.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DigBlockGoal extends Goal {
    private final PathfinderMob mob;
    private BlockPos targetPos;
    private int breakTime;
    private int lastBreakProgress = -1;
    private static final int MAX_BREAK_TIME = 60;

    public DigBlockGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null) return false;
        
        Vec3 targetVec = mob.getTarget().position();
        Vec3 mobVec = mob.position();
        
        if (targetVec.y > mobVec.y + 1) {
            BlockPos checkPos = mob.blockPosition().above();
            if (shouldBreak(checkPos)) {
                targetPos = checkPos;
                return true;
            }
        }
        
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : directions) {
            BlockPos checkPos = mob.blockPosition().relative(dir);
            if (shouldBreak(checkPos)) {
                targetPos = checkPos;
                return true;
            }
        }
        
        return false;
    }

    private boolean shouldBreak(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        return !state.isAir() && !state.is(net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD) 
            && state.getDestroySpeed(mob.level(), pos) >= 0 
            && state.getDestroySpeed(mob.level(), pos) < 50;
    }

    @Override
    public void start() {
        breakTime = 0;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos == null) return;
        
        mob.getLookControl().setLookAt(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        
        BlockState state = mob.level().getBlockState(targetPos);
        float hardness = state.getDestroySpeed(mob.level(), targetPos);
        if (hardness < 0) {
            stop();
            return;
        }
        
        int adjustedBreakTime = Math.max(5, (int)(MAX_BREAK_TIME * (hardness / 2.0f)));
        breakTime++;
        
        int breakProgress = (int)((breakTime / (float)adjustedBreakTime) * 10);
        if (breakProgress != lastBreakProgress) {
            lastBreakProgress = breakProgress;
            if (mob.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlockProgress(mob.getId(), targetPos, breakProgress);
            }
        }
        
        if (breakTime >= adjustedBreakTime) {
            if (mob.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlock(targetPos, true, mob);
                serverLevel.destroyBlockProgress(mob.getId(), targetPos, -1);
            }
            breakTime = 0;
            targetPos = null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return targetPos != null && mob.getTarget() != null && mob.isAlive();
    }

    @Override
    public void stop() {
        if (mob.level() instanceof ServerLevel serverLevel && targetPos != null) {
            serverLevel.destroyBlockProgress(mob.getId(), targetPos, -1);
        }
        targetPos = null;
        breakTime = 0;
        lastBreakProgress = -1;
    }
}
