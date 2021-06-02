package may.baseraids;

import may.baseraids.entities.*;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
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
    public static final RegistryObject<TileEntityType<NexusEffectsTileEntity>> NEXUS_TILE_ENTITY_TYPE =
    		TILE_ENTITIES.register("nexus_effects_tile_entity",
    		() -> TileEntityType.Builder.create(NexusEffectsTileEntity::new, Baseraids.NEXUS_BLOCK.get()).build(null));
    public static final RegistryObject<EntityType<BaseraidsZombieEntity>> BASERAIDS_ZOMBIE_TYPE =
    		ENTITIES.register("baseraids_zombie_entity",
    		() -> EntityType.Builder.<BaseraidsZombieEntity>create(BaseraidsZombieEntity::new, EntityClassification.MONSTER).build("baseraids_zombie_entity"));
    		
    
    
    public static BaseraidsWorldSavedData baseraidsData;
    //public static RaidManager raidManager;
    public static ItemEntity nexusItem = null;
    
    public Baseraids() {
    	IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus(); 
        // Register the setup method for modloading
        bus.addListener(this::setup);

        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILE_ENTITIES.register(bus);
        ENTITIES.register(bus);
    }
    
    
    
    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
    	// 
    	
    	// connect attributes of BASERAIDS_ZOMBIE_TYPE to those of ZombieEntity
    	// if custom attributes are desired, mimic func_234342_eQ_() in the entity class and call it instead
    	GlobalEntityTypeAttributes.put(BASERAIDS_ZOMBIE_TYPE.get(), ZombieEntity.func_234342_eQ_().create());
    }
    
    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load event) {
    	if(event.getWorld().isRemote()) return;
    	if(!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) return;
    	if(event.getWorld() instanceof ServerWorld) {
    		LOGGER.info("loading baseraidsSavedData for: " + event.getWorld().getDimensionType().toString());
    		baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
    		Baseraids.LOGGER.info("Baseraidsdata, time since last raid: " + baseraidsData.raidManagerData.timeSinceRaid);
    	}
    	
    }
	
    @SubscribeEvent
    public void onWorldSaved(WorldEvent.Save event) {
    	if(event.getWorld().isRemote()) return;
    	if(!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) return;
    	if(event.getWorld() instanceof ServerWorld) {
    		LOGGER.info("loading baseraidsSavedData after save for: " + event.getWorld().getDimensionType().toString());
    		baseraidsData = BaseraidsWorldSavedData.get((ServerWorld) event.getWorld());
    		Baseraids.LOGGER.info("Baseraidsdata, time since last raid: " + baseraidsData.raidManagerData.timeSinceRaid);
    	}
    }
	

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
    	
    }
    
    
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
    	if(event.phase != TickEvent.Phase.START){
    		return;
    	}
    	// ticks in each dimension
    	World world = event.world;
    	if(baseraidsData.placedNexusBlockPos.getX() == -1) {
    		// if nexus block is not placed
    		
    		if (world.getGameTime() % 80L == 0L) {
    			addDebuff(world);
        	}
    		
    		// check if a dropped nexus is still alive, otherwise give a new nexus to a random player
    		if (nexusItem != null && !world.isRemote()) {
    			if(!nexusItem.isAlive()) {
    				List<? extends PlayerEntity> playerList = world.getPlayers();
    				 if(playerList.size() < 1) {
    					 return;
    				 }
    				NexusBlock.giveNexusToRandomPlayer(playerList);
    				nexusItem = null;
    			}
    		}
    		
    	}
        	
        
    }
    
    private void addDebuff(World world) {
    	// Add permanent slowness debuff
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
