package se.mickelus.tetra.blocks.forged.container;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.AbstractPacket;
import se.mickelus.mutil.util.CastOptional;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ChangeCompartmentPacket extends AbstractPacket {
    public static final Type<ChangeCompartmentPacket> TYPE = TetraPacketHandler.type("change_compartment_packet");

    @Override
    public Type<ChangeCompartmentPacket> type() {
        return TYPE;
    }

    private int compartmentIndex;

    public ChangeCompartmentPacket() {
    }

    public ChangeCompartmentPacket(int compartmentIndex) {
        this.compartmentIndex = compartmentIndex;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(compartmentIndex);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        compartmentIndex = buffer.readInt();
    }

    @Override
    public void handle(Player player) {
        CastOptional.cast(player.containerMenu, ForgedContainerMenu.class)
                .ifPresent(container -> container.changeCompartment(compartmentIndex));
    }
}
