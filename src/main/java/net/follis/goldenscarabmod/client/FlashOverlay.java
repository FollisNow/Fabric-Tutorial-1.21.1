package net.follis.goldenscarabmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.follis.goldenscarabmod.network.FlashPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** The white screen of the flash: fully white for a moment, then fading out. */
public final class FlashOverlay {
    private static boolean active;
    private static int age, hold, fade;

    private FlashOverlay() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FlashPayload.ID, (payload, context) ->
                context.client().execute(() -> start(payload.holdTicks(), payload.fadeTicks())));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active) return;
            age++;
            if (age > hold + fade) active = false;
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context, tickCounter.getTickDelta(false)));
    }

    private static void start(int holdTicks, int fadeTicks) {
        hold = holdTicks;
        fade = Math.max(1, fadeTicks);
        age = 0;
        active = true;
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!active) return;
        float t = age + tickDelta;
        float alpha = t <= hold ? 1.0F : 1.0F - (t - hold) / fade;
        if (alpha <= 0.0F) return;

        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        MinecraftClient client = MinecraftClient.getInstance();
        context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(),
                (a << 24) | 0xFFFFFF);
    }
}