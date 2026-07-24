package mod.client.client.mixin;

import mod.client.client.ClientClient;
import mod.client.client.modules.KeystrokeOverlay;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * KeyboardMixin for Fabric 1.21.11 with Mojang Official Mappings
 * 
 * CRITICAL FIX: Minecraft 1.21.11 changed the keyboard handler to use
 * KeyEvent objects instead of separate int parameters.
 * 
 * Correct method signature: keyPress(long, int, KeyEvent)
 */
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (action == 1 && ClientClient.getHudManager() != null) {
            int key = keyEvent.key();
            ClientClient.getHudManager().getModules().forEach(module -> {
                if (module instanceof KeystrokeOverlay keys) {
                    keys.onKeyPress(key);
                }
            });
        }
    }
}
