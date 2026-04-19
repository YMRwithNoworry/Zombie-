package alku.zombie_plus.fabric.client;

import alku.zombie_plus.Zombie_plus;
import alku.zombie_plus.client.ZombiePlusKeys;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.ClientModInitializer;

public final class Zombie_plusFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingRegistry.register(ZombiePlusKeys.CONFIG_KEY);
        Zombie_plus.initClient();
    }
}
