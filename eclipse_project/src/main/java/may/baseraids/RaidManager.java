package may.baseraids;

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
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaidManager {

	// RUNTIME VARIABLES
	private World world = null;
	public boolean isInitialized = false;

	private Boolean isRaidActive;
	private int tick = 0;
	private int timeUntilRaidInLastWarnPlayersOfRaidRun = -1;
	private int curRaidLevel = -1;
	private int lastRaidGameTime = -1;

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
		setDefaultWriteParametersIfNotSet();
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
			world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F,
					0.1F);
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

	/**
	 * Initiates a raid, that means it sets the <code>lastRaidGameTime</code> and
	 * the <code>isRaidActive</code> and calls the spawning of the mobs.
	 */
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

	/**
	 * Takes care of everything that happens when a raid is lost. This includes
	 * resetting the raid level and calling <code>endRaid()</code>.
	 */
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

	/**
	 * Takes care of everything that happens when a raid is lost. This includes
	 * increasing the raid level, placing a reward chest, calling
	 * <code>endRaid()</code> and playing the <code>RaidWinSound</code>.
	 */
	public void winRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid won");
		Baseraids.sendChatMessage("You have won the raid!");

		spawnAndFillRewardChest();
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
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				1.5F);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				2);

	}

	/**
	 * Ends a raid by setting <code>isRaidActive</code> to false and killing all
	 * mobs spawned by the raid.
	 */
	private void endRaid() {
		Baseraids.sendChatMessage("Your next raid will have level " + curRaidLevel);
		setRaidActive(false);
		raidSpawningMng.killAllMobs();
		world.sendBlockBreakProgress(-1, NexusBlock.getBlockPos(), -1);
	}

	/**
	 * Spawns a chest at the position specified by
	 * <code>ConfigOptions.lootChestPositionRelative</code> relative form the nexus
	 * and fills it with loot defined in the loottables referenced in the field
	 * <code>REWARD_CHEST_LOOTTABLES</code>.
	 */
	private void spawnAndFillRewardChest() {
		// spawn chest
		BlockPos chestPos = NexusBlock.getBlockPos().add(ConfigOptions.lootChestPositionRelative);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());

		if (world.getTileEntity(chestPos) instanceof ChestTileEntity) {
			ChestTileEntity chestEntity = (ChestTileEntity) world.getTileEntity(chestPos);
			// fill chest
			chestEntity.setLootTable(REWARD_CHEST_LOOTTABLES[curRaidLevel - 1], world.getRandom().nextLong());
			chestEntity.fillWithLoot(null);
			Baseraids.LOGGER.info("Added loot to loot chest");
		} else {
			Baseraids.LOGGER.error("Could not add loot to loot chest");
		}
	}

	/**
	 * Gets the time in ticks until the next raid with considering only the
	 * <code>ConfigOptions.timeBetweenRaids</code> and not the night time which is
	 * required for a raid.
	 * 
	 * @return the raw number of ticks until the next raid according to
	 *         <code>ConfigOptions.timeBetweenRaids</code>
	 */
	private int getRawTimeUntilRaid() {
		return ConfigOptions.timeBetweenRaids.get() - getTimeSinceRaid();
	}

	/**
	 * Gets the actual time in ticks until the next raid considering all
	 * requirements for a raid.
	 * 
	 * @return the number of ticks until the next raid
	 */
	private int getTimeUntilRaid() {
		// Math.floorMod returns only positive values (for a positive modulus) while %
		// returns the actual remainder.
		long timeUntilNightTime = Math.floorMod(START_OF_NIGHT_IN_WORLD_DAY_TIME - (world.getDayTime() % 24000), 24000);
		return (int) Math.max(getRawTimeUntilRaid(), timeUntilNightTime);
	}

	public int getTimeUntilRaidInSec() {
		return getTimeUntilRaid() / 20;
	}

	/**
	 * Converts the time until the next raid into a string that can be used for all
	 * displaying purposes. The format is "<code>M</code>min<code>S</code>s" for
	 * <code>M</code> the remaining minutes and <code>S</code> the remaining seconds
	 * for <code>M>0</code> otherwise "<code>S</code>s".
	 * 
	 * @return a formatted String showing the time until the next raid
	 */
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

	/**
	 * Increases the raid level by one, unless <code>MAX_RAID_LEVEL</code> is
	 * reached.
	 */
	private void increaseRaidLevel() {
		int new_level = Math.min(curRaidLevel + 1, MAX_RAID_LEVEL);
		setRaidLevel(new_level);
	}

	/**
	 * Resets the raid level to <code>MIN_RAID_LEVEL</code>.
	 */
	private void resetRaidLevel() {
		setRaidLevel(MIN_RAID_LEVEL);
	}

	/**
	 * Writes the necessary data to a <code>CompoundNBT</code> and returns the
	 * <code>CompoundNBT</code> object.
	 * 
	 * @return the adapted <code>CompoundNBT</code> that was written to
	 */
	public CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("isRaidActive", isRaidActive);
		nbt.putInt("curRaidLevel", curRaidLevel);
		nbt.putInt("lastRaidGameTime", lastRaidGameTime);

		CompoundNBT raidSpawning = raidSpawningMng.writeAdditional();
		nbt.put("raidSpawningManager", raidSpawning);

		return nbt;
	}

	/**
	 * Reads the data stored in the given <code>CompoundNBT</code>. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements. If an exception was thrown during the
	 * reading process (this could very well happen for incompatible versions), the
	 * parameters that were not set are given a default value using
	 * <code>setDefaultWriteParametersIfNotSet()</code>.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 * @param serverWorld the world that is loaded. It is used in the
	 *                    <code>RaidSpawningManager</code> to get references to
	 *                    previously spawned mobs.
	 */
	public void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		try {
			isRaidActive = nbt.getBoolean("isRaidActive");
			curRaidLevel = nbt.getInt("curRaidLevel");
			lastRaidGameTime = nbt.getInt("lastRaidGameTime");

			CompoundNBT raidSpawningNBT = nbt.getCompound("raidSpawningManager");
			raidSpawningMng.readAdditional(raidSpawningNBT, serverWorld);

			Baseraids.LOGGER.debug("Finished loading RaidManager");

		} catch (Exception e) {
			Log.warn("Exception while reading data for RaidManager. Setting parameters to default. Exception: " + e);
			setDefaultWriteParametersIfNotSet();
		}
	}

	/**
	 * Gives the parameters that are normally saved and loaded a default value if
	 * they have not been successfully loaded.
	 */
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

	public void markDirty() {
		Baseraids.baseraidsData.markDirty();
	}

	public int getTimeSinceRaid() {
		if (world == null)
			return -1;
		return (int) (world.getGameTime()) - lastRaidGameTime;
	}

	private void setRaidLevel(int level) {
		curRaidLevel = level;
		markDirty();
	}

	public int getRaidLevel() {
		return curRaidLevel;
	}
}
