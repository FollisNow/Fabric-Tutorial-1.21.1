package net.follis.tutorialmod.entity.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.DartEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DartEntityRenderer extends ProjectileEntityRenderer<DartEntity> {
    public static final Identifier TEXTURE = Identifier.of(TutorialMod.MOD_ID, "textures/entity/dart/dart.png");

    public DartEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(DartEntity entity) {
        return TEXTURE;
    }
}
