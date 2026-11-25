package se.mickelus.tetra.items;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.network.TetraPacketHandler;

public interface InitializableItem {
    default void registerPackets(TetraPacketHandler packetHandler) {
    }

    @OnlyIn(Dist.CLIENT)
    default void clientInit() {
    }

    default void commonInit(TetraPacketHandler packetHandler) {
    }
}
