package net.follis.goldenscarabmod.network;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MesmerizeManager {
    private static final Map<UUID, UUID> playerToSource = new HashMap<>(); // player UUID -> owning source's UUID (moth or item frame)

    /** Attempts to claim exclusive mesmerize rights over a player. Returns true if this source now owns (or already owned) the claim. */
    public static boolean tryClaim(ServerPlayerEntity player, Entity source) {
        UUID owner = playerToSource.get(player.getUuid());
        if (owner == null) {
            playerToSource.put(player.getUuid(), source.getUuid());
            return true;
        }
        return owner.equals(source.getUuid());
    }

    /** Releases the claim, but only if this source is the one currently holding it. */
    public static void release(ServerPlayerEntity player, Entity source) {
        playerToSource.remove(player.getUuid(), source.getUuid());
    }

    /** Call when a source is removed/discarded, to free any player it was still holding. */
    public static void releaseAll(Entity source) {
        playerToSource.values().removeIf(ownerId -> ownerId.equals(source.getUuid()));
    }

    /** Call on disconnect: drops whatever claim the player had. */
    public static void releasePlayer(ServerPlayerEntity player) {
        playerToSource.remove(player.getUuid());
    }
}