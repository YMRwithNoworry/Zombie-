package alku.zombie_plus.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class BuildBlockGoal extends Goal {
    private final PathfinderMob mob;
    private BlockPos targetBuildPos;
    private int buildCooldown;
    private static final int COOLDOWN_TICKS = 30;

    public BuildBlockGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null || buildCooldown > 0) return false;
        
        Vec3 targetVec = mob.getTarget().position();
        Vec3 mobVec = mob.position();
        
        if (targetVec.y > mobVec.y + 1.5) {
            targetBuildPos = mob.blockPosition().above();
            if (canPlaceBlock(targetBuildPos)) {
                return true;
            }
        }
        
        if (mob.distanceTo(mob.getTarget()) > 2) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos checkPos = mob.blockPosition().relative(dir);
                if (canPlaceBlock(checkPos)) {
                    targetBuildPos = checkPos;
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean canPlaceBlock(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    @Override
    public void start() {
        buildCooldown = 0;
    }

    @Override
    public void tick() {
        if (targetBuildPos == null) return;
        
        mob.getLookControl().setLookAt(
            targetBuildPos.getX() + 0.5, 
            targetBuildPos.getY() + 0.5, 
            targetBuildPos.getZ() + 0.5
        );
        
        if (buildCooldown <= 0 && canPlaceBlock(targetBuildPos)) {
            placeBlock();
            buildCooldown = COOLDOWN_TICKS;
            targetBuildPos = null;
        } else {
            buildCooldown--;
        }
    }

    private void placeBlock() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        
        BlockState stateToPlace = Blocks.COBBLESTONE.defaultBlockState();
        serverLevel.setBlock(targetBuildPos, stateToPlace, 3);
        serverLevel.levelEvent(2001, targetBuildPos, Block.getId(stateToPlace));
        
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean canContinueToUse() {
        return targetBuildPos != null && mob.getTarget() != null && mob.isAlive();
    }

    @Override
    public void stop() {
        targetBuildPos = null;
        buildCooldown = 0;
    }
}
