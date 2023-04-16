package may.baseraids;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MessageManager {

	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param messageKey the key to the message that is sent in the chat
	 */
	public void sendStatusMessage(String messageKey) {
		sendStatusMessage(messageKey, true);
	}

	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param messageKey   the key to the message that is sent in the chat
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public void sendStatusMessage(String messageKey, boolean actionBar) {
		sendStatusMessage(Component.translatable(messageKey), actionBar);
	}
	
	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param messageComponent   the component containing the message that is sent in the chat
	 */
	public void sendStatusMessage(Component messageComponent) {
		sendStatusMessage(messageComponent, true);
	}
	
	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param messageComponent   the component containing the message that is sent in the chat
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public void sendStatusMessage(Component messageComponent, boolean actionBar) {
		Baseraids.LOGGER.debug("Sending chat message");
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
				.forEach(x -> x.displayClientMessage(messageComponent, actionBar));
	}

	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param messageKey the key to the message that is sent in the chat
	 * @param player  the player that the message is sent to
	 */
	public void sendStatusMessage(String messageKey, ServerPlayer player) {
		sendStatusMessage(messageKey, player, true);
	}

	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param messageKey   the string that is sent in the chat
	 * @param player    the player that the message is sent to
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public void sendStatusMessage(String messageKey, ServerPlayer player, boolean actionBar) {
		sendStatusMessage(Component.translatable(messageKey), player, actionBar);
	}
	
	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param messageComponent   the component containing the message that is sent in the chat
	 * @param player    the player that the message is sent to
	 */
	public void sendStatusMessage(Component messageComponent, ServerPlayer player) {
		sendStatusMessage(messageComponent, player, true);
	}
	
	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param messageComponent   the component containing the message that is sent in the chat
	 * @param player    the player that the message is sent to
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public void sendStatusMessage(Component messageComponent, ServerPlayer player, boolean actionBar) {
		Baseraids.LOGGER.debug("Sending chat message to %s", player.getDisplayName().getString());
		player.displayClientMessage(messageComponent, actionBar);
	}
	
}
