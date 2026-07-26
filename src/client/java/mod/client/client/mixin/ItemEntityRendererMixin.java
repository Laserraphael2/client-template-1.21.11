package mod.client.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.client.client.ClientClient;
import mod.client.client.modules.ItemBeam;
import mod.client.client.render.XenonTheme;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Inject(method = "submit", at = @At("TAIL"))
    private void client$submitItemBeam(ItemEntityRenderState state, PoseStack poseStack,
                                       SubmitNodeCollector collector, CameraRenderState camera,
                                       CallbackInfo ci) {
        if (!ItemBeam.isActive() || state.distanceToCameraSq > 32.0D * 32.0D) {
            return;
        }

        int color = XenonTheme.fromId(ClientClient.getInstance().getThemeId()).accent;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, vertices) -> {
            float radius = 0.035F;
            float bottom = 0.12F;
            float top = 2.25F;
            int alpha = 48;
            vertices.addVertex(pose, -radius, bottom, 0.0F).setColor(red, green, blue, alpha);
            vertices.addVertex(pose, -radius, top, 0.0F).setColor(red, green, blue, 0);
            vertices.addVertex(pose, radius, top, 0.0F).setColor(red, green, blue, 0);
            vertices.addVertex(pose, radius, bottom, 0.0F).setColor(red, green, blue, alpha);
            vertices.addVertex(pose, 0.0F, bottom, -radius).setColor(red, green, blue, alpha);
            vertices.addVertex(pose, 0.0F, top, -radius).setColor(red, green, blue, 0);
            vertices.addVertex(pose, 0.0F, top, radius).setColor(red, green, blue, 0);
            vertices.addVertex(pose, 0.0F, bottom, radius).setColor(red, green, blue, alpha);
        });
    }
}