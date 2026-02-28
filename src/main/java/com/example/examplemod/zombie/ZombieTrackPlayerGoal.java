package com.example.examplemod.zombie;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class ZombieTrackPlayerGoal extends Goal {
    
    private final Zombie zombie;
    private int recomputePathCooldown;
    private static final int RECOMPUTE_INTERVAL = 40;
    private static final double TRACK_RANGE = 512.0;
    
    public ZombieTrackPlayerGoal(Zombie zombie) {
        this.zombie = zombie;
    }
    
    @Override
    public boolean canUse() {
        if (zombie.getTarget() != null && zombie.getTarget().isAlive()) {
            return false;
        }
        
        Player nearestPlayer = findNearestPlayer();
        if (nearestPlayer != null) {
            zombie.setTarget(nearestPlayer);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean canContinueToUse() {
        return zombie.getTarget() != null && zombie.getTarget().isAlive();
    }
    
    @Override
    public void start() {
        recomputePathCooldown = 0;
    }
    
    @Override
    public void stop() {
        zombie.getNavigation().stop();
    }
    
    @Override
    public void tick() {
        LivingEntity target = zombie.getTarget();
        if (target == null) {
            Player nearestPlayer = findNearestPlayer();
            if (nearestPlayer != null) {
                zombie.setTarget(nearestPlayer);
            }
            return;
        }
        
        zombie.getLookControl().setLookAt(target);
        
        recomputePathCooldown--;
        if (recomputePathCooldown <= 0) {
            recomputePathCooldown = RECOMPUTE_INTERVAL;
            recomputePath(target);
        }
        
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        
        Path currentPath = zombie.getNavigation().getPath();
        
        if (currentPath == null || currentPath.isDone()) {
            recomputePath(target);
        }
    }
    
    private void recomputePath(LivingEntity target) {
        Vec3 targetPos = target.position();
        
        Path path = zombie.getNavigation().createPath(targetPos.x, targetPos.y, targetPos.z, 0);
        
        if (path != null) {
            zombie.getNavigation().moveTo(path, 1.0);
        } else {
            BlockPos targetBlock = new BlockPos((int)targetPos.x, (int)targetPos.y, (int)targetPos.z);
            BlockPos zombieBlock = zombie.blockPosition();
            
            BlockPos intermediate = findIntermediateTarget(zombieBlock, targetBlock);
            if (intermediate != null) {
                Path intermediatePath = zombie.getNavigation().createPath(intermediate, 0);
                if (intermediatePath != null) {
                    zombie.getNavigation().moveTo(intermediatePath, 1.0);
                }
            }
        }
    }
    
    private BlockPos findIntermediateTarget(BlockPos start, BlockPos end) {
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();
        
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 1) return null;
        
        int stepSize = Math.max(1, steps / 4);
        
        for (int i = stepSize; i <= steps; i += stepSize) {
            double ratio = (double) i / steps;
            int x = start.getX() + (int)(dx * ratio);
            int y = start.getY() + (int)(dy * ratio);
            int z = start.getZ() + (int)(dz * ratio);
            
            BlockPos checkPos = new BlockPos(x, y, z);
            Path path = zombie.getNavigation().createPath(checkPos, 0);
            if (path != null) {
                return checkPos;
            }
        }
        
        return null;
    }
    
    private Player findNearestPlayer() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return null;
        
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Player player : serverLevel.players()) {
            if (player.isCreative() || player.isSpectator()) continue;
            
            double distance = zombie.distanceToSqr(player);
            if (nearestPlayer == null || distance < nearestDistance) {
                nearestPlayer = player;
                nearestDistance = distance;
            }
        }
        
        return nearestPlayer;
    }
}
