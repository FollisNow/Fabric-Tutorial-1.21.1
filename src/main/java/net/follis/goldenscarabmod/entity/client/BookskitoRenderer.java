package net.follis.goldenscarabmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.BookskitoEntity;
import net.follis.goldenscarabmod.entity.custom.BookskitoVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class BookskitoRenderer extends MobEntityRenderer<BookskitoEntity, BookskitoModel<BookskitoEntity>> {
    private static final Map<BookskitoVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(BookskitoVariant.class), map -> {
                map.put(BookskitoVariant.GOLD,
                        Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/bookskito/bookskito_gold.png"));
                map.put(BookskitoVariant.REGULAR,
                        Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/bookskito/bookskito.png"));
            });

    public BookskitoRenderer(EntityRendererFactory.Context context) {
        super(context, new BookskitoModel<>(context.getPart(BookskitoModel.BOOKSKITO)), 0.4f);
        this.addFeature(new BookskitoEnchantGlintFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(BookskitoEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }
    public static Identifier getTexture(BookskitoVariant variant) { return LOCATION_BY_VARIANT.get(variant); }
    @Override
    public void render(BookskitoEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int light) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
