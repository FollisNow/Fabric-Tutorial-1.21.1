package net.follis.tutorialmod.block.entity.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.follis.tutorialmod.block.custom.GoldenHotelBlock;
import net.follis.tutorialmod.block.entity.ImplementedInventory;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.recipe.CustomRecipeManager;
import net.follis.tutorialmod.recipe.GoldenHotelRecipeBuilder;
import net.follis.tutorialmod.screen.custom.GoldenHotelScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GoldenHotelBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private float rotation = 0;
    private final List<Integer> offsets = List.of(-2, 2);
    public GoldenHotelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLDEN_HOTEL_BE, pos, state);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    public float getRenderingRotation() {
        rotation += 0.5f;
        if(rotation >= 360) {
            rotation = 0;
        }
        return rotation;
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if(!this.isEmpty()) {
            List<GoldenPedestalBlockEntity> pedestals = getPedestals(world, pos);
            if (pedestals.size() == 2) {
                boolean matchCount = false;
                GoldenHotelRecipeBuilder currentRecipe = getMatchingRecipe();
                if (currentRecipe != null){
                    if ((currentRecipe.getInput1().test(pedestals.get(0).getStack()) && currentRecipe.getInput2().test(pedestals.get(1).getStack())) ||
                            (currentRecipe.getInput1().test(pedestals.get(1).getStack()) && currentRecipe.getInput2().test(pedestals.get(0).getStack()))) {
                        matchCount = true;
                    }
                }
                if (matchCount) {
                    for (GoldenPedestalBlockEntity pedestal : pedestals) {
                        pedestal.removeStack(0);
                        pedestal.markDirty();
                    }
                    this.removeStack(0);
                    this.setStack(0, currentRecipe.getOutputItemStack());
                    this.markDirty();
                }
            }
        }
    }

    private GoldenHotelRecipeBuilder getMatchingRecipe() {
        List<GoldenHotelRecipeBuilder> recipes = CustomRecipeManager.getRecipes();
        for (GoldenHotelRecipeBuilder recipe : recipes) {
            if (recipe.getInputMain().test(this.getStack(0)))
                return recipe;
        }
        return null;
    }

    private List<GoldenPedestalBlockEntity> getPedestals(World world, BlockPos pos) {
        Direction facing = world.getBlockState(pos).get(GoldenHotelBlock.FACING);
        List<GoldenPedestalBlockEntity> pedestals = new ArrayList<>(List.of());
        offsets.forEach(offset -> {
            if (facing == Direction.NORTH || facing == Direction.SOUTH){
                if (world.getBlockEntity(pos.offset(Direction.EAST, offset)) instanceof GoldenPedestalBlockEntity pedestal) {
                    pedestals.add(pedestal);
                }
            }
            if (facing == Direction.EAST || facing == Direction.WEST){
                if (world.getBlockEntity(pos.offset(Direction.NORTH, offset)) instanceof GoldenPedestalBlockEntity pedestal) {
                    pedestals.add(pedestal);
                }
            }
        });
        return pedestals;
    }
    //so the hopper doesn't push in input if there's already an item
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return this.isEmpty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.getPos(), getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        this.getItems().clear();
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Golden Hotel");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new GoldenHotelScreenHandler(syncId, playerInventory, this.pos);
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
