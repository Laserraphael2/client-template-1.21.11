package mod.client.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.client.shield.ShieldPatternData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.core.component.DataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldSpecialRenderer.class)
public class ShieldSpecialRendererMixin {
    @Inject(method = "submit", at = @At("TAIL"))
    private void client$submitPattern(DataComponentMap components, PoseStack poseStack,
                                      SubmitNodeCollector collector, int light, int overlay,
                                      boolean foil, int outlineColor, CallbackInfo ci) {
        ShieldPatternData.Pattern pattern = ShieldPatternData.read(components);
        if (pattern.isEmpty()) {
            return;
        }

        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, vertices) -> {
            int red = (pattern.color() >> 16) & 0xFF;
            int green = (pattern.color() >> 8) & 0xFF;
            int blue = pattern.color() & 0xFF;
            float pixelWidth = 11.0F / ShieldPatternData.GRID_SIZE;
            float pixelHeight = 21.0F / ShieldPatternData.GRID_SIZE;
            float startX = -5.5F;
            float startY = -10.5F;
            float z = -2.02F;

            for (int row = 0; row < ShieldPatternData.GRID_SIZE; row++) {
                for (int column = 0; column < ShieldPatternData.GRID_SIZE; column++) {
                    if (pattern.pixels().charAt(row * ShieldPatternData.GRID_SIZE + column) != '1') {
                        continue;
                    }
                    float x0 = (startX + column * pixelWidth) / 16.0F;
                    float y0 = (startY + row * pixelHeight) / 16.0F;
                    float x1 = (startX + (column + 1) * pixelWidth) / 16.0F;
                    float y1 = (startY + (row + 1) * pixelHeight) / 16.0F;
                    float depth = z / 16.0F;
                    vertices.addVertex(pose, x0, y0, depth).setColor(red, green, blue, 255);
                    vertices.addVertex(pose, x0, y1, depth).setColor(red, green, blue, 255);
                    vertices.addVertex(pose, x1, y1, depth).setColor(red, green, blue, 255);
                    vertices.addVertex(pose, x1, y0, depth).setColor(red, green, blue, 255);
                }
            }
        });
    }
}