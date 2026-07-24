package mod.client.client.mixin;

import mod.client.client.render.CrosshairCustomizer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * InGameHudMixin for Fabric 1.21.11 with Mojang Official Mappings
 * 
 * CRITICAL FIX: Minecraft 1.21.11 changed renderCrosshair to use
 * DeltaTracker instead of float partialTick parameter.
 * 
 * Correct method signature: renderCrosshair(GuiGraphics, DeltaTracker)
 */
@Mixin(Gui.class)
public class InGameHudMixin {
    
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (CrosshairCustomizer.isEnabled()) {
            ci.cancel();
            // Render custom crosshair
            Minecraft mc = Minecraft.getInstance();
            CrosshairCustomizer.render(context, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }
}
