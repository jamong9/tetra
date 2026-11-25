package se.mickelus.tetra.module.improvement;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.network.AbstractPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HonePacket extends AbstractPacket {
    public static final Type<HonePacket> TYPE = TetraPacketHandler.type("hone_packet");

    @Override
    public Type<HonePacket> type() {
        return TYPE;
    }

    ItemStack itemStack;

    public HonePacket() {
    }

    public HonePacket(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, itemStack);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        itemStack = ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    }

    @Override
    public void handle(Player player) {
        ProgressionHelper.showHoneToastClient(itemStack);
    }
}
