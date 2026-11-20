package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldPannerEntity;
import net.follis.goldenscarabmod.entity.custom.GoldStalkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class GoldStalkerRenderer extends MobEntityRenderer<GoldStalkerEntity, GoldStalkerModel<GoldStalkerEntity>> {
    public static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/gold_stalker/gold_stalker.png");

    public GoldStalkerRenderer(EntityRendererFactory.Context context) {
        // shadow radius 0.75 is large for an 8px wide body: tweak to taste
        super(context, new GoldStalkerModel<>(context.getPart(GoldStalkerModel.GOLD_STALKER)), 0.75f);
    }

    @Override
    public Identifier getTexture(GoldStalkerEntity entity) {
        return TEXTURE;
    }
}