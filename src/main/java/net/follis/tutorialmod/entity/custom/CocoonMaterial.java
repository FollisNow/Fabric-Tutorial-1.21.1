package net.follis.tutorialmod.entity.custom;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

public enum CocoonMaterial implements StringIdentifiable {
    DIAMOND("Diamond", 0x4AEDD9),
    GLOWSTONE("Glowstone", 0xFDDE4C),
    LAPIS("Lapis", 0x1E5B9E),
    EMERALD("Emerald", 0x17DD62),
    IRON("Iron", 0xD8D8D8),
    REDSTONE("Redstone", 0xAE1C1C),
    OBSIDIAN("Obsidian", 0x6B5B95),
    NETHERITE("Netherite", 0x443F3E),
    PRISMARINE("Prismarine", 0x6BC7B8),
    COPPER("Copper", 0xED6109),
    ECHO_SHARD("Echo Shard", 0x009295);

    private final String name;
    private final int color;

    CocoonMaterial(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String asString() { return name; }

    public static final Codec<CocoonMaterial> CODEC = StringIdentifiable.createCodec(CocoonMaterial::values);
    public static final PacketCodec<ByteBuf, CocoonMaterial> PACKET_CODEC =
            PacketCodecs.indexed(id -> values()[id], CocoonMaterial::ordinal);
}