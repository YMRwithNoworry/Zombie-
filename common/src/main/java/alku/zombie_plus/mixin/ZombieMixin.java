package alku.zombie_plus.mixin;

import alku.zombie_plus.DayTracker;
import alku.zombie_plus.ModConfig;
import alku.zombie_plus.ZombieEnhancer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public class ZombieMixin {
    
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void onFinalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType spawnType,
            net.minecraft.world.entity.SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<net.minecraft.world.entity.SpawnGroupData> cir) {
        
        Zombie zombie = (Zombie) (Object) this;
        if (level instanceof ServerLevel serverLevel) {
            DayTracker tracker = DayTracker.get(serverLevel);
            int currentDay = tracker.getCurrentDay();
            int totalDays = ModConfig.get().getTotalDays();
            int stage = ZombieEnhancer.getEvolutionStage(currentDay, totalDays);
            ZombieEnhancer.applyEnhancements(zombie, stage);
        }
    }
}
