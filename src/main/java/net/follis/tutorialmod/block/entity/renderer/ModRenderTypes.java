package net.follis.tutorialmod.block.entity.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL14;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderLayer;


/*
*       Made by translating Cucumber ModRenderTypes
*                   (or trying to)
*/
public class ModRenderTypes {
    private static final RenderPhase.Transparency GHOST_TRANSPARENCY = new RenderPhase.Transparency("ghost_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.CONSTANT_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            GL14.glBlendColor(1.0F, 1.0F, 1.0F, 0.25F);
        },
        () -> {
            GL14.glBlendColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        });

    public static final RenderLayer GHOST_RENDER_LAYER = RenderLayer.of(
            "tutorialmod:ghost",
            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
            VertexFormat.DrawMode.QUADS, 2097152, true, false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_TEXTURE_LIGHTMAP_PROGRAM)
                    .texture(RenderPhase.BLOCK_ATLAS_TEXTURE)
                    .transparency(GHOST_TRANSPARENCY)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .build(false)
    );
}
