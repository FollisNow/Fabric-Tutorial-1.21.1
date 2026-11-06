package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldChannelerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GoldChannelerRenderer
        extends MobEntityRenderer<GoldChannelerEntity, GoldChannelerModel<GoldChannelerEntity>> {
    public static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/gold_channeler/gold_channeler.png");

    public GoldChannelerRenderer(EntityRendererFactory.Context context) {
        super(context, new GoldChannelerModel<>(context.getPart(GoldChannelerModel.GOLD_CHANNELER)), 0.4f);
        this.addFeature(new GoldChannelerFilterIconFeature(this, context.getHeldItemRenderer()));
    }

    @Override
    public Identifier getTexture(GoldChannelerEntity entity) {
        return TEXTURE;
    }
}