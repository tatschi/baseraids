package may.baseraids;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

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
		Baseraids.LOGGER.debug("Sending chat message: \"{}\"", message);
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
				.forEach(x -> x.sendStatusMessage(new TextComponent(message), actionBar));
	}

	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param message the string that is sent in the chat
	 * @param player  the player that the message is sent to
	 */
	public void sendStatusMessage(String message, ServerPlayer player) {
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
	public void sendStatusMessage(String message, ServerPlayer player, boolean actionBar) {
		Baseraids.LOGGER.debug("Sending chat message: \"%1$s\" to %2$s", message, player.getDisplayName().getString());
		player.sendStatusMessage(new TextComponent(message), actionBar);
	}
}
