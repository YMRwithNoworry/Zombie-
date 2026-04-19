package alku.zombie_plus.fabric;

import alku.zombie_plus.Zombie_plus;
import net.fabricmc.api.ModInitializer;

public final class Zombie_plusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Zombie_plus.init();
    }
}
