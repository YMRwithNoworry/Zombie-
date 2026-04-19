package alku.zombie_plus.mixin;

import alku.zombie_plus.DayTracker;
import alku.zombie_plus.ModConfig;
import alku.zombie_plus.ZombieEnhancer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    
    @Inject(
        method = "spawnCategoryForPosition",
        at = @At("RETURN")
    )
    private static void onSpawnCategoryForPosition(
            MobCategory category,
            ServerLevel level,
            BlockPos pos,
            CallbackInfo ci) {
        
        if (category == MobCategory.MONSTER) {
            DayTracker tracker = DayTracker.get(level);
            int currentDay = tracker.getCurrentDay();
            int totalDays = ModConfig.get().getTotalDays();
            int stage = ZombieEnhancer.getEvolutionStage(currentDay, totalDays);
            
            if (stage >= 9 && level.random.nextFloat() < 0.5f) {
                NaturalSpawner.spawnCategoryForPosition(category, level, pos);
            }
        }
    }
}
