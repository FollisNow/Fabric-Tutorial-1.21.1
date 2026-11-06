package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.StemBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BeetleEntity extends AngerableBugEntity implements Flutterer {
    private static final int WING_FLAP_INTERVAL = MathHelper.ceil(1.4959966F);

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT =
            DataTracker.registerData(BeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Raw registry id of the status effect this beetle carries, or -1 for none. */
    private static final TrackedData<Integer> POTION_GENE =
            DataTracker.registerData(BeetleEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final Map<RegistryKey<Biome>, BeetleVariant> BIOME_VARIANTS = Map.ofEntries(
            Map.entry(BiomeKeys.PLAINS, BeetleVariant.ROSE_CHAFER),
            Map.entry(BiomeKeys.SUNFLOWER_PLAINS, BeetleVariant.LADYBUG),
            Map.entry(BiomeKeys.FLOWER_FOREST, BeetleVariant.LADYBUG),
            Map.entry(BiomeKeys.CHERRY_GROVE, BeetleVariant.LADYBUG),
            Map.entry(BiomeKeys.FOREST, BeetleVariant.BARK),
            Map.entry(BiomeKeys.DARK_FOREST, BeetleVariant.RHINOCEROS),
            Map.entry(BiomeKeys.MUSHROOM_FIELDS, BeetleVariant.OMEN),
            Map.entry(BiomeKeys.GROVE, BeetleVariant.BARK),
            Map.entry(BiomeKeys.MEADOW, BeetleVariant.ROSE_CHAFER)
    );

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    private int particleTickCounter = 0;

    public BeetleEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 20, true);
    }

    // =====================================================================
    // Goals / attributes
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BugMeleeGoal(this, 1.4F, true)); // gated by canFight() (OMEN only)

        this.goalSelector.add(1, new AnimalMateGoal(this, 1.15D));
        this.goalSelector.add(2, new TemptGoal(this, 1.25D, this::foodSelector, false));

        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(6, new SwimGoal(this));
        this.goalSelector.add(7, new GrowCropsGoal());

        this.addCombatTargetGoals();
    }

    /** Only OMEN beetles are aggressive. */
    @Override
    public boolean canFight() {
        return this.getVariant() == BeetleVariant.OMEN;
    }

    private boolean foodSelector(ItemStack stack) {
        if (this.getVariant() == BeetleVariant.OMEN) {
            return stack.isIn(ModTags.Items.GOLDEN_FOOD) || stack.isIn(ItemTags.MEAT);
        }
        return stack.isIn(ItemTags.BEE_FOOD);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return foodSelector(stack);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 7.0F)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.65F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0F);
    }

    // =====================================================================
    // Breeding
    // =====================================================================

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        BeetleEntity baby = ModEntities.BEETLE.create(world);
        if (baby == null || !(entity instanceof BeetleEntity other)) {
            return baby;
        }

        BeetleVariant mine = this.getVariant();
        BeetleVariant theirs = other.getVariant();
        BeetleVariant result;
        if (mine == BeetleVariant.OMEN && theirs == BeetleVariant.OMEN) {
            result = BeetleVariant.OMEN;
        } else if (this.random.nextInt(100) == 0) {
            result = BeetleVariant.OMEN; // rare mutation
        } else {
            result = this.random.nextBoolean() ? mine : theirs;
        }
        baby.setVariant(result);

        if (result == BeetleVariant.OMEN) {
            StatusEffect myGene = this.getPotionGene();
            if (myGene != null && myGene == other.getPotionGene()) {
                baby.setPotionGene(myGene);
                world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            } else if (this.random.nextFloat() <= 0.33F) {
                List<RegistryEntry<StatusEffect>> shared = new ArrayList<>();
                for (RegistryEntry<StatusEffect> effect : this.getActiveStatusEffects().keySet()) {
                    if (other.getActiveStatusEffects().containsKey(effect)) {
                        shared.add(effect);
                    }
                }
                if (!shared.isEmpty()) {
                    baby.setPotionGene(shared.get(this.random.nextInt(shared.size())).value());
                    world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                }
            }
        }
        return baby;
    }

    // =====================================================================
    // Potion gene
    // =====================================================================

    @Nullable
    public StatusEffect getPotionGene() {
        int rawId = this.dataTracker.get(POTION_GENE);
        return rawId < 0 ? null : Registries.STATUS_EFFECT.get(rawId);
    }

    public void setPotionGene(@Nullable StatusEffect potion) {
        this.dataTracker.set(POTION_GENE, potion == null ? -1 : Registries.STATUS_EFFECT.getRawId(potion));
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        return super.canHaveStatusEffect(effect) && this.getPotionGene() == null;
    }

    private void summonPotionParticle() {
        StatusEffect effect = this.getPotionGene();
        if (effect == null) return;

        int color = effect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        this.getWorld().addParticle(EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, r, g, b),
                this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5), r, g, b);
    }

    // =====================================================================
    // Tick / combat
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(isNavigating());

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
            if (++this.particleTickCounter >= 10) {
                this.particleTickCounter = 0;
                this.summonPotionParticle();
            }
        }
    }

    /** Vanilla melee hit (attack damage attribute, knockback, ...) plus the carried effect. */
    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        StatusEffect gene = this.getPotionGene();
        if (hit && gene != null && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(gene), 5 * 20), this);
        }
        return hit;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker != null && !(attacker instanceof PlayerEntity) && !(attacker instanceof PersistentProjectileEntity)) {
            amount = (amount + 1.0F) / 2.0F;
        }
        return super.damage(source, amount);
    }

    // =====================================================================
    // Variant / NBT
    // =====================================================================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(POTION_GENE, -1);
    }

    public BeetleVariant getVariant() {
        return BeetleVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariant(BeetleVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt); // also writes anger
        nbt.putInt("Variant", this.getTypeVariant());
        StatusEffect gene = this.getPotionGene();
        if (gene != null) {
            nbt.putString("PotionGene", String.valueOf(Registries.STATUS_EFFECT.getId(gene)));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt); // also reads anger
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        if (nbt.contains("PotionGene", NbtElement.STRING_TYPE)) {
            Identifier id = Identifier.tryParse(nbt.getString("PotionGene"));
            this.setPotionGene(id == null ? null : Registries.STATUS_EFFECT.get(id));
        }
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {
        BeetleVariant variant;
        if (this.random.nextInt(100) < 5) {
            variant = BeetleVariant.OMEN;
        } else {
            RegistryKey<Biome> biome = world.getBiome(this.getBlockPos()).getKey().orElse(null);
            BeetleVariant biomeVariant = biome == null ? null : BIOME_VARIANTS.get(biome);
            if (biomeVariant != null && this.random.nextFloat() < 0.5F) {
                variant = biomeVariant;
            } else {
                // id 0 is OMEN, so it is excluded here
                variant = BeetleVariant.byId(this.random.nextBetween(1, BeetleVariant.values().length - 1));
            }
        }
        this.setVariant(variant);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    // =====================================================================
    // Sounds / animation / navigation
    // =====================================================================

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
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

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    public boolean isFlappingWings() {
        return this.isInAir() && this.age % WING_FLAP_INTERVAL == 0;
    }

    @Override
    public boolean isInAir() {
        return !this.isOnGround();
    }

    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {}

    /** Note: this is called from the MobEntity constructor, so it must not depend on any state. */
    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world) {
            @Override
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }
        };
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(false);
        navigation.setCanEnterOpenDoors(true);
        return navigation;
    }

    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    // =====================================================================
    // Crop growing
    // =====================================================================

    abstract class NotAngryGoal extends Goal {
        public abstract boolean canBeetleStart();

        public abstract boolean canBeetleContinue();

        @Override
        public boolean canStart() {
            return this.canBeetleStart() && !BeetleEntity.this.hasAngerTime();
        }

        @Override
        public boolean shouldContinue() {
            return this.canBeetleContinue() && !BeetleEntity.this.hasAngerTime();
        }
    }

    class GrowCropsGoal extends NotAngryGoal {
        private static final int RUN_TICKS = 40;
        private int ticksLeft;

        @Override
        public boolean canBeetleStart() {
            return BeetleEntity.this.random.nextFloat() < 0.025F;
        }

        @Override
        public boolean canBeetleContinue() {
            return this.ticksLeft > 0;
        }

        @Override
        public void start() {
            this.ticksLeft = RUN_TICKS;
        }

        @Override
        public void tick() {
            --this.ticksLeft;
            if (BeetleEntity.this.random.nextInt(this.getTickCount(30)) != 0) return;

            // look at our own block and the two below us (crops sit under a flying/walking beetle)
            for (int i = 0; i <= 2; ++i) {
                BlockPos blockPos = BeetleEntity.this.getBlockPos().down(i);
                BlockState blockState = BeetleEntity.this.getWorld().getBlockState(blockPos);
                if (!blockState.isIn(BlockTags.BEE_GROWABLES)) continue;

                Block block = blockState.getBlock();
                BlockState grown = null;
                if (block instanceof CropBlock cropBlock) {
                    if (!cropBlock.isMature(blockState)) {
                        grown = cropBlock.withAge(cropBlock.getAge(blockState) + 1);
                    }
                } else if (block instanceof StemBlock) {
                    int age = blockState.get(StemBlock.AGE);
                    if (age < 7) {
                        grown = blockState.with(StemBlock.AGE, age + 1);
                    }
                } else if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
                    int age = blockState.get(SweetBerryBushBlock.AGE);
                    if (age < 3) {
                        grown = blockState.with(SweetBerryBushBlock.AGE, age + 1);
                    }
                } else if (blockState.isOf(Blocks.CAVE_VINES) || blockState.isOf(Blocks.CAVE_VINES_PLANT)) {
                    ((Fertilizable) block).grow((ServerWorld) BeetleEntity.this.getWorld(),
                            BeetleEntity.this.random, blockPos, blockState);
                }

                if (grown != null) {
                    BeetleEntity.this.getWorld().syncWorldEvent(2011, blockPos, 15);
                    BeetleEntity.this.getWorld().setBlockState(blockPos, grown);
                }
            }
        }
    }
}