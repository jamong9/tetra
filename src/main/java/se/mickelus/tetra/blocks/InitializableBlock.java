package se.mickelus.tetra.blocks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.network.TetraPacketHandler;

public interface InitializableBlock {
    default void registerPackets(TetraPacketHandler packetHandler) {
    }

    @OnlyIn(Dist.CLIENT)
    default void clientInit() {
    }

    default void commonInit(TetraPacketHandler packetHandler) {
    }
}
