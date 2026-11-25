package se.mickelus.tetra.items.modular.impl.toolbelt;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.AbstractPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class StoreToolbeltItemPacket extends AbstractPacket {
    public static final Type<StoreToolbeltItemPacket> TYPE = TetraPacketHandler.type("store_toolbelt_item_packet");

    @Override
    public Type<StoreToolbeltItemPacket> type() {
        return TYPE;
    }
    public StoreToolbeltItemPacket() {
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
    }

    @Override
    public void handle(Player player) {
        ToolbeltHelper.storeItemInToolbelt(player);
    }
}
