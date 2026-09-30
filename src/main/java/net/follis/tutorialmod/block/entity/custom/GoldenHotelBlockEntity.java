package net.follis.tutorialmod.block.entity.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.follis.tutorialmod.block.custom.GoldenHotelBlock;
import net.follis.tutorialmod.block.entity.ImplementedInventory;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem;
import net.follis.tutorialmod.item.custom.BugItem;
import net.follis.tutorialmod.recipe.*;
import net.follis.tutorialmod.screen.custom.GoldenHotelScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoldenHotelBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private float rotation = 0;
    private final List<Integer> offsets = List.of(-2, 2);
    private static final int RITUAL_DURATION = 60; // 3 seconds
    private static final int TRAIL_INTERVAL = 4; // spawn a trail pulse every 4 ticks

    private int ritualProgress = 0;
    private boolean ritualActive = false;

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
        if (this.isEmpty()) {
            cancelRitual();
            return;
        }

        List<GoldenPedestalBlockEntity> pedestals = getPedestals(world, pos);
        if (pedestals.size() != 2) {
            cancelRitual();
            return;
        }

        ItemStack catalystStack = this.getStack(0);
        ItemStack p1 = pedestals.get(0).getStack();
        ItemStack p2 = pedestals.get(1).getStack();
        GoldenHotelRecipeInput input = new GoldenHotelRecipeInput(catalystStack, p1, p2);

        boolean isRevival = catalystStack.getItem() instanceof BugItem
                && catalystStack.get(ModDataComponentTypes.CAPTURED_BUG) != null;

        boolean matches = isRevival
                ? world.getRecipeManager().getFirstMatch(ModRecipes.GOLDEN_HOTEL_REVIVAL_TYPE, input, world).isPresent()
                : world.getRecipeManager().getFirstMatch(ModRecipes.GOLDEN_HOTEL_TYPE, input, world).isPresent();

        if (!matches) {
            cancelRitual();
            return;
        }

        if (!ritualActive) {
            ritualActive = true;
            ritualProgress = 0;
        }

        if (world instanceof ServerWorld serverWorld) {
//            spawnTrailPulse(serverWorld, pedestals.get(0), (float) ritualProgress / RITUAL_DURATION);
//            spawnTrailPulse(serverWorld, pedestals.get(1), (float) ritualProgress / RITUAL_DURATION);

            spawnLinePulse(serverWorld, pedestals.get(0));
            spawnLinePulse(serverWorld, pedestals.get(1));
        }

        ritualProgress++;

        if (ritualProgress >= RITUAL_DURATION) {
            completeRitual(world, pos, pedestals, isRevival);
        }
    }

    private void cancelRitual() {
        ritualActive = false;
        ritualProgress = 0;
    }

    private void spawnLinePulse(ServerWorld world, GoldenPedestalBlockEntity pedestal) {
        ItemStack stack = pedestal.getStack();
        if (stack.isEmpty()) return;

        Vec3d start = pedestal.getPos().toCenterPos().add(0, 0.5, 0);
        Vec3d end = this.getPos().toCenterPos().add(0, 0.2, 0);
        Vec3d diff = end.subtract(start).normalize();

        world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
                start.x, start.y, start.z, 0, diff.x, diff.y, diff.z, 0.4f);
    }

    private void completeRitual(World world, BlockPos pos, List<GoldenPedestalBlockEntity> pedestals, boolean isRevival) {
        ItemStack catalystStack = this.getStack(0);
        ItemStack p1 = pedestals.get(0).getStack();
        ItemStack p2 = pedestals.get(1).getStack();
        GoldenHotelRecipeInput input = new GoldenHotelRecipeInput(catalystStack, p1, p2);

        if (isRevival && world instanceof ServerWorld serverWorld) {
            AbstractEntityJarItem.BugData bugData = catalystStack.get(ModDataComponentTypes.CAPTURED_BUG);
            Entity entity = bugData != null ? bugData.loadEntity(serverWorld) : null;
            if (entity != null) {
                entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0F, 0.0F);
                serverWorld.spawnEntity(entity);
                consumeIngredients(pedestals);
                serverWorld.spawnParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            Optional<RecipeEntry<GoldenHotelRecipe>> recipe = world.getRecipeManager().getFirstMatch(ModRecipes.GOLDEN_HOTEL_TYPE, input, world);
            if (recipe.isPresent()) {
                consumeIngredients(pedestals);
                this.setStack(0, recipe.get().value().craft(input, null));
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.02);
                    serverWorld.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.7F, 1.4F);
                }
            }
        }

        cancelRitual();
    }

    private void consumeIngredients(List<GoldenPedestalBlockEntity> pedestals) {
        for (GoldenPedestalBlockEntity pedestal : pedestals) {
            pedestal.removeStack(0);
            pedestal.markDirty();
        }
        this.removeStack(0);
        this.markDirty();
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
