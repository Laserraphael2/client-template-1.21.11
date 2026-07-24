package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import mod.client.client.util.SessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SessionStats extends HudModule {
    private boolean wasDead;

    public SessionStats() {
        this.x = 5;
        this.y = 81;
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null) {
            wasDead = false;
            return;
        }

        boolean isDead = !client.player.isAlive();
        if (isDead && !wasDead) {
            SessionTracker.onDeath();
        }
        wasDead = isDead;

        var combo = mod.client.client.ClientClient.getHudManager().getModule(ComboCounter.class);
        combo.ifPresent(c -> SessionTracker.updateCombo(c.getCombo()));
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        long sec = SessionTracker.getSessionSeconds();
        long mins = sec / 60;
        long rem = sec % 60;

        String text = "S " + mins + ":" + String.format("%02d", rem)
                + " H " + SessionTracker.getTrackedHits()
                + " D " + SessionTracker.getDeaths()
                + " MC " + SessionTracker.getMaxCombo();
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }

    @Override
    public String getName() {
        return "Session Stats";
    }

    @Override
    public int getWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 170;
        }
        return client.font.width("S 999:59 H 99999 D 999 MC 99");
    }

    @Override
    public int getHeight() {
        return 10;
    }
}
