package se.mickelus.tetra.blocks.workbench.action;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.BlockPosPacket;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WorkbenchActionPacket extends BlockPosPacket {
    public static final Type<WorkbenchActionPacket> TYPE = TetraPacketHandler.type("workbench_action_packet");

    @Override
    public Type<WorkbenchActionPacket> type() {
        return TYPE;
    }

    private String actionKey;

    public WorkbenchActionPacket() {
    }

    public WorkbenchActionPacket(BlockPos pos, String actionKey) {
        super(pos);
        this.actionKey = actionKey;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeUtf(actionKey);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        super.fromBytes(buffer);

        actionKey = buffer.readUtf();
    }

    @Override
    public void handle(Player player) {
        WorkbenchTile workbench = (WorkbenchTile) player.level().getBlockEntity(pos);
        if (workbench != null) {
            workbench.performAction(player, actionKey);
        }
    }
}
