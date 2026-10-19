package net.follis.goldenscarabmod.world.structure;

import net.follis.goldenscarabmod.block.ModBlocks;
import net.follis.goldenscarabmod.world.gen.ModStructures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import static net.follis.goldenscarabmod.world.structure.TempleConstants.*;

public class TempleShellPiece extends StructurePiece {
    private final BlockPos corner;
    private final int size, floors;

    public TempleShellPiece(BlockPos corner, int size, int floors) {
        super(ModStructures.TEMPLE_SHELL_PIECE, 0, new BlockBox(
                corner.getX(), corner.getY(), corner.getZ(),
                corner.getX() + size * MODULE_X - 1,
                corner.getY() + (floors + 1) * MODULE_Y - 1,
                corner.getZ() + size * MODULE_Z - 1));
        this.corner = corner; this.size = size; this.floors = floors;
    }

    public TempleShellPiece(NbtCompound nbt) {
        super(ModStructures.TEMPLE_SHELL_PIECE, nbt);
        corner = new BlockPos(nbt.getInt("CX"), nbt.getInt("CY"), nbt.getInt("CZ"));
        size = nbt.getInt("Size");
        floors = nbt.getInt("Floors");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("CX", corner.getX()); nbt.putInt("CY", corner.getY()); nbt.putInt("CZ", corner.getZ());
        nbt.putInt("Size", size); nbt.putInt("Floors", floors);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor accessor, ChunkGenerator generator,
                         Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockState wall = ModBlocks.GOLD_BRICKS.getDefaultState();     // swap for your own palette
        BlockState roof = ModBlocks.CUT_GOLD.getDefaultState();
        BlockState terrace = ModBlocks.GOLDEN_DIRT.getDefaultState();   // the "hanging garden" ring

        BlockBox b = this.boundingBox;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = Math.max(b.getMinX(), chunkBox.getMinX()); x <= Math.min(b.getMaxX(), chunkBox.getMaxX()); x++)
            for (int z = Math.max(b.getMinZ(), chunkBox.getMinZ()); z <= Math.min(b.getMaxZ(), chunkBox.getMaxZ()); z++)
                for (int y = Math.max(b.getMinY(), chunkBox.getMinY()); y <= Math.min(b.getMaxY(), chunkBox.getMaxY()); y++) {
                    int rx = x - corner.getX(), rz = z - corner.getZ(), ry = y - corner.getY();
                    int f = ry / MODULE_Y, ly = ry % MODULE_Y;
                    int lo = f * MODULE_X, hi = (size - f) * MODULE_X - 1;
                    if (rx < lo || rx > hi || rz < lo || rz > hi) continue;     // outside this floor's footprint

                    BlockState state = null;
                    if (ly == MODULE_Y - 1) {                                   // roof layer
                        int nlo = (f + 1) * MODULE_X, nhi = (size - f - 1) * MODULE_X - 1;
                        boolean underUpperFloor = f < floors && rx >= nlo && rx <= nhi && rz >= nlo && rz <= nhi;
                        state = underUpperFloor || f == floors ? roof : terrace;
                    } else if (f == 0 && ly == 0) {
                        state = roof;                                           // base slab of the ground floor
                    } else if (rx == lo || rx == hi || rz == lo || rz == hi) {
                        state = wall;                                           // outer wall; the interior stays hollow
                    }
                    if (state != null) world.setBlockState(pos.set(x, y, z), state, Block.NOTIFY_LISTENERS);
                }
    }
}
