package alku.zombie_plus;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = 
        DeferredRegister.create(Zombie_plus.MOD_ID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> CORROSION = 
        EFFECTS.register("corrosion", CorrosionEffect::new);

    public static void init() {
        EFFECTS.register();
    }
}
