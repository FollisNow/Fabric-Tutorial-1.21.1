package net.follis.goldenscarabmod.world.structure;

import com.mojang.serialization.MapCodec;
import net.fabricmc.loader.api.FabricLoader;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.world.gen.ModStructures;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.List;
import java.util.Optional;

import static net.follis.goldenscarabmod.world.structure.TempleConstants.*;

public class TempleStructure extends Structure {
    public static final MapCodec<TempleStructure> CODEC = createCodec(TempleStructure::new);

    // Debug overrides, set by /temple and cleared at the end of the tick. Left at defaults, normal world generation is unaffected.
    public static volatile Integer debugSize = null;
    public static volatile boolean debugShell = true;

    // In dev, only /temple creates temples. Run with -Dtutorialmod.naturalTemples=true to test natural generation.
    private static final boolean ALLOW_NATURAL = !FabricLoader.getInstance().isDevelopmentEnvironment()
            || Boolean.getBoolean("goldenscarabmod.naturalTemples");

    /** Vertical offset of the cover relative to the pyramid's base (negative sinks it into the ground). */
    private static final int COVER_Y_OFFSET = 0;

    public TempleStructure(Config config) { super(config); }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        if (debugSize == null && !ALLOW_NATURAL) return Optional.empty();

        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getStartX() + 8, cz = chunk.getStartZ() + 8;
        int groundY = context.chunkGenerator().getHeightInGround(cx, cz,
                Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
        BlockPos center = new BlockPos(cx, groundY, cz);
        return Optional.of(new StructurePosition(center, collector -> generatePieces(context, collector, center)));
    }

    private void generatePieces(Context context, StructurePiecesCollector collector, BlockPos center) {
        Random rng = context.random();

        int size = debugSize != null ? debugSize
                : MIN_SIZE + 2 * rng.nextInt((MAX_SIZE - MIN_SIZE) / 2 + 1);

        int floors = size / 2 - 2;                                               // regular floors; the boss floor sits above
        BlockPos corner = center.add(-size * MODULE_X / 2, 0, -size * MODULE_Z / 2);

        TempleLayout.Params params = new TempleLayout.Params(TempleRooms.ALL, TempleRooms.ENTRANCE,
                TempleRooms.PENULTIMATE, TempleRooms.BOSS, size, floors, size * size / 3);
        List<TempleLayout.Placed> layout = TempleLayout.generate(params, rng, 200);
        if (layout == null) {
            GoldenScarabMod.LOGGER.warn("Temple layout failed for size {}; skipping", size);
            return;
        }
        GoldenScarabMod.LOGGER.info("Temple at chunk {}: size={} floors={} rooms={} shell={} override={}",
                context.chunkPos(), size, floors, layout.size(), debugShell, debugSize != null);
        for (TempleLayout.Placed p : layout) {
            for (Identifier t : p.def().templates()) {
                if (context.structureTemplateManager().getTemplate(t).isEmpty()) {
                    GoldenScarabMod.LOGGER.warn("Missing temple template: {}", t);
                }
            }
        }

        // Placement order is priority order, lowest first: shell < cover < rooms

        // 1. the procedural shell: only there to guarantee the walls are thick enough
        if (debugShell) collector.addPiece(new TempleShellPiece(corner, size, floors));

        // 2. the decorative cover, which overwrites the shell wherever its solid blocks coincide with it
        if (debugShell) {
            Identifier coverId = Identifier.of(GoldenScarabMod.MOD_ID, "temple/cover_" + size);
            Optional<StructureTemplate> cover = context.structureTemplateManager().getTemplate(coverId);
            if (cover.isPresent()) {
                int turns = layout.get(0).turns();   // the entrance room is always first; the cover turns with it
                BlockPos origin = coverOrigin(corner, cover.get().getSize(), size * MODULE_X, size * MODULE_Z, turns);
                collector.addPiece(new TempleCoverPiece(context.structureTemplateManager(), coverId, origin, rotationFor(turns)));
            } else {
                GoldenScarabMod.LOGGER.info("No cover for size {}: temple/cover_{}.nbt not found", size, size);
            }
        }

        // 3. the rooms, last, so a room's interior always wins
        BlockPos bossCenter = corner.add((floors + 2) * MODULE_X, floors * MODULE_Y + 1, (floors + 2) * MODULE_Z);
        for (TempleLayout.Placed p : layout) {
            collector.addPiece(new TempleRoomPiece(context.structureTemplateManager(), p.template(),
                    roomOrigin(corner, p), rotationFor(p.turns()),
                    p.def() == TempleRooms.PENULTIMATE ? bossCenter : null));
        }
    }

    public static BlockRotation rotationFor(int turns) {
        return switch (turns) {
            case 1 -> BlockRotation.CLOCKWISE_90;
            case 2 -> BlockRotation.CLOCKWISE_180;
            case 3 -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    /** World position to place a template at: the module corner, shifted to account for rotation about the template's corner. */
    public static BlockPos roomOrigin(BlockPos corner, TempleLayout.Placed p) {
        int sx = p.def().w() * MODULE_X, sz = p.def().d() * MODULE_Z;   // unrotated template size
        BlockPos base = corner.add(p.x() * MODULE_X, p.y() * MODULE_Y, p.z() * MODULE_Z);
        return base.add(switch (p.turns()) {
            case 1 -> new BlockPos(sz - 1, 0, 0);
            case 2 -> new BlockPos(sx - 1, 0, sz - 1);
            case 3 -> new BlockPos(0, 0, sx - 1);
            default -> BlockPos.ORIGIN;
        });
    }

    /** Placement position for the cover: centred on the pyramid, whatever the rotation. */
    static BlockPos coverOrigin(BlockPos corner, Vec3i coverSize, int pyramidX, int pyramidZ, int turns) {
        int sx = coverSize.getX(), sz = coverSize.getZ();
        boolean quarter = turns % 2 == 1;
        int spanX = quarter ? sz : sx, spanZ = quarter ? sx : sz;   // footprint after rotation
        BlockPos min = corner.add(-((spanX - pyramidX) / 2), COVER_Y_OFFSET, -((spanZ - pyramidZ) / 2));
        return min.add(switch (turns) {
            case 1 -> new BlockPos(sz - 1, 0, 0);
            case 2 -> new BlockPos(sx - 1, 0, sz - 1);
            case 3 -> new BlockPos(0, 0, sx - 1);
            default -> BlockPos.ORIGIN;
        });
    }

    @Override
    public StructureType<?> getType() { return ModStructures.TEMPLE; }
}