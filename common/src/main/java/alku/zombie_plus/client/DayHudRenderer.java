package alku.zombie_plus.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DayHudRenderer {

    public static void renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return;

        int currentDay = ClientData.getCurrentDay();
        int totalDays = ClientData.getTotalDays();
        int stage = ClientData.getEvolutionStage();

        Component dayText = Component.translatable("hud.zombie_plus.day", currentDay, totalDays);
        Component stageText = Component.translatable("hud.zombie_plus.stage", stage);

        int x = 10;
        int y = mc.getWindow().getGuiScaledHeight() - 30;

        guiGraphics.drawString(mc.font, dayText, x, y, 0xFFFFFF, true);
        y += mc.font.lineHeight + 2;

        int stageColor = getStageColor(stage);
        guiGraphics.drawString(mc.font, stageText, x, y, stageColor, true);
    }

    private static int getStageColor(int stage) {
        if (stage <= 0) return 0xFFFFFF;
        if (stage <= 2) return 0xFFFF00;
        if (stage <= 4) return 0xFFAA00;
        if (stage <= 6) return 0xFF6600;
        if (stage <= 8) return 0xFF3300;
        return 0xFF0000;
    }
}
