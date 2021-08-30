package may.baseraids;


import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.jline.utils.Log;

import com.google.common.collect.Sets;

import may.baseraids.entities.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IServerWorld;
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

	
	// RUNTIME VARIABLES
	private World world = null;
	public boolean isInitialized = false;
	
	private boolean isRaidActive;
	private int tick = 0;
	private int lastTickGameTime = -1;
	private int curRaidLevel;
	private int lastRaidGameTime;
	
	private List<MobEntity> spawnedMobs = new ArrayList<MobEntity>();
	
	
	// RAID SETTINGS
	private int maxRaidLevel = 3, minRaidLevel = 1;
	private int timeBetweenRaids = 24000; // time between to raids in GameTime, 10min is 12000, 1min is 1200
	private int nightTimeInWorldDayTime = 13000; // defines the world.daytime at which it starts to be night (one day == 24000)
	private int maxRaidDuration = 3600;
	private int raidSoundInterval = 60;
	
	
	// stores the amount of mobs to spawn for each raid level and mob using <amount, Entry<raidlevel, mobname>>
	//private HashMap<Entry<Integer, String>, Integer> amountOfMobsToSpawn = new HashMap<Entry<Integer, String>, Integer>();
	private HashMap<Integer, HashMap<EntityType<?>, Integer>> amountOfMobsToSpawn;
	
	private boolean deactivateMonsterNightSpawn;
	
	// sets the times (remaining time until raid) for when to warn all players of the coming raid (approximated, in seconds)	
	private Set<Integer> warnAllPlayersOfRaidTimes = Sets.newHashSet(18000, 6000, 1200, 600, 300, 60, 30, 10, 5, 4, 3, 2, 1);;
	
	private static final ResourceLocation[] LOOTTABLES = {
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_1"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_2")
	};
	
	public RaidManager() {
		MinecraftForge.EVENT_BUS.register(this);
		setDefaultWriteParameters();
		amountOfMobsToSpawn = new HashMap<Integer, HashMap<EntityType<?>, Integer>>();
		setAmountOfMobsToSpawn();
		Baseraids.LOGGER.info("RaidManager created");		
	}
	
	
	
	
	private void setAmountOfMobsToSpawn() {
		/*
		 * DEFINES WHAT AND HOW MANY MOBS WILL SPAWN FOR EACH LEVEL
		 */
		
		// LEVEL 1
		HashMap<EntityType<?>, Integer> level1 = new HashMap<EntityType<?>, Integer>();
		level1.put(Baseraids.BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), 3);
		level1.put(Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(), 1);
		level1.put(Baseraids.BASERAIDS_PHANTOM_ENTITY_TYPE.get(), 1);
		
		// LEVEL 2
		HashMap<EntityType<?>, Integer> level2 = new HashMap<EntityType<?>, Integer>();
		level2.put(Baseraids.BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), 5);
		level2.put(Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(), 2);
		//level2.put(Baseraids.BASERAIDS_PHANTOM_ENTITY_TYPE.get(), 2);
		level2.put(Baseraids.BASERAIDS_SPIDER_ENTITY_TYPE.get(), 2);
		
		// LEVEL 3
		HashMap<EntityType<?>, Integer> level3 = new HashMap<EntityType<?>, Integer>();
		level3.put(Baseraids.BASERAIDS_ZOMBIE_ENTITY_TYPE.get(), 8);
		level3.put(Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(), 4);
		level3.put(Baseraids.BASERAIDS_SPIDER_ENTITY_TYPE.get(), 4);
		//level3.put(Baseraids.BASERAIDS_PHANTOM_ENTITY_TYPE.get(), 3);
		
		amountOfMobsToSpawn.put(1, level1);
		amountOfMobsToSpawn.put(2, level2);
		amountOfMobsToSpawn.put(3, level3);
	}
	
	
	@SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
		if(!isInitialized) return;
		if (event.phase != TickEvent.Phase.START) return;
		if(event.world.isRemote()) return;
		if (!event.world.getDimensionKey().equals(World.OVERWORLD)) return;
		if(world == null) world = event.world;

		if(world.getDifficulty() == Difficulty.PEACEFUL) {
			if(isRaidActive()) {
				endRaid();
			}
			return;
		}
		
		handleTimers();
		
		tick++;
		tick = tick % 1000;
		
		// CHECK FOR RAID
		if(getTimeSinceRaid() > timeBetweenRaids) {
			if(event.world.getDayTime() % 24000 >= nightTimeInWorldDayTime) {				
				initiateRaid();
			}
		}
		
		if(isRaidActive()) {
			raidTick();
		}
		
		
	}
	
	private void handleTimers() {
		if(lastTickGameTime == -1) {
			lastTickGameTime = (int) (world.getGameTime());
		}
		if (tick % 20 == 0) {
			
			// Log timeSinceRaid
			if(tick % 100 == 0) {
				Baseraids.LOGGER.info("GameTime since last raid: " + getTimeSinceRaid());
			}
			
			
			// PLAYER WARNINGS
			
			// warn players at specified times
			int timeUntilRaidInSec = getTimeUntilRaidInSec();
			if(warnAllPlayersOfRaidTimes.stream().anyMatch(time -> time == timeUntilRaidInSec)) {
				if(timeUntilRaidInSec > 60) {
					Baseraids.sendChatMessage("Time until next raid: " + (int) timeUntilRaidInSec / 60 + "min");
				}else {
					Baseraids.sendChatMessage("Time until next raid: " + timeUntilRaidInSec + "s");
				}
			}
			
			
		}
	}
	
	private void raidTick() {
		// HANDLE SOUND		
		if(isRaidActive && tick % raidSoundInterval == 0) {
			world.playSound(null, Baseraids.baseraidsData.placedNexusBlockPos, SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F, 0.1F);	
		}
		
		// CHECK IF RAID IS WON
		if(isRaidWon()) {
			Baseraids.LOGGER.info("Raid ended: all mobs are dead");
			winRaid();
		}
		
		// END RAID AFTER MAX DURATION
		if(isRaidActive && getTimeSinceRaid() > maxRaidDuration) {
			Baseraids.LOGGER.info("Raid ended: reached max duration");
			winRaid();
		}
	}
	
	
	
	@SubscribeEvent
	public void onMonsterSpawn(WorldEvent.PotentialSpawns event){
		if(event.getWorld().isRemote()) return;
		if(deactivateMonsterNightSpawn) {
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
	
    
    public void initiateRaid() {
    	if(world == null) return;
    	if (!world.getDimensionKey().equals(World.OVERWORLD)) return;
    	
    	Baseraids.sendChatMessage("You are being raided!");
    	
    	setLastRaidGameTime((int) (world.getGameTime()));
    	
    	BlockPos nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
    	if(nexusPos.getX() == -1) {
    		Baseraids.LOGGER.info("No Nexus placed, skipping raid");
    		return;
    	}
    	
    	
    	Baseraids.LOGGER.info("Initiating raid");
    	setRaidActive(true);
    	
    	// SPAWNING
    	spawnedMobs.clear();
    	amountOfMobsToSpawn.get(curRaidLevel).forEach(
    			(type, num) -> spawnedMobs.addAll(Arrays.asList(spawnRaidMobs(type, num)))
    			);
    	
    }
    
	private <T extends Entity> MobEntity[] spawnRaidMobs(EntityType<T> entityType, int numMobs) {
    	BlockPos nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
    	int radius = 50;
    	double angleInterval = 2*Math.PI/100;
    	BlockPos centerSpawnPos = new BlockPos(nexusPos).add(0, 1, 0);
    	
    	ILivingEntityData ilivingentitydata = null;
    	MobEntity[] mobs = new MobEntity[numMobs];
    	for(int i = 0; i < numMobs; i++) {
    		
    		// find random coordinates in a circle around the nexus to spawn the current mob
    		Random r = new Random();
    		int randomAngle = r.nextInt(100);
        	double angle = randomAngle * angleInterval;
        	
    		int x = (int) (radius * Math.cos(angle));
    		int z = (int) (radius * Math.sin(angle));
    		BlockPos spawnPosXZ = centerSpawnPos.add(x, 0, z);
    		
    		
    		// find the right height
    		BlockPos spawnPos;    		     
    		if(EntitySpawnPlacementRegistry.getPlacementType(entityType).equals((EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS))) {
    			spawnPos = world.getHeight(Heightmap.Type.WORLD_SURFACE, spawnPosXZ).add(0, 5, 0);
    		}else {
    			spawnPos = world.getHeight(Heightmap.Type.WORLD_SURFACE, spawnPosXZ);
    		}
    		
        	Baseraids.LOGGER.debug("Spawn " + entityType.getName().getString() + " at radius " + radius + " and angle " + angle);
        	
        	
        	
        	if(EntitySpawnPlacementRegistry.canSpawnEntity(entityType, (IServerWorld) world, SpawnReason.MOB_SUMMONED, spawnPos, r)) {
        		
        		if(entityType.equals(Baseraids.BASERAIDS_PHANTOM_ENTITY_TYPE.get())) {
        			mobs[i] = EntityType.PHANTOM.create(world);
        			mobs[i].moveToBlockPosAndAngles(spawnPos, 0.0F, 0.0F);
                    ilivingentitydata = mobs[i].onInitialSpawn((IServerWorld) world, world.getDifficultyForLocation(spawnPos), SpawnReason.NATURAL, ilivingentitydata, (CompoundNBT)null);
                    ((IServerWorld) world).func_242417_l(mobs[i]);
        		}else {
        			mobs[i] = (MobEntity) entityType.spawn((ServerWorld) world, null, null, spawnPos, SpawnReason.MOB_SUMMONED, false, false);
        		}
        		
        		
        	}else {
        		Baseraids.LOGGER.error("Couldn't spawn entity");
        	}
        	
    	}
    	return mobs;
    }
    
	
    public void loseRaid() {
    	if(world == null) return;
    	Baseraids.LOGGER.info("Raid lost");
    	Baseraids.sendChatMessage("You have lost the raid!");
    	// make sure the raid level is adjusted before endRaid() because endRaid() uses the new level
    	resetRaidLevel();
    	endRaid();
    	
    }
    
    public void winRaid() {
    	if(world == null) return;
    	Baseraids.LOGGER.info("Raid won");
    	Baseraids.sendChatMessage("You have won the raid!");
    	
    	// PLACE LOOT CHEST
    	BlockPos chestPos = Baseraids.baseraidsData.placedNexusBlockPos.add(0, 1, 0);    	
    	world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
    	if(world.getTileEntity(chestPos) instanceof ChestTileEntity) {
    		ChestTileEntity chestEntity = (ChestTileEntity) world.getTileEntity(chestPos);
    		
    		chestEntity.setLootTable(LOOTTABLES[curRaidLevel-1], world.getRandom().nextLong());
    		chestEntity.fillWithLoot(null);
    		
    		Baseraids.LOGGER.info("Added loot to loot chest");
    	}else {
    		Baseraids.LOGGER.error("Could not add loot to loot chest");
    	}
    	
    	
    	
    	// make sure the raid level is adjusted before endRaid() because endRaid() uses the new level
    	increaseRaidLevel();
    	endRaid();
    	
    }
    
    
    private void endRaid() {
    	Baseraids.sendChatMessage("Your next raid will have level " + curRaidLevel);
    	setRaidActive(false);
    	spawnedMobs.forEach(mob -> mob.onKillCommand());
    	spawnedMobs.clear();
    	world.sendBlockBreakProgress(-1, Baseraids.baseraidsData.placedNexusBlockPos, -1); 
    }
    
    public int getTimeUntilRaidInSec() {
    	// Math.floorMod returns only positive values (for a positive modulus) while % returns the actual remainder
		return (int) Math.max(
				(timeBetweenRaids-getTimeSinceRaid()),
				Math.floorMod(nightTimeInWorldDayTime - (world.getDayTime() % 24000), 24000)
				) /20;
    }
    
    
    private boolean isRaidWon() {
    	if(spawnedMobs.isEmpty()) return false;
    	for(MobEntity mob : spawnedMobs) {
    		if(mob.isAlive()) {
    			return false;
    		}
    	}
    	return true;
    }
    
    
    private void increaseRaidLevel() {
    	curRaidLevel++;
    	if(curRaidLevel > maxRaidLevel) curRaidLevel = maxRaidLevel;
    	markDirty();
    }
    
    private void resetRaidLevel() {
    	curRaidLevel = minRaidLevel;
    	markDirty();
    }
    
    
    public CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("deactivateMonsterNightSpawn", deactivateMonsterNightSpawn);
		nbt.putInt("curRaidLevel", curRaidLevel);
		nbt.putInt("lastRaidGameTime", lastRaidGameTime);
		nbt.putBoolean("isRaidActive", isRaidActive);
		
		// spawned mobs
		ListNBT spawnedMobsList = new ListNBT();
		for(MobEntity mob : spawnedMobs) {
			CompoundNBT compound = new CompoundNBT();
			compound.putUniqueId("ID", mob.getUniqueID());
			spawnedMobsList.add(compound);
		}
		
		nbt.put("spawnedMobs", spawnedMobsList);
		
		return nbt;
	}
	
	public void readAdditional(CompoundNBT nbt) {
		try {
			deactivateMonsterNightSpawn = nbt.getBoolean("deactivateMonsterNightSpawn");
			lastRaidGameTime = nbt.getInt("lastRaidGameTime");
			curRaidLevel = nbt.getInt("curRaidLevel");
			isRaidActive = nbt.getBoolean("isRaidActive");
			ListNBT spawnedMobsList = nbt.getList("spawnedMobs", 10);
			for(INBT compound : spawnedMobsList) {
				CompoundNBT compoundNBT = (CompoundNBT) compound;
				Entity entity = world.getServer().func_241755_D_().getEntityByUuid(compoundNBT.getUniqueId("ID"));
				if(entity == null) {
					Log.warn("Could not read entity from data");
					continue;
				}
				if(!(entity instanceof MobEntity)) {
					Log.warn("Error while reading data for RaidManager: Read entity not of type MobEntity");
					continue;
				}
				
				spawnedMobs.add((MobEntity) entity);
			}
			
		}catch(Exception e) {
			setDefaultWriteParameters();
			markDirty();
		}
	}
	
	private void setDefaultWriteParameters() {
		deactivateMonsterNightSpawn = true;
		lastRaidGameTime = 0;
		curRaidLevel = 1;
		isRaidActive = false;
	}

	
	private void setLastRaidGameTime(int time) {
		lastRaidGameTime = time;
		markDirty();
	}
	
	public void setRaidActive(boolean active) {
		isRaidActive = active;
		markDirty();
	}
    
	public boolean isRaidActive() {
		return isRaidActive;
	}
	
	public void markDirty() {
		Baseraids.baseraidsData.markDirty();
	}


	public int getTimeSinceRaid() {
		if(world == null) return -1;
		return (int) (world.getGameTime()) -lastRaidGameTime;
	}


	public int getRaidLevel() {
		return curRaidLevel;
	}
}
