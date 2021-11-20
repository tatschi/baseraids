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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeBlock;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
public class NexusBlock extends Block implements IForgeBlock{

	static final Properties properties = Block.Properties.create(Material.ROCK)
			.hardnessAndResistance(15f, 30f)
			.harvestTool(ToolType.PICKAXE).harvestLevel(1)
			.sound(SoundType.GLASS)
			.setLightLevel((light) -> {return 15;});


	enum State{
		BLOCK, ITEM, UNINITIALIZED
	}

	private static State curState = State.UNINITIALIZED; 
	public static BlockPos curBlockPos = new BlockPos(0, 0, 0);


	/**
	 * 	Initializes the state of the nexus to UNINITIALIZED because
	 * 	the previous position or state will be loaded by readAdditional,
	 * 	otherwise a player will receive the nexus upon a PlayerLoggedInEvent (should only be the case on first log in in a world)
	 */
	public NexusBlock() {
		super(properties);
		
	}


	// Connect to TileEntity NexusEffectsTileEntity
	@Override
	public boolean hasTileEntity(BlockState state){
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new NexusEffectsTileEntity();
	}



	@SubscribeEvent
	public static void onBlockPlaced(final BlockEvent.EntityPlaceEvent event) {
		if(event.getWorld().isRemote()) return;
		if(event.getPlacedBlock().getBlock() instanceof NexusBlock) {
			setState(State.BLOCK);
			setBlockPos(event.getPos());
		}
	}


	@SubscribeEvent
	public static void onBlockBreak(final BlockEvent.BreakEvent event) {
		if(event.getPlayer().world.isRemote()) return;
		if(event.getState().getBlock() instanceof NexusBlock) {

			if(Baseraids.baseraidsData.raidManager.isRaidActive()) {
				event.setCanceled(true);
				Baseraids.LOGGER.warn("NexusBlock cannot be removed during raid");
				Baseraids.sendChatMessage("You cannot remove the nexus during a raid!");
			}
			PlayerEntity player = event.getPlayer();

			// try to give the nexus to the player, if it fails cancel the block breaking (a warning will be given inside the giveNexusToPlayer() method)
			if(!giveNexusToPlayer(player)) {
				event.setCanceled(true);
			}


		}

	}

	@SubscribeEvent
	public static void onItemDropped(final ItemTossEvent event) {
		if(event.getPlayer().world.isRemote()) return;
		if(event.getEntityItem().getItem().getItem() instanceof BlockItem) {
			ItemStack item = event.getEntityItem().getItem();


			if(((BlockItem) item.getItem()).getBlock() instanceof NexusBlock) {

				event.setCanceled(true);
				Baseraids.LOGGER.warn("NexusBlock cannot be tossed");
				Baseraids.sendChatMessage("You cannot toss away the Nexus. It needs to be placed!");
				giveNexusToPlayer(event.getPlayer());
			}
		}
	}

	@SubscribeEvent
	public static void onItemPickedUp(final EntityItemPickupEvent event) {
		if(event.getPlayer().world.isRemote()) return;

		ItemStack itemStack = event.getItem().getItem();
		Item item = itemStack.getItem();
		if(item instanceof BlockItem) {
			BlockItem blockitem = (BlockItem) item;
			if(blockitem.getBlock() instanceof NexusBlock) {

				Baseraids.LOGGER.warn("Nexus PickUp event was triggered");

				setState(State.ITEM);

			}
		}

	}
	
	/**
	 *  if the nexus has not been initialized via readAdditional (this should only be the case in a new world),
	 *  give the first player to join the world the nexus 
	 */
	@SubscribeEvent
    public static void onPlayerLogIn(final PlayerEvent.PlayerLoggedInEvent event) {
    	PlayerEntity player = event.getPlayer();
		World world = player.getEntityWorld(); 
    	if(world.isRemote()) return;
    	Baseraids.LOGGER.info("PlayerLoggedInEvent");
    	if(curState == State.UNINITIALIZED && world.getPlayers().size() == 1) {
    		Baseraids.LOGGER.info("PlayerLoggedInEvent giving nexus to player");
    		NexusBlock.giveNexusToPlayer(player);
    		
    	}
    	
    }

	/**
	 * if a player tries to log out with the Nexus in his inventory, cancel the event and send a chat message about it
	 * 
	 */
	@SubscribeEvent
	public static void onPlayerLogOut(final PlayerEvent.PlayerLoggedOutEvent event) {
		Baseraids.LOGGER.info("PlayerLoggedOutEvent");
		PlayerEntity playerLogOut = event.getPlayer();
		World world = playerLogOut.world;

		// is this correct? will this be called on the server side for every player?
		if(world.isRemote()) return;



		if(playerHasNexus(playerLogOut)) {

			Baseraids.LOGGER.info("PlayerLoggedOutEvent Player has nexus");
			event.setCanceled(false);
			Baseraids.sendChatMessage("You cannot log out with the Nexus in your inventory!");
			
			/*
			
			if(instance.curState != State.ITEM) {
				// TODO: double nexus
				return;
			}else {

				// select 
				List<? extends PlayerEntity> playerList = world.getPlayers();
				playerList.remove(playerLogOut);
				if(playerList.size() > 0) {
					// give nexus to other player (if he is not the only player
					Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus given to other player");
					giveNexusToRandomPlayerFromList(playerList);


				}else {

					// place Block in world
					Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus is placed on player position");
					world.setBlockState(playerLogOut.getPosition(), Baseraids.NEXUS_BLOCK.get().getDefaultState());
					// necessary or BlockPlacedEvent called anyways?
					setState(State.BLOCK);
					setBlockPos(playerLogOut.getPosition());
				}
			}

			// remove all nexus items from the player that is logging out
			IItemHandler itemHandler = (IItemHandler) playerLogOut.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElseThrow(null);

			for(int i = 0; i < itemHandler.getSlots(); i++) {

				if(ItemStack.areItemsEqual(itemHandler.getStackInSlot(i), new ItemStack(Baseraids.NEXUS_ITEM.get()))) {
					Baseraids.LOGGER.info("PlayerLoggedOutEvent Found nexus stack, removing...");
					itemHandler.extractItem(i, itemHandler.getStackInSlot(i).getCount(), false);
				}
			}*/
		}

	}


	// https://forums.minecraftforge.net/topic/42843-item-despawn-chest/
	// ItemExpireEvent
	// https://skmedix.github.io/ForgeJavaDocs/javadoc/forge/1.9.4-12.17.0.2051/net/minecraftforge/event/entity/item/ItemExpireEvent.html
	// check Entity#isDead() every tick?

	
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
	 * Attempts to give the nexus to the specified player. If not successful, sends a chat message and returns false.
	 * @param the player that the nexus should be given to
	 * @return flag whether the method was successful
	 */
	public static boolean giveNexusToPlayer(PlayerEntity player) {
		if(!playerHasNexus(player)) {

			// attempt to automatically give block to player 
			ItemStack itemStack = new ItemStack(Baseraids.NEXUS_ITEM.get());

			if(!player.addItemStackToInventory(itemStack)) {
				Baseraids.LOGGER.warn("NexusBlock could not be added to player's inventory");
				Baseraids.sendChatMessage("Could not add Nexus to inventory!");
				return false;
			}else {
				Baseraids.LOGGER.info("Successfully added nexus to player's inventory");
				setState(State.ITEM);
			}


		}else {
			Baseraids.LOGGER.warn("NexusBlock already exists in player's inventory");
		}
		return true;
	}


	private static void giveNexusToRandomPlayerFromList(List<? extends PlayerEntity> playerList) {
		// select random player from list
		Random rand = new Random();
		int rand_index = rand.nextInt(playerList.size());
		PlayerEntity selectedPlayer = playerList.get(rand_index);
		giveNexusToPlayer(selectedPlayer);
	}

	private static boolean playerHasNexus(PlayerEntity player) {
		return player.inventory.hasAny(Sets.newHashSet(Baseraids.NEXUS_ITEM.get()));
	}

	public static void readAdditional(CompoundNBT nbt) {
		curState = State.valueOf(nbt.getString("curState"));
		curBlockPos = new BlockPos(
				nbt.getInt("curBlockPosX"),
				nbt.getInt("curBlockPosY"),
				nbt.getInt("curBlockPosZ")
				);
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
