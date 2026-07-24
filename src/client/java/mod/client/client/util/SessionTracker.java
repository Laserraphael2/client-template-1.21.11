package mod.client.client.util;

public final class SessionTracker {
    private static final long START_MS = System.currentTimeMillis();

    private static int leftClicks;
    private static int rightClicks;
    private static int trackedHits;
    private static int deaths;
    private static int maxCombo;
    private static float lastReach;
    private static long lastReachMs;

    private SessionTracker() {
    }

    public static void onLeftClick() {
        leftClicks++;
    }

    public static void onRightClick() {
        rightClicks++;
    }

    public static void onHit(float reach) {
        trackedHits++;
        if (reach > 0.0f) {
            lastReach = reach;
            lastReachMs = System.currentTimeMillis();
        }
    }

    public static void onDeath() {
        deaths++;
    }

    public static void updateCombo(int combo) {
        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    public static int getLeftClicks() {
        return leftClicks;
    }

    public static int getRightClicks() {
        return rightClicks;
    }

    public static int getTrackedHits() {
        return trackedHits;
    }

    public static int getDeaths() {
        return deaths;
    }

    public static int getMaxCombo() {
        return maxCombo;
    }

    public static long getSessionSeconds() {
        return (System.currentTimeMillis() - START_MS) / 1000L;
    }

    public static float getLastReach() {
        return lastReach;
    }

    public static long getLastReachAgeMs() {
        return System.currentTimeMillis() - lastReachMs;
    }
}
