package mod.client.client.mixin;

import mod.client.client.render.MaceEnchantmentVisuals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Inject(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("TAIL")
    )
    private void xenon$renderMaceEnchantmentOverlay(Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
        MaceEnchantmentVisuals.renderItemOverlay((GuiGraphics) (Object) this, stack, x, y);
    }
}