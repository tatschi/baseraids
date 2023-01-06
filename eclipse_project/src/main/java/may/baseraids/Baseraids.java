package may.baseraids;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import may.baseraids.config.Config;
import may.baseraids.nexus.NexusBlock;
import may.baseraids.nexus.NexusEffectsTileEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The base class of this mod. Here, blocks, items and entities are registered,
 * saving and loading is intiated and basic, general functionality is provided.
 * 
 * @author Natascha May
 */
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

	public static final WorldManager worldManager = new WorldManager();
	public static final MessageManager messageManager = new MessageManager();

	public Baseraids() {
		setup();
	}

	/**
	 * Registers all registries, the mod event bus and loads the config file using
	 * the class {@link Config}.
	 */
	private void setup() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.config);

		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(worldManager::onFMLCommonSetup);
		registerDeferredRegistries(bus);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		Config.loadConfig(Config.config, FMLPaths.CONFIGDIR.get().resolve(MODID + ".toml").toString());
	}
	
	private void registerDeferredRegistries(IEventBus bus) {
		// register custom registries
		BLOCKS.register(bus);
		ITEMS.register(bus);
		TILE_ENTITIES.register(bus);
		ENTITIES.register(bus);
		SOUNDS.register(bus);
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

}
