package net.follis.goldenscarabmod.block.entity.custom;

import net.follis.goldenscarabmod.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrownBlockEntity extends BlockEntity {
    /** How long a player must stand on the crown before being teleported (30 ticks = 1.5 seconds). */
    private static final int CHARGE_TICKS = 30;

    @Nullable
    private BlockPos target;
    private int charge;

    public CrownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CROWN_BE, pos, state);
    }

    public void setTarget(@Nullable BlockPos target) {
        this.target = target;
        markDirty();
    }

    @Nullable
    public BlockPos getTarget() {
        return target;
    }

    public static void tick(World world, BlockPos pos, BlockState state, CrownBlockEntity crown) {
        if (!(world instanceof ServerWorld serverWorld) || crown.target == null) return;

        // players standing on top of the block
        List<ServerPlayerEntity> players = serverWorld.getEntitiesByClass(ServerPlayerEntity.class,
                new Box(pos.up()), p -> p.isAlive() && !p.isSpectator());
        if (players.isEmpty()) {
            crown.charge = 0;
            return;
        }

        crown.charge++;
        if (crown.charge == 1) {
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.8F, 1.2F);
        }
        if (crown.charge % 4 == 0) {
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 3, 0.3, 0.4, 0.3, 0.02);
        }

        if (crown.charge >= CHARGE_TICKS) {
            BlockPos landing = safeSpot(serverWorld, crown.target);
            for (ServerPlayerEntity player : players) {
                player.requestTeleport(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5);
                serverWorld.playSound(null, landing, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
            serverWorld.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            crown.charge = 0;
        }
    }

    /** Moves up (at most 8 blocks) until there is room for a player, in case the destination is inside a block. */
    private static BlockPos safeSpot(ServerWorld world, BlockPos target) {
        BlockPos p = target;
        for (int i = 0; i < 8; i++) {
            boolean feetFree = world.getBlockState(p).getCollisionShape(world, p).isEmpty();
            boolean headFree = world.getBlockState(p.up()).getCollisionShape(world, p.up()).isEmpty();
            if (feetFree && headFree) return p;
            p = p.up();
        }
        return target;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (target != null) nbt.putIntArray("Target", new int[]{target.getX(), target.getY(), target.getZ()});
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        int[] t = nbt.getIntArray("Target");
        target = t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
    }
}