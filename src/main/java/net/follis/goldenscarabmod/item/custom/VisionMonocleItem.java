package net.follis.goldenscarabmod.item.custom;

import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.util.BugRegistry;
import net.follis.goldenscarabmod.util.IBugVariants;
import net.follis.goldenscarabmod.util.IVariant;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        if (entity instanceof IBugVariants bug) {
            ItemStack held = user.getStackInHand(hand);
            // Store the registry id ("goldenscarabmod:beetle"), NOT the localized display name:
            // the name changes with the language, the id never does.
            held.set(ModDataComponentTypes.ENTITY_TYPE, Registries.ENTITY_TYPE.getId(entity.getType()).toString());
            held.set(ModDataComponentTypes.BUG_VARIANT, bug.getTypeVariant());
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
        ItemStack stack = user.getStackInHand(hand);
        stack.set(ModDataComponentTypes.COORDINATES_BLOCK, null);
        stack.set(ModDataComponentTypes.COORDINATES_CHUNK, null);
        stack.set(ModDataComponentTypes.BIOME, null);
        stack.set(ModDataComponentTypes.ENTITY_COUNT0, null);
        stack.set(ModDataComponentTypes.ENTITY_COUNT1, null);
        stack.set(ModDataComponentTypes.ENTITY_TYPE, null);
        stack.set(ModDataComponentTypes.BUG_VARIANT, null);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (Screen.hasShiftDown()) {
            BlockPos pos = stack.get(ModDataComponentTypes.COORDINATES_BLOCK);
            if (pos != null) {
                tooltip.add(Text.literal("Block absolute position at x=" + pos.getX() + ", y=" + pos.getY() + ", z=" + pos.getZ()).formatted(Formatting.GRAY));
            }

            Vec3i chunkPos = stack.get(ModDataComponentTypes.COORDINATES_CHUNK);
            if (chunkPos != null) {
                tooltip.add(Text.literal("Chunk absolute position at x=" + chunkPos.getX() + ", z=" + chunkPos.getZ()).formatted(Formatting.GRAY));
            }

            String biome = stack.get(ModDataComponentTypes.BIOME);
            if (biome != null) {
                tooltip.add(Text.literal("Biome is " + biome).formatted(Formatting.GRAY));
            }

            Integer count0 = stack.get(ModDataComponentTypes.ENTITY_COUNT0);
            Integer count1 = stack.get(ModDataComponentTypes.ENTITY_COUNT1);
            if (count0 != null && count1 != null) {
                tooltip.add(Text.literal(count0 + " entities are present within 16 blocks").formatted(Formatting.GRAY));
                tooltip.add(Text.literal(count1 + " entities are present within 8 blocks").formatted(Formatting.GRAY)); // was count0 twice
            }

            String storedType = stack.get(ModDataComponentTypes.ENTITY_TYPE);
            Integer bugVariant = stack.get(ModDataComponentTypes.BUG_VARIANT);
            if (storedType != null && bugVariant != null) {
                EntityType<?> bugType = resolveType(storedType);

                MutableText entityText;
                if (bugType != null) {
                    entityText = Text.empty().append(bugType.getName())
                            .append(" " + getNamedVariant(bugType, bugVariant))
                            .withColor(getColorVariant(bugType, bugVariant).getRgb());
                } else {
                    // stack saved before the switch to registry ids: it holds a localized name
                    entityText = Text.literal(storedType).formatted(Formatting.GRAY);
                }
                tooltip.add(Text.translatable("tooltip.goldenscarabmod.entity_of").formatted(Formatting.GRAY)
                        .append(" ")
                        .append(entityText));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.goldenscarabmod.vision_monocle"));
        }

        super.appendTooltip(stack, context, tooltip, type);
    }

    @Nullable
    private static EntityType<?> resolveType(String stored) {
        Identifier id = Identifier.tryParse(stored);
        // get() would silently return the default (pig) for unknown ids, so check first
        return id != null && Registries.ENTITY_TYPE.containsId(id) ? Registries.ENTITY_TYPE.get(id) : null;
    }

    TextColor getColorVariant(EntityType<?> bugType, int variant) {
        List<TextColor> colors = BugRegistry.colors(bugType);
        return colors.isEmpty()
                ? TextColor.fromFormatting(Formatting.GRAY)
                : IBugVariants.getColorOrDefault(colors, variant);
    }

    public String getNamedVariant(EntityType<?> bugType, int variant) {
        Class<?> variantClass = BugRegistry.variantClass(bugType);
        if (variantClass != null && IVariant.class.isAssignableFrom(variantClass)) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<? extends IVariant>> enumClass = (Class<? extends Enum<? extends IVariant>>) variantClass;
                IVariant namedVariant = IVariant.byId(enumClass, variant);
                if (namedVariant != null) {
                    return namedVariant.getName();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "not found";
    }
}