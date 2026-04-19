package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldenSilverfishEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GoldenSilverfishRenderer extends MobEntityRenderer<GoldenSilverfishEntity, GoldenSilverfishModel<GoldenSilverfishEntity>> {
    private static final Identifier TEXTURE = Identifier.of(TutorialMod.MOD_ID,"textures/entity/golden_silverfish/golden_silverfish.png");

    public GoldenSilverfishRenderer(EntityRendererFactory.Context context) {
        super(context, new GoldenSilverfishModel<>(context.getPart(GoldenSilverfishModel.GOLDEN_SILVERFISH)), 0.3F);
    }

    protected float getLyingAngle(GoldenSilverfishEntity silverfishEntity) {
        return 180.0F;
    }

    public Identifier getTexture(GoldenSilverfishEntity silverfishEntity) {
        return TEXTURE;
    }

}
