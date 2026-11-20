package net.follis.goldenscarabmod.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.MothEntity;
import net.follis.goldenscarabmod.entity.custom.MothVariant;
import net.follis.goldenscarabmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lets item frames holding a mesmerizing moth figurine mesmerize players like a live moth does.
 * Player-centric: every tick we validate the frame a player is held by, and every few ticks we
 * look for new frames for players who aren't held by anything.
 */
public final class ItemFrameMesmerizer {
    private record Claim(UUID frameUuid, int frameId) {}

    private static final Map<UUID, Claim> CLAIMS = new HashMap<>(); // player UUID -> frame holding them

    private ItemFrameMesmerizer() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ItemFrameMesmerizer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CLAIMS.remove(handler.getPlayer().getUuid());
            MesmerizeManager.releasePlayer(handler.getPlayer());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CLAIMS.clear());
    }

    private static void tick(MinecraftServer server) {
        boolean scan = server.getTicks() % MothEntity.TRIGGER_SCAN_INTERVAL == 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Claim claim = CLAIMS.get(player.getUuid());
            if (claim != null) {
                updateClaim(player, claim);
            } else if (scan && !player.isSpectator() && !player.isCreative()) {
                scanForNewStares(player);
            }
        }
    }

    private static void updateClaim(ServerPlayerEntity player, Claim claim) {
        ItemFrameEntity frame = player.getServerWorld().getEntity(claim.frameUuid()) instanceof ItemFrameEntity f ? f : null;

        // frame gone/unloaded/other dimension, moth taken out, player moved away, looked away, or looked straight at it
        if (frame == null || !canMesmerize(player, frame, true)) {
            if (frame != null) {
                MesmerizeManager.release(player, frame);
            } else {
                MesmerizeManager.releasePlayer(player);
            }
            CLAIMS.remove(player.getUuid());
            send(player, claim.frameId(), false);
            return;
        }
        // the client does the actual rotating, we just keep it informed
        send(player, claim.frameId(), true);
    }

    private static void scanForNewStares(ServerPlayerEntity player) {
        List<ItemFrameEntity> frames = player.getServerWorld().getEntitiesByClass(ItemFrameEntity.class,
                player.getBoundingBox().expand(MothEntity.MESMERIZE_RANGE),
                frame -> holdsMesmerizingMoth(frame.getHeldItemStack()));
        frames.sort(Comparator.comparingDouble(frame -> frame.squaredDistanceTo(player)));

        for (ItemFrameEntity frame : frames) {
            if (canMesmerize(player, frame, false) && MesmerizeManager.tryClaim(player, frame)) {
                CLAIMS.put(player.getUuid(), new Claim(frame.getUuid(), frame.getId()));
                return;
            }
        }
    }

    /** @param holding true when the player is already held (skips nothing, just documents the call site). */
    private static boolean canMesmerize(ServerPlayerEntity player, ItemFrameEntity frame, boolean holding) {
        if (frame.isRemoved() || !holdsMesmerizingMoth(frame.getHeldItemStack())) return false;
        if (player.getWorld() != frame.getWorld() || player.isRemoved()) return false;
        if (player.squaredDistanceTo(frame) > MothEntity.MESMERIZE_RANGE_SQ) return false;

        // only from the side the frame faces (the raycast below already blocks the wall, this is a cheap early out)
        Vec3d toPlayer = player.getEyePos().subtract(frame.getPos());
        if (toPlayer.dotProduct(Vec3d.of(frame.getHorizontalFacing().getVector())) <= 0) return false;

        if (!player.canSee(frame)) return false;

        double dot = lookDot(player, frame);
        return dot > MothEntity.MESMERIZE_CONE_THRESHOLD
                && dot <= MothEntity.MESMERIZE_INCONSEQUENTIAL_THRESHOLD; // staring straight at it breaks the spell
    }

    private static double lookDot(ServerPlayerEntity player, ItemFrameEntity frame) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toFrame = frame.getPos().subtract(player.getEyePos()).normalize();
        return look.dotProduct(toFrame);
    }

    private static boolean holdsMesmerizingMoth(ItemStack stack) {
        if (stack.isEmpty()) return false;
        BugData data = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        if (data == null || Registries.ENTITY_TYPE.get(data.getIdentifier()) != ModEntities.MOTH) return false;
        return !MothVariant.byId(data.getNbt().getInt("Variant")).isNotMesmerizing();
    }

    private static void send(ServerPlayerEntity player, int entityId, boolean active) {
        ServerPlayNetworking.send(player,
                new MesmerizePayload(entityId, active, MothEntity.MESMERIZE_TURN_SPEED_DEGREES * 20)); // deg/tick -> deg/sec
    }
}