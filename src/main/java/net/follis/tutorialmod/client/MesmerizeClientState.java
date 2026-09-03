package net.follis.tutorialmod.client;

public class MesmerizeClientState {
    public static int targetEntityId = -1;
    public static boolean active = false;
    public static float degreesPerSecond = 60f;

    // frame-timing bookkeeping
    public static long lastFrameTimeNanos = -1L;
}