package may.baseraids;

import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import may.baseraids.NexusBlock.State;
import may.baseraids.config.Config;
import may.baseraids.entities.BaseraidsPhantomEntity;
import may.baseraids.entities.BaseraidsSkeletonEntity;
import may.baseraids.entities.BaseraidsSpiderEntity;
import may.baseraids.entities.BaseraidsZombieEntity;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
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

// The value here should match an entry in the META-INF/mods.toml file
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
@Mod("baseraids")
public class Baseraids
{
	public static final String MODID = "baseraids";
	
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Baseraids.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Baseraids.MODID);
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Baseraids.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Baseraids.MODID);
    
    
    public static final RegistryObject<Block> NEXUS_BLOCK = BLOCKS.register("nexus_block", () -> new NexusBlock());
    public static final RegistryObject<BlockItem> NEXUS_ITEM =
    		ITEMS.register("nexus_block",
    		() -> new BlockItem(Baseraids.NEXUS_BLOCK.get(), new Item.Properties().group(ItemGroup.COMBAT)));
    
    // ENTITIES
    public static final RegistryObject<TileEntityType<NexusEffectsTileEntity>> NEXUS_TILE_ENTITY_TYPE =
    		TILE_ENTITIES.register("nexus_effects_tile_entity",
    		() -> TileEntityType.Builder.create(NexusEffectsTileEntity::new, Baseraids.NEXUS_BLOCK.get()).build(null));
    
    
    public static final RegistryObject<EntityType<BaseraidsZombieEntity>> BASERAIDS_ZOMBIE_ENTITY_TYPE =
    		ENTITIES.register("baseraids_zombie_entity",
    		() -> EntityType.Builder.<BaseraidsZombieEntity>create(BaseraidsZombieEntity::new, EntityClassification.MONSTER).build("baseraids_zombie_entity"));
    public static final RegistryObject<EntityType<BaseraidsSkeletonEntity>> BASERAIDS_SKELETON_ENTITY_TYPE =
    		ENTITIES.register("baseraids_skeleton_entity",
    		() -> EntityType.Builder.<BaseraidsSkeletonEntity>create(BaseraidsSkeletonEntity::new, EntityClassification.MONSTER).build("baseraids_skeleton_entity"));
    public static final RegistryObject<EntityType<BaseraidsSpiderEntity>> BASERAIDS_SPIDER_ENTITY_TYPE =
    		ENTITIES.register("baseraids_spider_entity",
    		() -> EntityType.Builder.<BaseraidsSpiderEntity>create(BaseraidsSpiderEntity::new, EntityClassification.MONSTER).build("baseraids_spider_entity"));
    public static final RegistryObject<EntityType<BaseraidsPhantomEntity>> BASERAIDS_PHANTOM_ENTITY_TYPE =
    		ENTITIES.register("baseraids_phantom_entity",
    		() -> EntityType.Builder.<BaseraidsPhantomEntity>create(BaseraidsPhantomEntity::new, EntityClassification.MONSTER).build("baseraids_phantom_entity"));
    
    
    public static final HashMap<String, EntityType<?>> configRegister = new HashMap<String, EntityType<?>>();
    
    public static BaseraidsWorldSavedData baseraidsData;
    
    
    public Baseraids() {
    	
    	
    	ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.config);
    	
    	IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus(); 
        // Register the setup method for modloading
        bus.addListener(this::setup);

        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILE_ENTITIES.register(bus);
        ENTITIES.register(bus);
        
        
        
        Config.loadConfig(Config.config, FMLPaths.CONFIGDIR.get().resolve(MODID + ".toml").toString());
        
    }
    
    @SubscribeEvent
    public void registerConfigEntityTypes(final RegistryEvent.Register<EntityType<?>> event) {
    	configRegister.put(BaseraidsZombieEntity.CONFIG_NAME, BASERAIDS_ZOMBIE_ENTITY_TYPE.get());
        configRegister.put(BaseraidsSkeletonEntity.CONFIG_NAME, BASERAIDS_SKELETON_ENTITY_TYPE.get());
        configRegister.put(BaseraidsSpiderEntity.CONFIG_NAME, BASERAIDS_SPIDER_ENTITY_TYPE.get());
        configRegister.put(BaseraidsPhantomEntity.CONFIG_NAME, BASERAIDS_PHANTOM_ENTITY_TYPE.get());
    }
    

    @SuppressWarnings("unchecked")
    private void setup(final FMLCommonSetupEvent event)
    {
    	
    	// connect attributes of custom type to those of vanilla entities
    	// if custom attributes are desired, mimic func_234342_eQ_() in the entity class and call it instead
    	GlobalEntityTypeAttributes.put(BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), ZombieEntity.func_234342_eQ_().create());
    	GlobalEntityTypeAttributes.put(BASERAIDS_SKELETON_ENTITY_TYPE.get(), SkeletonEntity.registerAttributes().create());
    	GlobalEntityTypeAttributes.put(BASERAIDS_SPIDER_ENTITY_TYPE.get(), SpiderEntity.func_234305_eI_().create());
    	GlobalEntityTypeAttributes.put(BASERAIDS_PHANTOM_ENTITY_TYPE.get(), BaseraidsPhantomEntity.registerAttributes().create());
    	
    	EntityRendererManager renderManager = Minecraft.getInstance().getRenderManager();
    	
    	// add renderers to custom entity types
		EntityRenderer<ZombieEntity> zombieRenderer = (EntityRenderer<ZombieEntity>) renderManager.renderers.get(EntityType.ZOMBIE);
    	renderManager.register(BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), zombieRenderer);
    	EntityRenderer<SkeletonEntity> skeletonRenderer = (EntityRenderer<SkeletonEntity>) renderManager.renderers.get(EntityType.SKELETON);
    	renderManager.register(BASERAIDS_SKELETON_ENTITY_TYPE.get(), skeletonRenderer);
    	EntityRenderer<BaseraidsSpiderEntity> spiderRenderer = (EntityRenderer<BaseraidsSpiderEntity>) renderManager.renderers.get(EntityType.SPIDER);
    	renderManager.register(BASERAIDS_SPIDER_ENTITY_TYPE.get(), spiderRenderer);
    	EntityRenderer<BaseraidsPhantomEntity> phantomRenderer = (EntityRenderer<BaseraidsPhantomEntity>) renderManager.renderers.get(EntityType.PHANTOM);
    	renderManager.register(BASERAIDS_PHANTOM_ENTITY_TYPE.get(), phantomRenderer);
    }
    
    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load event) {
    	if(event.getWorld().isRemote()) return;
    	if(!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) return;
    	
    	if(event.getWorld() instanceof ServerWorld) {
    		LOGGER.info("loading baseraidsSavedData");
    		baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
    	}
    	
    	
    	
    }
    
    
	
    @SubscribeEvent
    public void onWorldSaved(WorldEvent.Save event) {
    	if(event.getWorld().isRemote()) return;
    	if(!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) return;
    	if(event.getWorld() instanceof ServerWorld) {
    		LOGGER.info("loading baseraidsSavedData after save");
    		baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
    		LOGGER.info("loaded time since last raid: " + baseraidsData.raidManager.getTimeSinceRaid());
    	}
    }
	
    
    
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
    	if(event.phase != TickEvent.Phase.START){
    		return;
    	}
    	// ticks in each dimension
    	World world = event.world;
    	
    	// as long as the nexus block is not placed, regularly add a debuff to all players in the world
    	if(NexusBlock.getState() != State.BLOCK) {
    		if (world.getGameTime() % 80L == 0L) {
    			addDebuff(world);
        	}
    		
    	}
        	
        
    }

    /**
     * Sends a string message in the in game chat to all players on the server. 
     * @param message
     */
    public static void sendChatMessage(String message) {
    	for(PlayerEntity player : Minecraft.getInstance().getIntegratedServer().getPlayerList().getPlayers()) {
			player.sendMessage(new StringTextComponent(message), null);
		}
    }
    
    /**
     * Adds a slowness debuff of the duration that is specified in the method to all players in the world
     * @param world
     */
    private void addDebuff(World world) {
    	Effect effect = Effects.SLOWNESS;
		if (!world.isRemote) {
			int amplifier = 0;
			int duration = 200;
			
			List<? extends PlayerEntity> list = world.getPlayers();
	
			for(PlayerEntity playerentity : list) {
				playerentity.addPotionEffect(new EffectInstance(effect, duration, amplifier, true, true));
			}
		}
    }
    
    
}
