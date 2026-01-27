package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.util.IBugVariants;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class LocustEntity extends AnimalEntity implements IBugVariants {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT = DataTracker.registerData(LocustEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private int ticksSinceHarvest;

    public LocustEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));

        this.goalSelector.add(1, new HarvestBlockGoal(this));

        this.goalSelector.add(3, new AnimalMateGoal(this, 1.15D));
        this.goalSelector.add(4, new TemptGoal(this, 1.25D, this::foodSelector, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
    }

    private boolean foodSelector(ItemStack stack) {
        if(this.getVariant() == LocustVariant.GOLD) {
            return stack.isIn(ModTags.Items.GOLDEN_VEGETAL_FOOD);
        } else {
            return stack.isIn(ItemTags.BEE_FOOD);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return foodSelector(stack);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 60;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        }
    }


    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        LocustEntity baby = ModEntities.LOCUST.create(world);
        if (baby != null && entity instanceof LocustEntity locust) {
            if (((this.getVariant() != LocustVariant.GOLD || locust.getVariant() != LocustVariant.GOLD) && random.nextInt(100) == 0) ||
                    (this.getVariant() == LocustVariant.GOLD && locust.getVariant() == LocustVariant.GOLD)) {
                baby.setVariant(LocustVariant.GOLD);
            } else {
                baby.setVariant(this.getVariant());
            }

            if (baby.getVariant() == LocustVariant.GOLD) {
                world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }
        return baby;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.isBaby()){
            switch(this.getVariant()) {
                case GOLD -> this.dropItem(ModItems.LOCUST_GOLD);
                case DREAM -> this.dropItem(ModItems.LOCUST_DREAM);
                case GRASSHOPPER -> this.dropItem(ModItems.LOCUST_GRASSHOPPER);
                case RED -> this.dropItem(ModItems.LOCUST_RED);
                default -> {
                }
            }
        }
    }

    @Override
    protected int getXpToDrop() {
        return this.random.nextInt(4) + 2;
    }

    /* SOUNDS */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND;
    }

    @Override
    public void playAmbientSound() {
        this.playSound(this.getAmbientSound(), 0.3F, 2F);
    }

    @Override
    protected void playHurtSound(DamageSource damageSource) {
        this.ambientSoundChance = -this.getMinAmbientSoundDelay();
        this.playSound(this.getHurtSound(damageSource), 0.6F, 2F);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
    }

    public LocustVariant getVariant() {
        return LocustVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    public void setVariant(LocustVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    public int getHarvestTicks() {
        return this.ticksSinceHarvest;
    }
    public void setHarvestTicks(int harvestTicks) {
        this.ticksSinceHarvest = harvestTicks;
    }

    @Override
    protected void mobTick() {
        this.ticksSinceHarvest--;
        super.mobTick();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        nbt.putInt("TicksSincePollination", this.ticksSinceHarvest);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        this.ticksSinceHarvest = nbt.getInt("TicksSincePollination");

    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {

        Optional<RegistryKey<Biome>> currentBiomeKey = world.getBiome(this.getBlockPos()).getKey();
        LocustVariant variant;
        if (this.random.nextInt(100) == 0) {
            variant = LocustVariant.GOLD;
        }
        else if (currentBiomeKey.isPresent() && biomeMap.containsKey(currentBiomeKey.get()) && this.random.nextFloat() < 0.8F){
            variant = biomeMap.get(currentBiomeKey.get());
        } else {
            variant = LocustVariant.byId(this.random.nextBetween(2, LocustVariant.values().length - 1));
        }
        this.setVariant(variant);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    private static final Map<RegistryKey<Biome>, LocustVariant> biomeMap = new HashMap<>() {{
        put(BiomeKeys.PLAINS, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.SUNFLOWER_PLAINS, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.FLOWER_FOREST, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.CHERRY_GROVE, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.FOREST, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.GROVE, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.MEADOW, LocustVariant.GRASSHOPPER);
        put(BiomeKeys.DESERT, LocustVariant.RED);
    }};


    class HarvestBlockGoal extends Goal {
        protected final LocustEntity locust;
        private int range;
        private BlockPos targetPos;
        private List<Block> harvestableBlocks;

        private static final List<Block> BASE_BLOCKS = List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS); // Inferior species blocks
        private static final Map<LocustVariant, List<Block>> harvestMap = new HashMap<>() {{
        put(LocustVariant.DREAM, new ArrayList<>(BASE_BLOCKS){{
            add(Blocks.WHEAT);
            add(Blocks.CARROTS);
            add(Blocks.POTATOES);
        }});
        put(LocustVariant.GRASSHOPPER, new ArrayList<>(BASE_BLOCKS){{
            add(Blocks.WHEAT);
            add(Blocks.CARROTS);
        }});
        put(LocustVariant.RED, new ArrayList<>(BASE_BLOCKS){{
            add(Blocks.BEETROOTS);
        }});
        }};

        public HarvestBlockGoal(LocustEntity locust) {
            this.locust = locust;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return LocustEntity.this.getHarvestTicks() <= 0 && LocustEntity.this.getRandom().nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinue() {
            return this.targetPos != null;
        }

        @Override
        public void start() {
            switch (LocustEntity.this.getVariant()) {
                case GOLD -> this.range = 32;
                case DREAM -> this.range = 16;
                default -> this.range = 8;
            }
            this.harvestableBlocks = harvestMap.get(LocustEntity.this.getVariant());
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            switch (LocustEntity.this.getVariant()) {
                case LocustVariant.GOLD -> searchRoutine(40, 800);
                case LocustVariant.DREAM -> searchRoutine(2400, 4800);
                default -> searchRoutine(4800, 9600);
            }

            if (this.targetPos != null) {
                this.locust.getNavigation().startMovingTo(this.targetPos.up().getX(), this.targetPos.up().getY(), this.targetPos.up().getZ(), 1.1F);

                // Check if the entity is close enough to harvest
                if (this.targetPos.isWithinDistance(this.locust.getBlockPos(), 2)) {
                    harvestBlock(this.targetPos);
                }
            }
        }

        private void searchRoutine(int minCooldown, int maxCooldown) {
            Predicate<BlockPos> condition = blockPos -> {
                BlockState state = this.locust.getWorld().getBlockState(blockPos);
                Block block = state.getBlock();
                if (LocustEntity.this.getVariant() == LocustVariant.GOLD) {
                    if (block instanceof CropBlock) {
                        return ((CropBlock) block).isMature(state);
                    }
                } else if (this.harvestableBlocks.contains(block)) {
                    // If it's a CropBlock, check if it's mature
                    if (block instanceof CropBlock) {
                        return ((CropBlock) block).isMature(state);
                    }
                    return true; // Non-CropBlock can be harvested
                }
                return false;
            };

            Optional<BlockPos> closestCropOrBlock = BlockPos.findClosest(this.locust.getBlockPos(), this.range, this.range / 2, condition);
            this.targetPos = closestCropOrBlock.orElse(null);
            if (this.targetPos != null){
                LocustEntity.this.setHarvestTicks(minCooldown);
            } else {
                LocustEntity.this.setHarvestTicks(maxCooldown);
            }

        }

        private void harvestBlock(BlockPos pos) {
            this.locust.getWorld().breakBlock(pos, true, this.locust);
            this.targetPos = null;
        }
    }
}
