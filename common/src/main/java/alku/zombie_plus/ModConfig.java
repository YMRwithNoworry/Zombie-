package alku.zombie_plus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;

    private int totalDays = 100;

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int days) {
        if (days < 1) {
            Zombie_plus.LOGGER.warn("Attempted to set totalDays to {}, using minimum value 1", days);
            days = 1;
        }
        
        if (this.totalDays != days) {
            int oldValue = this.totalDays;
            this.totalDays = days;
            Zombie_plus.LOGGER.info("Config updated: totalDays changed from {} to {}", oldValue, days);
            save();
        }
    }

    public void setTotalDays(int days, net.minecraft.server.MinecraftServer server) {
        setTotalDays(days);
        if (server != null) {
            ModNetworking.syncToAll(server);
        }
    }

    void setTotalDaysSilent(int days) {
        this.totalDays = days < 1 ? 1 : days;
    }

    public static ModConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static ModConfig load() {
        Path path = Platform.getConfigFolder().resolve("zombie_plus.json");
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                config.setTotalDaysSilent(config.totalDays);
                return config;
            } catch (IOException e) {
                Zombie_plus.LOGGER.error("Failed to load config", e);
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        Path path = Platform.getConfigFolder().resolve("zombie_plus.json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            Zombie_plus.LOGGER.error("Failed to save config", e);
        }
    }

    public static void reload() {
        instance = load();
    }
}
