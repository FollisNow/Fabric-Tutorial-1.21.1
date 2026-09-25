package net.follis.tutorialmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.LarvaeEntity;
import net.follis.tutorialmod.entity.custom.LarvaeVariant;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class LarvaeRenderer extends MobEntityRenderer<LarvaeEntity, LarvaeModel<LarvaeEntity>> {
    private static final Map<LarvaeVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(LarvaeVariant.class), map -> {
                map.put(LarvaeVariant.REGULAR,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/larvae/larvae.png"));
                map.put(LarvaeVariant.CADDISFLY,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/larvae/larvae.png"));
            });

    public LarvaeRenderer(EntityRendererFactory.Context context) {
        super(context, new LarvaeModel<>(context.getPart(LarvaeModel.LARVAE)), 0.2f);
        this.addFeature(new LarvaeCocoonFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(LarvaeEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }
    public static Identifier getTexture(LarvaeVariant variant) { return LOCATION_BY_VARIANT.get(variant); }

    public RenderLayer getCocoonRenderLayer(LarvaeEntity entity) {
        return this.getRenderLayer(entity, true, false, false);
    }
    @Override
    public void render(LarvaeEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int light) {
        if (livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }
        super.render(livingEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
