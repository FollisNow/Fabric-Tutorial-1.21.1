package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.particle.ModParticles;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class GoldPannerEntity extends PathAwareEntity implements InventoryOwner{
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState pickupAnimationState = new AnimationState();
    public final AnimationState craftAnimationState = new AnimationState();
    private static final int idleCooldown = 40;
    private static final int pickupCooldown = 40;
    private static final int craftCooldown = 30;

    private int idleAnimationTimeout = 40;
    private int pickupAnimationTimeout = 40;
    private int craftAnimationTimeout = 40;
    private final SimpleInventory inventory = new SimpleInventory(1);

    private static final TrackedData<Integer> DATA_CURRENT_STATE =
            DataTracker.registerData(GoldPannerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public GoldPannerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setCanPickUpLoot(this.canPickUpLoot());
    }

    public enum States {
        IDLE,
        PICKUP,
        CRAFT
    }


    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MoveToTargetPosGoal(this, 1.0F, 12, 3) {
            @Override
            protected boolean isTargetPos(WorldView world, BlockPos pos) {
                if (world instanceof ServerWorld serverWorld) {
                    ItemStack itemStack = this.mob.getStackInHand(Hand.MAIN_HAND);
                    boolean canGather = !itemStack.isEmpty();

                    return canGather && findIfPosHasItem(serverWorld, pos, Items.RAW_GOLD);
                }
                return false;
            }
            @Override
            public boolean canStart() {
                return super.canStart() && GoldPannerEntity.this.getCurrentState() != States.CRAFT;
            }
        });

        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5F) {
            @Override
            public boolean canStart() {
                return super.canStart() && GoldPannerEntity.this.getCurrentState() != States.CRAFT;
            }
        });
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 2)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    private void setupStates() {
        ItemStack inventoryStack = this.getInventory().getStack(0);
        int pickAmount = 1;

        switch (this.getCurrentState()) {
            case IDLE -> {
                if (this.idleAnimationTimeout == 20) {
                    if (this.getWorld().isClient()) {
                        this.idleAnimationState.start(this.age);
                    }
                } else if (this.hasItemTemplate() && !(inventoryStack.isEmpty() || inventoryStack.getCount() < pickAmount) && this.idleAnimationTimeout <= 0) {
                    this.pickupAnimationTimeout = pickupCooldown;
                    this.setCurrentState(States.PICKUP);
                    this.idleAnimationTimeout = idleCooldown;
                } else if (this.idleAnimationTimeout <= 0) {
                    this.idleAnimationTimeout = idleCooldown;
                }
                --this.idleAnimationTimeout;
            }
            case PICKUP -> {
                if (this.pickupAnimationTimeout == 35) {
                    if (this.getWorld().isClient()) {
                        this.pickupAnimationState.start(this.age);
                    }
                } else if (this.pickupAnimationTimeout <= 0) {
                    this.craftAnimationTimeout = craftCooldown;
                    this.setCurrentState(States.CRAFT);
                    this.pickupAnimationTimeout = pickupCooldown;
                }
                this.pickupAnimationTimeout--;
            }
            case CRAFT -> {
                if ((inventoryStack.isEmpty() || inventoryStack.getCount() < pickAmount) && !this.getWorld().isClient()) {
                    this.idleAnimationTimeout = idleCooldown;
                    this.setCurrentState(States.IDLE);
                    this.craftAnimationTimeout = craftCooldown;
                } else if (this.craftAnimationTimeout == 20) {
                    if (this.getWorld().isClient()) {
                        this.craftAnimationState.start(this.age);
                    }
                } else if (this.craftAnimationTimeout <= 15 ) {
                    this.getInventory().removeStack(0, pickAmount);
                    int dropAmount = 2;

                    if (inventoryStack.isOf(Items.GOLD_NUGGET)) {
                        dropAmount = 18;
                    }

                    playThrowSound(this, this.getMainHandStack().copyWithCount(dropAmount), this.getPos().add(0.0, 2.0, 0.0));
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        addParticles(serverWorld);
                    }
                    this.craftAnimationTimeout = craftCooldown;
                }
                this.craftAnimationTimeout--;
            }
            case null, default -> {
                this.idleAnimationTimeout = 40;
                this.setCurrentState(States.IDLE);
            }
        }
    }

    public static void playThrowSound(LivingEntity entity, ItemStack stack, Vec3d target) {
        Vec3d vec3d = new Vec3d(0.2F, 0.3F, 0.2F);
        LookTargetUtil.give(entity, stack, target, vec3d, 0.2F);
        World world = entity.getWorld();
        world.playSoundFromEntity(null, entity, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.NEUTRAL, 0.8F, 1.5f);
    }

    private void addParticles(ServerWorld serverWorld) {
        serverWorld.spawnParticles(ModParticles.GOLDEN_LEAVES_PARTICLE,
                this.getPos().getX(), this.getPos().getY() + 1.1F, this.getPos().getZ(),
                20, 0.25F, 0.25F, 0.25F, 0.1F);
    }

    @Override
    public SimpleInventory getInventory() {
        return this.inventory;
    }
    @Override
    protected void loot(ItemEntity item) {
        InventoryOwner.pickUpItem(this, this, item);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        this.inventory.clearToList().forEach(this::dropStack);
        ItemStack itemStack = this.getEquippedStack(EquipmentSlot.MAINHAND);
        if (!itemStack.isEmpty() && !EnchantmentHelper.hasAnyEnchantmentsWith(itemStack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
            this.dropStack(itemStack);
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }

    }

    @Override
    public void tick() {
        super.tick();

        this.setupStates();
    }

    //GETTERS & SETTERS
    private void setCurrentState(States state) {
        this.dataTracker.set(DATA_CURRENT_STATE, state.ordinal());
    }
    private States getCurrentState() {
        return States.values()[this.dataTracker.get(DATA_CURRENT_STATE)];
    }

    public boolean hasItemTemplate() {
        ItemStack itemStack = this.getEquippedStack(EquipmentSlot.MAINHAND).copyWithCount(1);
        return !itemStack.isEmpty();
    }

    /* SOUNDS */
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

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_CURRENT_STATE, 0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeInventory(nbt, this.getRegistryManager());
        nbt.putInt("State", this.getCurrentState().ordinal());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(DATA_CURRENT_STATE, nbt.getInt("State"));
        this.readInventory(nbt, this.getRegistryManager());

        super.readCustomDataFromNbt(nbt);
    }

    @Override
    public boolean canGather(ItemStack stack) {
        ItemStack itemStack = this.getStackInHand(Hand.MAIN_HAND);
        return !itemStack.isEmpty() &&
                this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) &&
                this.inventory.canInsert(stack) &&
                !this.areItemsEqual(itemStack, stack) &&
                stack.isOf(Items.RAW_GOLD);
    }
    @Override
    public boolean canPickUpLoot() {
        return this.hasItemTemplate();
    }

    @Override
    public boolean canEquip(ItemStack stack) {
        return false;
    }

    private boolean areItemsEqual(ItemStack stack, ItemStack stack2) {
        return ItemStack.areItemsEqual(stack, stack2);
    }
    @Override
    public boolean areItemsDifferent(ItemStack stack, ItemStack stack2) {
        return !this.areItemsEqual(stack, stack2);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStackPlayer = player.getStackInHand(hand);
        ItemStack itemStackSelf = this.getStackInHand(Hand.MAIN_HAND);
         if (itemStackSelf.isEmpty() && !itemStackPlayer.isEmpty() && (itemStackPlayer.isOf(Items.GOLD_NUGGET) || itemStackPlayer.isOf(Items.GOLD_INGOT))) {
            ItemStack itemStack3 = itemStackPlayer.copyWithCount(1);
            this.setStackInHand(Hand.MAIN_HAND, itemStack3);
            this.decrementStackUnlessInCreative(player, itemStackPlayer);
            this.getWorld().playSoundFromEntity(player, this, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.NEUTRAL, 2.0F, 1.0F);
            return ActionResult.SUCCESS;
        } else if (!itemStackSelf.isEmpty() && hand == Hand.MAIN_HAND && itemStackPlayer.isEmpty()) {
             this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
             this.getWorld().playSoundFromEntity(player, this, SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.NEUTRAL, 2.0F, 1.0F);

             player.giveItemStack(itemStackSelf);
             return ActionResult.SUCCESS;
        } else {
            return super.interactMob(player, hand);
        }
    }
    private void decrementStackUnlessInCreative(PlayerEntity player, ItemStack stack) {
        stack.decrementUnlessCreative(1, player);
    }

    private boolean findIfPosHasItem(ServerWorld world, BlockPos pos, Item item) {

        // Define a predicate to filter item entities with tag
        Predicate<Entity> predicate = entity -> entity instanceof ItemEntity itemEntity && itemEntity.getStack().isOf(item) && !this.areItemsEqual(itemEntity.getStack(), this.getMainHandStack());

        // Get all ItemEntity instances within the specified radius
        for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, Box.from(Vec3d.ofCenter(pos)), predicate)) {
            if (itemEntity != null) {
                return true;
            }
        }
        return false; // Return the closest hostile entity found
    }
}


