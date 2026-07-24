package mod.client.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PotionStatusHud extends HudModule {
    
    public PotionStatusHud() {
        this.x = 10;
        this.y = 5;
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        Collection<MobEffectInstance> effects = client.player.getActiveEffects();
        if (effects.isEmpty()) return;

        List<MobEffectInstance> effectList = new ArrayList<>(effects);
        int offsetY = 0;

        for (MobEffectInstance effect : effectList) {
            String name = effect.getEffect().value().getDisplayName().getString();
            int duration = effect.getDuration();
            int amplifier = effect.getAmplifier();

            int seconds = duration / 20;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            String timeStr = String.format("%d:%02d", minutes, seconds);

            String displayText = name;
            if (amplifier > 0) {
                displayText += " " + toRoman(amplifier + 1);
            }
            displayText += "  " + timeStr;

            int iconColor = effect.getEffect().value().getColor();
            int color = duration < 200 ? 0xFFFF5555 : (duration < 600 ? 0xFFFFAA00 : (0xFF000000 | iconColor));
            context.drawString(client.font, displayText, x, y + offsetY, color, true);
            offsetY += 11;
        }
    }

    private String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(value);
        };
    }
    
    @Override
    public String getName() {
        return "Potion Status";
    }
    
    @Override
    public int getWidth() {
        return 150;
    }
    
    @Override
    public int getHeight() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 20;
        int effectCount = client.player.getActiveEffects().size();
        return Math.max(20, effectCount * 19);
    }
}
