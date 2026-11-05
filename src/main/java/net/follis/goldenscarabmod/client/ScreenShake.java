package net.follis.goldenscarabmod.entity.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/**
 * A simple screen shake. Hits add "trauma" (0 to 1) that fades out over time; the shake is trauma squared, so small
 * hits are gentle and big ones violent. It is measured in real time, so it needs no tick registration.
 * The camera offset is applied by CameraShakeMixin, which calls apply() every frame.
 */
public final class ScreenShake {

    /** Set to false to turn the shake off. */
    public static boolean ENABLED = true;

    private static final float DECAY_PER_SECOND = 1.1f;   // a full-strength shake lasts about a second
    private static final float MAX_ROLL = 5.0f;           // degrees at full shake
    private static final float MAX_PITCH = 2.5f;
    private static final float MAX_YAW = 2.5f;

    private static float trauma;
    private static long lastNanos = System.nanoTime();

    private ScreenShake() {
    }

    /** Shake the screen if the local player is within radius blocks of (x, y, z); stronger the closer they are. */
    public static void trigger(double x, double y, double z, float strength, double radius) {
        if (!ENABLED) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        double dx = player.getX() - x, dy = player.getY() - y, dz = player.getZ() - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist >= radius) return;

        float falloff = 1f - (float) (dist / radius);
        decay();
        trauma = Math.min(1f, trauma + strength * falloff);
    }

    /** Called every frame by the camera mixin: rotates the view by a small, fast, fading wobble. */
    public static void apply(MatrixStack matrices) {
        decay();
        if (trauma <= 0.001f) return;

        float shake = trauma * trauma;
        double time = System.currentTimeMillis() / 1000.0;
        // a few sines at unrelated frequencies read as noise without needing a noise function
        float roll = (float) (Math.sin(time * 37.0) + Math.sin(time * 61.0 + 1.7)) * 0.5f * MAX_ROLL * shake;
        float pitch = (float) (Math.sin(time * 43.0 + 0.6) + Math.sin(time * 71.0 + 2.9)) * 0.5f * MAX_PITCH * shake;
        float yaw = (float) (Math.sin(time * 47.0 + 1.1) + Math.sin(time * 67.0 + 4.2)) * 0.5f * MAX_YAW * shake;

        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
    }

    private static void decay() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1.0e9f;
        lastNanos = now;
        trauma = Math.max(0f, trauma - DECAY_PER_SECOND * Math.min(dt, 0.5f));
    }
}