package net.follis.tutorialmod.item.render;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.follis.tutorialmod.mixin.EntityRenderDispatcherAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenericBugFigurineRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private static final int MAX_CACHE_SIZE = 256;
    static final Map<BugData, Entity> ENTITY_CACHE =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<BugData, Entity> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    private static Entity getOrLoadEntity(BugData data, World world) {
        return ENTITY_CACHE.computeIfAbsent(data, d -> {
            Entity entity = d.loadEntity(world);
            if (entity != null) {
                entity.setYaw(0F);
                entity.setPitch(0F);
                entity.prevYaw = 0F;
                entity.prevPitch = 0F;
                if (entity instanceof LivingEntity living) {
                    living.setBodyYaw(0F);
                    living.setHeadYaw(0F);
                    living.prevBodyYaw = 0F;
                    living.prevHeadYaw = 0F;
                }
            }
            return entity;
        });
    }
    public static void clearCache() {
        ENTITY_CACHE.clear();
    }
    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BugData data = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        if (data == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Entity entity = getOrLoadEntity(data, client.world);
        if (entity == null) return;

        entity.setYaw(0F);
        entity.setPitch(0F);
        entity.prevYaw = 0F;
        entity.prevPitch = 0F;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setBodyYaw(0F);
            livingEntity.setHeadYaw(0F);
            livingEntity.prevBodyYaw = 0F;
            livingEntity.prevHeadYaw = 0F;
        }

        int effectiveLight = (mode == ModelTransformationMode.GUI) ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
        if (mode == ModelTransformationMode.GUI) DiffuseLighting.enableGuiDepthLighting();

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.scale(0.5F, 0.5F, 0.5F);

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        EntityRenderDispatcherAccessorMixin accessor = (EntityRenderDispatcherAccessorMixin) dispatcher;
        accessor.setRenderShadows(false);
        try {
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, matrices, vertexConsumers, effectiveLight);
        } finally {
            accessor.setRenderShadows(true);
        }

        matrices.pop();
        if (mode == ModelTransformationMode.GUI) DiffuseLighting.disableGuiDepthLighting();
    }
}