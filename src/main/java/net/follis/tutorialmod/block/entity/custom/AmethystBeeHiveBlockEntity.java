package net.follis.tutorialmod.block.entity.custom;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.follis.tutorialmod.block.custom.AmethystBeeHiveBlock;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.AmethystBeeEntity;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AmethystBeeHiveBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FLOWER_POS_KEY = "flower_pos";
    private static final String BEES_KEY = "bees";
    static final List<String> IRRELEVANT_BEE_NBT_KEYS = Arrays.asList("Air", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot", "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances", "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "SleepingX", "SleepingY", "SleepingZ", "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "hive_pos", "Passengers", "leash", "UUID");
    public static final int MAX_BEE_COUNT = 5;
    private static final int ANGERED_CANNOT_ENTER_HIVE_TICKS = 400;
    private static final int MIN_OCCUPATION_TICKS_WITH_NECTAR = 2400;
    public static final int MIN_OCCUPATION_TICKS_WITHOUT_NECTAR = 600;
    private final List<AmethystBeeHiveBlockEntity.Bee> bees = Lists.newArrayList();
    @Nullable
    private BlockPos flowerPos;


    public AmethystBeeHiveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMETHYST_BEE_HIVE_BE, pos, state);
    }
    public void markDirty() {
        if (this.isNearFire()) {
            assert this.world != null;
            this.angerBees(null, this.world.getBlockState(this.getPos()), AmethystBeeState.EMERGENCY);
        }

        super.markDirty();
    }

    public boolean isNearFire() {
        if (this.world == null) {
            return false;
        } else {
            for(BlockPos blockPos : BlockPos.iterate(this.pos.add(-1, -1, -1), this.pos.add(1, 1, 1))) {
                if (this.world.getBlockState(blockPos).getBlock() instanceof FireBlock) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean hasNoBees() {
        return this.bees.isEmpty();
    }

    public boolean isFullOfBees() {
        return this.bees.size() == 5;
    }

    public void angerBees(@Nullable PlayerEntity player, BlockState state, AmethystBeeState amethystBeeState) {
        List<Entity> list = this.tryReleaseBee(state, amethystBeeState);
        if (player != null) {
            for(Entity entity : list) {
                if (entity instanceof AmethystBeeEntity amethystBeeEntity) {
                    if (player.getPos().squaredDistanceTo(entity.getPos()) <= (double)16.0F) {
                        if (!this.isSmoked()) {
                            amethystBeeEntity.setTarget(player);
                        } else {
                            amethystBeeEntity.setCannotEnterHiveTicks(400);
                        }
                    }
                }
            }
        }

    }

    private List<Entity> tryReleaseBee(BlockState state, AmethystBeeState amethystBeeState) {
        List<Entity> list = Lists.newArrayList();
        this.bees.removeIf((bee) -> {
            assert this.world != null;
            return releaseBee(this.world, this.pos, state, bee.createData(), list, amethystBeeState, this.flowerPos);
        });
        if (!list.isEmpty()) {
            super.markDirty();
        }

        return list;
    }

    @Debug
    public int getBeeCount() {
        return this.bees.size();
    }

    public static int getHoneyLevel(BlockState state) {
        return state.get(AmethystBeeHiveBlock.HONEY_LEVEL);
    }

    @Debug
    public boolean isSmoked() {
        return CampfireBlock.isLitCampfireInRange(this.world, this.getPos());
    }

    public void tryEnterHive(Entity entity) {
        if (this.bees.size() < 5) {
            entity.stopRiding();
            entity.removeAllPassengers();
            this.addBee(AmethystBeeData.of(entity));
            if (this.world != null) {
                if (entity instanceof AmethystBeeEntity amethystBeeEntity) {
                    if (amethystBeeEntity.hasFlower() && (!this.hasFlowerPos() || this.world.random.nextBoolean())) {
                        this.flowerPos = amethystBeeEntity.getFlowerPos();
                    }
                }

                BlockPos blockPos = this.getPos();
                this.world.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
                this.world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(entity, this.getCachedState()));
            }

            entity.discard();
            super.markDirty();
        }
    }

    public void addBee(AmethystBeeData bee) {
        this.bees.add(new Bee(bee));
    }

    private static boolean releaseBee(World world, BlockPos pos, BlockState state, AmethystBeeData bee, @Nullable List<Entity> entities, AmethystBeeState amethystBeeState, @Nullable BlockPos flowerPos) {
        if ((world.isRaining()) && amethystBeeState != AmethystBeeState.EMERGENCY) {
            return false;
        } else {
            Direction direction = state.get(AmethystBeeHiveBlock.FACING);
            BlockPos blockPos = pos.offset(direction);
            boolean bl = !world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty();
            if (bl && amethystBeeState != AmethystBeeState.EMERGENCY) {
                return false;
            } else {
                Entity entity = bee.loadEntity(world, pos);
                if (entity != null) {
                    if (entity instanceof AmethystBeeEntity amethystBeeEntity) {
                        if (flowerPos != null && !amethystBeeEntity.hasFlower() && world.random.nextFloat() < 0.9F) {
                            amethystBeeEntity.setFlowerPos(flowerPos);
                        }

                        if (amethystBeeState == AmethystBeeState.HONEY_DELIVERED) {
                            amethystBeeEntity.onHoneyDelivered();
                            if (state.isIn(BlockTags.BEEHIVES, (statex) -> statex.contains(AmethystBeeHiveBlock.HONEY_LEVEL))) {
                                int i = getHoneyLevel(state);
                                if (i < 5) {
                                    int j = world.random.nextInt(100) == 0 ? 2 : 1;
                                    if (i + j > 5) {
                                        --j;
                                    }

                                    world.setBlockState(pos, state.with(AmethystBeeHiveBlock.HONEY_LEVEL, i + j));
                                }
                            }
                        }

                        if (entities != null) {
                            entities.add(amethystBeeEntity);
                        }

                        float f = entity.getWidth();
                        double d = bl ? (double)0.0F : 0.55 + (double)(f / 2.0F);
                        double e = (double)pos.getX() + (double)0.5F + d * (double)direction.getOffsetX();
                        double g = (double)pos.getY() + (double)0.5F - (double)(entity.getHeight() / 2.0F);
                        double h = (double)pos.getZ() + (double)0.5F + d * (double)direction.getOffsetZ();
                        entity.refreshPositionAndAngles(e, g, h, entity.getYaw(), entity.getPitch());
                    }

                    world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, world.getBlockState(pos)));
                    return world.spawnEntity(entity);
                } else {
                    return false;
                }
            }
        }
    }

    private boolean hasFlowerPos() {
        return this.flowerPos != null;
    }

    private static void tickBees(World world, BlockPos pos, BlockState state, List<Bee> bees, @Nullable BlockPos flowerPos) {
        boolean bl = false;
        Iterator<Bee> iterator = bees.iterator();

        while(iterator.hasNext()) {
            Bee bee = iterator.next();
            if (bee.canExitHive()) {
                AmethystBeeState amethystBeeState = bee.hasNectar() ? AmethystBeeState.HONEY_DELIVERED : AmethystBeeState.BEE_RELEASED;
                if (releaseBee(world, pos, state, bee.createData(), null, amethystBeeState, flowerPos)) {
                    bl = true;
                    iterator.remove();
                }
            }
        }

        if (bl) {
            markDirty(world, pos, state);
        }

    }

    public static void serverTick(World world, BlockPos pos, BlockState state, AmethystBeeHiveBlockEntity blockEntity) {
        tickBees(world, pos, state, blockEntity.bees, blockEntity.flowerPos);
        if (!blockEntity.bees.isEmpty() && world.getRandom().nextDouble() < 0.005) {
            double d = (double)pos.getX() + (double)0.5F;
            double e = pos.getY();
            double f = (double)pos.getZ() + (double)0.5F;
            world.playSound(null, d, e, f, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        //DebugInfoSender.sendBeehiveDebugData(world, pos, state, blockEntity);
    }

    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.bees.clear();
        if (nbt.contains("bees")) {
            AmethystBeeData.LIST_CODEC.parse(NbtOps.INSTANCE, nbt.get("bees")).resultOrPartial((string) -> LOGGER.error("Failed to parse bees: '{}'", string)).ifPresent((list) -> list.forEach(this::addBee));
        }

        this.flowerPos = NbtHelper.toBlockPos(nbt, "flower_pos").orElse(null);
    }

    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("bees", AmethystBeeData.LIST_CODEC.encodeStart(NbtOps.INSTANCE, this.createBeesData()).getOrThrow());
        if (this.hasFlowerPos()) {
            nbt.put("flower_pos", NbtHelper.fromBlockPos(this.flowerPos));
        }

    }

    protected void readComponents(BlockEntity.ComponentsAccess components) {
        super.readComponents(components);
        this.bees.clear();
        List<AmethystBeeData> list = components.getOrDefault(ModDataComponentTypes.AMETHYST_BEES, List.of());
        list.forEach(this::addBee);
    }

    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(ModDataComponentTypes.AMETHYST_BEES, this.createBeesData());
    }

    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove("bees");
    }

    private List<AmethystBeeData> createBeesData() {
        return this.bees.stream().map(Bee::createData).toList();
    }

    public enum AmethystBeeState {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY;
    }

    static class Bee {
        private final AmethystBeeData data;
        private int ticksInHive;

        Bee(AmethystBeeData data) {
            this.data = data;
            this.ticksInHive = data.ticksInHive();
        }

        public boolean canExitHive() {
            return this.ticksInHive++ > this.data.minTicksInHive;
        }

        public AmethystBeeData createData() {
            return new AmethystBeeData(this.data.entityData, this.ticksInHive, this.data.minTicksInHive);
        }

        public boolean hasNectar() {
            return this.data.entityData.getNbt().getBoolean("HasNectar");
        }
    }

    public record AmethystBeeData(NbtComponent entityData, int ticksInHive, int minTicksInHive) {
        public static final Codec<AmethystBeeData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(NbtComponent.CODEC.optionalFieldOf("entity_data", NbtComponent.DEFAULT).forGetter(AmethystBeeData::entityData), Codec.INT.fieldOf("ticks_in_hive").forGetter(AmethystBeeData::ticksInHive), Codec.INT.fieldOf("min_ticks_in_hive").forGetter(AmethystBeeData::minTicksInHive)).apply(instance, AmethystBeeData::new));
        public static final Codec<List<AmethystBeeData>> LIST_CODEC;
        public static final PacketCodec<ByteBuf, AmethystBeeData> PACKET_CODEC;

        public static AmethystBeeData of(Entity entity) {
            NbtCompound nbtCompound = new NbtCompound();
            entity.saveNbt(nbtCompound);
            Objects.requireNonNull(nbtCompound);
            AmethystBeeHiveBlockEntity.IRRELEVANT_BEE_NBT_KEYS.forEach(nbtCompound::remove);
            boolean bl = nbtCompound.getBoolean("HasNectar");
            return new AmethystBeeData(NbtComponent.of(nbtCompound), 0, bl ? 2400 : 600);
        }

        public static AmethystBeeData create(int ticksInHive) {
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putString("id", Registries.ENTITY_TYPE.getId(ModEntities.AMETHYST_BEE).toString());
            return new AmethystBeeData(NbtComponent.of(nbtCompound), ticksInHive, 600);
        }

        @Nullable
        public Entity loadEntity(World world, BlockPos pos) {
            NbtCompound nbtCompound = this.entityData.copyNbt();
            Objects.requireNonNull(nbtCompound);
            AmethystBeeHiveBlockEntity.IRRELEVANT_BEE_NBT_KEYS.forEach(nbtCompound::remove);
            Entity entity = EntityType.loadEntityWithPassengers(nbtCompound, world, (entityx) -> entityx);
            if (entity != null && entity.getType().isIn(EntityTypeTags.BEEHIVE_INHABITORS)) {
                entity.setNoGravity(true);
                if (entity instanceof AmethystBeeEntity amethystBeeEntity) {
                    amethystBeeEntity.setHivePos(pos);
                    tickEntity(this.ticksInHive, amethystBeeEntity);
                }

                return entity;
            } else {
                return null;
            }
        }

        private static void tickEntity(int ticksInHive, AmethystBeeEntity amethystBeeEntity) {
            int i = amethystBeeEntity.getBreedingAge();
            if (i < 0) {
                amethystBeeEntity.setBreedingAge(Math.min(0, i + ticksInHive));
            } else if (i > 0) {
                amethystBeeEntity.setBreedingAge(Math.max(0, i - ticksInHive));
            }

            amethystBeeEntity.setLoveTicks(Math.max(0, amethystBeeEntity.getLoveTicks() - ticksInHive));
        }

        static {
            LIST_CODEC = CODEC.listOf();
            PACKET_CODEC = PacketCodec.tuple(NbtComponent.PACKET_CODEC, AmethystBeeData::entityData, PacketCodecs.VAR_INT, AmethystBeeData::ticksInHive, PacketCodecs.VAR_INT, AmethystBeeData::minTicksInHive, AmethystBeeData::new);
        }
    }
}
