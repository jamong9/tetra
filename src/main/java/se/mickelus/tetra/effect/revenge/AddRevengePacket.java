package se.mickelus.tetra.effect.revenge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.AbstractPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class AddRevengePacket extends AbstractPacket {
    public static final Type<AddRevengePacket> TYPE = TetraPacketHandler.type("add_revenge_packet");

    @Override
    public Type<AddRevengePacket> type() {
        return TYPE;
    }
    private int entityId = -1;

    public AddRevengePacket(Entity attacker) {
        this.entityId = attacker.getId();
    }

    public AddRevengePacket() {
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
    }

    @Override
    public void handle(Player player) {
        RevengeTracker.addEnemy(player, entityId);
    }
}
