package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldBreederEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/**
 * Plain MobEntityRenderer: the breeder has no held template item (GoldBreederEntity doesn't extend
 * AbstractGoldWorkerEntity). Its mode icon is drawn by GoldBreederModeIconFeatureRenderer, pinned on the gun.
 */
public class GoldBreederRenderer extends MobEntityRenderer<GoldBreederEntity, GoldBreederModel<GoldBreederEntity>> {
    public static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/gold_breeder/gold_breeder.png");

    public GoldBreederRenderer(EntityRendererFactory.Context context) {
        // shadow radius 0.75 is large for an 8px wide body: tweak to taste
        super(context, new GoldBreederModel<>(context.getPart(GoldBreederModel.GOLD_BREEDER)), 0.75f);
        // the mode icon follows the gun bone, which AbstractGoldWorkerRenderer's fixed body-relative offset cannot do
        this.addFeature(new GoldBreederModeIconFeatureRenderer(this, context.getHeldItemRenderer()));
        // the locust in the glass bottle uses its own baked model layer (already registered for the locust entity)
        this.addFeature(new GoldBreederBottleLocustFeatureRenderer(this, new LocustModel<>(context.getPart(LocustModel.LOCUST))));
    }

    @Override
    public Identifier getTexture(GoldBreederEntity entity) {
        return TEXTURE;
    }
}