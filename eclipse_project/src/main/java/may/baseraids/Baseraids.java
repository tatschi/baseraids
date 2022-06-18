package may.baseraids;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import may.baseraids.config.Config;
import may.baseraids.config.ConfigOptions;
import may.baseraids.entities.BaseraidsEntityManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The base class of this mod. Here, blocks, items and entities are registered,
 * saving and loading is intiated and basic, general functionality is provided.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1
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
	private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister
			.create(ForgeRegistries.TILE_ENTITIES, Baseraids.MODID);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES,
			Baseraids.MODID);

	// BLOCKS & ITEMS
	public static final RegistryObject<Block> NEXUS_BLOCK = BLOCKS.register("nexus_block", () -> new NexusBlock());
	public static final RegistryObject<BlockItem> NEXUS_ITEM = ITEMS.register("nexus_block",
			() -> new BlockItem(Baseraids.NEXUS_BLOCK.get(), new Item.Properties().group(ItemGroup.COMBAT)));

	// ENTITIES
	public static final RegistryObject<TileEntityType<NexusEffectsTileEntity>> NEXUS_TILE_ENTITY_TYPE = TILE_ENTITIES
			.register("nexus_effects_tile_entity", () -> TileEntityType.Builder
					.create(NexusEffectsTileEntity::new, Baseraids.NEXUS_BLOCK.get()).build(null));

	public static final HashMap<String, EntityType<?>> configRegister = new HashMap<>();

	public static BaseraidsWorldSavedData baseraidsData;

	/**
	 * Registers all registries, the mod event bus and loads the config file using
	 * the class <code>Config</code>.
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

		Config.loadConfig(Config.config, FMLPaths.CONFIGDIR.get().resolve(MODID + ".toml").toString());

	}

	/**
	 * Registers the attributes for the custom entity types and registers the
	 * renderers for the custom entity types. Called through the
	 * <code>FMLCommonSetupEvent</code>.
	 * 
	 * @param event the event of type <code>FMLCommonSetupEvent</code> that calls
	 *              this function
	 */
	private void onFMLCommonSetup(final FMLCommonSetupEvent event) {
		BaseraidsEntityManager.registerSetups();
	}

	/**
	 * Initiates the loading process for this mod using the class
	 * <code>BaseraidsWorldSavedData</code> when the world is loaded.
	 * 
	 * @param event the event of type <code>WorldEvent.Load</code> that calls this
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
	 * <code>BaseraidsWorldSavedData</code> when the world is saved.
	 * 
	 * @param event the event of type <code>WorldEvent.Save</code> that calls this
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
	 * @param event the event of type <code>WorldEvent.PotentialSpawns</code> that
	 *              calls this function
	 */
	@SubscribeEvent
	public void onMonsterSpawn(final WorldEvent.PotentialSpawns event) {
		World world = (World) event.getWorld();
		if (world.isRemote())
			return;

		onMonsterSpawnOutsideCave_cancelSpawning(event);
	}

	/**
	 * Cancels a monster spawning event, if it is not inside a cave and the config
	 * option <code>ConfigOptions.deactivateMonsterNightSpawn</code> is true.
	 * 
	 * @param event the event of type <code>WorldEvent.PotentialSpawns</code> that
	 *              calls this function
	 */
	// TODO check if this also disables monsters in the nether and end which would
	// not be desired
	private void onMonsterSpawnOutsideCave_cancelSpawning(final WorldEvent.PotentialSpawns event) {

		if (!ConfigOptions.deactivateMonsterNightSpawn.get())
			return;

		if (event.getType() == EntityClassification.MONSTER)
			return;

		if (event.getWorld().getBlockState(event.getPos()).equals(Blocks.CAVE_AIR.getDefaultState()))
			return;

		if (event.isCancelable()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onMonsterSpawnDuringRaid_setupGoals(final EntityJoinWorldEvent event) {
		if (!baseraidsData.raidManager.isRaidActive())
			return;
		if (!(event.getEntity() instanceof MobEntity))
			return;

		MobEntity mobEntity = (MobEntity) event.getEntity();

		// unsupported entities are handled in the setupGoals method

		BaseraidsEntityManager.setupGoals(mobEntity);
	}

	/**
	 * Sends a string message in the in-game chat to all players on the server.
	 * 
	 * @param message the string that is sent in the chat
	 */
	public static void sendChatMessage(String message) {
		for (PlayerEntity player : Minecraft.getInstance().getIntegratedServer().getPlayerList().getPlayers()) {
			player.sendMessage(new StringTextComponent(message), null);
		}
	}

	/**
	 * Sends a string message in the in-game chat to a specific player.
	 * 
	 * @param message the string that is sent in the chat
	 * @param player  the player that the message is sent to
	 */
	public static void sendChatMessage(String message, PlayerEntity player) {
		player.sendMessage(new StringTextComponent(message), null);
	}

}
