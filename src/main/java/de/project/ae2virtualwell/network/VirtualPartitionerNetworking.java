package de.project.ae2virtualwell.network;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.menu.VirtualPartitionerMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class VirtualPartitionerNetworking {
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AE2VirtualWell.MODID);
        registrar.playToServer(
                SetPartitionsPayload.TYPE,
                SetPartitionsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player.containerMenu instanceof VirtualPartitionerMenu menu) {
                        menu.applyPartitions(player, payload.partitions());
                    }
                })
        );
    }
}
