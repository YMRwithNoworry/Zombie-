package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class DayDisplayRenderer {
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientDayData.tick();
        }
    }
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        
        long day = ClientDayData.getDay();
        String dayText = String.format("Day %d", day + 1);
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth = font.width(dayText);
        
        int x = (screenWidth - textWidth) / 2;
        int y = 10;
        
        RenderSystem.enableBlend();
        
        guiGraphics.drawString(font, dayText, x, y, 0xFFFFFF, true);
        
        RenderSystem.disableBlend();
    }
}
