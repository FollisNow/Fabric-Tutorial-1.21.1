package net.follis.goldenscarabmod.world.structure;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.block.ModBlocks;
import net.follis.goldenscarabmod.block.entity.custom.CrownBlockEntity;
import net.follis.goldenscarabmod.world.gen.ModStructures;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.Nullable;

public class TempleRoomPiece extends SimpleStructurePiece {
    private static final RegistryKey<LootTable> LOOT =
            RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(GoldenScarabMod.MOD_ID, "chests/temple"));
    @Nullable private final BlockPos tpTarget;

    public TempleRoomPiece(StructureTemplateManager manager, Identifier template, BlockPos pos,
                           BlockRotation rotation, @Nullable BlockPos tpTarget) {
        super(ModStructures.TEMPLE_ROOM_PIECE, 0, manager, template, template.toString(), placement(rotation), pos);
        this.tpTarget = tpTarget;
    }

    public TempleRoomPiece(StructureTemplateManager manager, NbtCompound nbt) {
        super(ModStructures.TEMPLE_ROOM_PIECE, nbt, manager,
                id -> placement(BlockRotation.valueOf(nbt.getString("Rot"))));
        this.tpTarget = nbt.contains("TpX") ? new BlockPos(nbt.getInt("TpX"), nbt.getInt("TpY"), nbt.getInt("TpZ")) : null;
    }

    private static StructurePlacementData placement(BlockRotation rotation) {
        return new StructurePlacementData().setRotation(rotation).setMirror(BlockMirror.NONE).setLiquidSettings(StructureLiquidSettings.IGNORE_WATERLOGGING)
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
