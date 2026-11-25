package se.mickelus.tetra.blocks.workbench;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.BlockPosPacket;
import se.mickelus.mutil.util.CastOptional;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class WorkbenchPacketTweak extends BlockPosPacket {
    public static final Type<WorkbenchPacketTweak> TYPE = TetraPacketHandler.type("workbench_packet_tweak");

    @Override
    public Type<WorkbenchPacketTweak> type() {
        return TYPE;
    }

    String slot;
    Map<String, Integer> tweaks;

    public WorkbenchPacketTweak() {
    }

    public WorkbenchPacketTweak(BlockPos pos, String slot, Map<String, Integer> tweaks) {
        super(pos);

        this.slot = slot;
        this.tweaks = tweaks;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        super.toBytes(buffer);

        buffer.writeUtf(slot);
        buffer.writeMap(tweaks, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeInt);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        super.fromBytes(buffer);

        slot = buffer.readUtf();
        tweaks = buffer.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readInt);
    }

    @Override
    public void handle(Player player) {
        CastOptional.cast(player.level().getBlockEntity(pos), WorkbenchTile.class)
                .ifPresent(workbench -> workbench.tweak(player, slot, tweaks));
    }
}
