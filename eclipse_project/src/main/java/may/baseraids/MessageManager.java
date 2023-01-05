package may.baseraids;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class MessageManager {

	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param message the string that is sent in the chat
	 */
	public void sendStatusMessage(String message) {
		sendStatusMessage(message, true);
	}

	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param message   the string that is sent in the chat
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public void sendStatusMessage(String message, boolean actionBar) {
		Baseraids.LOGGER.debug("Sending chat message: \"" + message + "\"");
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
				.forEach(x -> x.sendStatusMessage(new StringTextComponent(message), actionBar));
	}

	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param message the string that is sent in the chat
	 * @param player  the player that the message is sent to
	 */
	public void sendStatusMessage(String message, PlayerEntity player) {
		sendStatusMessage(message, player, true);
	}

	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param message   the string that is sent in the chat
	 * @param player    the player that the message is sent to
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public void sendStatusMessage(String message, PlayerEntity player, boolean actionBar) {
		Baseraids.LOGGER.debug("Sending chat message: \"" + message + "\" to " + player.getDisplayName().getString());
		player.sendStatusMessage(new StringTextComponent(message), actionBar);
	}
}
