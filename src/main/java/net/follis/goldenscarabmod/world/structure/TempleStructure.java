package net.follis.goldenscarabmod.world.structure;

import com.mojang.serialization.MapCodec;
import net.fabricmc.loader.api.FabricLoader;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.block.ModBlocks;
import net.follis.goldenscarabmod.block.entity.custom.CrownBlockEntity;
import net.follis.goldenscarabmod.world.gen.ModStructures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.*;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** The temple structure, its size constants and its three piece types (shell, cover, room). */
public class TempleStructure extends Structure {
    public static final MapCodec<TempleStructure> CODEC = createCodec(TempleStructure::new);

    // ================================================================ constants
    public static final int MODULE_X = 7, MODULE_Z = 7, MODULE_Y = 9;
    public static final int MIN_SIZE = 8, MAX_SIZE = 12;   // modules on the ground floor

    // Debug overrides, set by /temple and cleared at the end of the tick. Left at defaults, normal world generation is unaffected.
    public static volatile Integer debugSize = null;
    public static volatile boolean debugShell = true;

    // In dev, only /temple creates temples. Run with -Dgoldenscarabmod.naturalTemples=true to test natural generation.
    private static final boolean ALLOW_NATURAL = !FabricLoader.getInstance().isDevelopmentEnvironment()
            || Boolean.getBoolean("goldenscarabmod.naturalTemples");

    /** Vertical offset of the cover relative to the pyramid's base (negative sinks it into the ground). */
    private static final int COVER_Y_OFFSET = 0;

    public TempleStructure(Config config) { super(config); }

    // ================================================================ structure
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

        int floors = size / 2 - 3;                                               // regular floors; the 6x6 boss floor sits above
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
        if (debugShell) collector.addPiece(new ShellPiece(corner, size, floors));

        // 2. the decorative cover, which overwrites the shell wherever its solid blocks coincide with it
        if (debugShell) {
            Identifier coverId = Identifier.of(GoldenScarabMod.MOD_ID, "temple/cover_" + size);
            Optional<StructureTemplate> cover = context.structureTemplateManager().getTemplate(coverId);
            if (cover.isPresent()) {
                int turns = layout.get(0).turns();   // the entrance room is always first; the cover turns with it
                BlockPos origin = coverOrigin(corner, cover.get().getSize(), size * MODULE_X, size * MODULE_Z, turns);
                collector.addPiece(new CoverPiece(context.structureTemplateManager(), coverId, origin, rotationFor(turns)));
            } else {
                GoldenScarabMod.LOGGER.info("No cover for size {}: temple/cover_{}.nbt not found", size, size);
            }
        }

        // 3. the rooms, last, so a room's interior always wins
        BlockPos bossCenter = corner.add((floors + 3) * MODULE_X, floors * MODULE_Y + 1, (floors + 3) * MODULE_Z);
        for (TempleLayout.Placed p : layout) {
            collector.addPiece(new RoomPiece(context.structureTemplateManager(), p.template(),
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

    // ================================================================ piece: procedural shell
    /** Hollow stepped pyramid that guarantees thick enough walls under the decorative cover. */
    public static class ShellPiece extends StructurePiece {
        private final BlockPos corner;
        private final int size, floors;

        public ShellPiece(BlockPos corner, int size, int floors) {
            super(ModStructures.TEMPLE_SHELL_PIECE, 0, new BlockBox(
                    corner.getX(), corner.getY(), corner.getZ(),
                    corner.getX() + size * MODULE_X - 1,
                    corner.getY() + (floors + 1) * MODULE_Y - 1,
                    corner.getZ() + size * MODULE_Z - 1));
            this.corner = corner; this.size = size; this.floors = floors;
        }

        public ShellPiece(NbtCompound nbt) {
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

    // ================================================================ piece: decorative cover
    /** The decorative outer shell around the pyramid. Only its solid blocks are placed. */
    public static class CoverPiece extends SimpleStructurePiece {

        public CoverPiece(StructureTemplateManager manager, Identifier template, BlockPos pos, BlockRotation rotation) {
            super(ModStructures.TEMPLE_COVER_PIECE, 0, manager, template, template.toString(), placement(rotation), pos);
        }

        public CoverPiece(StructureTemplateManager manager, NbtCompound nbt) {
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

    // ================================================================ piece: room
    /** One room template. Handles its data markers: chests, mob spawns and the teleporter crown. */
    public static class RoomPiece extends SimpleStructurePiece {
        private static final RegistryKey<LootTable> LOOT =
                RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(GoldenScarabMod.MOD_ID, "chests/temple"));
        @Nullable private final BlockPos tpTarget;

        public RoomPiece(StructureTemplateManager manager, Identifier template, BlockPos pos,
                         BlockRotation rotation, @Nullable BlockPos tpTarget) {
            super(ModStructures.TEMPLE_ROOM_PIECE, 0, manager, template, template.toString(), placement(rotation), pos);
            this.tpTarget = tpTarget;
        }

        public RoomPiece(StructureTemplateManager manager, NbtCompound nbt) {
            super(ModStructures.TEMPLE_ROOM_PIECE, nbt, manager,
                    id -> placement(BlockRotation.valueOf(nbt.getString("Rot"))));
            this.tpTarget = nbt.contains("TpX") ? new BlockPos(nbt.getInt("TpX"), nbt.getInt("TpY"), nbt.getInt("TpZ")) : null;
        }

        private static StructurePlacementData placement(BlockRotation rotation) {
            return new StructurePlacementData().setRotation(rotation).setMirror(BlockMirror.NONE)
                    .setLiquidSettings(StructureLiquidSettings.IGNORE_WATERLOGGING)
                    .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);   // markers aren't placed as blocks
        }

        @Override
        protected void writeNbt(StructureContext context, NbtCompound nbt) {
            super.writeNbt(context, nbt);
            nbt.putString("Rot", this.placementData.getRotation().name());
            if (tpTarget != null) { nbt.putInt("TpX", tpTarget.getX()); nbt.putInt("TpY", tpTarget.getY()); nbt.putInt("TpZ", tpTarget.getZ()); }
        }

        @Override
        protected void handleMetadata(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox box) {
            if (metadata.equals("chest")) {
                world.setBlockState(pos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_LISTENERS);
                LootableInventory.setLootTable(world, random, pos, LOOT);      // check the exact helper in your mappings
            } else if (metadata.startsWith("spawn:")) {
                EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(metadata.substring(6)));
                Entity e = type.create(world.toServerWorld());
                if (e != null) {
                    e.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
                    if (e instanceof MobEntity mob) {
                        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
                        mob.setPersistent();
                    }
                    world.spawnEntityAndPassengers(e);
                }
            } else if (metadata.equals("teleporter") && tpTarget != null) {
                world.setBlockState(pos, ModBlocks.CROWN_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
                if (world.getBlockEntity(pos) instanceof CrownBlockEntity crown) {
                    crown.setTarget(tpTarget);
                }
            }
        }
    }
}