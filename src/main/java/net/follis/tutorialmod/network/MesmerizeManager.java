package net.follis.tutorialmod.network;

import net.follis.tutorialmod.entity.custom.MothEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MesmerizeManager {
    private static final Map<UUID, UUID> playerToMoth = new HashMap<>(); // player UUID -> owning moth's UUID

    /** Attempts to claim exclusive mesmerize rights over a player. Returns true if this moth now owns (or already owned) the claim. */
    public static boolean tryClaim(ServerPlayerEntity player, MothEntity moth) {
        UUID owner = playerToMoth.get(player.getUuid());
        if (owner == null) {
            playerToMoth.put(player.getUuid(), moth.getUuid());
            return true;
        }
        return owner.equals(moth.getUuid());
    }

    /** Releases the claim, but only if this moth is the one currently holding it. */
    public static void release(ServerPlayerEntity player, MothEntity moth) {
        playerToMoth.remove(player.getUuid(), moth.getUuid());
    }

    /** Call when a moth is removed/discarded, to free any player it was still holding. */
    public static void releaseAll(MothEntity moth) {
        playerToMoth.values().removeIf(ownerId -> ownerId.equals(moth.getUuid()));
    }
}
