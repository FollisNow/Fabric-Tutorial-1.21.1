package net.follis.goldenscarabmod.block.custom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.GoldenSilverfishEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Infested version of a golden block.
 *
 * <p>IMPORTANT: this deliberately does NOT extend vanilla's InfestedBlock. Its constructor registers every
 * block in vanilla's static REGULAR_TO_INFESTED maps, which is exactly what lets vanilla silverfish infest
 * your golden blocks, and what makes the inherited static isInfestable()/fromRegularState() answer for
 * vanilla stone etc. Here every map is private to this class, so:
 * <ul>
 *   <li>vanilla silverfish never see these blocks,</li>
 *   <li>{@link #isInfestable(BlockState)} is only true for golden blocks that have an infested twin.</li>
 * </ul>
 *
 * Public API matches what GoldenSilverfishEntity used before: {@link #isInfestable},
 * {@link #fromRegularState}, {@link #toRegularState}, {@link #getRegularBlock}.
 */
public class InfestedGoldenBlocks extends Block {
    public static final MapCodec<InfestedGoldenBlocks> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Registries.BLOCK.getCodec().fieldOf("host").forGetter(InfestedGoldenBlocks::getRegularBlock),
            createSettingsCodec()
    ).apply(instance, InfestedGoldenBlocks::new));

    private static final Map<Block, Block> REGULAR_TO_INFESTED_BLOCK = new IdentityHashMap<>();
    private static final Map<BlockState, BlockState> REGULAR_TO_INFESTED_STATE = new IdentityHashMap<>();
    private static final Map<BlockState, BlockState> INFESTED_TO_REGULAR_STATE = new IdentityHashMap<>();

    private final Block regularBlock;

    public InfestedGoldenBlocks(Block regularBlock, AbstractBlock.Settings settings) {
        super(settings.hardness(regularBlock.getHardness() / 2.0F).resistance(0.75F));
        this.regularBlock = regularBlock;
        REGULAR_TO_INFESTED_BLOCK.put(regularBlock, this);
    }

    @Override
    protected MapCodec<? extends InfestedGoldenBlocks> getCodec() {
        return CODEC;
    }

    public Block getRegularBlock() {
        return this.regularBlock;
    }

    /** True only for golden blocks that have an infested equivalent. */
    public static boolean isInfestable(BlockState state) {
        return REGULAR_TO_INFESTED_BLOCK.containsKey(state.getBlock());
    }

    /** Call only for states that pass {@link #isInfestable}. */
    public static BlockState fromRegularState(BlockState regularState) {
        return copyProperties(REGULAR_TO_INFESTED_STATE, regularState,
                () -> REGULAR_TO_INFESTED_BLOCK.get(regularState.getBlock()).getDefaultState());
    }

    public BlockState toRegularState(BlockState infestedState) {
        return copyProperties(INFESTED_TO_REGULAR_STATE, infestedState, () -> this.getRegularBlock().getDefaultState());
    }

    // ---------------------------------------------------------------------
    // Mining: spawn a GOLDEN silverfish (never a vanilla one)
    // ---------------------------------------------------------------------

    @Override
    protected void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.onStacksDropped(state, world, pos, tool, dropExperience);
        if (world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)
                && !EnchantmentHelper.hasAnyEnchantmentsIn(tool, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            this.spawnSilverfish(world, pos);
        }
    }

    private void spawnSilverfish(ServerWorld world, BlockPos pos) {
        GoldenSilverfishEntity silverfish = ModEntities.GOLDEN_SILVERFISH.create(world);
        if (silverfish != null) {
            silverfish.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            world.spawnEntity(silverfish);
            silverfish.playSpawnEffects();
        }
    }

    // ---------------------------------------------------------------------

    private static BlockState copyProperties(Map<BlockState, BlockState> cache, BlockState from, Supplier<BlockState> defaultState) {
        return cache.computeIfAbsent(from, source -> {
            BlockState result = defaultState.get();
            for (Property<?> property : source.getProperties()) {
                if (result.contains(property)) {
                    result = copyProperty(result, source, property);
                }
            }
            return result;
        });
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState to, BlockState from, Property<T> property) {
        return to.with(property, from.get(property));
    }
}