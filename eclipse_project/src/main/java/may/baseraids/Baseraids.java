package may.baseraids;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.math.Vector3d;

import may.baseraids.config.ConfigLoader;
import may.baseraids.nexus.NexusBlock;
import may.baseraids.nexus.NexusEffectsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
			.create(ForgeRegistries.BLOCK_ENTITIES, Baseraids.MODID);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES,
			Baseraids.MODID);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
			Baseraids.MODID);

	// BLOCKS & ITEMS
	public static final RegistryObject<Block> NEXUS_BLOCK = BLOCKS.register("nexus_block",
			() -> new NexusBlock(BlockBehaviour.Properties.of(Material.STONE).strength(15f, 30f).sound(SoundType.GLASS)
					.lightLevel(light -> 15)));
	public static final RegistryObject<BlockItem> NEXUS_ITEM = ITEMS.register("nexus_block",
			() -> new BlockItem(Baseraids.NEXUS_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));

	// ENTITIES
	public static final RegistryObject<BlockEntityType<NexusEffectsBlockEntity>> NEXUS_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES
			.register("nexus_effects_tile_entity", () -> BlockEntityType.Builder
					.of(NexusEffectsBlockEntity::new, Baseraids.NEXUS_BLOCK.get()).build(null));

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
	 * the class {@link ConfigLoader}.
	 */
	private void setup() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigLoader.config);

		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(worldManager::onFMLCommonSetup);
		registerDeferredRegistries(bus);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		ConfigLoader.loadConfig(ConfigLoader.config, FMLPaths.CONFIGDIR.get().resolve(MODID + ".toml").toString());
	}
	
	private void registerDeferredRegistries(IEventBus bus) {
		// register custom registries
		BLOCKS.register(bus);
		ITEMS.register(bus);
		BLOCK_ENTITIES.register(bus);
		ENTITIES.register(bus);
		SOUNDS.register(bus);
	}

	/**
	 * Converts a {@link BlockPos} to a Vector3d for convenience.
	 * 
	 * @param pos the given {@link BlockPos}
	 * @return the {@link Vector3d} converted from the {@link BlockPos}
	 */
	public static Vec3 getVector3dFromBlockPos(BlockPos pos) {
		return new Vec3(pos.getX(), pos.getY(), pos.getZ());
	}

}
