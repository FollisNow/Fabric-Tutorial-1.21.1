package net.follis.tutorialmod.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class JawTrapBlock extends Block {
    public static final BooleanProperty ARMED = BooleanProperty.of("armed");
    private static final int COOLDOWN_TICKS = 60;   // 3 seconds

    public JawTrapBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ARMED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ARMED);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity && state.get(ARMED)) {
            // Disarm and schedule the rearm
            world.setBlockState(pos, state.with(ARMED, false), Block.NOTIFY_LISTENERS);
            world.scheduleBlockTick(pos, this, COOLDOWN_TICKS);

            Vec3d vel = entity.getPos().subtract(entity.prevX, entity.prevY, entity.prevZ);   // blocks per tick
            int delayTicks = 3;   // roughly the remaining delay after step 1; use ~8 if you leave warm-up at 0
            Vec3d spawn = entity.getPos().add(vel.x * delayTicks, 0, vel.z * delayTicks);

            EvokerFangsEntity jaw = new EvokerFangsEntity(world, spawn.x, entity.getY(), spawn.z, 0, -5, null);
            world.spawnEntity(jaw);
            world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.BLOCKS, 1f, 1f);
        }

        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.setBlockState(pos, state.with(ARMED, true), Block.NOTIFY_LISTENERS);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.tutorialmod.jaw_trap_block.tooltip"));
        super.appendTooltip(stack, context, tooltip, options);
    }
}