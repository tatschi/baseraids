package may.baseraids.networking;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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


	@OnlyIn(Dist.CLIENT)
	public boolean getIsWin() {
		return this.isWin;
	}


}
