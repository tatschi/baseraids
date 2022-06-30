package may.baseraids.networking;

import java.util.function.Supplier;

import may.baseraids.Baseraids;
import may.baseraids.sounds.RaidWinSound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * The packet handler receives packets that allow for communication between
 * client and server.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
 */
public class BaseraidsPacketHandler {

	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(Baseraids.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals);

	public static void handleRaidEnd(BaseraidsRaidEndPacket msg, Supplier<NetworkEvent.Context> ctx) {
		if(msg.getIsWin()) {
			new RaidWinSound();
		}
		
	}
}
