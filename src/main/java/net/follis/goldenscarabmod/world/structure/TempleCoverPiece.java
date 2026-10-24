package net.follis.goldenscarabmod.world.structure;

import net.follis.goldenscarabmod.world.gen.ModStructures;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.*;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

/** The decorative outer shell around the pyramid. Only its solid blocks are placed. */
public class TempleCoverPiece extends SimpleStructurePiece {

    public TempleCoverPiece(StructureTemplateManager manager, Identifier template, BlockPos pos, BlockRotation rotation) {
        super(ModStructures.TEMPLE_COVER_PIECE, 0, manager, template, template.toString(), placement(rotation), pos);
    }

    public TempleCoverPiece(StructureTemplateManager manager, NbtCompound nbt) {
        super(ModStructures.TEMPLE_COVER_PIECE, nbt, manager,
                id -> placement(BlockRotation.valueOf(nbt.getString("Rot"))));
    }

    private static StructurePlacementData placement(BlockRotation rotation) {
        return new StructurePlacementData().setRotation(rotation).setMirror(BlockMirror.NONE)
                .setLiquidSettings(StructureLiquidSettings.IGNORE_WATERLOGGING)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putString("Rot", this.placementData.getRotation().name());
    }

    @Override
    protected void handleMetadata(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox box) {
        // the cover has no data markers
    }
}