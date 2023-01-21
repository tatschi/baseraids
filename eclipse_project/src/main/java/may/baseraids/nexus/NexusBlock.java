package may.baseraids.nexus;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;

import may.baseraids.Baseraids;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * This class handles the nexus block and its special behavior that ensures that
 * there is always exactly one nexus on a server (except in creative mode).
 * <p>
 * Details on the behavior:
 * <ul>
 * <li>When a player joins a world and the nexus is still uninitialized, he is
 * given a nexus. @see onPlayerLogInAndStateUNINITIALZED_giveNexus
 * <li>When a player exits a world and has the nexus in his inventory, it is
 * attempted to give the nexus to another player on the server and remove it
 * from the player that is logging out. @see
 * onPlayerLogOutWithNexus_transferNexusToOtherPlayerOrIgnore
 * <li>When the nexus is not placed as a block, a debuff is added to every
 * player in the world. @see onWorldTick_addDebuff
 * <li>When the nexus is block is broken by a player, the item is not dropped
 * but directly added to the players inventory. @see
 * onNexusBreak_giveNexusOrCancelEvent
 * <li>The nexus cannot be tossed out of the inventory. @see
 * onNexusDropped_cancelEventAndGiveNexusBack
 * </ul>
 * 
 * @author Natascha May
 */
//@Mod.EventBusSubscriber annotation automatically registers STATIC event handlers
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NexusBlock extends Block implements EntityBlock {

	private static Random rand = new Random();

	public enum NexusState {
		BLOCK, ITEM, UNINITIALIZED
	}

	/**
	 * The current state of the nexus needs to be tracked through its interactions
	 * at all times to allow for the special behavior of the block.
	 * <p>
	 * This state is static, because there is always assumed to be at maximum one
	 * instance of this class. This means, if in creative mode a second nexus is
	 * brought into the game, any action with one of the blocks will simply override
	 * the last known state of any nexus.
	 * <p>
	 * The state of the nexus is UNINITIALIZED until the data of this class is first
	 * loaded.
	 */
	private static NexusState curState = NexusState.UNINITIALIZED;
	/**
	 * The current position of the nexus needs to be tracked through its
	 * interactions at all times to allow for the special behavior of the block.
	 * <p>
	 * This position is static, because there is always assumed to be at maximum one
	 * instance of this class. This means, if in creative mode a second nexus is
	 * brought into the game, any action with one of the blocks will simply override
	 * the last known position of any nexus.
	 */
	private static BlockPos curBlockPos = new BlockPos(0, 0, 0);

	public NexusBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Creates a {@link NexusEffectsBlockEntity} and returns it. This entity will be
	 * connected to the block.
	 * 
	 * @param state the {@link BlockState} for which the entity is created
	 * @param world the world in which the block is
	 * @returns the created {@link BlockEntity}
	 */
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new NexusEffectsBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState,
			BlockEntityType<T> blockEntityType) {
		return level.isClientSide() ? null : createTickerHelper(blockEntityType);
	}

	/**
	 * Copied and adapted from {@link BaseEntityBlock#createTickerHelper} due to
	 * buggy behavior cause by the {@link BaseEntityBlock} class. Returns the ticker
	 * method for a {@link BlockEntity} of the {@link BlockEntityType} that belongs
	 * to this class.
	 * 
	 * @param <A>
	 * @param entityTypeOfIncomingEntity
	 * @return the ticker method for a {@link BlockEntity} of the
	 *         {@link BlockEntityType} that belongs to this class
	 */
	@Nullable
	protected static <A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
			BlockEntityType<A> entityTypeOfIncomingEntity) {
		return Baseraids.NEXUS_BLOCK_ENTITY_TYPE.get() == entityTypeOfIncomingEntity
				? (BlockEntityTicker<A>) NexusEffectsBlockEntity::tick
				: null;
	}

	/**
	 * Adds a debuff with properties specified in the inner class
	 * {@link NexusEffects#DEBUFF} to all players, if the nexus is not placed. This
	 * is done in all dimensions.
	 * 
	 * @param event the event of type {@link TickEvent.WorldTickEvent} that triggers
	 *              this method
	 */
	@SubscribeEvent
	public static void onWorldTickAddDebuff(final TickEvent.WorldTickEvent event) {
		Level level = event.world;
		if (level.isClientSide) {
			return;
		}
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		if (NexusBlock.getState() == NexusState.BLOCK) {
			return;
		}
		// only apply debuff every time half the duration of the effect has passed
		if (level.getGameTime() % (NexusEffects.DEBUFF.duration / 2) != 0L) {
			return;
		}

		for (Player player : level.players()) {
			player.addEffect(NexusEffects.getEffectInstance(NexusEffects.DEBUFF));
		}
	}

	/**
	 * Sets the state to {@link NexusState#BLOCK} and the new position of the nexus,
	 * when a nexus block is placed.
	 * 
	 * @param event the event of type {@link BlockEvent.EntityPlaceEvent} that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onNexusPlacedSetStateAndBlockPos(final BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isClientSide())
			return;
		if (event.getPlacedBlock().getBlock() instanceof NexusBlock) {
			setState(NexusState.BLOCK);
			setBlockPos(event.getPos());
		}
	}

	/**
	 * Attempts to directly give the nexus to the player that broke the block, when
	 * a nexus block is broken. If it is not successful or the event happens during
	 * a raid, the breaking event is cancelled.
	 * 
	 * @param event the event of type {@link BlockEvent.BreakEvent} that triggers
	 *              this method
	 */
	@SubscribeEvent
	public static void onNexusBreakGiveNexusOrCancelEvent(final BlockEvent.BreakEvent event) {
		if (event.getPlayer().level.isClientSide)
			return;
		if (!(event.getState().getBlock() instanceof NexusBlock)) {
			return;
		}

		if (Baseraids.worldManager.getRaidManager().isRaidActive()) {
			event.setCanceled(true);
			Baseraids.LOGGER.warn("NexusBlock cannot be removed during raid");
			Baseraids.messageManager.sendStatusMessage("The NexusBlock cannot be removed during raid!",
					(ServerPlayer) event.getPlayer(), true);
			return;
		}
		if (!giveNexusToPlayer(event.getPlayer())) {
			event.setCanceled(true);
			Baseraids.LOGGER.warn("Couldn't add nexus to player inventory");
		}		
	}

	/**
	 * When a nexus item is tossed out of an inventory, cancels the tossing event
	 * and gives the nexus back to the player that tossed it.
	 * 
	 * @param event the event of type {@link ItemTossEvent} that triggers this
	 *              method
	 */
	@SubscribeEvent
	public static void onNexusDroppedCancelEventAndGiveNexusBack(final ItemTossEvent event) {
		if (event.getPlayer().level.isClientSide)
			return;
		Item item = event.getEntityItem().getItem().getItem();
		if (!(item instanceof BlockItem)) {
			return;
		}
		if (!(((BlockItem) item).getBlock() instanceof NexusBlock)) {
			return;
		}
		event.setCanceled(true);
		Baseraids.LOGGER.warn("NexusBlock cannot be tossed");
		Baseraids.messageManager.sendStatusMessage("You cannot toss away the Nexus. It needs to be placed!");
		// Canceling the event means that the item is not dropped by is still removed
		// from the inventory.
		// Therefore, give it the nexus to the player again.
		giveNexusToPlayer(event.getPlayer());

	}

	/**
	 * Sets the state of {@link NexusBlock} to {@link NexusState#ITEM} when a nexus
	 * is picked up.
	 * 
	 * @param event the event of type {@link EntityItemPickupEvent} that triggers
	 *              this method
	 */
	@SubscribeEvent
	public static void onNexusPickedUpSetStateToITEM(final EntityItemPickupEvent event) {
		if (event.getPlayer().level.isClientSide)
			return;
		Item item = event.getItem().getItem().getItem();
		if (!(item instanceof BlockItem)) {
			return;
		}
		BlockItem blockitem = (BlockItem) item;
		if (!(blockitem.getBlock() instanceof NexusBlock)) {
			return;
		}
		setState(NexusState.ITEM);
	}

	/**
	 * Gives a nexus to the player when a player logs in and the state of the
	 * {@link NexusBlock} is {@link NexusState#UNINITIALIZED}. This should only
	 * occur in a world where the mod was not active yet.
	 * 
	 * @param event the event of type {@link PlayerEvent.PlayerLoggedInEvent} that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onPlayerLogInAndStateUNINITIALZEDGiveNexus(final PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getPlayer();
		Level level = player.level;
		if (level.isClientSide)
			return;
		Baseraids.LOGGER.debug("PlayerLoggedInEvent: curState = %s, numOfPlayers = %i", curState,
				level.players().size());
		if (curState != NexusState.UNINITIALIZED) {
			return;
		}
		Baseraids.LOGGER.info("PlayerLoggedInEvent giving nexus to player");
		NexusBlock.giveNexusToPlayer(player);
	}

	/**
	 * Attempts to transfer the nexus to another player when a player logs out with
	 * the nexus in his inventory. If there are not other players on the server or
	 * the transfer was not successful, this method allows the log out with the
	 * nexus.
	 * 
	 * @param event the event of type {@link PlayerEvent.PlayerLoggedOutEvent} that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onPlayerLogOutWithNexusTransferNexusToOtherPlayerOrIgnore(
			final PlayerEvent.PlayerLoggedOutEvent event) {
		Player playerLogOut = event.getPlayer();
		Level level = playerLogOut.level;
		if (level.isClientSide)
			return;
		if (!playerHasNexus(playerLogOut)) {
			return;
		}

		Baseraids.LOGGER.debug("PlayerLoggedOutEvent Player has nexus");

		// If there are other players on the server, randomly gives the nexus to one of
		// the other players.
		// Otherwise, this allows the log out with the nexus.
		List<? extends Player> playerList = level.players();
		playerList.remove(playerLogOut);
		if (!playerList.isEmpty() && giveNexusToRandomPlayerFromList(playerList)) {
			Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus given to other player");
			removeNexusFromPlayer(playerLogOut);
		}
	}

	private static void setState(NexusState state) {
		curState = state;
		Baseraids.worldManager.markDirty();
	}

	public static NexusState getState() {
		return curState;
	}

	private static void setBlockPos(BlockPos pos) {
		curBlockPos = pos;
		Baseraids.worldManager.markDirty();
	}

	public static BlockPos getBlockPos() {
		return curBlockPos;
	}

	/**
	 * Attempts to give the nexus to the specified player. If not successful, sends
	 * a chat message and returns false.
	 * 
	 * @param player the player that the nexus should be given to
	 * @return a flag whether the method was successful
	 */
	public static boolean giveNexusToPlayer(Player player) {
		if (playerHasNexus(player)) {
			// In some situations, the nexus could be in the inventory and the state could
			// be State.BLOCK at the same time.
			// In any case, we should make sure the state is set to State.ITEM here to stay
			// consistent.
			Baseraids.LOGGER.warn("NexusBlock already exists in player's inventory");
			setState(NexusState.ITEM);
			return true;
		}
		ItemStack itemStack = new ItemStack(Baseraids.NEXUS_ITEM.get());
		if (!player.addItem(itemStack)) {
			Baseraids.LOGGER.warn("NexusBlock could not be added to player's inventory");
			Baseraids.messageManager.sendStatusMessage("Error: Could not add Nexus to inventory!");
			return false;
		}
		Baseraids.LOGGER.debug("Successfully added nexus to player's inventory");
		setState(NexusState.ITEM);
		return true;
	}

	/**
	 * Attempts to give the nexus to a random player from a specified list of
	 * players. As long as it's not successful, it selects a new random player from
	 * the remaining list. If the remaining list is empty, it returns false.
	 * 
	 * @param playerList list of players to choose from
	 * @return a flag whether the method was successful
	 */
	private static boolean giveNexusToRandomPlayerFromList(List<? extends Player> playerList) {

		do {
			int randIndex = rand.nextInt(playerList.size());
			Player selectedPlayer = playerList.get(randIndex);
			playerList.remove(selectedPlayer);
			if (giveNexusToPlayer(selectedPlayer)) {
				return true;
			}
		} while (playerList.isEmpty());
		return false;
	}

	/**
	 * Removes all nexus items from the player's inventory and returns a flag
	 * whether it was successful.
	 * 
	 * @param player the player to remove the items from
	 * @return a flag whether the method was successful
	 */
	private static boolean removeNexusFromPlayer(Player player) {
		LazyOptional<IItemHandler> capabilityLazyOpt = player
				.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		Optional<Boolean> successful = capabilityLazyOpt.map(NexusBlock::removeNexusFromItemHandler);
		return successful.orElse(false);
	}

	/**
	 * Removes all nexus items from the given {@link IItemHandler} and returns a
	 * flag whether it was successful.
	 * 
	 * @param itemHandler the {@link IItemHandler} to remove the items from
	 * @return a flag whether the method was successful
	 */
	private static boolean removeNexusFromItemHandler(IItemHandler itemHandler) {
		ItemStack nexusItemStack = new ItemStack(Baseraids.NEXUS_ITEM.get());
		boolean successful = false;
		for (int i = 0; i < itemHandler.getSlots(); i++) {
			ItemStack stackInSlot = itemHandler.getStackInSlot(i);
			if (ItemStack.isSameItemSameTags(stackInSlot, nexusItemStack)) {
				itemHandler.extractItem(i, itemHandler.getStackInSlot(i).getCount(), false);
				Baseraids.LOGGER.debug("PlayerLoggedOutEvent Found and removed nexus stack");
				successful = true;
				// don't return to make sure we remove all nexus items
			}
		}
		return successful;
	}

	private static boolean playerHasNexus(Player player) {
		return player.getInventory().hasAnyOf(Sets.newHashSet(Baseraids.NEXUS_ITEM.get()));
	}

	/**
	 * Reads the data stored in the given {@link CompoundTag}. This function assumes
	 * that the nbt was previously written by this class or to be precise, that the
	 * nbt includes certain elements.
	 * 
	 * @param nbt the nbt that will be read out. It is assumed to include certain
	 *            elements.
	 */
	public static void read(CompoundTag nbt) {
		curState = NexusState.valueOf(nbt.getString("curState"));
		curBlockPos = new BlockPos(nbt.getInt("curBlockPosX"), nbt.getInt("curBlockPosY"), nbt.getInt("curBlockPosZ"));
	}

	/**
	 * Writes the necessary data to a {@link CompoundTag} and returns the
	 * {@link CompoundTag} object.
	 * 
	 * @return the adapted {@link CompoundTag} that was written to
	 */
	public static CompoundTag write() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("curState", curState.name());
		nbt.putInt("curBlockPosX", curBlockPos.getX());
		nbt.putInt("curBlockPosY", curBlockPos.getY());
		nbt.putInt("curBlockPosZ", curBlockPos.getZ());
		return nbt;
	}

}
