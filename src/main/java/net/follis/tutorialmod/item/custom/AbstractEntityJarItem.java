package net.follis.tutorialmod.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractEntityJarItem extends Item {
    TextColor darkGold = TextColor.fromRgb(0xBC7F04); // Custom gold-like color

    Map<Integer, TextColor> BeetleColors = Map.of(
            0, darkGold,
            1, TextColor.fromRgb(0x55FF55),
            2, TextColor.fromRgb(0xFF5555),
            3, TextColor.fromRgb(0x8A4A09)
    );
    Map<Integer, TextColor> LocustColors = Map.of(
            0, darkGold,
            1, TextColor.fromRgb(0x4DCF9F),
            2, TextColor.fromRgb(0x55FF55),
            3, TextColor.fromRgb(0xFF5555)
    );

    public AbstractEntityJarItem(Settings settings) {
        super(settings);
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
        return blockPos2;
    }

    protected void captureEntity(Entity entity, PlayerEntity player) {
        entity.stopRiding();
        entity.removeAllPassengers();

        // Ensure we always have a mutable list
        List<BugData> bugDataList = getMutableBugDataList(player);
        bugDataList.add(BugData.of(entity));
        player.getMainHandStack().set(ModDataComponentTypes.BUGS, bugDataList);

        player.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
        entity.discard();
    }

    protected boolean tryReleaseBugs(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null)
            return false;
        List<BugData> bugDataList = getMutableBugDataList(player);

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
            player.getMainHandStack().set(ModDataComponentTypes.BUGS, bugDataList);
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
