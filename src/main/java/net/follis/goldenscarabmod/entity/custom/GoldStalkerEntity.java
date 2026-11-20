package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.util.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gold Stalker: walks to dropped gold nuggets, gold ingots and gold-like blocks, picks them up into its own
 * inventory and, once there is nothing left to collect nearby (or it is full), carries them to the closest chest.
 *
 * <ul>
 *   <li>Server only logic, in two goals: PickUpGoldGoal and DepositGoal.</li>
 *   <li>It only MOVES gold around, it never creates any, so it does drop what it carries when it dies.</li>
 *   <li>Chests that are full (or unreachable) are skipped for a while, so it moves on to the next closest one.</li>
 * </ul>
 */
public class GoldStalkerEntity extends PathAwareEntity implements InventoryOwner {

    // =====================================================================
    // Tuning
    // =====================================================================
    private static final int INVENTORY_SIZE = 9;

    private static final double PICKUP_RANGE = 12.0D;                       // blocks around it to look for gold
    /** The item counts as reached when its hitbox touches the golem's hitbox grown by this much (like vanilla's loot box). */
    private static final double PICKUP_REACH_HORIZONTAL = 1.0D;
    private static final double PICKUP_REACH_VERTICAL = 1.0D;
    /** Closer than this (squared) and the pathfinder has stopped: walk straight at the item instead. */
    private static final double NUDGE_DISTANCE_SQ = 4.0D * 4.0D;
    private static final int STUCK_CHECK_TICKS = 20;
    private static final int STUCK_LIMIT = 3;            // this many checks without getting closer = give up on the item
    private static final double MIN_PROGRESS_SQ = 0.04D;
    private static final int PICKUP_GIVE_UP_TICKS = 200;                    // can't reach an item for this long: ignore it
    private static final int IGNORE_ITEM_TICKS = 600;

    private static final int CHEST_RANGE_HORIZONTAL = 16;
    private static final int CHEST_RANGE_VERTICAL = 6;
    private static final double CHEST_REACH_SQ = 2.5D * 2.5D;
    private static final int DEPOSIT_GIVE_UP_TICKS = 400;
    private static final int SKIP_CHEST_TICKS = 1200;                       // full / unreachable chests are skipped this long
    private static final int NO_CHEST_RETRY_TICKS = 100;
    private static final int CHEST_OPEN_TICKS = 20;                         // how long the lid stays open after a deposit

    private static final int SCAN_INTERVAL_TICKS = 10;

    // client animation lengths (ticks), used to stop the one-shot animations
    private static final int PICKUP_ANIM_TICKS = 10;
    private static final int DEPOSIT_ANIM_TICKS = 15;

    private static final byte STATUS_PICKUP = 110;
    private static final byte STATUS_DEPOSIT = 111;

    // ---- client animation states (read by the model once it exists) ----
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState pickupAnimationState = new AnimationState();
    public final AnimationState depositAnimationState = new AnimationState();
    private int pickupStopTick;
    private int depositStopTick;

    private final SimpleInventory inventory = new SimpleInventory(INVENTORY_SIZE);

    // ---- server state ----
    private final Map<Integer, Integer> ignoredItems = new HashMap<>();   // item entity id -> age until which it is ignored
    private final Map<BlockPos, Integer> skippedChests = new HashMap<>(); // chest pos -> age until which it is skipped
    @Nullable private ItemEntity cachedTarget;
    private int nextScanAge;
    private int nextDepositAge;
    /** Chest whose lid we opened for the animation, and when to close it again. */
    @Nullable private BlockPos openChest;
    private int closeChestAtAge;

    public GoldStalkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent(); // carries player items: never despawn
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AbstractGoldWorkerEntity.createWorkerAttributes();
    }

    // =====================================================================
    // What it collects
    // =====================================================================

    public static boolean isGold(ItemStack stack) {
        return stack.isOf(Items.GOLD_NUGGET)
                || stack.isOf(Items.GOLD_INGOT)
                || stack.isIn(ModTags.Items.GOLD_BLOCKS);
    }

    /** Used by InventoryOwner.pickUpItem. */
    @Override
    public boolean canGather(ItemStack stack) {
        return isGold(stack);
    }

    @Override
    public SimpleInventory getInventory() {
        return this.inventory;
    }

    // =====================================================================
    // Goals
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new DepositGoal());
        this.goalSelector.add(3, new PickUpGoldGoal());
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    // ---------------- finding things ----------------

    private boolean canTake(ItemEntity item) {
        if (item.isRemoved() || !item.isAlive()) return false;
        ItemStack stack = item.getStack();
        if (!isGold(stack) || !this.inventory.canInsert(stack)) return false;
        Integer until = this.ignoredItems.get(item.getId());
        return until == null || until <= this.age;
    }

    /** Nearest dropped gold item it can carry. Rescans at most every SCAN_INTERVAL_TICKS, in between it reuses the last result. */
    @Nullable
    private ItemEntity findCollectable() {
        if (this.age < this.nextScanAge) {
            return this.cachedTarget != null && this.canTake(this.cachedTarget) ? this.cachedTarget : null;
        }
        this.nextScanAge = this.age + SCAN_INTERVAL_TICKS;
        this.ignoredItems.values().removeIf(until -> until <= this.age);

        Box area = this.getBoundingBox().expand(PICKUP_RANGE, PICKUP_RANGE / 2.0, PICKUP_RANGE);
        List<ItemEntity> items = this.getWorld().getEntitiesByClass(ItemEntity.class, area, this::canTake);

        ItemEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double distance = this.squaredDistanceTo(item);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = item;
            }
        }
        this.cachedTarget = best;
        return best;
    }

    @Nullable
    private BlockPos findClosestChest() {
        World world = this.getWorld();
        this.skippedChests.values().removeIf(until -> until <= this.age);
        return BlockPos.findClosest(this.getBlockPos(), CHEST_RANGE_HORIZONTAL, CHEST_RANGE_VERTICAL,
                        pos -> !this.skippedChests.containsKey(pos) && world.getBlockState(pos).getBlock() instanceof ChestBlock)
                .map(BlockPos::toImmutable) // findClosest hands out a mutable position
                .orElse(null);
    }

    private void skipChest(BlockPos chest) {
        this.skippedChests.put(chest, this.age + SKIP_CHEST_TICKS);
    }

    /**
     * Hitbox contact instead of "distance between the two positions": an item lying in a corner, against a wall or on
     * a ledge can be well within arm's reach while the golem's center can never get closer than ~1.5 blocks to it.
     */
    private boolean isWithinPickupReach(ItemEntity item) {
        return this.getBoundingBox()
                .expand(PICKUP_REACH_HORIZONTAL, PICKUP_REACH_VERTICAL, PICKUP_REACH_HORIZONTAL)
                .intersects(item.getBoundingBox());
    }

    // ---------------- picking up ----------------

    class PickUpGoldGoal extends Goal {
        @Nullable private ItemEntity target;
        private int giveUpTicks;
        private double bestDistanceSq;
        private int stalledChecks;

        PickUpGoldGoal() {
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            this.target = GoldStalkerEntity.this.findCollectable();
            return this.target != null;
        }

        @Override
        public boolean shouldContinue() {
            return this.target != null && this.giveUpTicks > 0 && GoldStalkerEntity.this.canTake(this.target);
        }

        @Override
        public void start() {
            this.giveUpTicks = PICKUP_GIVE_UP_TICKS;
            this.stalledChecks = 0;
            this.bestDistanceSq = this.target == null ? 0.0D : GoldStalkerEntity.this.squaredDistanceTo(this.target);
            GoldStalkerEntity.this.getNavigation().startMovingTo(this.target, 1.0D);
        }

        @Override
        public void tick() {
            if (this.target == null) return;
            this.giveUpTicks--;

            GoldStalkerEntity stalker = GoldStalkerEntity.this;
            if (stalker.isWithinPickupReach(this.target)) {
                InventoryOwner.pickUpItem(stalker, stalker, this.target);
                stalker.getWorld().sendEntityStatus(stalker, STATUS_PICKUP);
                this.target = null; // the goal ends; the next one starts right away if there is more gold
                return;
            }

            if (stalker.age % 10 == 0) {
                stalker.getNavigation().startMovingTo(this.target, 1.0D); // items can be pushed around: re-path now and then
            }
            // The pathfinder often stops short (or finds no path at all) for an item in a corner or on a ledge.
            // Once it has stopped and we are close, just walk straight at the item; MoveControl handles small steps.
            if (stalker.getNavigation().isIdle() && stalker.squaredDistanceTo(this.target) <= NUDGE_DISTANCE_SQ) {
                stalker.getMoveControl().moveTo(this.target.getX(), this.target.getY(), this.target.getZ(), 1.0D);
            }

            // Not getting any closer for a while: give up on this item now instead of after the full timeout.
            if (stalker.age % STUCK_CHECK_TICKS == 0) {
                double distance = stalker.squaredDistanceTo(this.target);
                if (distance < this.bestDistanceSq - MIN_PROGRESS_SQ) {
                    this.bestDistanceSq = distance;
                    this.stalledChecks = 0;
                } else if (++this.stalledChecks >= STUCK_LIMIT) {
                    this.giveUpTicks = 0; // shouldContinue() turns false and stop() ignores the item for a while
                }
            }
        }

        @Override
        public void stop() {
            GoldStalkerEntity stalker = GoldStalkerEntity.this;
            if (this.target != null && this.giveUpTicks <= 0) {
                stalker.ignoredItems.put(this.target.getId(), stalker.age + IGNORE_ITEM_TICKS); // unreachable
            }
            stalker.cachedTarget = null;
            stalker.getNavigation().stop();
            this.target = null;
        }
    }

    // ---------------- chest lid animation ----------------

    /**
     * Chest lids are animated by a synced block event (type 1, data = number of viewers). A golem is not a "viewer"
     * in vanilla's sense (that needs a player with the screen open), so we send the event ourselves.
     */
    private void openChestFor(BlockPos pos) {
        if (this.openChest != null && !this.openChest.equals(pos)) {
            this.closeChest(); // moved on to another chest
        }
        if (this.openChest == null) {
            this.setChestLid(pos, true);
        }
        this.openChest = pos;
        this.closeChestAtAge = this.age + CHEST_OPEN_TICKS;
    }

    private void closeChest() {
        if (this.openChest != null) {
            this.setChestLid(this.openChest, false);
            this.openChest = null;
        }
    }

    private void setChestLid(BlockPos pos, boolean open) {
        World world = this.getWorld();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) return;

        this.sendLidEvent(world, pos, chestBlock, open);
        if (state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.offset(ChestBlock.getFacing(state)); // the other half of a double chest
            if (world.getBlockState(other).isOf(chestBlock)) {
                this.sendLidEvent(world, other, chestBlock, open);
            }
        }
        world.playSound(null, pos, open ? SoundEvents.BLOCK_CHEST_OPEN : SoundEvents.BLOCK_CHEST_CLOSE,
                SoundCategory.BLOCKS, 0.5F, this.random.nextFloat() * 0.1F + 0.9F);
    }

    private void sendLidEvent(World world, BlockPos pos, ChestBlock block, boolean open) {
        // if a player really has the chest open, never close the lid under them
        int viewers = ChestBlockEntity.getPlayersLookingInChestCount(world, pos);
        world.addSyncedBlockEvent(pos, block, 1, open ? Math.max(1, viewers) : viewers);
    }

    // ---------------- depositing ----------------

    class DepositGoal extends Goal {
        @Nullable private BlockPos chest;
        private int giveUpTicks;

        DepositGoal() {
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            GoldStalkerEntity stalker = GoldStalkerEntity.this;
            if (stalker.inventory.isEmpty() || stalker.age < stalker.nextDepositAge) return false;
            // keep collecting while there is still something it can pick up
            if (stalker.findCollectable() != null) return false;

            this.chest = stalker.findClosestChest();
            if (this.chest == null) {
                stalker.nextDepositAge = stalker.age + NO_CHEST_RETRY_TICKS;
                return false;
            }
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return this.chest != null && this.giveUpTicks > 0 && !GoldStalkerEntity.this.inventory.isEmpty();
        }

        @Override
        public void start() {
            this.giveUpTicks = DEPOSIT_GIVE_UP_TICKS;
            this.moveToChest();
        }

        private void moveToChest() {
            if (this.chest == null) return;
            GoldStalkerEntity.this.getNavigation().startMovingTo(
                    this.chest.getX() + 0.5, this.chest.getY(), this.chest.getZ() + 0.5, 1.0D);
        }

        @Override
        public void tick() {
            if (this.chest == null) return;
            this.giveUpTicks--;

            GoldStalkerEntity stalker = GoldStalkerEntity.this;
            if (stalker.age % 10 == 0) {
                this.moveToChest();
            }
            if (stalker.squaredDistanceTo(Vec3d.ofCenter(this.chest)) <= CHEST_REACH_SQ) {
                this.deposit(this.chest);
                this.chest = null; // ends the goal
            }
        }

        /** Same transfer a hopper uses: handles double chests and stacking, and marks the chest dirty. */
        private void deposit(BlockPos pos) {
            GoldStalkerEntity stalker = GoldStalkerEntity.this;
            Inventory target = HopperBlockEntity.getInventoryAt(stalker.getWorld(), pos);
            if (target == null) {
                stalker.skipChest(pos);
                return;
            }

            boolean movedAny = false;
            for (int slot = 0; slot < stalker.inventory.size(); slot++) {
                ItemStack stack = stalker.inventory.getStack(slot);
                if (stack.isEmpty()) continue;
                ItemStack remainder = HopperBlockEntity.transfer(null, target, stack.copy(), null);
                if (remainder.getCount() != stack.getCount()) movedAny = true;
                stalker.inventory.setStack(slot, remainder);
            }

            if (movedAny) {
                stalker.openChestFor(pos); // lid opens now, closes CHEST_OPEN_TICKS later
                stalker.getWorld().sendEntityStatus(stalker, STATUS_DEPOSIT);
            }
            if (!stalker.inventory.isEmpty()) {
                stalker.skipChest(pos); // it is full: the next attempt goes to the next closest chest
            }
        }

        @Override
        public void stop() {
            GoldStalkerEntity stalker = GoldStalkerEntity.this;
            if (this.chest != null && this.giveUpTicks <= 0) {
                stalker.skipChest(this.chest); // couldn't get there
            }
            stalker.getNavigation().stop();
            this.chest = null;
        }
    }

    // =====================================================================
    // Client animations
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            this.idleAnimationState.startIfNotRunning(this.age);
            if (this.pickupStopTick > 0 && this.age >= this.pickupStopTick) {
                this.pickupAnimationState.stop();
                this.pickupStopTick = 0;
            }
            if (this.depositStopTick > 0 && this.age >= this.depositStopTick) {
                this.depositAnimationState.stop();
                this.depositStopTick = 0;
            }
        } else if (this.openChest != null && this.age >= this.closeChestAtAge) {
            this.closeChest();
        }
    }

    /** Never leave a lid stuck open if the golem dies or its chunk unloads while a chest is open. */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.getWorld().isClient) {
            this.closeChest();
        }
        super.remove(reason);
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case STATUS_PICKUP -> {
                this.pickupAnimationState.start(this.age);
                this.pickupStopTick = this.age + PICKUP_ANIM_TICKS;
            }
            case STATUS_DEPOSIT -> {
                this.depositAnimationState.start(this.age);
                this.depositStopTick = this.age + DEPOSIT_ANIM_TICKS;
            }
            default -> super.handleStatus(status);
        }
    }

    // =====================================================================
    // Drops / NBT / sounds
    // =====================================================================

    /** It only carries gold that already existed, so unlike the breeder it gives it back when it dies. */
    @Override
    protected void dropInventory() {
        super.dropInventory();
        this.inventory.clearToList().forEach(this::dropStack);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeInventory(nbt, this.getRegistryManager());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.readInventory(nbt, this.getRegistryManager());
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PARROT_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ALLAY_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PANDA_DEATH;
    }
}