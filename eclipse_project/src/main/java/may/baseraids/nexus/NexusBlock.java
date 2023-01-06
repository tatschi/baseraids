package may.baseraids.nexus;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Sets;

import may.baseraids.Baseraids;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeBlock;
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
public class NexusBlock extends Block implements IForgeBlock {

	static final Properties properties = Block.Properties.create(Material.ROCK).hardnessAndResistance(15f, 30f)
			.harvestTool(ToolType.PICKAXE).harvestLevel(1).sound(SoundType.GLASS).setLightLevel((light) -> {
				return 15;
			});

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
	public static BlockPos curBlockPos = new BlockPos(0, 0, 0);

	public NexusBlock() {
		super(properties);
	}

	/**
	 * Overrides the inherited method to always return true, because this block has
	 * a <code>TileEntity</code> connected in any state.
	 * 
	 * @param state the <code>BlockState</code> that is checked for having a
	 *              <code>TileEntity</code>
	 * @return always true
	 */
	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	/**
	 * Creates a <code>NexusEffectsTileEntity</code> and returns it. This entity
	 * will be connected to the block.
	 * 
	 * @param state the <code>BlockState</code> for which the entity is created
	 * @param world the world in which the block is
	 * @returns the created <code>TileEntity</code>
	 */
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new NexusEffectsTileEntity();
	}

	/**
	 * Adds a debuff with properties specified in the inner class
	 * <code>Debuff</code> to all players, if the nexus is not placed. This is done
	 * in all dimensions.
	 * 
	 * @param event the event of type <code>TickEvent.WorldTickEvent</code> that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onWorldTick_addDebuff(final TickEvent.WorldTickEvent event) {
		World world = event.world;
		if (world.isRemote) {
			return;
		}
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		if (NexusBlock.getState() == NexusState.BLOCK) {
			return;
		}
		// only apply debuff every time half the duration of the effect has passed
		if (world.getGameTime() % (NexusEffects.DEBUFF.DURATION / 2) != 0L) {
			return;
		}

		for (PlayerEntity playerentity : world.getPlayers()) {
			playerentity.addPotionEffect(NexusEffects.getEffectInstance(NexusEffects.DEBUFF));
		}
	}

	/**
	 * Sets the state to <code>State.BLOCK</code> and the new position of the nexus,
	 * when a nexus block is placed.
	 * 
	 * @param event the event of type <code>BlockEvent.EntityPlaceEvent</code> that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onNexusPlaced_setStateAndBlockPos(final BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isRemote())
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
	 * @param event the event of type <code>BlockEvent.BreakEvent</code> that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onNexusBreak_giveNexusOrCancelEvent(final BlockEvent.BreakEvent event) {
		if (event.getPlayer().world.isRemote())
			return;
		if (!(event.getState().getBlock() instanceof NexusBlock)) {
			return;
		}
		
		if (Baseraids.worldManager.getRaidManager().isRaidActive()) {
			event.setCanceled(true);
			Baseraids.LOGGER.warn("NexusBlock cannot be removed during raid");
			Baseraids.messageManager.sendStatusMessage("The NexusBlock cannot be removed during raid!", event.getPlayer(), true);
			return;
		}
		if (!giveNexusToPlayer(event.getPlayer())) {
			event.setCanceled(true);
		}
		Baseraids.worldManager.getServerWorld().getTileEntity(getBlockPos()).remove();
	}

	/**
	 * When a nexus item is tossed out of an inventory, cancels the tossing event
	 * and gives the nexus back to the player that tossed it.
	 * 
	 * @param event the event of type <code>ItemTossEvent</code> that triggers this
	 *              method
	 */
	@SubscribeEvent
	public static void onNexusDropped_cancelEventAndGiveNexusBack(final ItemTossEvent event) {
		if (event.getPlayer().world.isRemote())
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
	 * Sets the state of <code>NexusBlock</code> to <code>State.ITEM</code> when a
	 * nexus is picked up.
	 * 
	 * @param event the event of type <code>EntityItemPickupEvent</code> that
	 *              triggers this method
	 */
	@SubscribeEvent
	public static void onNexusPickedUp_setStateToITEM(final EntityItemPickupEvent event) {
		if (event.getPlayer().world.isRemote())
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
	 * <code>NexusBlock</code> is <code>State.UNINITIALIZED</code>. This should only
	 * occur in a world where the mod was not active yet.
	 * 
	 * @param event the event of type <code>PlayerEvent.PlayerLoggedInEvent</code>
	 *              that triggers this method
	 */
	@SubscribeEvent
	public static void onPlayerLogInAndStateUNINITIALZED_giveNexus(final PlayerEvent.PlayerLoggedInEvent event) {
		PlayerEntity player = event.getPlayer();
		World world = player.getEntityWorld();
		if (world.isRemote())
			return;
		Baseraids.LOGGER
				.debug("PlayerLoggedInEvent: curState = " + curState + ", numOfPlayers = " + world.getPlayers().size());
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
	 * @param event the event of type <code>PlayerEvent.PlayerLoggedOutEvent</code>
	 *              that triggers this method
	 */
	@SubscribeEvent
	public static void onPlayerLogOutWithNexus_transferNexusToOtherPlayerOrIgnore(
			final PlayerEvent.PlayerLoggedOutEvent event) {
		PlayerEntity playerLogOut = event.getPlayer();
		World world = playerLogOut.world;
		if (world.isRemote())
			return;
		if (!playerHasNexus(playerLogOut)) {
			return;
		}

		Baseraids.LOGGER.debug("PlayerLoggedOutEvent Player has nexus");

		// If there are other players on the server, randomly gives the nexus to one of
		// the other players.
		// Otherwise, this allows the log out with the nexus.
		List<? extends PlayerEntity> playerList = world.getPlayers();
		playerList.remove(playerLogOut);
		if (playerList.size() > 0) {
			if (giveNexusToRandomPlayerFromList(playerList)) {
				Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus given to other player");
				removeNexusFromPlayer(playerLogOut);
				return;
			}
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
	public static boolean giveNexusToPlayer(PlayerEntity player) {
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
		if (!player.addItemStackToInventory(itemStack)) {
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
	 * players. As long as it's not successfull, it selects a new random player from
	 * the remaining list. If the remaining list is empty, it returns false.
	 * 
	 * @param playerList list of players to choose from
	 * @return a flag whether the method was successful
	 */
	private static boolean giveNexusToRandomPlayerFromList(List<? extends PlayerEntity> playerList) {
		Random rand = new Random();
		do {
			int rand_index = rand.nextInt(playerList.size());
			PlayerEntity selectedPlayer = playerList.get(rand_index);
			playerList.remove(selectedPlayer);
			if (giveNexusToPlayer(selectedPlayer)) {
				return true;
			}
		} while (playerList.size() <= 0);
		return false;
	}

	/**
	 * Removes all nexus items from the player's inventory and returns a flag
	 * whether it was successful.
	 * 
	 * @param player the player to remove the items from
	 * @return a flag whether the method was successful
	 */
	private static boolean removeNexusFromPlayer(PlayerEntity player) {
		IItemHandler itemHandler = (IItemHandler) player
				.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElseThrow(null);
		ItemStack nexusItemStack = new ItemStack(Baseraids.NEXUS_ITEM.get());
		boolean successful = false;
		for (int i = 0; i < itemHandler.getSlots(); i++) {
			ItemStack stackInSlot = itemHandler.getStackInSlot(i);
			if (ItemStack.areItemsEqual(stackInSlot, nexusItemStack)) {
				itemHandler.extractItem(i, itemHandler.getStackInSlot(i).getCount(), false);
				Baseraids.LOGGER.debug("PlayerLoggedOutEvent Found and removed nexus stack");
				successful = true;
				// don't return to make sure we remove all nexus items
			}
		}
		return successful;
	}

	private static boolean playerHasNexus(PlayerEntity player) {
		return player.inventory.hasAny(Sets.newHashSet(Baseraids.NEXUS_ITEM.get()));
	}

	/**
	 * Reads the data stored in the given <code>CompoundNBT</code>. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements.
	 * 
	 * @param nbt the nbt that will be read out. It is assumed to include certain
	 *            elements.
	 */
	public static void readAdditional(CompoundNBT nbt) {
		curState = NexusState.valueOf(nbt.getString("curState"));
		curBlockPos = new BlockPos(nbt.getInt("curBlockPosX"), nbt.getInt("curBlockPosY"), nbt.getInt("curBlockPosZ"));
	}

	/**
	 * Writes the necessary data to a <code>CompoundNBT</code> and returns the
	 * <code>CompoundNBT</code> object.
	 * 
	 * @return the adapted <code>CompoundNBT</code> that was written to
	 */
	public static CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString("curState", curState.name());
		nbt.putInt("curBlockPosX", curBlockPos.getX());
		nbt.putInt("curBlockPosY", curBlockPos.getY());
		nbt.putInt("curBlockPosZ", curBlockPos.getZ());
		return nbt;
	}

}
