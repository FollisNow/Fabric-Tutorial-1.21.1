package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.util.IBugVariants;
import net.follis.tutorialmod.util.IVariant;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
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
import java.util.Map;

import static net.follis.tutorialmod.util.IBugVariants.BugColors;
import static net.follis.tutorialmod.util.IBugVariants.bugVariants;

public class VisionMonocleItem extends Item {

    public VisionMonocleItem(Settings settings) {
        super(settings);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null) {
            resetMonocle(context.getPlayer(), context.getHand());
        }
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
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        resetMonocle(user, hand);

        if (entity instanceof IBugVariants bugVariants) {
            user.getStackInHand(hand).set(ModDataComponentTypes.ENTITY_TYPE, entity.getName().getString());
            user.getStackInHand(hand).set(ModDataComponentTypes.BUG_VARIANT, bugVariants.getTypeVariant());
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            resetMonocle(user, hand);

            return TypedActionResult.success(user.getStackInHand(hand), world.isClient());

        }
            return TypedActionResult.pass(user.getStackInHand(hand));
    }

    private static void resetMonocle(PlayerEntity user, Hand hand) {
        user.getStackInHand(hand).set(ModDataComponentTypes.COORDINATES_BLOCK, null);
        user.getStackInHand(hand).set(ModDataComponentTypes.COORDINATES_CHUNK, null);
        user.getStackInHand(hand).set(ModDataComponentTypes.BIOME, null);
        user.getStackInHand(hand).set(ModDataComponentTypes.ENTITY_COUNT0, null);
        user.getStackInHand(hand).set(ModDataComponentTypes.ENTITY_COUNT1, null);
        user.getStackInHand(hand).set(ModDataComponentTypes.ENTITY_TYPE, null);
        user.getStackInHand(hand).set(ModDataComponentTypes.BUG_VARIANT, null);
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



            if (stack.get(ModDataComponentTypes.ENTITY_TYPE) != null && stack.get(ModDataComponentTypes.BUG_VARIANT) != null) {
                String entityType = stack.get(ModDataComponentTypes.ENTITY_TYPE);
                int bugVariant = stack.getOrDefault(ModDataComponentTypes.BUG_VARIANT, -1);
                tooltip.add(Text.translatable("tooltip.tutorialmod.entity_of").formatted(Formatting.GRAY)
                        .append(" ")
                        .append(Text.literal(entityType+ " " + getNamedVariant(entityType, bugVariant)).withColor(getColorVariant(entityType, bugVariant).getRgb())));
            }

        } else {
            tooltip.add(Text.translatable("tooltip.tutorialmod.vision_monocle"));
        }



        super.appendTooltip(stack, context, tooltip, type);
    }

    TextColor getColorVariant(String bugType, int variant){
        if (BugColors.containsKey(bugType)) {
            Map<Integer, TextColor> colors = BugColors.get(bugType);

            if (colors != null) {
                return colors.getOrDefault(variant, TextColor.fromFormatting(Formatting.GRAY));
            }
        }
        return TextColor.fromFormatting(Formatting.GRAY);
    }
    public String getNamedVariant(String bugType, int variant) {
        if (bugVariants.containsKey(bugType)) {
            Class<?> variantClass = bugVariants.get(bugType);

            try {
                // Ensure the class implements IVariant
                if (IVariant.class.isAssignableFrom(variantClass)) {
                    // Cast to the appropriate enum class
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<? extends IVariant>> enumClass = (Class<? extends Enum<? extends IVariant>>) variantClass;

                    // Get the variant using the byId method
                    IVariant namedVariant = IVariant.byId(enumClass, variant);

                    // Check if namedVariant is not null
                    if (namedVariant != null) {
                        return namedVariant.getName(); // Return the associated name
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Handle exceptions as necessary
            }
        }
        return "not found";
    }
}
