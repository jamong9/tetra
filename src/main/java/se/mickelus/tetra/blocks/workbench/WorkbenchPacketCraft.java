package se.mickelus.tetra.blocks.workbench;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.BlockPosPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WorkbenchPacketCraft extends BlockPosPacket {
    public static final Type<WorkbenchPacketCraft> TYPE = TetraPacketHandler.type("workbench_packet_craft");

    @Override
    public Type<WorkbenchPacketCraft> type() {
        return TYPE;
    }

    public WorkbenchPacketCraft() {
    }

    public WorkbenchPacketCraft(BlockPos pos) {
        super(pos);
    }

    @Override
    public void handle(Player player) {
        WorkbenchTile workbench = (WorkbenchTile) player.level().getBlockEntity(pos);
        if (workbench != null) {
            workbench.craft(player);
        }
    }
}
