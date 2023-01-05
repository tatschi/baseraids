package may.baseraids;

import java.util.Set;

import org.jline.utils.Log;

import com.google.common.collect.Sets;

import may.baseraids.config.ConfigOptions;
import may.baseraids.entities.ai.GlobalBlockBreakProgressManager;
import may.baseraids.nexus.NexusBlock;
import may.baseraids.nexus.NexusBlock.NexusState;
import may.baseraids.nexus.NexusEffects;
import may.baseraids.nexus.NexusEffectsTileEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity.SleepResult;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class controls everything concerning raids: spawning, timers, starting
 * and ending a raid, rewards and more.
 * 
 * @author Natascha May
 */
// @Mod.EventBusSubscriber annotation automatically registers STATIC event handlers 
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaidManager {

	// RUNTIME VARIABLES
	private World world = null;
	private WorldManager worldManager;
	private boolean isInitialized = false;

	private Boolean isRaidActive;
	private int timeUntilRaidInLastWarnPlayersOfRaidRun = -1;
	private int curRaidLevel = -1;
	private long nextRaidGameTime = -1;
	private int activeRaidTicks = 0;
	private long daytimeBeforeRaid = 0;

	public static final int MAX_RAID_LEVEL = 10, MIN_RAID_LEVEL = 1;

	/**
	 * defines the daytime that the {@link ServerWorld#setDayTime(long)} is set to
	 * when a raid is started (one day = 24000)
	 */
	private static final int START_RAID_DAY_TIME = 14000;
	/**
	 * defines the amount of ticks before the next raid start at which you can no
	 * longer sleep
	 */
	private static final int SLEEP_RESTRICTION_TICKS = 12020;

	/**
	 * Sets the times (remaining time until raid) at which all players will be
	 * warned of the coming raid (approximated, in seconds), @see
	 * {@link RaidManager#warnPlayersOfRaid()}.
	 */
	private static final Set<Integer> TIMES_TO_WARN_PLAYERS_OF_RAID = Sets.newHashSet(4800, 3600, 2400, 1800, 1200, 900,
			600, 300, 120, 60, 30, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1);

	private RaidSpawningManager raidSpawningMng;
	public GlobalBlockBreakProgressManager globalBlockBreakProgressMng;
	public RestoreDestroyedBlocksManager restoreDestroyedBlocksMng;

	private static final ResourceLocation[] REWARD_CHEST_LOOTTABLES = { new ResourceLocation(Baseraids.MODID, "level1"),
			new ResourceLocation(Baseraids.MODID, "level2"), new ResourceLocation(Baseraids.MODID, "level3"),
			new ResourceLocation(Baseraids.MODID, "level4"), new ResourceLocation(Baseraids.MODID, "level5"),
			new ResourceLocation(Baseraids.MODID, "level6"), new ResourceLocation(Baseraids.MODID, "level7"),
			new ResourceLocation(Baseraids.MODID, "level8"), new ResourceLocation(Baseraids.MODID, "level9"),
			new ResourceLocation(Baseraids.MODID, "level10") };

	public RaidManager(World world, WorldManager worldManager) {
		MinecraftForge.EVENT_BUS.register(this);
		this.world = world;
		this.worldManager = worldManager;
		raidSpawningMng = new RaidSpawningManager(this, world, worldManager);
		globalBlockBreakProgressMng = new GlobalBlockBreakProgressManager(this, world);
		restoreDestroyedBlocksMng = new RestoreDestroyedBlocksManager(this, world);
		setDefaultWriteParametersIfNotSet();
		Baseraids.LOGGER.info("RaidManager created");
		isInitialized = true;
	}

	/**
	 * Controls everything that happens every tick like warning of raids, starting
	 * raids and calling the {@link RaidManager#activeRaidTick()} during a raid.
	 * 
	 * @param event the event of type {@link TickEvent.WorldTickEvent} that triggers
	 *              this function
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
				Baseraids.messageManager.sendStatusMessage("Raid was ended because difficulty is peaceful", true);
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
	}

	/**
	 * Warns all players in the world of an upcoming raid via chat messages and
	 * sounds. The times at which to warn are specified in the field
	 * {@link RaidManager#TIMES_TO_WARN_PLAYERS_OF_RAID}.
	 */
	private void warnPlayersOfRaid() {
		int timeUntilRaidInSec = getTimeUntilRaidInSec();
		// Because this method is called multiple times per second,
		// avoid multiple messages for the same second by remembering the last warning
		// time
		if (timeUntilRaidInLastWarnPlayersOfRaidRun == timeUntilRaidInSec) {
			return;
		}

		if (TIMES_TO_WARN_PLAYERS_OF_RAID.stream().noneMatch(time -> time == timeUntilRaidInSec)) {
			return;
		}

		timeUntilRaidInLastWarnPlayersOfRaidRun = timeUntilRaidInSec;
		Baseraids.messageManager.sendStatusMessage("Time until next raid: " + getTimeUntilRaidInDisplayString());
		if (timeUntilRaidInSec <= 5 && Boolean.TRUE.equals(ConfigOptions.enableSoundCountdown.get())) {
			world.playSound(null, NexusBlock.getBlockPos(), Baseraids.SOUND_RAID_TICKING.get(), SoundCategory.BLOCKS,
					2.0F, 1.0F);
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
		if (world.getGameTime() < nextRaidGameTime) {
			return false;
		}
		if (NexusBlock.getState() != NexusState.BLOCK) {
			return false;
		}
		return true;
	}

	/**
	 * Ticks during raids and checks if the raid is won or the max duration is over.
	 */
	private void activeRaidTick() {
		if (Boolean.FALSE.equals(isRaidActive)) {
			return;
		}
		incrementActiveRaidTicks();

		if (raidSpawningMng.areAllSpawnedMobsDead()) {
			Baseraids.LOGGER.info("Raid ended: all mobs are dead");
			winRaid();
			return;
		}
		if (isMaxRaidDurationOver()) {
			Baseraids.LOGGER.info("Raid ended: reached max duration");
			winRaid();
		}
	}

	/**
	 * Starts a raid. That means it resets the raid specific parameters of this
	 * instance and related managing instances and calls the spawning of the mobs.
	 */
	public void startRaid() {
		Baseraids.messageManager.sendStatusMessage("You are being raided!");
		Baseraids.LOGGER.info("Initiating raid");

		resetActiveRaidTicks();
		resetNextRaidGameTime();
		restoreDestroyedBlocksMng.clearSavedBlocks();

		setTimeToNighttime();
		setRaidActive(true);
		raidSpawningMng.spawnRaidMobs();
	}

	/**
	 * Takes care of everything that happens when a raid is lost. This includes
	 * resetting the raid level and calling the more common
	 * {@link RaidManager#endRaid()}.
	 */
	public void loseRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid lost");
		Baseraids.messageManager.sendStatusMessage("You lost the raid!");
		// make sure the raid level is adjusted before endRaid() because endRaid() uses
		// the new level
		resetRaidLevel();
		endRaid();

		playLoseSound();
	}

	private void playWinSound() {
		if (Boolean.TRUE.equals(ConfigOptions.enableSoundWinLose.get())) {
			world.playSound(null, NexusBlock.getBlockPos(), Baseraids.SOUND_RAID_WON.get(), SoundCategory.BLOCKS, 2.0F,
					1.0F);
		}
	}

	private void playLoseSound() {
		if (Boolean.TRUE.equals(ConfigOptions.enableSoundWinLose.get())) {
			world.playSound(null, NexusBlock.getBlockPos(), Baseraids.SOUND_RAID_LOST.get(), SoundCategory.BLOCKS, 2.0F,
					1.0F);
		}
	}

	/**
	 * Takes care of everything that happens when a raid is lost. This includes
	 * increasing the raid level, placing a reward chest, calling
	 * {@link RaidManager#endRaid()} and playing the win sound.
	 */
	public void winRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid won");
		Baseraids.messageManager.sendStatusMessage("You won the raid!");

		spawnAndFillRewardChest();

		// make sure to add these effects before increasing the raid level
		NexusEffectsTileEntity nexusEntity = (NexusEffectsTileEntity) worldManager.getServerWorld()
				.getTileEntity(NexusBlock.getBlockPos());
		nexusEntity.addEffectsToPlayers(NexusEffects.getEffectInstance(NexusEffects.REGEN_EFFECT_AFTER_RAID_WIN));
		nexusEntity.setLastWonRaidLevel(getRaidLevel());

		// make sure the raid level is adjusted before endRaid() because endRaid() uses
		// the new level
		increaseRaidLevel();
		endRaid();

		playWinSound();
	}

	/**
	 * Ends a raid by setting resetting raid specific parameters of this instance
	 * and related managing instances and killing all mobs spawned by the raid.
	 */
	private void endRaid() {
		Baseraids.messageManager.sendStatusMessage("Your next raid will have level " + curRaidLevel, false);
		setRaidActive(false);
		world.sendBlockBreakProgress(-1, NexusBlock.getBlockPos(), -1);
		globalBlockBreakProgressMng.resetAllProgress();
		raidSpawningMng.killAllMobs();
		if (Boolean.TRUE.equals(ConfigOptions.restoreDestroyedBlocks.get())) {
			restoreDestroyedBlocksMng.restoreSavedBlocks();
		}
		resetDaytimeToDaytimeBeforeRaid();
	}

	/**
	 * Spawns a chest at the position specified by
	 * {@link ConfigOptions#lootChestPositionRelative} relative form the nexus and
	 * fills it with loot defined in the loottables referenced in the field
	 * {@link RaidManager#REWARD_CHEST_LOOTTABLES}.
	 */
	private void spawnAndFillRewardChest() {
		// spawn chest
		BlockPos chestPos = NexusBlock.getBlockPos().add(ConfigOptions.lootChestPositionRelative);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());

		if (world.getTileEntity(chestPos) instanceof ChestTileEntity) {
			ChestTileEntity chestEntity = (ChestTileEntity) world.getTileEntity(chestPos);
			// fill chest
			for (int i = 0; i < world.getPlayers().size(); i++) {
				chestEntity.setLootTable(REWARD_CHEST_LOOTTABLES[curRaidLevel - 1], world.getRandom().nextLong());
				chestEntity.fillWithLoot(null);
			}
			Baseraids.LOGGER.info("Added loot to loot chest");
		} else {
			Baseraids.LOGGER.error("Could not add loot to loot chest");
		}
	}

	/**
	 * Forbids the player to sleep in certain situations like shortly before or
	 * during a raid.
	 * 
	 * @param event the event of type {@link PlayerSleepInBedEvent} that triggers
	 *              this function
	 */
	@SubscribeEvent
	public void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
		if (event.getPlayer().world.isRemote()) {
			return;
		}
		restrictSleepDuringRaid(event);
		if (Boolean.TRUE.equals(ConfigOptions.enableTimeReductionFromSleeping.get())) {
			restrictSleepBeforeRaid(event);
		}
	}

	/**
	 * Forbids the player to sleep in bed when a raid is coming in less than
	 * {@link RaidManager#SLEEP_RESTRICTION_TICKS}.
	 * 
	 * @param event the event of type {@link PlayerSleepInBedEvent} that may be
	 *              cancelled
	 */
	private void restrictSleepBeforeRaid(PlayerSleepInBedEvent event) {
		if (getTimeUntilRaid() < SLEEP_RESTRICTION_TICKS) {
			event.setResult(SleepResult.OTHER_PROBLEM);
			event.getPlayer().sendStatusMessage(new StringTextComponent("You cannot sleep before a raid!"), true);
		}
	}

	/**
	 * Forbids the player to sleep in bed when a raid is active.
	 * 
	 * @param event the event of type {@link PlayerSleepInBedEvent} that may be
	 *              cancelled
	 */
	private void restrictSleepDuringRaid(PlayerSleepInBedEvent event) {
		if (isRaidActive()) {
			event.setResult(SleepResult.OTHER_PROBLEM);
			event.getPlayer().sendStatusMessage(new StringTextComponent("You cannot sleep during a raid!"), true);
		}
	}

	/**
	 * Reduces the time until the next raid after sleeping in bed by the difference
	 * between the new and old time.
	 * 
	 * @param event the event of type {@link SleepFinishedTimeEvent} that may be
	 *              cancelled
	 */
	@SubscribeEvent
	public void onSleepFinished(SleepFinishedTimeEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (Boolean.FALSE.equals(ConfigOptions.enableTimeReductionFromSleeping.get())) {
			return;
		}
		int reductionTime = (int) (event.getNewTime() - ((ServerWorld) event.getWorld()).getDayTime());
		reduceTimeUntilRaid(reductionTime);
		Baseraids.messageManager.sendStatusMessage("Time until next raid: " + getTimeUntilRaidInDisplayString(), true);
	}

	/**
	 * Gets the time in ticks until the next raid based on
	 * {@link #nextRaidGameTime}.
	 * 
	 * @return the number of ticks until the next raid
	 */
	private int getTimeUntilRaid() {
		return (int) (nextRaidGameTime - world.getGameTime());
	}

	public int getTimeUntilRaidInSec() {
		return getTimeUntilRaid() / 20;
	}

	public int getTimeUntilRaidInMin() {
		return getTimeUntilRaidInSec() / 60;
	}

	/**
	 * Converts the time until the next raid into a string that can be used for all
	 * displaying purposes.
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
		return activeRaidTicks > ConfigOptions.maxRaidDuration.get();
	}

	/**
	 * Increases the raid level by one, unless {@link #MAX_RAID_LEVEL} is reached.
	 */
	private void increaseRaidLevel() {
		int new_level = Math.min(curRaidLevel + 1, MAX_RAID_LEVEL);
		setRaidLevel(new_level);
	}

	/**
	 * Resets the raid level to {@link #MIN_RAID_LEVEL}.
	 */
	private void resetRaidLevel() {
		setRaidLevel(MIN_RAID_LEVEL);
	}

	/**
	 * Saves data relevant for the RaidManager: Writes the necessary data to a
	 * {@link CompoundNBT} and returns the {@link CompoundNBT} object.
	 * 
	 * @return the adapted {@link CompoundNBT} that was written to
	 */
	public CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("isRaidActive", isRaidActive);
		nbt.putInt("curRaidLevel", curRaidLevel);
		nbt.putLong("nextRaidGameTime", nextRaidGameTime);
		nbt.putInt("activeRaidTicks", activeRaidTicks);
		nbt.putLong("daytimeBeforeRaid", daytimeBeforeRaid);

		CompoundNBT raidSpawning = raidSpawningMng.writeAdditional();
		nbt.put("raidSpawningManager", raidSpawning);
		CompoundNBT restoreDestroyedBlocksMngNBT = restoreDestroyedBlocksMng.writeAdditional();
		nbt.put("restoreDestroyedBlocksManager", restoreDestroyedBlocksMngNBT);

		return nbt;
	}

	/**
	 * Reads the data stored in the given {@link CompoundNBT}. This function assumes
	 * that the nbt was previously written by this class or to be precise, that the
	 * nbt includes certain elements. If an exception was thrown during the reading
	 * process (this could very well happen for incompatible versions), the
	 * parameters that were not set are given a default value using
	 * {@link #setDefaultWriteParametersIfNotSet()}.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 * @param serverWorld the world that is loaded. It is used in the
	 *                    {@link RaidSpawningManager} to get references to
	 *                    previously spawned mobs.
	 */
	public void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		try {
			isRaidActive = nbt.getBoolean("isRaidActive");
			curRaidLevel = nbt.getInt("curRaidLevel");
			nextRaidGameTime = nbt.getLong("nextRaidGameTime");
			activeRaidTicks = nbt.getInt("activeRaidTicks");
			daytimeBeforeRaid = nbt.getLong("daytimeBeforeRaid");

			CompoundNBT raidSpawningNBT = nbt.getCompound("raidSpawningManager");
			raidSpawningMng.readAdditional(raidSpawningNBT, serverWorld);
			CompoundNBT restoreDestroyedBlocksMngNBT = nbt.getCompound("restoreDestroyedBlocksManager");
			restoreDestroyedBlocksMng.readAdditional(restoreDestroyedBlocksMngNBT, serverWorld);

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
		if (curRaidLevel == -1) {
			curRaidLevel = 1;
		}
		if (isRaidActive == null) {
			isRaidActive = false;
		}
		if (nextRaidGameTime == -1) {
			nextRaidGameTime = getNewNextRaidGameTime();
		}
	}

	private long getNewNextRaidGameTime() {
		return world.getGameTime() + ConfigOptions.timeBetweenRaids.get();
	}

	private void resetNextRaidGameTime() {
		setNextRaidGameTime(getNewNextRaidGameTime());
	}

	private void setNextRaidGameTime(long time) {
		if (time < world.getGameTime()) {
			return;
		}
		nextRaidGameTime = time;
		Baseraids.LOGGER.debug("Set next raid game time to " + time);
		markDirty();
	}

	private void setTimeUntilRaid(long time) {
		if (time < 0) {
			return;
		}
		setNextRaidGameTime(world.getGameTime() + time);
	}

	public void setTimeUntilRaidInMin(int min) {
		setTimeUntilRaid((long) (min) * 60 * 20);
	}

	private void reduceTimeUntilRaid(int reductionTime) {
		setTimeUntilRaid(getTimeUntilRaid() - reductionTime);
	}

	public void reduceTimeUntilRaidInMin(int reductionTimeInMin) {
		reduceTimeUntilRaid(reductionTimeInMin * 60 * 20);
	}

	public boolean isRaidActive() {
		return isRaidActive;
	}

	public void setRaidActive(boolean active) {
		isRaidActive = active;
		markDirty();
	}

	private void resetActiveRaidTicks() {
		activeRaidTicks = 0;
		markDirty();
	}

	private void incrementActiveRaidTicks() {
		activeRaidTicks++;
		markDirty();
	}

	private void resetDaytimeBeforeRaid() {
		daytimeBeforeRaid = 0;
		markDirty();
	}

	private void setDaytimeBeforeRaid(long daytime) {
		daytimeBeforeRaid = daytime;
		markDirty();
	}

	void markDirty() {
		worldManager.markDirty();
	}

	public void setRaidLevel(int level) {
		curRaidLevel = level;
		markDirty();
	}

	public int getRaidLevel() {
		return curRaidLevel;
	}

	public boolean isEntityRaiding(LivingEntity entity) {
		if (!isRaidActive()) {
			return false;
		}
		return raidSpawningMng.isEntityRaiding(entity);
	}

	private void setTimeToNighttime() {
		setDaytimeBeforeRaid(world.getDayTime());
		((ServerWorld) world).setDayTime(START_RAID_DAY_TIME);
	}

	private void resetDaytimeToDaytimeBeforeRaid() {
		((ServerWorld) world).setDayTime(daytimeBeforeRaid);
		resetDaytimeBeforeRaid();
	}
}
