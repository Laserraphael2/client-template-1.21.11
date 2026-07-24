package mod.client.client.mixin;

import mod.client.client.ClientClient;
import mod.client.client.modules.ComboCounter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), remap = false)
    private void client$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == net.minecraft.client.Minecraft.getInstance().player) {
            if (ClientClient.getHudManager() != null) {
                ClientClient.getHudManager().getModules().forEach(module -> {
                    if (module instanceof ComboCounter combo) {
                        combo.onPlayerHurt();
                    }
                });
            }
        }
    }
}
