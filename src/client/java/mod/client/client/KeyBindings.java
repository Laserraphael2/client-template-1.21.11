package mod.client.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

public class KeyBindings {
    public static KeyMapping hudEditorKey;
    
    public static void register() {
        hudEditorKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.client.xenon_menu",
                InputConstants.Type.KEYSYM,
                341, // GLFW_KEY_RIGHT_SHIFT
                new KeyMapping.Category(Identifier.fromNamespaceAndPath("minecraft", "key.categories.misc"))
            )
        );
    }
}
