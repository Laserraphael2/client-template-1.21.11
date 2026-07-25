package mod.client.client.mixin;

import mod.client.client.ClientClient;
import mod.client.client.modules.CPSCounter;
import mod.client.client.modules.KeystrokeOverlay;
import mod.client.client.modules.ComboCounter;
import mod.client.client.util.SessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Abilities;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MouseMixin for Fabric 1.21.11 with Mojang Official Mappings
 * 
 * CRITICAL FIX: Minecraft 1.21.11 changed the mouse button handler to use
 * MouseButtonInfo objects instead of primitive int parameters.
 * 
 * Correct method signature: onButton(long, MouseButtonInfo, int)
 * Refmap: client-client-refmap.json (configured in client.client.mixins.json)
 */
@Mixin(MouseHandler.class)
public class MouseMixin {
    private static final float DEFAULT_FLYING_SPEED = 0.05F;
    private static final int MAX_FLYING_SPEED_LEVEL = 10;
    
    /**
     * Inject into the actual mouse button handler for Minecraft 1.21.11
     * Method: onButton(long window, MouseButtonInfo buttonInfo, int action)
     */
    @Inject(
        method = "onButton",
        at = @At("HEAD")
    )
    private void client$onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        // action == 1 means GLFW_PRESS
        if (action == 1 && ClientClient.getHudManager() != null) {
            int button = buttonInfo.button();

            if (button == 0) {
                SessionTracker.onLeftClick();
                Minecraft client = Minecraft.getInstance();
                Entity target = client.crosshairPickEntity;
                if (target != null && client.player != null) {
                    float reach = (float) client.player.distanceTo(target);
                    SessionTracker.onHit(reach);
                }
            } else if (button == 1) {
                SessionTracker.onRightClick();
            }
            
            ClientClient.getHudManager().getModules().forEach(module -> {
                if (module instanceof CPSCounter cps) {
                    if (button == 0) cps.addLeftClick();
                    else if (button == 1) cps.addRightClick();
                }
                if (module instanceof KeystrokeOverlay keys) {
                    if (button == 0) keys.onLeftClick();
                    else if (button == 1) keys.onRightClick();
                }
                if (module instanceof ComboCounter combo) {
                    if (button == 0) combo.onHit(); // Left click counts as hit
                }
            });
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void client$adjustCreativeFlightSpeed(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (vertical == 0.0 || client.screen != null || client.player == null) {
            return;
        }

        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        Abilities abilities = client.player.getAbilities();
        if (!shiftDown || !abilities.mayfly || !abilities.flying) {
            return;
        }

        int currentLevel = Math.round(abilities.getFlyingSpeed() / DEFAULT_FLYING_SPEED);
        int direction = vertical > 0.0 ? 1 : -1;
        int nextLevel = Math.max(1, Math.min(MAX_FLYING_SPEED_LEVEL, currentLevel + direction));
        abilities.setFlyingSpeed(DEFAULT_FLYING_SPEED * nextLevel);
        client.player.onUpdateAbilities();
        client.player.displayClientMessage(Component.literal("Flight speed: " + nextLevel + "x"), true);
        ci.cancel();
    }
}
