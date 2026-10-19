package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.BambooTrapEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class BambooTrapRenderer extends MobEntityRenderer<BambooTrapEntity, BambooTrapModel> {
    protected BambooTrapModel model;

    public BambooTrapRenderer(EntityRendererFactory.Context context) {
        super(context,new BambooTrapModel(context.getPart(BambooTrapModel.BAMBOO_TRAP)) , 0.1f);
        this.model = new BambooTrapModel(context.getPart(BambooTrapModel.BAMBOO_TRAP));
    }

    @Override
    public Identifier getTexture(BambooTrapEntity entity) {
        return Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/bamboo_trap/bamboo_trap.png");
    }

    @Nullable
    protected RenderLayer getRenderLayer(BambooTrapEntity bambooTrapEntity, boolean bl, boolean bl2, boolean bl3) {
        Identifier identifier = this.getTexture(bambooTrapEntity);
        return this.model.getLayer(identifier);
    }
}
