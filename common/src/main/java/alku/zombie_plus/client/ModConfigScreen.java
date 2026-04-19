package alku.zombie_plus.client;

import alku.zombie_plus.ModConfig;
import alku.zombie_plus.ModNetworking;
import alku.zombie_plus.Zombie_plus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {
    private EditBox totalDaysInput;

    public ModConfigScreen() {
        super(Component.translatable("screen.zombie_plus.config.title"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.totalDaysInput = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20,
                Component.translatable("screen.zombie_plus.config.total_days"));
        this.totalDaysInput.setMaxLength(6);
        this.totalDaysInput.setValue(String.valueOf(ModConfig.get().getTotalDays()));
        this.totalDaysInput.setFilter(s -> s.matches("\\d*"));
        this.totalDaysInput.setResponder(s -> {
        });
        this.addRenderableWidget(this.totalDaysInput);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.zombie_plus.config.save"),
                        button -> this.saveConfig()
                ).bounds(centerX - 102, centerY + 10, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.zombie_plus.config.cancel"),
                        button -> this.onClose()
                ).bounds(centerX + 2, centerY + 10, 100, 20).build());
    }

    private void saveConfig() {
        try {
            int days = Integer.parseInt(this.totalDaysInput.getValue());
            if (days >= 1) {
                ModNetworking.sendConfigUpdate(days);
                this.onClose();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 55, 0xFFFFFF);

        Component label = Component.translatable("screen.zombie_plus.config.total_days_label");
        guiGraphics.drawString(this.font, label, this.width / 2 - this.font.width(label) / 2, this.height / 2 - 45, 0xAAAAAA);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.totalDaysInput.isFocused()) {
            return this.totalDaysInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.totalDaysInput.isFocused()) {
            return this.totalDaysInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
