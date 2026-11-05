package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.entity.client.ScreenShake;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the screen shake to the camera. Vanilla has no hook for this, so it piggybacks on the method that
 * tilts the view when the player is hurt: that one already receives the camera's matrix stack every frame.
 *
 * To enable it, add "CameraShakeMixin" to the "client" list of your client mixin config
 * (e.g. goldenscarabmod.client.mixins.json) and make sure that file's "package" matches this class's package.
 */
@Mixin(GameRenderer.class)
public class CameraShakeMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"))
    private void goldenscarabmod$shake(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        ScreenShake.apply(matrices);
    }
}