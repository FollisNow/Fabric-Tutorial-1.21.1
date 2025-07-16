package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.AmethystBeeEntity;
import net.follis.tutorialmod.entity.custom.LocustEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AmethystBeeRenderer extends MobEntityRenderer<AmethystBeeEntity, AmethystBeeModel<AmethystBeeEntity>> {

    private static final Identifier ANGRY_TEXTURE = Identifier.of(TutorialMod.MOD_ID,"textures/entity/amethyst_bee/amethyst_bee_angry.png");
    private static final Identifier ANGRY_NECTAR_TEXTURE = Identifier.of(TutorialMod.MOD_ID,"textures/entity/amethyst_bee/amethyst_bee_angry_nectar.png");
    private static final Identifier PASSIVE_TEXTURE = Identifier.of(TutorialMod.MOD_ID,"textures/entity/amethyst_bee/amethyst_bee.png");
    private static final Identifier NECTAR_TEXTURE = Identifier.of(TutorialMod.MOD_ID,"textures/entity/amethyst_bee/amethyst_bee_nectar.png");
    public AmethystBeeRenderer(EntityRendererFactory.Context context) {
        super(context, new AmethystBeeModel<>(context.getPart(AmethystBeeModel.AMETHYST_BEE)), 0.4f);
    }

    @Override
    public Identifier getTexture(AmethystBeeEntity beeEntity) {
        if (beeEntity.hasAngerTime()) {
            return beeEntity.hasNectar() ? ANGRY_NECTAR_TEXTURE : ANGRY_TEXTURE;
        } else {
            return beeEntity.hasNectar() ? NECTAR_TEXTURE : PASSIVE_TEXTURE;
        }
    }

    @Override
    public void render(AmethystBeeEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
