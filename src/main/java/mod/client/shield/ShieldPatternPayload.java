package mod.client.shield;

import mod.client.Client;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShieldPatternPayload(String pattern, int color, boolean offhand) implements CustomPacketPayload {
    public static final Type<ShieldPatternPayload> TYPE = new Type<>(Client.id("shield_pattern"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldPatternPayload> CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> {
                buffer.writeUtf(payload.pattern, ShieldPatternData.PATTERN_LENGTH);
                buffer.writeInt(payload.color);
                buffer.writeBoolean(payload.offhand);
            },
            buffer -> new ShieldPatternPayload(
                    buffer.readUtf(ShieldPatternData.PATTERN_LENGTH),
                    buffer.readInt(),
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}