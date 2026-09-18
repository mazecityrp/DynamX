package fr.dynamx.common.network;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.common.DynamXMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class VanillaNetworkHandler implements IDnxNetworkHandler {
    public final SimpleNetworkWrapper HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel(DynamXConstants.ID);

    @Override
    public <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if (EnumPacketTarget.SERVER == targetType) {
            HANDLER.sendToServer(packet);
        } else {
            sendPacketServer(packet, targetType, target);
        }
    }

    private <T> void sendPacketServer(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if (EnumPacketTarget.PLAYER == targetType) {
            //System.out.println("SEND TO PLAYER " + target);
            HANDLER.sendTo(packet, (EntityPlayerMP) target);
        } else if (EnumPacketTarget.ALL_AROUND == targetType) {
            HANDLER.sendToAllAround(packet, (NetworkRegistry.TargetPoint) target);
        } else if (EnumPacketTarget.ALL_TRACKING_ENTITY == targetType) {
            sendToAllTracking(packet, (Entity) target);
        } else if (EnumPacketTarget.ALL == targetType) {
            HANDLER.sendToAll(packet);
        }
    }

    /**
     * Replaces {@link SimpleNetworkWrapper#sendToAllTracking(IMessage, Entity)}, which looks the world up from {@link Entity#dimension}
     * and throws a NullPointerException (FMLOutboundHandler.TRACKING_ENTITY) when that id does not match a loaded world, as seen on
     * Mohist with vehicles whose saved "Dimension" tag is stale. Like the udp network handler, use the world the entity really is in,
     * and skip players without a network connection (fake players).
     */
    private void sendToAllTracking(IDnxPacket packet, Entity entity) {
        if (!(entity.world instanceof WorldServer)) {
            return;
        }
        int worldDimension = entity.world.provider.getDimension();
        if (entity.dimension != worldDimension) {
            if (DIMENSION_WARNED.add(entity.getEntityId())) {
                DynamXMain.log.warn("Entity " + entity + " has dimension id " + entity.dimension + " but is in world '" + entity.world.getWorldInfo().getWorldName()
                        + "' (dimension " + worldDimension + "). Fixing the entity dimension. Check the server worlds configuration if this happens often.");
            }
            entity.dimension = worldDimension;
        }
        for (EntityPlayer player : ((WorldServer) entity.world).getEntityTracker().getTrackingPlayers(entity)) {
            if (player instanceof EntityPlayerMP && ((EntityPlayerMP) player).connection != null) {
                HANDLER.sendTo(packet, (EntityPlayerMP) player);
            }
        }
    }

    private static final Set<Integer> DIMENSION_WARNED = new HashSet<>();

    @Override
    public EnumNetworkType getType() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {
    }

    public SimpleNetworkWrapper getChannel() {
        return HANDLER;
    }
}
