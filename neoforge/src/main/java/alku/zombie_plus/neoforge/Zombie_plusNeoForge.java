package alku.zombie_plus.neoforge;

import alku.zombie_plus.Zombie_plus;
import alku.zombie_plus.client.ZombiePlusKeys;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Zombie_plus.MOD_ID)
public final class Zombie_plusNeoForge {
    public Zombie_plusNeoForge(net.neoforged.fml.ModContainer container) {
        Zombie_plus.init();

        if (FMLEnvironment.dist.isClient()) {
            container.getEventBus().addListener(this::clientSetup);
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            KeyMappingRegistry.register(ZombiePlusKeys.CONFIG_KEY);
            Zombie_plus.initClient();
        });
    }
}
