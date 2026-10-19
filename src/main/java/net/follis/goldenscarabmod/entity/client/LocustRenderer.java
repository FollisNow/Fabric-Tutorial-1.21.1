package net.follis.goldenscarabmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.LocustEntity;
import net.follis.goldenscarabmod.entity.custom.LocustVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class LocustRenderer extends MobEntityRenderer<LocustEntity, LocustModel<LocustEntity>> {
    private static final Map<LocustVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(LocustVariant.class), map -> {
                map.put(LocustVariant.GOLD,
                        Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/locust/locust_gold.png"));
                map.put(LocustVariant.DREAM,
                        Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/locust/locust_dream.png"));
                map.put(LocustVariant.GRASSHOPPER,
                        Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/locust/locust_grasshopper.png"));
                map.put(LocustVariant.RED,
                        Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/locust/locust_red.png"));
            });

    public LocustRenderer(EntityRendererFactory.Context context) {
        super(context, new LocustModel<>(context.getPart(LocustModel.LOCUST)), 0.4f);
    }

    @Override
    public Identifier getTexture(LocustEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }
    public static Identifier getTexture(LocustVariant variant) { return LOCATION_BY_VARIANT.get(variant); }

    @Override
    public void render(LocustEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
