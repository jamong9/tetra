package se.mickelus.tetra.blocks.workbench;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import se.mickelus.tetra.network.TetraPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.network.AbstractPacket;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class WorkbenchPacketUpdate extends AbstractPacket {
    public static final Type<WorkbenchPacketUpdate> TYPE = TetraPacketHandler.type("workbench_packet_update");

    @Override
    public Type<WorkbenchPacketUpdate> type() {
        return TYPE;
    }

    private BlockPos pos;
    private UpgradeSchematic schematic;
    private String selectedSlot;

    public WorkbenchPacketUpdate() {
    }

    public WorkbenchPacketUpdate(BlockPos pos, UpgradeSchematic schematic, String selectedSlot) {
        this.pos = pos;
        this.schematic = schematic;
        this.selectedSlot = selectedSlot;
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(pos.getX());
        buffer.writeInt(pos.getY());
        buffer.writeInt(pos.getZ());

        buffer.writeUtf(schematic != null ? schematic.getKey() : "");
        buffer.writeUtf(Objects.requireNonNullElse(selectedSlot, ""));
    }

    @Override
    public void fromBytes(FriendlyByteBuf buffer) {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        pos = new BlockPos(x, y, z);

        schematic = SchematicRegistry.getSchematic(buffer.readUtf());
        selectedSlot = buffer.readUtf();
        if (selectedSlot.isEmpty()) {
            selectedSlot = null;
        }
    }

    @Override
    public void handle(Player player) {
        WorkbenchTile workbench = (WorkbenchTile) player.level().getBlockEntity(pos);
        if (workbench != null) {
            workbench.update(schematic, selectedSlot, player);
        }
    }
}
