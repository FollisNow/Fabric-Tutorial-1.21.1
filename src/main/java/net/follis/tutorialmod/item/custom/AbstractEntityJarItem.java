package net.follis.tutorialmod.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.BeetleEntity;
import net.follis.tutorialmod.entity.custom.BeetleVariant;
import net.follis.tutorialmod.entity.custom.MothEntity;
import net.follis.tutorialmod.entity.custom.SpiderlingEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.follis.tutorialmod.util.IBugVariants.*;

public abstract class AbstractEntityJarItem extends Item {
    protected World world;

    public AbstractEntityJarItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName() {
        return Text.translatable(this.getTranslationKey()).formatted(Formatting.GOLD);
    }
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        this.world = user.getWorld();
        if (world instanceof ServerWorld && entity.getType().isIn(EntityTypeTags.ARTHROPOD) && (hand == Hand.MAIN_HAND || (hand == Hand.OFF_HAND && !(user.getMainHandStack().getItem() instanceof AbstractEntityJarItem)))) {
            captureEntity(entity, user, hand);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        this.world = context.getWorld();
        Hand hand = context.getHand();
        if (context.getPlayer() != null && !context.getWorld().isClient && (hand == Hand.MAIN_HAND || (hand == Hand.OFF_HAND && !(context.getPlayer().getMainHandStack().getItem() instanceof AbstractEntityJarItem)))) {
            if (tryReleaseBugs(context)) {
                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock(context);
    }
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("tooltip.tutorialmod.bug_jar.shift_down").withColor(darkGold.getRgb()));
            Formatting[] formatting = new Formatting[]{Formatting.GRAY};
            List<BugData> bugDataList = new ArrayList<>(stack.getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
            if(!bugDataList.isEmpty()) {
                List<BugData> reversedList = bugDataList.reversed();
                for (BugData bugData: reversedList) {
                    if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.BEETLE) {
                        var potionId = getNbt(bugData).getString("PotionGene");
                        var statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(potionId));
                        if (statusEffect != null) {
                            tooltip.add(Text.translatable(convertToKey(bugData)).withColor(BeetleColors.get(0).getRgb())
                                    .append(" ")
                                    .append(Text.literal(Text.translatable(statusEffect.getTranslationKey()).getString().toLowerCase())
                                            .setStyle(Style.EMPTY)
                                            .withColor(statusEffect.getColor())));
                        } else {
                            TextColor color = BeetleColors.getOrDefault(getNbt(bugData).getInt("Variant"), TextColor.fromFormatting(Formatting.GRAY));
                            tooltip.add(Text.translatable(convertToKey(bugData)).withColor(color.getRgb()));
                        }

                    } else if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.LOCUST) {
                        TextColor color = LocustColors.getOrDefault(getNbt(bugData).getInt("Variant"), TextColor.fromFormatting(Formatting.GRAY));
                        tooltip.add(Text.translatable(convertToKey(bugData)).withColor(color.getRgb()));

                    } else if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.SPIDERLING) {
                        String formattedGrowthSize = String.format("%.3f", getNbt(bugData).getFloat("GrowthSize")); // Format to 3 decimal places
                        int growthSizeColor = interpolateColor(getNbt(bugData).getFloat("GrowthSize"), SpiderlingEntity.MINIMUM_SIZE, SpiderlingEntity.MAXIMUM_SIZE);

                        String formattedMaxHealth = String.format("%.3f", getNbt(bugData).getFloat("MaxHealth")); // Format to 3 decimal places
                        int maxHealthColor = interpolateColor(getNbt(bugData).getFloat("MaxHealth"), SpiderlingEntity.MINIMUM_HEALTH, SpiderlingEntity.MAXIMUM_HEALTH);

                        String formattedSpeed = String.format("%.3f", getNbt(bugData).getFloat("MovementSpeed")); // Format to 3 decimal places
                        int speedColor = interpolateColor(getNbt(bugData).getFloat("MovementSpeed"), SpiderlingEntity.MINIMUM_SPEED, SpiderlingEntity.MAXIMUM_SPEED);

                        String formattedJumpStrength = String.format("%.3f", getNbt(bugData).getFloat("JumpStrength")); // Format to 3 decimal places
                        int jumpStrengthColor = interpolateColor(getNbt(bugData).getFloat("JumpStrength"), SpiderlingEntity.MINIMUM_JUMP, SpiderlingEntity.MAXIMUM_JUMP);

                        tooltip.add(Text.translatable(convertToKey(bugData))
                                .append(" ")
                                .append(Text.literal("Size: " + formattedGrowthSize + " ").withColor(growthSizeColor))
                                .append(Text.literal("Health: " + formattedMaxHealth + " ").withColor(maxHealthColor))
                                .append(Text.literal("Speed: " + formattedSpeed + " ").withColor(speedColor))
                                .append(Text.literal("Jump: " + formattedJumpStrength).withColor(jumpStrengthColor))
                        );
                    } else {
                        tooltip.add(Text.translatable(convertToKey(bugData)).formatted(formatting));
                    }
                }
            }
        } else {
            tooltip.add(Text.translatable("tooltip.tutorialmod.bug_jar"));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }


    protected static Identifier getIdentifier(BugData bugData) {
        return Identifier.of(bugData.entityData.copyNbt().getString("id"));
    }
    protected static @NotNull String convertToKey(BugData bugData) {
        return "entity." + bugData.entityData.copyNbt().getString("id").replace(":", ".");
    }
    protected List<BugData> getMutableBugDataList(PlayerEntity player) {
        return new ArrayList<>(player.getMainHandStack().getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
    }
    protected List<BugData> getMutableBugDataList(PlayerEntity player, int slot) {
        return new ArrayList<>(player.getInventory().getStack(slot).getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
    }
    protected List<BugData> getMutableBugDataList(PlayerEntity player, Hand hand) {
        return new ArrayList<>(player.getStackInHand(hand).getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
    }
    protected int getBugCount(PlayerEntity player) {
        return player.getMainHandStack().getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()).size();
    }
    protected int getBugCount(PlayerEntity player, int slot) {
        return player.getInventory().getStack(slot).getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()).size();
    }

    private BlockPos positionEntity(Entity entity, ItemUsageContext context, World world) {
        // Get the block position and the side that was hit
        BlockPos blockPos = context.getBlockPos();
        Direction direction = context.getSide();
        BlockState blockState = world.getBlockState(blockPos);

        BlockPos blockPos2;
        if (blockState.getCollisionShape(world, blockPos).isEmpty()) {
            blockPos2 = blockPos;
        } else {
            blockPos2 = blockPos.offset(direction);
        }
        // Refresh the entity's position and angles
        entity.refreshPositionAndAngles(blockPos2, entity.getYaw(), entity.getPitch());
        if (entity instanceof MothEntity moth)
            moth.setRoosting(false);
        return blockPos2;
    }

    protected void captureEntity(Entity entity, PlayerEntity player, Hand hand) {
        entity.stopRiding();
        entity.removeAllPassengers();

        // Ensure we always have a mutable list
        List<BugData> bugDataList = getMutableBugDataList(player, hand);
        bugDataList.add(BugData.of(entity));
        player.getStackInHand(hand).set(ModDataComponentTypes.BUGS, bugDataList);
        player.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
        entity.discard();
    }

    protected boolean tryReleaseBugs(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null)
            return false;
        List<BugData> bugDataList = getMutableBugDataList(player, context.getHand());

        if (!bugDataList.isEmpty()) {
            if (player.isSneaking()) {
                for (BugData bugData : bugDataList) {
                    releaseBug(context, bugData);
                }
                bugDataList.clear();
            } else {
                BugData lastBug = bugDataList.removeLast();
                releaseBug(context, lastBug);
            }
            context.getStack().set(ModDataComponentTypes.BUGS, bugDataList);
            return true;
        }
        return false;
    }

    protected void releaseBug(ItemUsageContext context, BugData bugData) {
        PlayerEntity player = context.getPlayer();
        if (player == null)
            return ;
        Entity entity = bugData.loadEntity(player.getWorld());
        if (entity != null) {
            BlockPos pos = positionEntity(entity, context, player.getWorld());
            player.getWorld().playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            player.getWorld().emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, player.getWorld().getBlockState(pos)));
            player.getWorld().spawnEntity(entity);
        }
    }

    protected boolean tryKillBug(PlayerEntity player, int slot) {
        if (player == null)
            return false;
        List<BugData> bugDataList = getMutableBugDataList(player, slot);

        if (bugDataList.size() >= 2) {
                bugDataList.remove(bugDataList.get(player.getRandom().nextInt(bugDataList.size() - 1)));

            player.getInventory().getStack(slot).set(ModDataComponentTypes.BUGS, bugDataList);
            return true;
        }
        return false;
    }

    protected void makeBugsDeadlier(PlayerEntity player, int slot) {
        if (player == null)
            return;
        List<BugData> bugDataList = getMutableBugDataList(player, slot);
        List<Entity> newList = new ArrayList<>();

        bugDataList.forEach(bugData -> {
            Entity entity = bugData.loadEntity(player.getWorld());
            if (entity instanceof SpiderlingEntity spiderlingEntity) {
                spiderlingEntity.increaseAllStats();
                newList.add(spiderlingEntity);
            } else if (entity instanceof BeetleEntity beetle && beetle.getVariant().equals(BeetleVariant.OMEN)) {
                if (beetle.getPotionGene() == null) {
                    List<StatusEffect> allEffects = Registries.STATUS_EFFECT.stream().toList();
                    if (!allEffects.isEmpty()) {
                        StatusEffect randomEffect = allEffects.get(player.getRandom().nextBetween(0, allEffects.size()-1));
                        beetle.setPotionGene(randomEffect);
                    }

                }
                newList.add(beetle);
            } else {
                newList.add(entity);
            }
        });
        bugDataList.clear();
        player.getInventory().getStack(slot).set(ModDataComponentTypes.BUGS, bugDataList);


        List<BugData> newBugDataList = getMutableBugDataList(player, slot);
        newList.forEach(entity -> {
            newBugDataList.add(BugData.of(entity));
        });
        player.getInventory().getStack(slot).set(ModDataComponentTypes.BUGS, newBugDataList);

    }


    protected static NbtCompound getNbt(BugData bugData) {
        return bugData.entityData().copyNbt();
    }
    protected int interpolateColor(float value, float minValue, float maxValue) {
        float clampedValue = MathHelper.clamp((value - minValue) / (maxValue - minValue), 0F, 1F);

        int r = (int) (255 * (1 - clampedValue)); // Red decreases
        int g = (int) (255 * clampedValue);       // Green increases
        int b = 0; // Keep blue as 0

        return (r << 16) | (g << 8) | b; // Combine RGB to integer
    }

    public record BugData(NbtComponent entityData) {
        public static final Codec<BugData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(NbtComponent.CODEC.fieldOf("entity_data").forGetter(BugData::entityData))
                        .apply(instance, BugData::new));

        public static BugData of(Entity entity) {
            NbtCompound nbtCompound = new NbtCompound();
            entity.saveNbt(nbtCompound);
            return new BugData(NbtComponent.of(nbtCompound));
        }

        @Nullable
        public Entity loadEntity(World world) {
            NbtCompound nbtCompound = entityData.copyNbt();
            return EntityType.loadEntityWithPassengers(nbtCompound, world, entityx -> entityx);
        }
        public static final Codec<List<BugData>> LIST_CODEC;
        public static final PacketCodec<ByteBuf, BugData> PACKET_CODEC;
        static {
            // Static codec definitions
            LIST_CODEC = CODEC.listOf();
            PACKET_CODEC = PacketCodec.tuple(NbtComponent.PACKET_CODEC, BugData::entityData, BugData::new);
        }
    }
}
