package net.follis.goldenscarabmod.world.structure;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.block.Block;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

public final class TempleCoverTest {
    private TempleCoverTest() {}

    public static int run(ServerCommandSource source, int number, int turns) {
        ServerWorld world = source.getWorld();
        Identifier id = Identifier.of(GoldenScarabMod.MOD_ID, "temple/cover_" + number);
        Optional<StructureTemplate> template = world.getStructureTemplateManager().getTemplate(id);
        if (template.isEmpty()) {
            source.sendError(Text.literal("No template " + id + " (expected src/main/resources/data/"
                    + GoldenScarabMod.MOD_ID + "/structure/temple/cover_" + number + ".nbt)"));
            return 0;
        }

        Vec3i size = template.get().getSize();
        int sx = size.getX(), sz = size.getZ();
        BlockPos corner = BlockPos.ofFloored(source.getPosition());   // the cover's north-west corner
        BlockPos origin = corner.add(switch (turns) {                   // a rotated template pivots around its own corner
            case 1 -> new BlockPos(sz - 1, 0, 0);
            case 2 -> new BlockPos(sx - 1, 0, sz - 1);
            case 3 -> new BlockPos(0, 0, sx - 1);
            default -> BlockPos.ORIGIN;
        });

        StructurePlacementData data = new StructurePlacementData()
                .setRotation(TempleStructure.rotationFor(turns)).setMirror(BlockMirror.NONE)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);   // same as the real cover piece
        template.get().place(world, origin, origin, data, world.getRandom(), Block.NOTIFY_LISTENERS);

        boolean quarter = turns % 2 == 1;
        int width = quarter ? sz : sx, depth = quarter ? sx : sz;
        source.sendFeedback(() -> Text.literal("Placed cover_" + number + " (" + width + " x " + size.getY() + " x " + depth
                + "), turns=" + turns + ", north-west corner at your feet"), false);
        return 1;
    }
}
