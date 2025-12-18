package net.follis.tutorialmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.SpiderlingEntity;
import net.follis.tutorialmod.entity.custom.SpiderlingVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.SpiderEyesFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class SpiderlingRenderer<T extends SpiderlingEntity> extends MobEntityRenderer<T, SpiderlingModel<T>> {
    private static final Map<SpiderlingVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(SpiderlingVariant.class), map -> {
                map.put(SpiderlingVariant.DEFAULT,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/spiderling/spiderling.png"));
            });

    public SpiderlingRenderer(EntityRendererFactory.Context context) {
        super(context, new SpiderlingModel<>(context.getPart(SpiderlingModel.SPIDERLING)), 0.4f);
        this.addFeature(new SpiderEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(SpiderlingEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    protected float getLyingAngle(T spiderEntity) {
        return 180.0F;
    }

    @Override
    public void render(SpiderlingEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int light) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(0.7f, 0.7f, 0.7f);
        }

        super.render((T) livingEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
