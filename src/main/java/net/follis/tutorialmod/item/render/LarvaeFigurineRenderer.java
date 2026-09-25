package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.LarvaeModel;
import net.follis.tutorialmod.entity.client.LarvaeRenderer;
import net.follis.tutorialmod.entity.custom.LarvaeVariant;
import net.follis.tutorialmod.entity.custom.CocoonMaterial;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.List;

import static net.follis.tutorialmod.entity.custom.LarvaeEntity.COCOON_SLOT_COUNT;

public class LarvaeFigurineRenderer extends AbstractBugFigurineRenderer<LarvaeModel<?>> {
    @Override
    protected LarvaeModel<?> createModel(MinecraftClient client) {
        return new LarvaeModel<>(client.getEntityModelLoader().getModelPart(LarvaeModel.LARVAE));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        LarvaeVariant variant = data != null ? LarvaeVariant.byId(data.getVariantId()) : LarvaeVariant.REGULAR;
        return LarvaeRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(LarvaeModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);

        matrices.push();
        List<CocoonMaterial> segments = data != null ? readCocoonSegments(data) : List.of();
        for (int i = 0; i < COCOON_SLOT_COUNT && i < segments.size(); i++) {
            CocoonMaterial material = segments.get(i);
            if (material == null) continue;

            int argb = material.getColor() | 0xFF000000;
            model.renderMaterialPart(matrices, buffer, light, OverlayTexture.DEFAULT_UV, i, argb);
        }
        matrices.pop();
    }

    private List<CocoonMaterial> readCocoonSegments(BugData data) {
        NbtCompound nbt = data.getNbt();
        CocoonMaterial[] materials = CocoonMaterial.values();
        List<CocoonMaterial> result = new java.util.ArrayList<>();

        for (int i = 0; i < 4; i++) {
            String key = "MaterialSlot" + i;
            if (nbt.contains(key)) {
                int ordinal = nbt.getInt(key);
                result.add(ordinal == -1 ? null : materials[ordinal]);
            } else {
                result.add(null);
            }
        }
        return result;
    }
}