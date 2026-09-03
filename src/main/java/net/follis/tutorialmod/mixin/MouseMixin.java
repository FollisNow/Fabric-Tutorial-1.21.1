package net.follis.tutorialmod.mixin;

import net.follis.tutorialmod.client.MesmerizeClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "updateMouse", at = @At("TAIL"))
    private void tutorialmod$applyMesmerize(CallbackInfo ci) {
        long now = Util.getMeasuringTimeNano();

        if (MesmerizeClientState.lastFrameTimeNanos < 0) {
            // first frame since state was created — nothing to diff against yet
            MesmerizeClientState.lastFrameTimeNanos = now;
            return;
        }

        double deltaSeconds = (now - MesmerizeClientState.lastFrameTimeNanos) / 1_000_000_000.0;
        MesmerizeClientState.lastFrameTimeNanos = now;

        if (!MesmerizeClientState.active) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        Entity target = client.world.getEntityById(MesmerizeClientState.targetEntityId);
        if (target == null) return;

        Vec3d eyePos = player.getEyePos();
        Vec3d currentLook = player.getRotationVec(1.0F).normalize();
        Vec3d toTarget = target.getPos().subtract(eyePos);
        Vec3d targetLook = toTarget.normalize();

        double maxAngleRadians = Math.toRadians(MesmerizeClientState.degreesPerSecond) * deltaSeconds;
        Vec3d newLook = rotateTowards(currentLook, targetLook, maxAngleRadians);

        float yaw = (float) Math.toDegrees(Math.atan2(-newLook.x, newLook.z));
        float pitch = (float) Math.toDegrees(-Math.asin(newLook.y));

        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }

    private static Vec3d rotateTowards(Vec3d from, Vec3d to, double maxAngleRadians) {
        double dot = MathHelper.clamp(from.dotProduct(to), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= 1.0E-4) return to;

        double t = Math.min(1.0, maxAngleRadians / angle);
        double sinAngle = Math.sin(angle);
        if (sinAngle < 1.0E-6) return from.lerp(to, t).normalize();

        double a = Math.sin((1.0 - t) * angle) / sinAngle;
        double b = Math.sin(t * angle) / sinAngle;
        return from.multiply(a).add(to.multiply(b));
    }
}