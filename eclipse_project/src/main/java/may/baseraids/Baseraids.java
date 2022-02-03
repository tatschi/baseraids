package may.baseraids;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import may.baseraids.config.*;
import may.baseraids.entities.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.monster.AbstractSkeletonEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
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

	public static final RegistryObject<EntityType<BaseraidsZombieEntity>> BASERAIDS_ZOMBIE_ENTITY_TYPE = ENTITIES
			.register("baseraids_zombie_entity",
					() -> EntityType.Builder
							.<BaseraidsZombieEntity>create(BaseraidsZombieEntity::new, EntityClassification.MONSTER)
							.build("baseraids_zombie_entity"));
	public static final RegistryObject<EntityType<BaseraidsSkeletonEntity>> BASERAIDS_SKELETON_ENTITY_TYPE = ENTITIES
			.register("baseraids_skeleton_entity",
					() -> EntityType.Builder
							.<BaseraidsSkeletonEntity>create(BaseraidsSkeletonEntity::new, EntityClassification.MONSTER)
							.build("baseraids_skeleton_entity"));
	public static final RegistryObject<EntityType<BaseraidsSpiderEntity>> BASERAIDS_SPIDER_ENTITY_TYPE = ENTITIES
			.register("baseraids_spider_entity",
					() -> EntityType.Builder
							.<BaseraidsSpiderEntity>create(BaseraidsSpiderEntity::new, EntityClassification.MONSTER)
							.build("baseraids_spider_entity"));
	public static final RegistryObject<EntityType<BaseraidsPhantomEntity>> BASERAIDS_PHANTOM_ENTITY_TYPE = ENTITIES
			.register("baseraids_phantom_entity",
					() -> EntityType.Builder
							.<BaseraidsPhantomEntity>create(BaseraidsPhantomEntity::new, EntityClassification.MONSTER)
							.build("baseraids_phantom_entity"));

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
		bus.addListener(this::onFMLCommonSetup_registerEntityAttributesAndRenderers);

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
	 * Registers the entity types during the
	 * <code>RegistryEvent.Register<EntityType<?>></code> to be usable in the class
	 * <code>ConfigOptions</code>.
	 * 
	 * @param event the event of type
	 *              <code>RegistryEvent.Register<EntityType<?>></code> that calls
	 *              this function
	 */
	@SubscribeEvent
	public void onRegisterEntityTypes_registerConfigEntityTypes(final RegistryEvent.Register<EntityType<?>> event) {
		configRegister.put(BaseraidsZombieEntity.CONFIG_NAME, BASERAIDS_ZOMBIE_ENTITY_TYPE.get());
		configRegister.put(BaseraidsSkeletonEntity.CONFIG_NAME, BASERAIDS_SKELETON_ENTITY_TYPE.get());
		configRegister.put(BaseraidsSpiderEntity.CONFIG_NAME, BASERAIDS_SPIDER_ENTITY_TYPE.get());
		configRegister.put(BaseraidsPhantomEntity.CONFIG_NAME, BASERAIDS_PHANTOM_ENTITY_TYPE.get());
	}

	/**
	 * Registers the attributes for the custom entity types and registers the
	 * renderers for the custom entity types. Called through the
	 * <code>FMLCommonSetupEvent</code>.
	 * 
	 * @param event the event of type <code>FMLCommonSetupEvent</code> that calls
	 *              this function
	 */
	@SuppressWarnings("unchecked")
	private void onFMLCommonSetup_registerEntityAttributesAndRenderers(final FMLCommonSetupEvent event) {

		// Connects attributes of custom entity types to those of the inherited vanilla
		// entity types.
		// If custom attributes are desired, find the function in the inherited entitity
		// type (for example ZombieEntity#func_234342_eQ_()), mimic that function
		// and call it here instead.
		GlobalEntityTypeAttributes.put(BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), ZombieEntity.func_234342_eQ_().create());
		GlobalEntityTypeAttributes.put(BASERAIDS_SKELETON_ENTITY_TYPE.get(),
				AbstractSkeletonEntity.registerAttributes().create());
		GlobalEntityTypeAttributes.put(BASERAIDS_SPIDER_ENTITY_TYPE.get(), SpiderEntity.func_234305_eI_().create());
		GlobalEntityTypeAttributes.put(BASERAIDS_PHANTOM_ENTITY_TYPE.get(), LivingEntity.registerAttributes().create());

		EntityRendererManager renderManager = Minecraft.getInstance().getRenderManager();

		// Adds renderers from the vanilla entity types to the custom entity types
		EntityRenderer<ZombieEntity> zombieRenderer = (EntityRenderer<ZombieEntity>) renderManager.renderers
				.get(EntityType.ZOMBIE);
		renderManager.register(BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), zombieRenderer);
		EntityRenderer<SkeletonEntity> skeletonRenderer = (EntityRenderer<SkeletonEntity>) renderManager.renderers
				.get(EntityType.SKELETON);
		renderManager.register(BASERAIDS_SKELETON_ENTITY_TYPE.get(), skeletonRenderer);
		EntityRenderer<BaseraidsSpiderEntity> spiderRenderer = (EntityRenderer<BaseraidsSpiderEntity>) renderManager.renderers
				.get(EntityType.SPIDER);
		renderManager.register(BASERAIDS_SPIDER_ENTITY_TYPE.get(), spiderRenderer);
		EntityRenderer<BaseraidsPhantomEntity> phantomRenderer = (EntityRenderer<BaseraidsPhantomEntity>) renderManager.renderers
				.get(EntityType.PHANTOM);
		renderManager.register(BASERAIDS_PHANTOM_ENTITY_TYPE.get(), phantomRenderer);
	}

	/**
	 * Initiates the loading process for this mod using the class
	 * <code>BaseraidsWorldSavedData</code> when the world is loaded.
	 * 
	 * @param event the event of type <code>WorldEvent.Load</code> that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldLoaded_loadBaseraidsWorldSavedData(WorldEvent.Load event) {
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
	public void onWorldSaved_saveBaseraidsWorldSavedData(WorldEvent.Save event) {
		if (event.getWorld().isRemote() || !((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD))
			return;
		if (event.getWorld() instanceof ServerWorld) {
			baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
		}
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
	@SubscribeEvent
	public void onMonsterSpawnOutsideCave_cancelSpawning(WorldEvent.PotentialSpawns event) {
		World world = (World) event.getWorld();
		if (world.isRemote())
			return;

		if (!ConfigOptions.deactivateMonsterNightSpawn.get())
			return;

		if (event.getType() == EntityClassification.MONSTER)
			return;

		if (world.getBlockState(event.getPos()) != Blocks.CAVE_AIR.getDefaultState())
			return;

		if (event.isCancelable()) {
			event.setCanceled(true);
		}
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
