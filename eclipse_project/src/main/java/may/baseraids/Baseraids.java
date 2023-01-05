package may.baseraids;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import may.baseraids.config.Config;
import may.baseraids.config.ConfigOptions;
import may.baseraids.entities.BaseraidsEntityManager;
import may.baseraids.nexus.NexusBlock;
import may.baseraids.nexus.NexusEffectsTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The base class of this mod. Here, blocks, items and entities are registered,
 * saving and loading is intiated and basic, general functionality is provided.
 * 
 * @author Natascha May
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
//The value here should match an entry in the META-INF/mods.toml file
@Mod("baseraids")
public class Baseraids {
	public static final String MODID = "baseraids";

	public static final Logger LOGGER = LogManager.getLogger();

	// REGISTRIES
	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
			Baseraids.MODID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Baseraids.MODID);
	public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister
			.create(ForgeRegistries.TILE_ENTITIES, Baseraids.MODID);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES,
			Baseraids.MODID);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
			Baseraids.MODID);

	// BLOCKS & ITEMS
	public static final RegistryObject<Block> NEXUS_BLOCK = BLOCKS.register("nexus_block", NexusBlock::new);
	public static final RegistryObject<BlockItem> NEXUS_ITEM = ITEMS.register("nexus_block",
			() -> new BlockItem(Baseraids.NEXUS_BLOCK.get(), new Item.Properties().group(ItemGroup.COMBAT)));

	// ENTITIES
	public static final RegistryObject<TileEntityType<NexusEffectsTileEntity>> NEXUS_TILE_ENTITY_TYPE = TILE_ENTITIES
			.register("nexus_effects_tile_entity", () -> TileEntityType.Builder
					.create(NexusEffectsTileEntity::new, Baseraids.NEXUS_BLOCK.get()).build(null));

	// SOUNDS
	public static final RegistryObject<SoundEvent> SOUND_RAID_WON = SOUNDS.register("raid_win",
			() -> new SoundEvent(new ResourceLocation(Baseraids.MODID, "raid_win")));
	public static final RegistryObject<SoundEvent> SOUND_RAID_LOST = SOUNDS.register("raid_lose",
			() -> new SoundEvent(new ResourceLocation(Baseraids.MODID, "raid_lose")));
	public static final RegistryObject<SoundEvent> SOUND_RAID_TICKING = SOUNDS.register("pock_ticking",
			() -> new SoundEvent(new ResourceLocation(Baseraids.MODID, "pock_ticking")));
	public static final RegistryObject<SoundEvent> SOUND_RAID_ACTIVE = SOUNDS.register("pock_low",
			() -> new SoundEvent(new ResourceLocation(Baseraids.MODID, "pock_low")));

	private static BaseraidsWorldSavedData baseraidsData;

	/**
	 * Registers all registries, the mod event bus and loads the config file using
	 * the class {@link Config}.
	 */
	public Baseraids() {

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.config);

		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		// Register the setup method for modloading
		bus.addListener(this::onFMLCommonSetup);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		// register custom registries
		BLOCKS.register(bus);
		ITEMS.register(bus);
		TILE_ENTITIES.register(bus);
		ENTITIES.register(bus);
		SOUNDS.register(bus);

		Config.loadConfig(Config.config, FMLPaths.CONFIGDIR.get().resolve(MODID + ".toml").toString());
	}

	/**
	 * Registers the attributes for the custom entity types and registers the
	 * renderers for the custom entity types. Called through the
	 * {@link FMLCommonSetupEvent}.
	 * 
	 * @param event the event of type {@link FMLCommonSetupEvent} that calls this
	 *              function
	 */
	private void onFMLCommonSetup(final FMLCommonSetupEvent event) {
		BaseraidsEntityManager.registerSetups();
	}

	/**
	 * Initiates the loading process for this mod using the class
	 * {@link BaseraidsWorldSavedData} when the world is loaded.
	 * 
	 * @param event the event of type {@link WorldEvent.Load} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldLoaded_loadBaseraidsWorldSavedData(final WorldEvent.Load event) {
		if (event.getWorld().isRemote() || !((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD))
			return;

		if (event.getWorld() instanceof ServerWorld) {
			baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
		}

	}

	/**
	 * Initiates the saving process for this mod using the class
	 * {@link BaseraidsWorldSavedData} when the world is saved.
	 * 
	 * @param event the event of type {@link WorldEvent.Save} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldSaved_saveBaseraidsWorldSavedData(final WorldEvent.Save event) {
		if (event.getWorld().isRemote() || !((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD))
			return;
		if (event.getWorld() instanceof ServerWorld) {
			baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
		}
	}

	/**
	 * 
	 * @param event the event of type {@link WorldEvent.PotentialSpawns} that calls
	 *              this function
	 */
	@SubscribeEvent
	public void onMonsterSpawn(final WorldEvent.PotentialSpawns event) {
		World world = (World) event.getWorld();
		if (world.isRemote())
			return;

		if (!event.isCancelable()) {
			return;
		}

		if (onMonsterSpawnOutsideCave_shouldCancelSpawn(event)) {
			event.setCanceled(true);
		}
	}

	/**
	 * Cancels a monster spawning event, if it is not inside a cave and the config
	 * option {@link ConfigOptions#deactivateMonsterNightSpawn} is true.
	 * 
	 * @param event the event of type {@link WorldEvent.PotentialSpawns} that calls
	 *              this function
	 */
	private boolean onMonsterSpawnOutsideCave_shouldCancelSpawn(final WorldEvent.PotentialSpawns event) {
		if (!ConfigOptions.deactivateMonsterNightSpawn.get()) {
			return false;
		}

		if (!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) {
			return false;
		}

		if (event.getType() != EntityClassification.MONSTER) {
			return false;
		}

		if (!event.getWorld().getBlockState(event.getPos()).equals(Blocks.CAVE_AIR.getDefaultState())) {
			return true;
		}

		if (event.getWorld().getHeight(Heightmap.Type.WORLD_SURFACE, event.getPos()).equals(event.getPos())) {
			return true;
		}

		if (event.getWorld().canSeeSky(event.getPos())) {
			return true;
		}

		return false;
	}

	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param message the string that is sent in the chat
	 */
	public static void sendStatusMessage(String message) {
		sendStatusMessage(message, true);
	}

	/**
	 * Sends a status message to all players on the server.
	 * 
	 * @param message   the string that is sent in the chat
	 * @param actionBar boolean whether to show the message in the actionBar (true)
	 *                  or in the chat (false)
	 */
	public static void sendStatusMessage(String message, boolean actionBar) {
		LOGGER.debug("Sending chat message: \"" + message + "\"");
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
				.forEach(x -> x.sendStatusMessage(new StringTextComponent(message), actionBar));
	}

	/**
	 * Sends a status message to a specific player.
	 * 
	 * @param message the string that is sent in the chat
	 * @param player  the player that the message is sent to
	 */
	public static void sendStatusMessage(String message, PlayerEntity player) {
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
	public static void sendStatusMessage(String message, PlayerEntity player, boolean actionBar) {
		LOGGER.debug("Sending chat message: \"" + message + "\" to " + player.getDisplayName().getString());
		player.sendStatusMessage(new StringTextComponent(message), actionBar);
	}

	/**
	 * Converts a {@link BlockPos} to a Vector3d for convenience.
	 * 
	 * @param pos the given {@link BlockPos}
	 * @return the {@link Vector3d} converted from the {@link BlockPos}
	 */
	public static Vector3d getVector3dFromBlockPos(BlockPos pos) {
		return new Vector3d(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public static void markDirty() {
		baseraidsData.setDirty(true);
	}
	
	public static RaidManager getRaidManager() {
		return baseraidsData.raidManager;
	}
	
	public static ServerWorld getServerWorld() {
		return baseraidsData.serverWorld;
	}

}
