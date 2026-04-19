package alku.zombie_plus.fabric;

import alku.zombie_plus.Zombie_plus;
import net.fabricmc.api.ModInitializer;

public final class Zombie_plusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Zombie_plus.init();
    }
}
