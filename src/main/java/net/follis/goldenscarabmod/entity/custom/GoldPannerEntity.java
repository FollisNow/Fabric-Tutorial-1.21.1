package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.particle.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Pans raw gold into more gold.
 *   template = gold nugget -> 1 raw gold becomes 18 nuggets
 *   template = gold ingot  -> 1 raw gold becomes 2 ingots
 */
public class GoldPannerEntity extends AbstractGoldWorkerEntity {
    // Craft animation (GOLD_PANNER_CRAFT) is 1.0 s = 20 ticks; the pan finishes spinning at ~0.92 s.
    private static final int CRAFT_CYCLE_TICKS = 20;
    private static final int CRAFT_OUTPUT_TICK = 18;

    public GoldPannerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return createWorkerAttributes();
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.isOf(Items.RAW_GOLD);
    }

    @Override
    protected boolean isValidTemplate(ItemStack stack) {
        return stack.isOf(Items.GOLD_NUGGET) || stack.isOf(Items.GOLD_INGOT);
    }

    @Nullable
    @Override
    protected Craft computeCraft(ItemStack input, ItemStack template) {
        if (!input.isOf(Items.RAW_GOLD)) return null;
        if (template.isOf(Items.GOLD_NUGGET)) return new Craft(1, new ItemStack(Items.GOLD_NUGGET, 18));
        if (template.isOf(Items.GOLD_INGOT)) return new Craft(1, new ItemStack(Items.GOLD_INGOT, 2));
        return null;
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
        return SoundEvents.BLOCK_GRAVEL_BREAK;
    }

    @Override
    protected void spawnCraftParticles(ServerWorld world) {
        world.spawnParticles(ModParticles.GOLDEN_LEAVES_PARTICLE,
                this.getX(), this.getY() + 1.1, this.getZ(),
                20, 0.25, 0.25, 0.25, 0.1);
    }
}