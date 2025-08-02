package net.follis.tutorialmod.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
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
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BugJarItem extends Item {
    private World world;
    TextColor darkGold = TextColor.fromRgb(0xBC7F04); // Custom gold-like color

    public BugJarItem(Settings settings) {
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
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {

        if(Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("tooltip.tutorialmod.bug_jar.shift_down").withColor(darkGold.getRgb()));
            Formatting[] formatting = new Formatting[]{Formatting.GRAY};
            List<BugData> bugDataList = new ArrayList<>(stack.getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
            if(!bugDataList.isEmpty()) {
                List<BugData> reversedList = bugDataList.reversed();
                for (BugData bugData: reversedList) {
                    if (Registries.ENTITY_TYPE.get(getIdentifier(bugData)) == ModEntities.BEETLE) {
                        var potionId = bugData.entityData.copyNbt().getString("PotionGene");
                        var statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(potionId));
                        if (statusEffect != null) {
                            tooltip.add(Text.translatable(convertToKey(bugData)).withColor(darkGold.getRgb())
                                    .append(" ")
                                    .append(Text.literal(Text.translatable(statusEffect.getTranslationKey()).getString().toLowerCase())
                                            .setStyle(Style.EMPTY)
                                            .withColor(statusEffect.getColor())));
                        } else {
                            tooltip.add(Text.translatable(convertToKey(bugData)).formatted(formatting));
                        }

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

    private static Identifier getIdentifier(BugData bugData) {
        return Identifier.of(bugData.entityData.copyNbt().getString("id"));
    }

    private static @NotNull String convertToKey(BugData bugData) {
        return "entity." + bugData.entityData.copyNbt().getString("id").replace(":", ".");
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        this.world = user.getWorld();
        if (world instanceof ServerWorld) {
            captureEntity(entity, user);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        this.world = context.getWorld();
        if (context.getPlayer() != null && !context.getWorld().isClient) {
            if (tryReleaseBugs(context.getBlockPos(), context.getPlayer())) {
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    private void captureEntity(Entity entity, PlayerEntity player) {
        entity.stopRiding();
        entity.removeAllPassengers();

        // Ensure we always have a mutable list
        List<BugData> bugDataList = getMutableBugDataList(player);
        bugDataList.add(BugData.of(entity));
        player.getMainHandStack().set(ModDataComponentTypes.BUGS, bugDataList);

        world.playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
        entity.discard();
    }

    private boolean tryReleaseBugs(BlockPos pos, PlayerEntity player) {
        List<BugData> bugDataList = getMutableBugDataList(player);

        if (!bugDataList.isEmpty()) {
            if (player.isSneaking()) {
                for (BugData bugData : bugDataList) {
                    releaseBug(pos, bugData, player);
                }
                bugDataList.clear();
            } else {
                BugData lastBug = bugDataList.removeLast();
                releaseBug(pos, lastBug, player);
            }
            player.getMainHandStack().set(ModDataComponentTypes.BUGS, bugDataList);
            return true;
        }
        return false;
    }

    private void releaseBug(BlockPos pos, BugData bugData, PlayerEntity player) {
        Entity entity = bugData.loadEntity(player.getWorld());
        if (entity != null) {
            positionEntity(entity, pos);
            world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, world.getBlockState(pos)));
            world.spawnEntity(entity);
        }
    }

    private void positionEntity(Entity entity, BlockPos pos) {
        double x = pos.getX() + 0.5 + (double) entity.getRandom().nextInt(5) / 10;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5 + (double) entity.getRandom().nextInt(5) / 10;
        entity.refreshPositionAndAngles(x, y, z, entity.getYaw(), entity.getPitch());
    }

    private List<BugData> getMutableBugDataList(PlayerEntity player) {
        return new ArrayList<>(player.getMainHandStack().getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
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