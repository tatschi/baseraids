package may.baseraids;


import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry.PlacementType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// @Mod.EventBusSubscriber annotation automatically registers STATIC event handlers 
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
public class RaidManager {

	
	public RaidManagerData data = new RaidManagerData();
	public boolean isInitialized = false;
	
	
	private int tick = 0;
	private int lastTickGameTime = -1;
	private int raidSoundInterval = 60;
	// sets the times (remaining time until raid) for when to warn all players of the coming raid (approximated, in seconds)	
	private Set<Integer> warnAllPlayersOfRaidTimes = Set.of(600, 300, 60, 30, 10, 5, 4, 3, 2, 1);

	
	
	// RAID SETTINGS
	private int raidTimeInterval = 1000; // time between to raids in GameTime, 10min is 12000, 1min is 1200
	private int nightTimeInWorldDayTime = 13000; // defines the world.daytime at which it starts to be night (one day == 24000)
	private int numZombies = 10;
	private int numSkeletons = 10;
	private int numSpiders = 5;
	private int numEnderman = 2;
	
	
	public RaidManager() {
		MinecraftForge.EVENT_BUS.register(this);
		Baseraids.LOGGER.info("LOGID:constructRaidManager Constructing a RaidManager");
	}
	
	
	
	
	@SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
		if(!isInitialized) return;
		if (event.phase != TickEvent.Phase.START) return;
		if(event.world.isRemote()) return;
		if (!event.world.getDimensionKey().equals(World.OVERWORLD)) return;

		if(lastTickGameTime == -1) {
			lastTickGameTime = (int) (event.world.getGameTime());
		}
		if (tick % 20 == 0) {
			
			// HANDLE RAID TIMER
			data.setTimeSinceRaid(data.timeSinceRaid + (int) event.world.getGameTime() - lastTickGameTime);
			lastTickGameTime = (int) (event.world.getGameTime());
			
			// Log timeSinceRaid
			if(data.timeSinceRaid % 100 == 0) {
				Baseraids.LOGGER.info("GameTime since last raid: " + data.timeSinceRaid);
			}
			
			
			// END RAID AFTER SOME TIME
			if(data.isRaidActive && data.timeSinceRaid % 300 == 0) {
				data.setRaidActive(false);
				Baseraids.LOGGER.info("LOGID:raidEvent Raid over");
			}
			
			
			// PLAYER WARNINGS
			double timeUntilRaidInSec = Math.max((raidTimeInterval-data.timeSinceRaid) / 20, nightTimeInWorldDayTime - (event.world.getDayTime() % 24000) % 24000);
			if(timeUntilRaidInSec % 1 == 0 && warnAllPlayersOfRaidTimes.stream().anyMatch(time -> time == timeUntilRaidInSec)) {
				warnAllPlayersOfRaid(event.world, (int) timeUntilRaidInSec);
			}
			
			
		}
		
		// HANDLE SOUND DURING RAID
		if(data.isRaidActive && tick % raidSoundInterval == 0) {
			Baseraids.LOGGER.info("LOGID:soundEvent Playing Raid Sound, tick: " + tick);
			event.world.playSound(null, Baseraids.baseraidsData.placedNexusBlockPos, SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F, 0.1F);	
		}
		
		tick++;
		tick = tick % 1000;
		
		// CHECK FOR RAID
		if(data.timeSinceRaid > raidTimeInterval) {
			if(event.world.getDayTime() % 24000 >= nightTimeInWorldDayTime) {
				for(PlayerEntity player : event.world.getPlayers()) {
					player.sendMessage(new StringTextComponent("You are being raided!"), null);
				}
				data.setTimeSinceRaid(0);
				this.initiateRaid(event.world);
			}
		}
		
		
	}
	
	private void warnAllPlayersOfRaid(World world, int timeUntilRaidInSec) {
		for(PlayerEntity player : world.getPlayers()) {
			player.sendMessage(new StringTextComponent("Time until next raid: " + timeUntilRaidInSec + "s"), null);
		}
	}
	
	@SubscribeEvent
	public void onMonsterSpawn(WorldEvent.PotentialSpawns event){
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
    	
    	BlockPos nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
    	if(nexusPos.getX() == -1) {
    		Baseraids.LOGGER.info("No Nexus placed, skipping raid");
    		return;
    	}
    	
    	
    	Baseraids.LOGGER.info("Initiating raid");
    	data.setRaidActive(true);
    	
    	MobEntity[] zombies = (MobEntity[]) spawnRaidMobs(world, () -> new ZombieEntity(world), numZombies);
    	MobEntity[] spiders = (MobEntity[]) spawnRaidMobs(world, () -> new SpiderEntity(EntityType.SPIDER, world), numSpiders);
    	MobEntity[] enderman = (MobEntity[]) spawnRaidMobs(world, () -> new EndermanEntity(EntityType.ENDERMAN, world), numEnderman);
    	MobEntity[] skeletons = (MobEntity[]) spawnRaidMobs(world, () -> new SkeletonEntity(EntityType.SKELETON, world), numSkeletons);
    	
    	for(int i = 0; i < zombies.length; i++) {
    		// WORK IN PROGRESS
    		// look closer at AbstractRaiderEntity
    		
    		PlayerEntity player = world.getClosestPlayer(zombies[i].getPosX(), zombies[i].getPosY(), zombies[i].getPosZ(), 20000, false);
    		if(player == null) {
    			Baseraids.LOGGER.info("No player nearby found");
    			
    			//zombies[i].getMoveHelper().setMoveTo(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1.15);
    			zombies[i].getNavigator().clearPath();
    			boolean moved = zombies[i].getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1.15);
    			Baseraids.LOGGER.info("Moved: " + moved);
    		}else {
    			Baseraids.LOGGER.info("Try to target player");
    			zombies[i].setAttackTarget(player);
        		zombies[i].setAggroed(true);
        		Baseraids.LOGGER.info("Selected goal: " + zombies[i].goalSelector.getRunningGoals().findFirst().toString());
        		zombies[i].getNavigator().clearPath();
        		boolean moved = zombies[i].getNavigator().tryMoveToEntityLiving(player, (double)1.15F);
        		Baseraids.LOGGER.info("Moved: " + moved);
    		}
    		
    	}
    	
    	
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
