package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.util.GoldBlockValues;
import net.follis.goldenscarabmod.util.ModTags;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Converts any gold-like block into any other. The template decides what comes out.
 *
 * Conversions use the lossless exchange rate computed by {@link GoldBlockValues} from your existing
 * stonecutting recipes (block -> 2 slabs gives "2 slabs -> 1 block", "1 block -> 2 slabs",
 * "4 cut gold -> 1 gold stairs", ...), so no extra recipes are needed.
 * Every item involved must be in the gold_blocks item tag.
 */
public class GoldCarverEntity extends AbstractGoldWorkerEntity {
    // Craft animation (GOLD_CARVER_CRAFT) is 0.95 s = 19 ticks; the claw "chomps" end around 0.85 s.
    private static final int CRAFT_CYCLE_TICKS = 19;
    private static final int CRAFT_OUTPUT_TICK = 17;

    public GoldCarverEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return createWorkerAttributes();
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.isIn(ModTags.Items.GOLD_BLOCKS);
    }

    @Override
    protected boolean isValidTemplate(ItemStack stack) {
        return stack.isIn(ModTags.Items.GOLD_BLOCKS);
    }

    @Nullable
    @Override
    protected Craft computeCraft(ItemStack input, ItemStack template) {
        GoldBlockValues.Exchange exchange =
                GoldBlockValues.exchange(this.getWorld(), input.getItem(), template.getItem());
        return exchange == null
                ? null
                : new Craft(exchange.inputCount(), template.copyWithCount(exchange.outputCount()));
    }

    /** Lets the base class throw its craft cache away after a data pack reload. */
    @Override
    protected int getCraftCacheVersion() {
        return GoldBlockValues.generation();
    }

    @Override
    protected int getCraftCycleTicks() {
        return CRAFT_CYCLE_TICKS;
    }

    @Override
    protected int getCraftOutputTick() {
        return CRAFT_OUTPUT_TICK;
    }

    @Override
    protected SoundEvent getCraftSound() {
        return SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    }

    @Override
    protected void spawnCraftParticles(ServerWorld world) {
        world.spawnParticles(ParticleTypes.POOF,
                this.getX(), this.getY() + 2.0, this.getZ(),
                20, 0.25, 0.25, 0.25, 0.1);
    }
}