package may.baseraids;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.CapabilityItemHandler;

/**
 * This class handles the nexus block and its special behavior that ensures that
 * there is always exactly one nexus on a server (except in creative mode).
 * <p>
 * Details on the behavior:
 * <ul>
 * <li>When a player joins a world and the nexus is still uninitialized, he is
 * given a nexus. @see onPlayerLogIn
 * <li>When a player exits a world and has the nexus in his inventory, it is
 * attempted to give the nexus to another player on the server and remove it
 * from the player that is logging out. @see onPlayerLogOut
 * <li>When the nexus is not placed as a block, a debuff is added to every
 * player in the world. @see onWorldTick, @see addDebuff
 * <li>When the nexus is block is broken by a player, the item is not dropped
 * but directly added to the players inventory. @see onBlockBreak
 * <li>The nexus cannot be tossed out of the inventory. @see onItemDropped
 * </ul>
 * 
 * @author Natascha May
 * @since 1.16.4-0.1
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NexusBlock extends Block implements IForgeBlock {

	static final Properties properties = Block.Properties.create(Material.ROCK).hardnessAndResistance(15f, 30f)
			.harvestTool(ToolType.PICKAXE).harvestLevel(1).sound(SoundType.GLASS).setLightLevel((light) -> {
				return 15;
			});

	enum State {
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
	private static State curState = State.UNINITIALIZED;
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

	// Connect to TileEntity NexusEffectsTileEntity
	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new NexusEffectsTileEntity();
	}

	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		// ticks in each dimension
		World world = event.world;

		// as long as the nexus block is not placed, regularly add a debuff to all
		// players in the world
		if (NexusBlock.getState() != State.BLOCK) {
			if (world.getGameTime() % 80L == 0L) {
				addDebuff(world);
			}
		}
	}

	@SubscribeEvent
	public static void onBlockPlaced(final BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isRemote())
			return;
		if (event.getPlacedBlock().getBlock() instanceof NexusBlock) {
			setState(State.BLOCK);
			setBlockPos(event.getPos());
		}
	}

	@SubscribeEvent
	public static void onBlockBreak(final BlockEvent.BreakEvent event) {
		if (event.getPlayer().world.isRemote())
			return;
		if (event.getState().getBlock() instanceof NexusBlock) {

			if (Baseraids.baseraidsData.raidManager.isRaidActive()) {
				event.setCanceled(true);
				Baseraids.LOGGER.warn("NexusBlock cannot be removed during raid");
				Baseraids.sendChatMessage("You cannot remove the nexus during a raid!");
			}
			PlayerEntity player = event.getPlayer();

			// try to give the nexus to the player, if it fails cancel the block breaking (a
			// warning will be given inside the giveNexusToPlayer() method)
			if (!giveNexusToPlayer(player)) {
				event.setCanceled(true);
			}

		}

	}

	@SubscribeEvent
	public static void onItemDropped(final ItemTossEvent event) {
		if (event.getPlayer().world.isRemote())
			return;
		if (event.getEntityItem().getItem().getItem() instanceof BlockItem) {
			ItemStack item = event.getEntityItem().getItem();

			if (((BlockItem) item.getItem()).getBlock() instanceof NexusBlock) {

				event.setCanceled(true);
				Baseraids.LOGGER.warn("NexusBlock cannot be tossed");
				Baseraids.sendChatMessage("You cannot toss away the Nexus. It needs to be placed!");
				giveNexusToPlayer(event.getPlayer());
			}
		}
	}

	@SubscribeEvent
	public static void onItemPickedUp(final EntityItemPickupEvent event) {
		if (event.getPlayer().world.isRemote())
			return;

		ItemStack itemStack = event.getItem().getItem();
		Item item = itemStack.getItem();
		if (item instanceof BlockItem) {
			BlockItem blockitem = (BlockItem) item;
			if (blockitem.getBlock() instanceof NexusBlock) {

				Baseraids.LOGGER.warn("Nexus PickUp event was triggered");

				setState(State.ITEM);

			}
		}

	}

	/**
	 * if the nexus has not been initialized via readAdditional (this should only be
	 * the case in a new world), give the player that just joined the world the
	 * nexus
	 */
	@SubscribeEvent
	public static void onPlayerLogIn(final PlayerEvent.PlayerLoggedInEvent event) {
		PlayerEntity player = event.getPlayer();
		World world = player.getEntityWorld();
		if (world.isRemote())
			return;
		Baseraids.LOGGER
				.info("PlayerLoggedInEvent: curState = " + curState + ", numOfPlayers = " + world.getPlayers().size());
		if (curState == State.UNINITIALIZED) {
			Baseraids.LOGGER.info("PlayerLoggedInEvent giving nexus to player");
			NexusBlock.giveNexusToPlayer(player);

		}

	}

	/**
	 * if a player tries to log out with the Nexus in his inventory, cancel the
	 * event and send a chat message about it
	 * 
	 */
	@SubscribeEvent
	public static void onPlayerLogOut(final PlayerEvent.PlayerLoggedOutEvent event) {
		Baseraids.LOGGER.info("PlayerLoggedOutEvent");
		PlayerEntity playerLogOut = event.getPlayer();
		World world = playerLogOut.world;

		// is this correct? will this be called on the server side for every player?
		if (world.isRemote())
			return;

		if (playerHasNexus(playerLogOut)) {

			Baseraids.LOGGER.info("PlayerLoggedOutEvent Player has nexus");

			// if there is another player on the server, give him the nexus
			List<? extends PlayerEntity> playerList = world.getPlayers();
			playerList.remove(playerLogOut);
			if (playerList.size() > 0) {
				Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus given to other player");
				if (giveNexusToRandomPlayerFromList(playerList)) {
					return;
				}
			}

			// remove all nexus items from the player that is logging out
			IItemHandler itemHandler = (IItemHandler) playerLogOut
					.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElseThrow(null);

			for (int i = 0; i < itemHandler.getSlots(); i++) {

				if (ItemStack.areItemsEqual(itemHandler.getStackInSlot(i), new ItemStack(Baseraids.NEXUS_ITEM.get()))) {
					Baseraids.LOGGER.info("PlayerLoggedOutEvent Found nexus stack, removing...");
					itemHandler.extractItem(i, itemHandler.getStackInSlot(i).getCount(), false);
				}
			}
		}

	}

	private static void setState(State state) {
		curState = state;
		Baseraids.baseraidsData.markDirty();
	}

	public static State getState() {
		return curState;
	}

	private static void setBlockPos(BlockPos pos) {
		curBlockPos = pos;
		Baseraids.baseraidsData.markDirty();
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
		if (!playerHasNexus(player)) {

			// attempt to automatically give block to player
			ItemStack itemStack = new ItemStack(Baseraids.NEXUS_ITEM.get());

			if (!player.addItemStackToInventory(itemStack)) {
				Baseraids.LOGGER.warn("NexusBlock could not be added to player's inventory");
				Baseraids.sendChatMessage("Could not add Nexus to inventory!");
				return false;
			} else {
				Baseraids.LOGGER.info("Successfully added nexus to player's inventory");
				setState(State.ITEM);
			}

		} else {
			Baseraids.LOGGER.warn("NexusBlock already exists in player's inventory");
			// in some situations, the nexus could be in the inventory and the state could
			// be State.BLOCK at the same time
			// in any case, we should set the state to State.ITEM here to stay consistent
			setState(State.ITEM);
		}
		return true;
	}

	/**
	 * Adds a slowness debuff to all players in the world.
	 * 
	 * @param world the world in which the debuff is added
	 */
	private void addDebuff(World world) {
		Effect effect = Effects.SLOWNESS;
		if (!world.isRemote) {
			int amplifier = 0;
			int duration = 200;

			List<? extends PlayerEntity> list = world.getPlayers();

			for (PlayerEntity playerentity : list) {
				playerentity.addPotionEffect(new EffectInstance(effect, duration, amplifier, true, true));
			}
		}
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

	private static boolean playerHasNexus(PlayerEntity player) {
		return player.inventory.hasAny(Sets.newHashSet(Baseraids.NEXUS_ITEM.get()));
	}

	public static void readAdditional(CompoundNBT nbt) {
		curState = State.valueOf(nbt.getString("curState"));
		curBlockPos = new BlockPos(nbt.getInt("curBlockPosX"), nbt.getInt("curBlockPosY"), nbt.getInt("curBlockPosZ"));
	}

	public static CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString("curState", curState.name());
		nbt.putInt("curBlockPosX", curBlockPos.getX());
		nbt.putInt("curBlockPosY", curBlockPos.getY());
		nbt.putInt("curBlockPosZ", curBlockPos.getZ());

		return nbt;
	}

}
