package may.baseraids;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import may.baseraids.entities.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;

// @Mod.EventBusSubscriber annotation automatically registers STATIC event handlers 
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
public class RaidManager {

	
	public RaidManagerData data;
	public boolean isInitialized = false;
	
	
	private int tick = 0;
	private int lastTickGameTime = -1;
	private int raidSoundInterval = 60;
	// sets the times (remaining time until raid) for when to warn all players of the coming raid (approximated, in seconds)	
	private Set<Integer> warnAllPlayersOfRaidTimes = Set.of(18000, 6000, 1200, 600, 300, 60, 30, 10, 5, 4, 3, 2, 1);

	
	
	// RAID SETTINGS
	private int timeBetweenRaids = 24000; // time between to raids in GameTime, 10min is 12000, 1min is 1200
	private int nightTimeInWorldDayTime = 13000; // defines the world.daytime at which it starts to be night (one day == 24000)
	private int maxRaidDuration = 1600;
	private int numZombies = 10;
	private int numSkeletons = 10;
	private int numSpiders = 5;
	private int numEnderman = 2;
	
	
	private List<MobEntity> spawnedMobs = new ArrayList<MobEntity>();
	
	
	
	//private static final ResourceLocation LOOT_LEVEL_1 = LootTableList.register(new ResourceLocation(Baseraids.MODID, "abandoned_mineshaft"));
	
	public RaidManager() {
		MinecraftForge.EVENT_BUS.register(this);
		Baseraids.LOGGER.info("RaidManager created");
		data = new RaidManagerData();
	}
	
	
	@SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
		if(!isInitialized) return;
		if (event.phase != TickEvent.Phase.START) return;
		if(event.world.isRemote()) return;
		if (!event.world.getDimensionKey().equals(World.OVERWORLD)) return;

		if(event.world.getDifficulty() == Difficulty.PEACEFUL) {
			if(data.isRaidActive()) {
				endRaid();
			}
			return;
		}
		
		handleTimers(event.world);
		
		tick++;
		tick = tick % 1000;
		
		// CHECK FOR RAID
		if(data.timeSinceRaid > timeBetweenRaids) {
			if(event.world.getDayTime() % 24000 >= nightTimeInWorldDayTime) {				
				initiateRaid(event.world);
			}
		}
		
		if(data.isRaidActive()) {
			raidTick(event.world);
		}
		
		
	}
	
	private void handleTimers(World world) {
		if(lastTickGameTime == -1) {
			lastTickGameTime = (int) (world.getGameTime());
		}
		if (tick % 20 == 0) {
			
			// HANDLE RAID TIMER
			data.setTimeSinceRaid(data.timeSinceRaid + (int) world.getGameTime() - lastTickGameTime);
			lastTickGameTime = (int) (world.getGameTime());
			
			// Log timeSinceRaid
			if(data.timeSinceRaid % 100 == 0) {
				Baseraids.LOGGER.info("GameTime since last raid: " + data.timeSinceRaid);
			}
			
			
			// PLAYER WARNINGS
			
			// warn players at specified times
			int timeUntilRaidInSec = getTimeUntilRaidInSec(world);
			if(warnAllPlayersOfRaidTimes.stream().anyMatch(time -> time == timeUntilRaidInSec)) {
				if(timeUntilRaidInSec > 60) {
					Baseraids.sendChatMessage("Time until next raid: " + (int) timeUntilRaidInSec / 60 + "min");
				}else {
					Baseraids.sendChatMessage("Time until next raid: " + timeUntilRaidInSec + "s");
				}
			}
			
			
		}
	}
	
	private void raidTick(World world) {
		
		// HANDLE SOUND		
		if(data.isRaidActive && tick % raidSoundInterval == 0) {
			world.playSound(null, Baseraids.baseraidsData.placedNexusBlockPos, SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F, 0.1F);	
		}
		
		// END RAID AFTER MAX DURATION
		if(data.isRaidActive() && data.timeSinceRaid > maxRaidDuration) {
			Baseraids.LOGGER.info("Raid ended because of max duration");
			winRaid(world);
		}
	}
	
	
	
	@SubscribeEvent
	public void onMonsterSpawn(WorldEvent.PotentialSpawns event){
		if(event.getWorld().isRemote()) return;
		if(data == null) return;
		if(data.deactivateMonsterNightSpawn) {
			if(event.getType() == EntityClassification.MONSTER) {
				
				World world = (World) event.getWorld();
				if(world.getBlockState(event.getPos()) != Blocks.CAVE_AIR.getDefaultState()) {
					if(event.isCancelable()) {
						// Cancel Spawn if not in cave
						event.setCanceled(true);
					}
					
				}
				
			}
		}
 	}
	
    
    public void initiateRaid(World world) {
    	if(world.isRemote()) return;
    	if (!world.getDimensionKey().equals(World.OVERWORLD)) return;
    	
    	Baseraids.sendChatMessage("You are being raided!");
    	
    	data.setTimeSinceRaid(0);
    	
    	BlockPos nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
    	if(nexusPos.getX() == -1) {
    		Baseraids.LOGGER.info("No Nexus placed, skipping raid");
    		return;
    	}
    	
    	
    	Baseraids.LOGGER.info("Initiating raid");
    	data.setRaidActive(true);
    	
    	// SPAWNING
    	
    	MobEntity[] zombies = (MobEntity[]) spawnRaidMobs(world, () -> new BaseraidsZombieEntity(Baseraids.BASERAIDS_ZOMBIE_TYPE.get(), world), numZombies);
    	MobEntity[] spiders = (MobEntity[]) spawnRaidMobs(world, () -> new SpiderEntity(EntityType.SPIDER, world), numSpiders);
    	MobEntity[] enderman = (MobEntity[]) spawnRaidMobs(world, () -> new EndermanEntity(EntityType.ENDERMAN, world), numEnderman);
    	MobEntity[] skeletons = (MobEntity[]) spawnRaidMobs(world, () -> new BaseraidsSkeletonEntity(Baseraids.BASERAIDS_SKELETON_TYPE.get(), world), numSkeletons);
    	
    	spawnedMobs.addAll(Arrays.asList(zombies));
    	spawnedMobs.addAll(Arrays.asList(spiders));
    	spawnedMobs.addAll(Arrays.asList(enderman));
    	spawnedMobs.addAll(Arrays.asList(skeletons));
    	
    	
    }
    
    @SuppressWarnings("unchecked")
	private <T extends MobEntity> T[] spawnRaidMobs(World world, Supplier<T> entitySupp, int numMobs) {
    	BlockPos nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
    	int radius = 50;
    	double angleInterval = 2*Math.PI/100;
    	BlockPos spawnPos;
    	MobEntity[] mobs = new MobEntity[numMobs];
    	for(int i = 0; i < numMobs; i++) {
    		mobs[i] = entitySupp.get();
        	
    		
    		Random r = new Random();
    		int randomAngle = r.nextInt(100);
        	double angle = randomAngle * angleInterval;
        	
        	
        	BlockPos centerSpawnPos = new BlockPos(nexusPos).add(0, 1, 0);
        	BlockPos tryRadiusSpawnPos = centerSpawnPos;
        		
    		int x = (int) (radius * Math.cos(angle));
    		int z = (int) (radius * Math.sin(angle));
    		tryRadiusSpawnPos = centerSpawnPos.add(x, 0, z);
    		spawnPos = world.getHeight(Heightmap.Type.WORLD_SURFACE, tryRadiusSpawnPos);     
    		
        	Baseraids.LOGGER.info("Spawn " + mobs[i].getType().toString() + " at radius " + radius + " and angle " + angle);
        	
        	mobs[i].getType().spawn((ServerWorld) world, null, null, spawnPos, SpawnReason.MOB_SUMMONED, false, false);
        	
    	}
    	return (T[]) mobs;
    }
    
	
    public void loseRaid(World world) {
    	if(world.isRemote()) return;
    	Baseraids.LOGGER.info("Raid lost");
    	Baseraids.sendChatMessage("You have lost the raid!");
    	endRaid();
    }
    
    public void winRaid(World world) {
    	if(world.isRemote()) return;
    	if(!data.isRaidActive()) return;
    	Baseraids.LOGGER.info("Raid won");
    	Baseraids.sendChatMessage("You have won the raid!");
    	endRaid();
    	
    	// PLACE LOOT CHEST
    	BlockPos chestPos = Baseraids.baseraidsData.placedNexusBlockPos.add(0, 1, 0);    	
    	world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
    	if(world.getTileEntity(chestPos) instanceof ChestTileEntity) {
    		ChestTileEntity chestEntity = (ChestTileEntity) world.getTileEntity(chestPos);
    		ResourceLocation loottable_level_1 = new ResourceLocation(Baseraids.MODID, "chests/raid_level_1");
    		chestEntity.setLootTable(loottable_level_1, world.getRandom().nextLong());
    		chestEntity.fillWithLoot(null);
    		
    		LazyOptional<?> cap = chestEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
    		cap.ifPresent(); // need IItemHandler, research capabilities
    		// or find out how the loot tables work ...
    		Baseraids.LOGGER.info("Added loot to loot chest");
    	}else {
    		Baseraids.LOGGER.error("Could not add loot to loot chest");
    	}
    	
    }
    
    
    private void endRaid() {
    	Baseraids.LOGGER.info("Ending raid");
    	data.setRaidActive(false);
    	spawnedMobs.forEach(mob -> mob.onKillCommand());
    }
    
    public int getTimeUntilRaidInSec(World world) {
    	// Math.floorMod returns only positive values (for a positive modulus) while % returns the actual remainder
		return (int) Math.max(
				(timeBetweenRaids-data.timeSinceRaid),
				Math.floorMod(nightTimeInWorldDayTime - (world.getDayTime() % 24000), 24000)
				) /20;
    }
    
	public static class RaidManagerData {
		public boolean deactivateMonsterNightSpawn = true;
		public int timeSinceRaid = 0;
		public boolean isRaidActive = false;
		
		void setTimeSinceRaid(int timeSinceRaid) {
			this.timeSinceRaid = timeSinceRaid;
			Baseraids.baseraidsData.setRaidManagerData(this);
		}
		
		void setRaidActive(boolean active) {
			isRaidActive = active;
		}
		
		public boolean isRaidActive() {
			return isRaidActive;
		}
		
		public CompoundNBT write() {
			CompoundNBT nbt = new CompoundNBT();
			nbt.putBoolean("deactivateMonsterNightSpawn", deactivateMonsterNightSpawn);
			nbt.putInt("timeSinceRaid", timeSinceRaid);
			nbt.putBoolean("isRaidActive", isRaidActive);
			return nbt;
		}
		
		public static RaidManagerData read(CompoundNBT nbt) {
			RaidManagerData raidManagerData = new RaidManagerData();
			raidManagerData.deactivateMonsterNightSpawn = nbt.getBoolean("deactivateMonsterNightSpawn");
			raidManagerData.timeSinceRaid = nbt.getInt("timeSinceRaid");
			raidManagerData.isRaidActive = nbt.getBoolean("isRaidActive");
			return raidManagerData;
		}
		
		
	}
}
