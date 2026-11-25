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
public class SettlePacket extends AbstractPacket {
    public static final Type<SettlePacket> TYPE = TetraPacketHandler.type("settle_packet");

    @Override
    public Type<SettlePacket> type() {
        return TYPE;
    }
    ItemStack itemStack;
    String slot;

    public SettlePacket() {
    }

    public SettlePacket(ItemStack itemStack, String slot) {
        this.itemStack = itemStack;
        this.slot = slot;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, itemStack);
        buffer.writeUtf(slot);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        itemStack = ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
        slot = buffer.readUtf();
    }

    @Override
    public void handle(Player player) {
        ProgressionHelper.showSettleToastClient(itemStack, slot);
    }
}
