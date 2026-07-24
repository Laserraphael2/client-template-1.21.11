package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.LinkedList;
import java.util.Queue;

public class CPSCounter extends HudModule {
    private final Queue<Long> leftClicks = new LinkedList<>();
    private final Queue<Long> rightClicks = new LinkedList<>();
    private final int width;
    private int leftCPS = 0;
    private int rightCPS = 0;
    
    public CPSCounter() {
        this.x = 5;
        this.y = 5;
        this.width = resolveWidth();
    }
    
    public void addLeftClick() {
        leftClicks.add(System.currentTimeMillis());
    }
    
    public void addRightClick() {
        rightClicks.add(System.currentTimeMillis());
    }
    
    @Override
    public void tick(Minecraft client) {
        long currentTime = System.currentTimeMillis();
        leftClicks.removeIf(time -> currentTime - time > 1000);
        rightClicks.removeIf(time -> currentTime - time > 1000);
        leftCPS = leftClicks.size();
        rightCPS = rightClicks.size();
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        String text = "L: " + leftCPS + " | R: " + rightCPS + " CPS";
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }
    
    @Override
    public String getName() {
        return "CPS Counter";
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return 10;
    }

    private static int resolveWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 96;
        }
        return client.font.width("L: 99 | R: 99 CPS");
    }
}
