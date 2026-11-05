package net.follis.goldenscarabmod.entity.ai;

import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Walks to the nearest dropped item accepted by {@code filter} and picks it up into the entity's
 * {@link InventoryOwner} inventory.
 *
 * Replaces MoveToTargetPosGoal + a per-block entity query (thousands of queries per scan) with a
 * single entity query, throttled to roughly once every {@value #SCAN_INTERVAL} ticks.
 */
public class MoveToItemGoal extends Goal {
    private static final int SCAN_INTERVAL = 10;
    private static final int GIVE_UP_TICKS = 200;
    /** Once the path is over, finish the last stretch by steering directly when closer than 4 blocks. */
    private static final double STEER_DISTANCE_SQ = 16.0;

    private final PathAwareEntity mob;
    private final InventoryOwner owner;
    private final double speed;
    private final double range;
    private final BooleanSupplier active;
    private final Predicate<ItemStack> filter;

    @Nullable
    private ItemEntity target;
    private int runningTicks;

    public <T extends PathAwareEntity & InventoryOwner> MoveToItemGoal(
            T entity, double speed, double range, BooleanSupplier active, Predicate<ItemStack> filter) {
        this.mob = entity;
        this.owner = entity;
        this.speed = speed;
        this.range = range;
        this.active = active;
        this.filter = filter;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (!this.active.getAsBoolean()) return false;
        if (this.mob.getRandom().nextInt(this.toGoalTicks(SCAN_INTERVAL)) != 0) return false;

        List<ItemEntity> items = this.mob.getWorld().getEntitiesByClass(
                ItemEntity.class,
                this.mob.getBoundingBox().expand(this.range, 3.0, this.range),
                item -> item.isAlive() && !item.cannotPickup() && this.filter.test(item.getStack()));

        this.target = items.stream()
                .min(Comparator.comparingDouble(item -> this.mob.squaredDistanceTo(item)))
                .orElse(null);
        return this.target != null;
    }

    @Override
    public boolean shouldContinue() {
        return this.target != null
                && this.target.isAlive()
                && this.runningTicks < GIVE_UP_TICKS
                && this.active.getAsBoolean()
                && this.filter.test(this.target.getStack()); // e.g. inventory got full meanwhile
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.runningTicks = 0;
        this.moveToTarget();
    }

    @Override
    public void tick() {
        if (this.target == null) return;
        this.runningTicks++;
        this.mob.getLookControl().lookAt(this.target, 30.0F, 30.0F);

        if (this.canReach(this.target)) {
            InventoryOwner.pickUpItem(this.mob, this.owner, this.target);
            this.target = null; // ends the goal
            return;
        }

        // Pathfinding often ends 1-2 blocks short of an item (it targets the block above it, the item may
        // sit on an edge / slab / carpet, ...). When the navigation has given up, walk straight at the item.
        if (this.mob.getNavigation().isIdle()) {
            double dx = this.target.getX() - this.mob.getX();
            double dz = this.target.getZ() - this.mob.getZ();
            if (dx * dx + dz * dz <= STEER_DISTANCE_SQ) {
                this.mob.getMoveControl().moveTo(
                        this.target.getX(), this.target.getY(), this.target.getZ(), this.speed);
            } else if (this.runningTicks % 20 == 0) {
                this.moveToTarget(); // far away: try to path again now and then
            }
        }
    }

    /** Vanilla reach (box +1 block horizontally) plus 1 block vertically, so items on a step above/below count. */
    private boolean canReach(ItemEntity item) {
        return this.mob.getBoundingBox().expand(1.0, 1.0, 1.0).intersects(item.getBoundingBox());
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    private void moveToTarget() {
        if (this.target != null) {
            this.mob.getNavigation().startMovingTo(this.target, this.speed);
        }
    }
}