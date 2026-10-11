package net.follis.tutorialmod.block.entity.custom;

import net.follis.tutorialmod.block.custom.GoldRepulsorBlock;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldRepulsorBlockEntity extends BlockEntity {
    private static final int RANGE = 6;                 // blocks in front of the block
    private static final double ACCELERATION = 0.08;    // added per tick
    private static final double MAX_SPEED = 0.6;        // cap along the push direction
    private static final int PARTICLE_INTERVAL = 4;     // ticks between particles per entity

    public GoldRepulsorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLD_REPULSOR_BE, pos, state);
    }
    private static final double PLAYER_BOOST = 2.5; // players lose speed to ground friction, so they need a stronger push

    public void tick(World world, BlockPos pos, BlockState state) {
        Direction facing = state.get(GoldRepulsorBlock.FACING);
        boolean inversed = state.get(GoldRepulsorBlock.INVERSED);

        int reach = getReach(world, pos, facing);
        if (reach <= 0) return;

        Box zone = new Box(pos.offset(facing)).stretch(
                facing.getOffsetX() * (reach - 1),
                facing.getOffsetY() * (reach - 1),
                facing.getOffsetZ() * (reach - 1));

        Vec3d pushDir = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
        if (inversed) pushDir = pushDir.negate();

        boolean vertical = facing.getAxis() == Direction.Axis.Y;
        double baseAccel = vertical ? ACCELERATION * 2 : ACCELERATION;

        // Client: players move client-side, so the push must be applied here.
        // Remote players on this client are just interpolated, so pushing them is harmless.
        if (world.isClient) {
            for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, zone, p -> !p.isSpectator())) {
                push(player, pushDir, baseAccel * PLAYER_BOOST);
            }
            return;
        }

        // Server: pushes everything except players, and handles particles and fall damage.
        if (!(world instanceof ServerWorld serverWorld)) return;
        boolean emitParticles = world.getTime() % PARTICLE_INTERVAL == 0;

        for (Entity entity : world.getEntitiesByClass(Entity.class, zone, e -> !e.isSpectator() && !e.isRemoved())) {
            if (!(entity instanceof PlayerEntity)) {
                push(entity, pushDir, baseAccel);
            }
            if (vertical) entity.fallDistance = 0; // fall damage is tracked server-side, even for players

            if (emitParticles) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        entity.getX(), entity.getBodyY(0.5), entity.getZ(),
                        0, pushDir.x, pushDir.y, pushDir.z, 0.1);
            }
        }
    }

    private static void push(Entity entity, Vec3d pushDir, double accel) {
        double along = entity.getVelocity().dotProduct(pushDir);
        if (along >= MAX_SPEED) return;
        entity.addVelocity(pushDir.multiply(Math.min(accel, MAX_SPEED - along)));
    }

    // stops the push zone at the first solid block, so nothing gets pushed through walls
    private static int getReach(World world, BlockPos pos, Direction facing) {
        for (int i = 1; i <= RANGE; i++) {
            BlockPos check = pos.offset(facing, i);
            if (!world.getBlockState(check).getCollisionShape(world, check).isEmpty()) {
                return i - 1;
            }
        }
        return RANGE;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.getPos(), getCachedState(), getCachedState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}