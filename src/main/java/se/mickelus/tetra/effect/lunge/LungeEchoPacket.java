package se.mickelus.tetra.effect.lunge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.AbstractPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LungeEchoPacket extends AbstractPacket {
    public static final Type<LungeEchoPacket> TYPE = TetraPacketHandler.type("lunge_echo_packet");

    @Override
    public Type<LungeEchoPacket> type() {
        return TYPE;
    }
    boolean isVertical;

    public LungeEchoPacket() {
    }

    public LungeEchoPacket(boolean isVertical) {
        this.isVertical = isVertical;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isVertical);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        isVertical = buffer.readBoolean();
    }

    @Override
    public void handle(Player player) {
        LungeEffect.receiveEchoPacket(player, isVertical);
    }
}
