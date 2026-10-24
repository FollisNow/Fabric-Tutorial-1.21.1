package net.follis.goldenscarabmod.network;

import net.follis.goldenscarabmod.GoldenScarabMod;   // your main class, wherever MOD_ID lives
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sent to a player who just got flashed: how long the screen stays fully white, then how long it takes to fade. */
public record FlashPayload(int holdTicks, int fadeTicks) implements CustomPayload {
    public static final CustomPayload.Id<FlashPayload> ID =
            new CustomPayload.Id<>(Identifier.of(GoldenScarabMod.MOD_ID, "flash"));

    public static final PacketCodec<RegistryByteBuf, FlashPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, FlashPayload::holdTicks,
            PacketCodecs.VAR_INT, FlashPayload::fadeTicks,
            FlashPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}