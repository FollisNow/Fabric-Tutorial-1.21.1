package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.DominionEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DominionRenderer extends MobEntityRenderer<DominionEntity, DominionModel<DominionEntity>> {
    private static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/dominion/dominion.png");

    private static final float SWORD_SCALE = 1F;

    private final GoldenSwordModel swordModel;

    public DominionRenderer(EntityRendererFactory.Context context) {
        super(context, new DominionModel<>(context.getPart(DominionModel.DOMINION)), 0.5f);
        this.swordModel = new GoldenSwordModel(context.getPart(GoldenSwordModel.GOLDEN_SWORD));
    }

    @Override
    public void render(DominionEntity boss, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // while the cannon is up the cube tilts about its own centre, so its bottom points at the target
        // the body can sink into the ground (the chandelier) and tilt about the centre of the cube (the cannon, the flight)
        // the body can tilt about the centre of the cube (the cannon, the flight)
        matrices.push();
        if (boss.getBodyTiltAmount(tickDelta) > 0.0F) {
            matrices.translate(0.0, DominionEntity.BODY_CENTER_Y, 0.0);
            matrices.multiply(boss.getBodyTilt(tickDelta));
            matrices.translate(0.0, -DominionEntity.BODY_CENTER_Y, 0.0);
        }
        super.render(boss, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();

        // the swords: their positions already include the tilt and the extra spin; the blades follow the barrel
        int count = Math.min(boss.getSwordCount(), boss.getMaxSwords());
        if (count > 0) {
            float t = (float) (boss.clock(tickDelta) + boss.getCannonExtraSpin(tickDelta));
            Vec3d aim = boss.getSwordAim(tickDelta);
            for (int slot = 0; slot < count; slot++) {
                Vec3d offset = boss.getSwordOffset(slot, tickDelta);

                matrices.push();
                matrices.translate(offset.x, offset.y, offset.z);
                SwordDrawing.draw(swordModel, matrices, vertexConsumers, aim, SwordDrawing.CENTER_HEIGHT,
                        SWORD_SCALE, t * 10.0F + slot * 37.0F, 255, 255, 255);
                matrices.pop();
            }
        }

        // the beam: a thin line while it charges (it shows where the shot will go), then the beam itself
        int phase = boss.getCannonPhase();
        if (phase == DominionEntity.CANNON_CHARGE || phase == DominionEntity.CANNON_FIRE) {
            Vec3d aim = boss.getCannonAim();
            Vec3d start = boss.cannonMuzzle(boss.getLerpedPos(tickDelta), aim);
            double length = boss.beamLength(start, aim);

            matrices.push();
            Vec3d muzzle = new Vec3d(0.0, DominionEntity.BODY_CENTER_Y, 0.0).add(aim.multiply(DominionEntity.MUZZLE_DISTANCE));
            matrices.translate(muzzle.x, muzzle.y, muzzle.z);
            matrices.multiply(new Quaternionf().rotationTo(new Vector3f(0.0F, -1.0F, 0.0F),
                    new Vector3f((float) aim.x, (float) aim.y, (float) aim.z)));
            if (phase == DominionEntity.CANNON_CHARGE) {
                BeamDrawing.draw(matrices, vertexConsumers, length, 0.035F, 255, 210, 80, 110);
            } else {
                float pulse = 1.0F + 0.08F * MathHelper.sin((boss.age + tickDelta) * 2.5F);
                float radius = 0.55F * boss.getBeamStrength(tickDelta) * pulse;
                BeamDrawing.draw(matrices, vertexConsumers, length, radius, 255, 200, 60, 90);           // the glow
                BeamDrawing.draw(matrices, vertexConsumers, length, radius * 0.45F, 255, 245, 200, 220);  // the core
            }
            matrices.pop();
        }
    }

    @Override
    public Identifier getTexture(DominionEntity entity) {
        return TEXTURE;
    }
}