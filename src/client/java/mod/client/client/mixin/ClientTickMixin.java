package mod.client.client.mixin;

import mod.client.client.KeyBindings;
import mod.client.client.screen.XenonMenuScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        
        // Xenon Menu (Right Shift)
        while (KeyBindings.hudEditorKey.consumeClick()) {
            client.setScreen(new XenonMenuScreen());
        }
    }
}
