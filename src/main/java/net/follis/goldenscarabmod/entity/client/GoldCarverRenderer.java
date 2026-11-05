package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldCarverEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GoldCarverRenderer extends AbstractGoldWorkerRenderer<GoldCarverEntity, GoldCarverModel<GoldCarverEntity>> {
    public static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/gold_carver/gold_carver.png");

    public GoldCarverRenderer(EntityRendererFactory.Context context) {
        // shadow radius 0.75 is large for an 8px wide body: tweak to taste
        super(context, new GoldCarverModel<>(context.getPart(GoldCarverModel.GOLD_CARVER)), 0.75f);
    }

    @Override
    public Identifier getTexture(GoldCarverEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void applyItemTransform(MatrixStack matrices) {
        matrices.translate(0.09375F, 1.03125F, 0.25F);
        matrices.scale(0.1875F, 0.1875F, 0.1875F);
    }
}