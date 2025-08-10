package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class VisionMonocleItem extends Item {

    public VisionMonocleItem(Settings settings) {
        super(settings);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        context.getStack().set(ModDataComponentTypes.COORDINATES_BLOCK, context.getBlockPos());
        Chunk chunk = context.getWorld().getChunk(context.getBlockPos());
        context.getStack().set(ModDataComponentTypes.COORDINATES_CHUNK, new Vec3i(chunk.getPos().x, 0, chunk.getPos().z));

        String biomeName = context.getWorld().getBiome(context.getBlockPos()).getKey().map(key -> key.getValue().toString()).orElse("unknown");
        context.getStack().set(ModDataComponentTypes.BIOME, biomeName);

        Box box0 = Box.of(context.getBlockPos().toBottomCenterPos(), 16, 16, 16);
        context.getStack().set(ModDataComponentTypes.ENTITY_COUNT0, context.getWorld().getOtherEntities(context.getPlayer(), box0).size());

        Box box1 = Box.of(context.getBlockPos().toBottomCenterPos(), 8, 8, 8);
        context.getStack().set(ModDataComponentTypes.ENTITY_COUNT1, context.getWorld().getOtherEntities(context.getPlayer(), box1).size());
        if (context.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, context.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            user.getStackInHand(hand).set(ModDataComponentTypes.COORDINATES_BLOCK, null);
            user.getStackInHand(hand).set(ModDataComponentTypes.COORDINATES_CHUNK, null);
            user.getStackInHand(hand).set(ModDataComponentTypes.BIOME, null);
            user.getStackInHand(hand).set(ModDataComponentTypes.ENTITY_COUNT0, null);
            user.getStackInHand(hand).set(ModDataComponentTypes.ENTITY_COUNT1, null);

            return TypedActionResult.success(user.getStackInHand(hand), world.isClient());

        }
            return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(Screen.hasShiftDown()) {
            BlockPos pos = stack.get(ModDataComponentTypes.COORDINATES_BLOCK);
            if(pos != null) {
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                tooltip.add(Text.literal("Block absolute position at x=" + x + ", y=" + y + ", z=" + z ).formatted(Formatting.GRAY));
            }

            Vec3i vec3i = stack.get(ModDataComponentTypes.COORDINATES_CHUNK);
            if(vec3i != null) {
                int x = vec3i.getX();
                int z = vec3i.getZ();
                tooltip.add(Text.literal("Chunk absolute position at x=" + x + ", z=" + z ).formatted(Formatting.GRAY));
            }

            if(stack.get(ModDataComponentTypes.BIOME) != null) {
                tooltip.add(Text.literal("Biome is " + stack.get(ModDataComponentTypes.BIOME)).formatted(Formatting.GRAY));
            }

            if(stack.get(ModDataComponentTypes.ENTITY_COUNT0) != null && stack.get(ModDataComponentTypes.ENTITY_COUNT1) != null) {
                tooltip.add(Text.literal(stack.get(ModDataComponentTypes.ENTITY_COUNT0) + " entities are present within 16 blocks").formatted(Formatting.GRAY));
                tooltip.add(Text.literal(stack.get(ModDataComponentTypes.ENTITY_COUNT0) + " entities are present within 8 blocks").formatted(Formatting.GRAY));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.tutorialmod.vision_monocle"));
        }



        super.appendTooltip(stack, context, tooltip, type);
    }
}
