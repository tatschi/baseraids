package may.baseraids;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jline.utils.Log;

import com.google.common.collect.Sets;

import may.baseraids.NexusBlock.State;
import may.baseraids.config.ConfigOptions;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class controls everything concerning raids: spawning, timers, ending a
 * raid, rewards and more.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1
 */
// @Mod.EventBusSubscriber annotation automatically registers STATIC event handlers 
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
public class RaidManager {

	
	// RUNTIME VARIABLES
	private World world = null;
	public boolean isInitialized = false;
	
	private boolean isRaidActive;
	private int tick = 0;
	private int timeUntilRaidInLastWarnPlayersOfRaidRun = -1;
	private int curRaidLevel;
	private int lastRaidGameTime;

	public static final int MAX_RAID_LEVEL = 5, MIN_RAID_LEVEL = 1;
	/**
	 * defines the world.daytime at which it starts to be night (one day == 24000)
	 */
	private static final int START_OF_NIGHT_IN_WORLD_DAY_TIME = 13000;
	private static final int RAID_SOUND_INTERVAL = 60;

	/**
	 * Sets the times (remaining time until raid) for when to warn all players of
	 * the coming raid (approximated, in seconds), @see warnPlayersOfRaid().
	 */
	private static final Set<Integer> TIMES_TO_WARN_PLAYERS_OF_RAID = Sets.newHashSet(2400, 1800, 1200, 900, 600, 300,
			60, 30, 10, 5, 4, 3, 2, 1);

	private RaidSpawningManager raidSpawningMng;

	private static final ResourceLocation[] REWARD_CHEST_LOOTTABLES = {
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_1"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_2"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_3"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_4"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_5") };

	public RaidManager(World world) {
		MinecraftForge.EVENT_BUS.register(this);
		this.world = world;
		raidSpawningMng = new RaidSpawningManager(this, world);
		setDefaultWriteParameters();
		Baseraids.LOGGER.info("RaidManager created");
	}

	/**
	 * Controls everything that happens every tick like warning of raids, starting
	 * raids and calling the <code>activeRaidTick()</code> during a raid.
	 * 
	 * @param event the event of type <code>TickEvent.WorldTickEvent</code> that
	 *              triggers this function
	 */
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		if (!isInitialized)
			return;
		if (event.phase != TickEvent.Phase.START)
			return;
		if (world.isRemote())
			return;
		if (!world.getDimensionKey().equals(World.OVERWORLD))
			return;

		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			if (isRaidActive()) {
				endRaid();
			}
			return;
		}

		warnPlayersOfRaid();
		if (shouldStartRaid()) {
			startRaid();
		}
		if (isRaidActive()) {
			activeRaidTick();
		}

		tick = (tick + 1) % 1000;
	}

	/**
	 * Warns all players in the world of an upcoming raid via chat messages and
	 * sounds. The times at which to warn are specified in the field
	 * <code>TIMES_TO_WARN_PLAYERS_OF_RAID</code>.
	 */
	private void warnPlayersOfRaid() {
		int timeUntilRaidInSec = getTimeUntilRaidInSec();
		// remember and control for the last used time in order to avoid multiple
		// messages for the same time
		if (timeUntilRaidInLastWarnPlayersOfRaidRun == timeUntilRaidInSec) {
			return;
		}
		timeUntilRaidInLastWarnPlayersOfRaidRun = timeUntilRaidInSec;

		if (!TIMES_TO_WARN_PLAYERS_OF_RAID.stream().anyMatch(time -> time == timeUntilRaidInSec)) {
			return;
		}
		Baseraids.sendChatMessage("Time until next raid: " + getTimeUntilRaidInDisplayString());
		if (timeUntilRaidInSec < 5) {
			world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT,
					5.0F, 1F);
		}
	}

	/**
	 * Checks and returns whether a raid should start. A raid should start whenever
	 * the specified time between raids has passed and it is night and the nexus is
	 * placed.
	 * 
	 * @return a flag whether a raid should start or not
	 */
	private boolean shouldStartRaid() {
		if (getTimeSinceRaid() < ConfigOptions.timeBetweenRaids.get()) {
			return false;
		}
		if (world.getDayTime() % 24000 < START_OF_NIGHT_IN_WORLD_DAY_TIME) {
			return false;
		}
		if (NexusBlock.getState() != State.BLOCK) {
			return false;
		}
		return true;
	}

	/**
	 * Ticks during raids and checks if the raid is won.
	 */
	private void activeRaidTick() {
		if (!isRaidActive) {
			return;
		}
		if (tick % RAID_SOUND_INTERVAL == 0) {
			world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F, 0.1F);	
		}

		if (raidSpawningMng.areAllSpawnedMobsDead()) {
			Baseraids.LOGGER.info("Raid ended: all mobs are dead");
			winRaid();
			return;
		}
		if (isMaxRaidDurationOver()) {
			Baseraids.LOGGER.info("Raid ended: reached max duration");
			winRaid();
			return;
		}
	}
	
	
	

	public void startRaid() {
		if (world == null) {
			Baseraids.LOGGER.warn("Could not start raid because world == null");
			return;
		}
		if (!world.getDimensionKey().equals(World.OVERWORLD))
			return;

		Baseraids.sendChatMessage("You are being raided!");
		Baseraids.LOGGER.info("Initiating raid");

		setLastRaidGameTime((int) (world.getGameTime()));
		setRaidActive(true);

		raidSpawningMng.spawnRaidMobs();
	}

	public void loseRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid lost");
		Baseraids.sendChatMessage("You have lost the raid!");
		// make sure the raid level is adjusted before endRaid() because endRaid() uses
		// the new level
		resetRaidLevel();
		endRaid();

	}

	public void winRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid won");
		Baseraids.sendChatMessage("You have won the raid!");

		// PLACE LOOT CHEST
		BlockPos chestPos = NexusBlock.getBlockPos().add(ConfigOptions.lootChestPositionRelative);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
		if (world.getTileEntity(chestPos) instanceof ChestTileEntity) {
			ChestTileEntity chestEntity = (ChestTileEntity) world.getTileEntity(chestPos);

			chestEntity.setLootTable(REWARD_CHEST_LOOTTABLES[curRaidLevel - 1], world.getRandom().nextLong());
			chestEntity.fillWithLoot(null);

			Baseraids.LOGGER.info("Added loot to loot chest");
		} else {
			Baseraids.LOGGER.error("Could not add loot to loot chest");
		}

		// make sure the raid level is adjusted before endRaid() because endRaid() uses
		// the new level
		increaseRaidLevel();
		endRaid();

		// PLAY SOUND EFFECT
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				1.5F);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F, 1.5F);
    	try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				2);

	}

	private void endRaid() {
		Baseraids.sendChatMessage("Your next raid will have level " + curRaidLevel);
		setRaidActive(false);
		raidSpawningMng.killAllMobs();
		world.sendBlockBreakProgress(-1, NexusBlock.getBlockPos(), -1);
	}

	// in ticks
	// raw time means that the night time is not considered which is required for a
	// raid to actually start
	private int getRawTimeUntilRaid() {
		return ConfigOptions.timeBetweenRaids.get() - getTimeSinceRaid();
	}

	// in ticks
	private int getTimeUntilRaid() {
		// Math.floorMod returns only positive values (for a positive modulus) while %
		// returns the actual remainder
		long timeUntilNightTime = Math.floorMod(START_OF_NIGHT_IN_WORLD_DAY_TIME - (world.getDayTime() % 24000), 24000);
		return (int) Math.max(getRawTimeUntilRaid(), timeUntilNightTime);
	}

	public int getTimeUntilRaidInSec() {
		return getTimeUntilRaid() / 20;
	}

	public String getTimeUntilRaidInDisplayString() {
		int timeUntilRaidInSec = getTimeUntilRaidInSec();

		int displayTimeMin = (int) timeUntilRaidInSec / 60;
		int displayTimeSec = timeUntilRaidInSec % 60;

		String displayTime = "";
		if (displayTimeMin > 0) {
			displayTime += displayTimeMin + "min";
		}
		if (displayTimeSec > 0) {
			displayTime += displayTimeSec + "s";
		}
		return displayTime;
	}

	private boolean isMaxRaidDurationOver() {
		return getTimeSinceRaid() > ConfigOptions.maxRaidDuration.get();
	}

	private void increaseRaidLevel() {
		curRaidLevel++;
		if (curRaidLevel > MAX_RAID_LEVEL)
			curRaidLevel = MAX_RAID_LEVEL;
		markDirty();
	}

	private void resetRaidLevel() {
		curRaidLevel = MIN_RAID_LEVEL;
		markDirty();
	}

	public CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putInt("curRaidLevel", curRaidLevel);
		nbt.putInt("lastRaidGameTime", lastRaidGameTime);
		nbt.putBoolean("isRaidActive", isRaidActive);

		CompoundNBT raidSpawning = raidSpawningMng.writeAdditional();
		nbt.put("raidSpawningManager", raidSpawning);

		return nbt;
	}
	
	public void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		try {
			lastRaidGameTime = nbt.getInt("lastRaidGameTime");
			curRaidLevel = nbt.getInt("curRaidLevel");
			isRaidActive = nbt.getBoolean("isRaidActive");

			CompoundNBT raidSpawningNBT = nbt.getCompound("raidSpawningManager");
			raidSpawningMng.readAdditional(raidSpawningNBT, serverWorld);

			Baseraids.LOGGER.debug("Finished loading RaidManager");
			
			
		}catch(Exception e) {
			Log.warn("Exception while reading data for RaidManager. Setting parameters to default. Exception: " + e);
			setDefaultWriteParameters();
			markDirty();
		}
	}

	private void setDefaultWriteParametersIfNotSet() {
		if (lastRaidGameTime == -1) {
			lastRaidGameTime = 0;
		}
		if (curRaidLevel == -1) {
			curRaidLevel = 1;
		}
		if (isRaidActive == null) {
			isRaidActive = false;
		}
	}

	
	private void setLastRaidGameTime(int time) {
		lastRaidGameTime = time;
		markDirty();
	}

	public boolean isRaidActive() {
		return isRaidActive;
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

	private void setRaidLevel(int level) {
		curRaidLevel = level;
		markDirty();
	}

	public int getRaidLevel() {
		return curRaidLevel;
	}
}
