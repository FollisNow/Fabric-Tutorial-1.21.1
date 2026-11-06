package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.item.ModItems;
import net.follis.goldenscarabmod.item.custom.CaddisflyCocoonItem;
import net.follis.goldenscarabmod.util.IBugVariants;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LarvaeEntity extends AnimalEntity implements IBugVariants {
    public static final float BASE_MOVEMENT_SPEED = 0.12F;
    public static final int COCOON_SLOT_COUNT = 4;

    /** Speed MULTIPLIER handed to the goals (it is applied on top of the movement speed attribute). */
    private static final double GOAL_SPEED = 1.0D;

    private static final int NO_MATERIAL = -1;
    private static final int PICKUP_INTERVAL = 100; // ~5 seconds between attempts
    private static final double PICKUP_RANGE = 1.5D;

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT =
            DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_0 =
            DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_1 =
            DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_2 =
            DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MATERIAL_SLOT_3 =
            DataTracker.registerData(LarvaeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final List<TrackedData<Integer>> MATERIAL_SLOTS =
            List.of(MATERIAL_SLOT_0, MATERIAL_SLOT_1, MATERIAL_SLOT_2, MATERIAL_SLOT_3);

    private static final Map<Item, CocoonMaterial> MATERIAL_ITEMS = Map.ofEntries(
            Map.entry(Items.DIAMOND, CocoonMaterial.DIAMOND),
            Map.entry(Items.GLOWSTONE_DUST, CocoonMaterial.GLOWSTONE),
            Map.entry(Items.LAPIS_LAZULI, CocoonMaterial.LAPIS),
            Map.entry(Items.EMERALD, CocoonMaterial.EMERALD),
            Map.entry(Items.IRON_NUGGET, CocoonMaterial.IRON),
            Map.entry(Items.REDSTONE, CocoonMaterial.REDSTONE),
            Map.entry(Items.OBSIDIAN, CocoonMaterial.OBSIDIAN),
            Map.entry(Items.NETHERITE_SCRAP, CocoonMaterial.NETHERITE),
            Map.entry(Items.PRISMARINE_SHARD, CocoonMaterial.PRISMARINE),
            Map.entry(Items.COPPER_INGOT, CocoonMaterial.COPPER),
            Map.entry(Items.ECHO_SHARD, CocoonMaterial.ECHO_SHARD)
    );

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    private int materialPickupCooldown = 0;

    // client-side cocoon trail
    private double prevTickX;
    private double prevTickZ;
    private boolean trailInitialized = false;
    private float prevCocoonTrailDistance = 0.0F;
    private float cocoonTrailDistance = 0.0F;

    public LarvaeEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    // =====================================================================
    // Goals / attributes
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(3, new AnimalMateGoal(this, GOAL_SPEED));
        this.goalSelector.add(4, new TemptGoal(this, GOAL_SPEED, this::foodSelector, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, GOAL_SPEED));
    }

    private boolean foodSelector(ItemStack stack) {
        return stack.isIn(ItemTags.BEE_FOOD);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return foodSelector(stack);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    // =====================================================================
    // Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
            this.tickCocoonTrail();
        } else {
            this.tickMaterialPickup();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    private void tickCocoonTrail() {
        if (!this.trailInitialized) {
            this.prevTickX = this.getX();
            this.prevTickZ = this.getZ();
            this.trailInitialized = true;
            return;
        }

        double dx = this.getX() - this.prevTickX;
        double dz = this.getZ() - this.prevTickZ;
        this.prevTickX = this.getX();
        this.prevTickZ = this.getZ();

        this.prevCocoonTrailDistance = this.cocoonTrailDistance;
        this.cocoonTrailDistance += (float) Math.sqrt(dx * dx + dz * dz);
        this.cocoonTrailDistance *= 0.80F;
    }

    public float getCocoonTrailOffset(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevCocoonTrailDistance, this.cocoonTrailDistance);
    }

    // =====================================================================
    // Cocoon materials
    // =====================================================================

    private void tickMaterialPickup() {
        if (this.materialPickupCooldown > 0) {
            this.materialPickupCooldown--;
            return;
        }
        this.materialPickupCooldown = PICKUP_INTERVAL;

        int emptySlot = this.findEmptyCocoonSlot();
        if (emptySlot == -1) return; // cocoon already full

        List<ItemEntity> nearbyItems = this.getWorld().getEntitiesByClass(ItemEntity.class,
                this.getBoundingBox().expand(PICKUP_RANGE),
                item -> !item.isRemoved() && MATERIAL_ITEMS.containsKey(item.getStack().getItem()));
        if (nearbyItems.isEmpty()) return;

        ItemEntity target = nearbyItems.getFirst();
        ItemStack stack = target.getStack();
        CocoonMaterial material = MATERIAL_ITEMS.get(stack.getItem());

        this.setCocoonSegment(emptySlot, material);

        // Don't mutate the stack in place: setStack() is what marks the tracked data dirty,
        // otherwise clients keep rendering the old stack size.
        if (stack.getCount() <= 1) {
            target.discard();
        } else {
            target.setStack(stack.copyWithCount(stack.getCount() - 1));
        }

        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.7F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
        this.playSound(SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, 0.7F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
    }

    private int findEmptyCocoonSlot() {
        for (int i = 0; i < MATERIAL_SLOTS.size(); i++) {
            if (this.dataTracker.get(MATERIAL_SLOTS.get(i)) == NO_MATERIAL) return i;
        }
        return -1;
    }

    /** Null entries are empty slots. */
    public List<CocoonMaterial> getCocoonSegments() {
        List<CocoonMaterial> result = new ArrayList<>(MATERIAL_SLOTS.size());
        for (TrackedData<Integer> slot : MATERIAL_SLOTS) {
            result.add(materialFromOrdinal(this.dataTracker.get(slot)));
        }
        return result;
    }

    public void setCocoonSegment(int index, @Nullable CocoonMaterial material) {
        this.dataTracker.set(MATERIAL_SLOTS.get(index), material == null ? NO_MATERIAL : material.ordinal());
    }

    @Nullable
    private static CocoonMaterial materialFromOrdinal(int ordinal) {
        CocoonMaterial[] all = CocoonMaterial.values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    @Nullable
    private static CocoonMaterial materialFromName(String name) {
        for (CocoonMaterial material : CocoonMaterial.values()) {
            if (material.name().equals(name)) return material;
        }
        return null;
    }

    public static CaddisflyCocoonItem.CocoonData randomCocoonData() {
        return randomCocoonData(COCOON_SLOT_COUNT, Random.create());
    }

    public static CaddisflyCocoonItem.CocoonData randomCocoonData(int slots) {
        return randomCocoonData(slots, Random.create());
    }

    public static CaddisflyCocoonItem.CocoonData randomCocoonData(int slots, Random random) {
        CaddisflyCocoonItem.CocoonData.Builder builder = CaddisflyCocoonItem.CocoonData.builder();
        int count = MathHelper.clamp(slots, 0, COCOON_SLOT_COUNT);
        CocoonMaterial[] materials = CocoonMaterial.values();
        for (int i = 0; i < count; i++) {
            builder.addMaterial(materials[random.nextInt(materials.length)]);
        }
        return builder.build();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (this.getWorld().isClient || this.isBaby()) return;

        List<CocoonMaterial> segments = this.getCocoonSegments();
        if (segments.stream().anyMatch(m -> m != null)) {
            ItemStack cocoonStack = new ItemStack(ModItems.CADDISFLY_COCOON);
            cocoonStack.set(ModDataComponentTypes.COCOON, CaddisflyCocoonItem.CocoonData.fromEntitySegments(segments));
            this.dropStack(cocoonStack);
        }
    }

    // =====================================================================
    // Breeding
    // =====================================================================

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        LarvaeEntity baby = ModEntities.LARVAE.create(world);
        if (baby != null) {
            baby.setVariant(this.getVariant());
        }
        return baby;
    }

    // =====================================================================
    // Variant / NBT
    // =====================================================================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        for (TrackedData<Integer> slot : MATERIAL_SLOTS) {
            builder.add(slot, NO_MATERIAL);
        }
    }

    public LarvaeVariant getVariant() {
        return LarvaeVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    public void setVariant(LarvaeVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    /**
     * Materials are saved by NAME so reordering/removing enum constants can't corrupt saves.
     * Empty slots are simply not written.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        List<CocoonMaterial> segments = this.getCocoonSegments();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i) != null) {
                nbt.putString("MaterialSlot" + i, segments.get(i).name());
            }
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        for (int i = 0; i < MATERIAL_SLOTS.size(); i++) {
            String key = "MaterialSlot" + i;
            CocoonMaterial material = null;
            if (nbt.contains(key, NbtElement.STRING_TYPE)) {
                material = materialFromName(nbt.getString(key));
            } else if (nbt.contains(key, NbtElement.INT_TYPE)) { // legacy saves (ordinal)
                material = materialFromOrdinal(nbt.getInt(key));
            }
            this.setCocoonSegment(i, material);
        }
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {
        this.setVariant(LarvaeVariant.REGULAR);

        if (spawnReason == SpawnReason.COMMAND || spawnReason == SpawnReason.SPAWN_EGG) {
            // 0 to COCOON_SLOT_COUNT - 1 random materials (never a full cocoon)
            List<CocoonMaterial> materials =
                    randomCocoonData(this.random.nextInt(COCOON_SLOT_COUNT), this.random).segments();
            for (int i = 0; i < materials.size(); i++) {
                this.setCocoonSegment(i, materials.get(i));
            }
        }
        return super.initialize(world, difficulty, spawnReason, entityData);
    }
}