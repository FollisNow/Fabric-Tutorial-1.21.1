package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldPannerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class GoldPannerRenderer extends AbstractGoldWorkerRenderer<GoldPannerEntity, GoldPannerModel<GoldPannerEntity>> {
    public static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/gold_panner/gold_panner.png");

    public GoldPannerRenderer(EntityRendererFactory.Context context) {
        // shadow radius 0.75 is large for an 8px wide body: tweak to taste
        super(context, new GoldPannerModel<>(context.getPart(GoldPannerModel.GOLD_PANNER)), 0.75f);
    }

    @Override
    public Identifier getTexture(GoldPannerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void applyItemTransform(MatrixStack matrices) {
        matrices.translate(0.0F, 0.9375F, 0.3125F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
        matrices.scale(0.375F, 0.375F, 0.375F);
    }
}