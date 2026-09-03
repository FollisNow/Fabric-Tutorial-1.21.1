package net.follis.tutorialmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MesmerizePayload(int targetEntityId, boolean active, float degreesPerSecond) implements CustomPayload {
    public static final CustomPayload.Id<MesmerizePayload> ID =
            new CustomPayload.Id<>(Identifier.of("tutorialmod", "mesmerize"));

    public static final PacketCodec<RegistryByteBuf, MesmerizePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, MesmerizePayload::targetEntityId,
            PacketCodecs.BOOL, MesmerizePayload::active,
            PacketCodecs.FLOAT, MesmerizePayload::degreesPerSecond,
            MesmerizePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}