package se.mickelus.tetra.blocks.forged.extractor;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.BlockPosPacket;
import se.mickelus.mutil.util.TileEntityOptional;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CoreExtractorPistonUpdatePacket extends BlockPosPacket {
    public static final Type<CoreExtractorPistonUpdatePacket> TYPE = TetraPacketHandler.type("core_extractor_piston_update_packet");

    @Override
    public Type<CoreExtractorPistonUpdatePacket> type() {
        return TYPE;
    }
    private long timestamp;

    public CoreExtractorPistonUpdatePacket() {
    }

    public CoreExtractorPistonUpdatePacket(BlockPos pos, long timestamp) {
        super(pos);

        this.timestamp = timestamp;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeLong(timestamp);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        super.fromBytes(buffer);
        timestamp = buffer.readLong();
    }

    @Override
    public void handle(Player player) {
        TileEntityOptional.from(player.level(), pos, CoreExtractorPistonBlockEntity.class)
                .ifPresent(tile -> tile.setEndTime(timestamp));
    }
}
