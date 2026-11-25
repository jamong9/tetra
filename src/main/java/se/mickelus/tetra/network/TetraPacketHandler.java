package se.mickelus.tetra.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import se.mickelus.mutil.network.AbstractPacket;
import se.mickelus.mutil.network.PacketHandler;
import se.mickelus.tetra.TetraMod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TetraPacketHandler extends PacketHandler {
    private static final String VERSION = "1";
    private final List<Consumer<PayloadRegistrar>> registrations = new ArrayList<>();

    public TetraPacketHandler() {
        super(TetraMod.MOD_ID, "main", VERSION);
    }

    public static <T extends AbstractPacket> Type<T> type(String path) {
        return new Type<>(ResourceLocation.fromNamespaceAndPath(TetraMod.MOD_ID, path));
    }

    public <T extends AbstractPacket> void registerClientBoundPacket(Type<T> type, Supplier<T> factory) {
        registrations.add(registrar -> registrar.playToClient(type, codec(factory),
                (packet, context) -> packet.handle(context.player())));
    }

    public <T extends AbstractPacket> void registerServerBoundPacket(Type<T> type, Supplier<T> factory) {
        registrations.add(registrar -> registrar.playToServer(type, codec(factory),
                (packet, context) -> packet.handle(context.player())));
    }

    public void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrations.forEach(registration -> registration.accept(registrar));
    }

    private static <T extends AbstractPacket> StreamCodec<RegistryFriendlyByteBuf, T> codec(Supplier<T> factory) {
        return StreamCodec.ofMember(AbstractPacket::toBytes, buffer -> {
            T packet = factory.get();
            packet.fromBytes(buffer);
            return packet;
        });
    }
}
