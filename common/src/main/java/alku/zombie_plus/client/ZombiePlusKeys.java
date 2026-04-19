package alku.zombie_plus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ZombiePlusKeys {
    public static final String CATEGORY = "key.categories.zombie_plus";
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.zombie_plus.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );
}
