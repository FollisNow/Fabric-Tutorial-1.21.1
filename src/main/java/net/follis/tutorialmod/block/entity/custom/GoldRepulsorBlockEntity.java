package net.follis.tutorialmod.block.entity.custom;

import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldRepulsorBlockEntity extends BlockEntity {

    public GoldRepulsorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLD_REPULSOR_BE, pos, state);
    }

    public void tick(World world, BlockPos pos, BlockState state) {

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
