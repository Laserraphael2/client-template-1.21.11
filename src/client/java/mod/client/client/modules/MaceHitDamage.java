package mod.client.client.modules;

import mod.client.client.ClientClient;
import mod.client.client.render.RenderUtils;
import mod.client.client.render.XenonTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

public class MaceHitDamage extends HudModule {
    private LivingEntity pendingTarget;
    private float healthBeforeHit;
    private int pendingTicks;
    private float lastDamage;
    private long visibleUntil;

    public MaceHitDamage() {
        this.x = 8;
        this.y = 142;
    }

    public void trackHit(LivingEntity target) {
        pendingTarget = target;
        healthBeforeHit = target.getHealth() + target.getAbsorptionAmount();
        pendingTicks = 20;
    }

    @Override
    public void tick(Minecraft client) {
        if (pendingTarget == null || pendingTicks-- <= 0) {
            pendingTarget = null;
            return;
        }

        float currentHealth = pendingTarget.getHealth() + pendingTarget.getAbsorptionAmount();
        float damage = healthBeforeHit - Math.max(0.0F, currentHealth);
        if (damage > 0.01F) {
            lastDamage = damage;
            visibleUntil = System.currentTimeMillis() + 3000L;
            pendingTarget = null;
        }
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        if (System.currentTimeMillis() > visibleUntil) {
            return;
        }
        String text = String.format("Mace %.1f damage", lastDamage);
        int accent = XenonTheme.fromId(ClientClient.getInstance().getThemeId()).accent;
        RenderUtils.drawRoundedRect(context, x, y, getWidth(), getHeight(), 4, 0xB010141B);
        context.drawString(Minecraft.getInstance().font, text, x + 6, y + 5, accent, true);
    }

    @Override
    public String getName() {
        return "Mace Hit Damage";
    }

    @Override
    public int getWidth() {
        return 112;
    }

    @Override
    public int getHeight() {
        return 20;
    }
}