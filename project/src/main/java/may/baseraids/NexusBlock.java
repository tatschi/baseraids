package may.baseraids;


import java.util.List;
import java.util.Random;
import java.util.Set;


import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
public class NexusBlock extends Block implements IForgeBlock{


    
    static final Properties properties = Block.Properties.create(Material.ROCK)
			.hardnessAndResistance(15f, 30f)
			.harvestTool(ToolType.PICKAXE).harvestLevel(1)
			.sound(SoundType.GLASS)
			.setLightLevel((light) -> {return 12;});
	
	
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
		 if(event.getPlacedBlock().getBlock() instanceof NexusBlock) {
			 if(Baseraids.baseraidsData.placedNexusBlockPos.getX() != -1) {
				 Baseraids.LOGGER.info("Removed double nexus block");
				 event.setCanceled(true);
				 return;
			 }
			 
			 if(!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) {
				 Baseraids.LOGGER.info("Nexus can only be placed in the overworld");
				 event.setCanceled(true);
				 return;
			 }
			 
			 // set block as placed
			 Baseraids.baseraidsData.setPlacedNexusBlock(event.getPos());
			 
		 }
	 }
	 
	 @SubscribeEvent
	 public static void onBlockDestroyed(final BlockEvent.BreakEvent event) {
		 if(event.getState().getBlock() instanceof NexusBlock) {
			 Baseraids.baseraidsData.setPlacedNexusBlock(new BlockPos(-1, -1, -1));
		 }
		 
	 }
	 
	 @SubscribeEvent
	 public static void onItemDropped(final ItemTossEvent event) {
		 if(event.getEntityItem().getItem().getItem() instanceof BlockItem) {
			 BlockItem item = (BlockItem) event.getEntityItem().getItem().getItem();
			 
			 // Reference dropped nexus block in the baseraids class
			 if(item.getBlock() instanceof NexusBlock) {
				 Baseraids.nexusItem = event.getEntityItem();
				 
			 }
		 }
	 }
	 
	 @SubscribeEvent
	 public static void onItemPickedUp(final EntityItemPickupEvent event) {
		 Item item = event.getItem().getItem().getItem();
		 if(item instanceof BlockItem) {
			 BlockItem blockitem = (BlockItem) item;
			 if(blockitem.getBlock() instanceof NexusBlock) {
				// Reset nexus block reference in the baseraids class
				 Baseraids.nexusItem = null;
				 if(event.getPlayer().inventory.hasAny(Set.of(item))) {
					 // cancel picking up if item already in inventory
					 event.setCanceled(true);
					 event.getItem().remove();
				 }
			 }
		 }
	 }
	 
	 @SubscribeEvent
	 public static void onPlayerLogOut(final PlayerEvent.PlayerLoggedOutEvent event) {
		 Baseraids.LOGGER.info("PlayerLoggedOutEvent");
		 PlayerEntity playerLogOut = event.getPlayer();
		 World world = playerLogOut.world;
		 
		 if(playerLogOut.inventory.hasAny(Set.of(Baseraids.NEXUS_ITEM.get()))) {
			
			 Baseraids.LOGGER.info("PlayerLoggedOutEvent Player has nexus");
			 if(Baseraids.baseraidsData.placedNexusBlockPos.getX() == -1) {
				 // No nexus is placed in the world
				 
				 List<? extends PlayerEntity> playerList = world.getPlayers();
				 playerList.remove(playerLogOut);
				 if(playerList.size() > 0) {
					 Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus given to other player");
					 giveNexusToRandomPlayer(playerList);
				 }else {
					 // place Block in world
					 Baseraids.LOGGER.info("PlayerLoggedOutEvent Nexus is placed on player position");
					 world.setBlockState(playerLogOut.getPosition(), Baseraids.NEXUS_BLOCK.get().getDefaultState());
					 // necessary or BlockPlacedEvent called anyways?
					 Baseraids.baseraidsData.setPlacedNexusBlock(playerLogOut.getPosition());
				 }
			 }
			 
			// remove all nexus items from the player that is logging out
			 IItemHandler itemHandler = (IItemHandler) playerLogOut.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElseThrow(null);
			 
			 for(int i = 0; i < itemHandler.getSlots(); i++) {
				 
				 if(ItemStack.areItemsEqual(itemHandler.getStackInSlot(i), new ItemStack(Baseraids.NEXUS_ITEM.get()))) {
					 Baseraids.LOGGER.info("PlayerLoggedOutEvent Found nexus stack, removing...");
					 itemHandler.extractItem(i, itemHandler.getStackInSlot(i).getCount(), false);
				 }
			 }
		 }
		 
	 }
	 
	 // https://forums.minecraftforge.net/topic/42843-item-despawn-chest/
	 // ItemExpireEvent
	 // https://skmedix.github.io/ForgeJavaDocs/javadoc/forge/1.9.4-12.17.0.2051/net/minecraftforge/event/entity/item/ItemExpireEvent.html
	 // check Entity#isDead() every tick?
	 
	 // add function for giving a new NexusBlock to a random player
	 public static void giveNexusToRandomPlayer(List<? extends PlayerEntity> playerList) {
		 Random rand = new Random();
		 int rand_index = rand.nextInt(playerList.size());
		 PlayerEntity selectedPlayer = playerList.get(rand_index);
		 if(!selectedPlayer.inventory.hasAny(Set.of(Baseraids.NEXUS_ITEM.get()))) {
			 selectedPlayer.addItemStackToInventory(new ItemStack(Baseraids.NEXUS_ITEM.get()));
			 Baseraids.nexusItem = null;
		 }
	 }
	 
	 
	 
	 
}
