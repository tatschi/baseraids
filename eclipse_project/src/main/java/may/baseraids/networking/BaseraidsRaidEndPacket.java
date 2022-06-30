package may.baseraids.networking;

import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * This packet is sent from the server side to notify the client that a raid has ended.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
 */
public class BaseraidsRaidEndPacket {

	private boolean isWin;

	public BaseraidsRaidEndPacket(boolean isWin) {
		this.isWin = isWin;
	}


	public BaseraidsRaidEndPacket(PacketBuffer buff) {
		this(buff.readBoolean());
	}


	public void readPacketData(PacketBuffer buff) {
		isWin = buff.readBoolean();
	}

	public void writePacketData(PacketBuffer buff) {
		buff.writeBoolean(isWin);
	}

	/**
	 * Passes the packet on to the packet handler by calling the corresponding
	 * handle method.
	 * 
	 * @param msg the packet that is passed on
	 * @param ctx a network context that gives access to communication info like the
	 *            sender of the packet
	 */
	public static void handle(BaseraidsRaidEndPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> BaseraidsPacketHandler.handleRaidEnd(msg, ctx));
		ctx.get().setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	public boolean getIsWin() {
		return this.isWin;
	}


}
