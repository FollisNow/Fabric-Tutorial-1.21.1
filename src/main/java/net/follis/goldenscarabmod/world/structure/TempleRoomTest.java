package net.follis.goldenscarabmod.world.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** /temple room test: lays out every room template in rows, one row per room type, for visual checking. */
public final class TempleRoomTest {
    private static final int ROOM_GAP = 1;    // blocks between two rooms of the same group
    private static final int GROUP_GAP = 3;   // blocks between two groups
    private static final BlockState MARKER_BLOCK = Blocks.POLISHED_ANDESITE.getDefaultState();
    private static final boolean FACE_SOUTH = false;   // false: rooms stay in the orientation they were saved in

    private TempleRoomTest() {}

    public static int run(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        BlockPos start = BlockPos.ofFloored(source.getPosition()).down();   // floor level with the ground under you

        List<RoomDef> groups = new ArrayList<>(TempleRooms.ALL);
        groups.add(TempleRooms.ENTRANCE);
        groups.add(TempleRooms.PENULTIMATE);
        groups.add(TempleRooms.BOSS);

        int rowZ = start.getZ();
        int placed = 0;
        List<String> missing = new ArrayList<>();

        for (RoomDef def : groups) {
            int turns = FACE_SOUTH ? southTurns(def) : 0;

            // load the group's templates first, so the depth of the row is known
            List<Identifier> ids = new ArrayList<>();
            List<StructureTemplate> templates = new ArrayList<>();
            int depth = 0;
            for (Identifier id : def.templates()) {
                Optional<StructureTemplate> template = world.getStructureTemplateManager().getTemplate(id);
                if (template.isEmpty()) {
                    missing.add(nameOf(id));
                    continue;
                }
                ids.add(id);
                templates.add(template.get());
                depth = Math.max(depth, footprintZ(template.get().getSize(), turns));
            }
            if (templates.isEmpty()) continue;

            int x = start.getX();
            for (int i = 0; i < templates.size(); i++) {
                Vec3i size = templates.get(i).getSize();
                int fx = footprintX(size, turns), fz = footprintZ(size, turns);
                BlockPos corner = new BlockPos(x, start.getY(), rowZ + depth - fz);   // south edges line up
                place(world, templates.get(i), ids.get(i), corner, size, turns);
                x += fx + ROOM_GAP;
                placed++;
            }
            rowZ += depth + GROUP_GAP;
        }

        int count = placed;
        source.sendFeedback(() -> Text.literal("Placed " + count + " rooms, east and south of your position"), false);
        if (!missing.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Missing templates (" + missing.size() + "): " + String.join(", ", missing)), false);
        }
        return placed;
    }

    /** Rotation that gives the room a door on its south side, preferring none at all. */
    private static int southTurns(RoomDef def) {
        if (def == TempleRooms.ENTRANCE) return 0;   // its main door is the opening in its south wall, which has no port
        for (int t = 0; t < 4; t++)
            if (def.shape(t).ports().stream().anyMatch(q -> q.side() == Dir.S)) return t;
        return 0;
    }

    private static int footprintX(Vec3i size, int turns) { return turns % 2 == 1 ? size.getZ() : size.getX(); }

    private static int footprintZ(Vec3i size, int turns) { return turns % 2 == 1 ? size.getX() : size.getZ(); }

    /** Places one room with its north-west corner at 'corner', and puts its marker block and sign above it. */
    private static void place(ServerWorld world, StructureTemplate template, Identifier id, BlockPos corner, Vec3i size, int turns) {
        int sx = size.getX(), sz = size.getZ();
        BlockPos origin = corner.add(switch (turns) {   // a rotated template pivots around its own corner
            case 1 -> new BlockPos(sz - 1, 0, 0);
            case 2 -> new BlockPos(sx - 1, 0, sz - 1);
            case 3 -> new BlockPos(0, 0, sx - 1);
            default -> BlockPos.ORIGIN;
        });
        StructurePlacementData data = new StructurePlacementData()
                .setRotation(TempleStructure.rotationFor(turns)).setMirror(BlockMirror.NONE)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
        template.place(world, origin, origin, data, world.getRandom(), Block.NOTIFY_LISTENERS);

        // marker block just above the north-east corner of the room, with a sign standing on it
        BlockPos marker = new BlockPos(corner.getX() + footprintX(size, turns) - 1, corner.getY() + size.getY(), corner.getZ());
        BlockPos signPos = marker.up();
        world.setBlockState(marker, MARKER_BLOCK);
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0));   // rotation 0 faces south
        if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            List<String> lines = wrap(nameOf(id));
            for (int i = 0; i < Math.min(4, lines.size()); i++) text = text.withMessage(i, Text.literal(lines.get(i)));
            sign.setText(text, true);
            sign.setWaxed(true);   // nobody edits it by accident
        }
    }

    /** "temple/corner_0" -> "corner_0" */
    private static String nameOf(Identifier id) {
        String path = id.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Splits a name over several sign lines, only at underscores. */
    private static List<String> wrap(String name) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String part : name.split("_")) {
            if (line.length() > 0 && line.length() + 1 + part.length() > 14) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append('_');
            line.append(part);
        }
        lines.add(line.toString());
        return lines;
    }
}