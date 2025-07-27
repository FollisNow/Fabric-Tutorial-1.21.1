package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.entity.custom.ChairEntity;
import net.follis.tutorialmod.entity.custom.LockingEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class LockRenderer extends EntityRenderer<LockingEntity> {
    public LockRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(LockingEntity entity) {
        return null;
    }

    @Override
    public boolean shouldRender(LockingEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}
