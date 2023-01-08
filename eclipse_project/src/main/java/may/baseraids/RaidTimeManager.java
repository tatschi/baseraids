package may.baseraids;

import java.util.Objects;
import java.util.Set;

import org.jline.utils.Log;

import com.google.common.collect.Sets;

import may.baseraids.config.ConfigOptions;
import may.baseraids.nexus.NexusBlock;
import may.baseraids.nexus.NexusBlock.NexusState;
import net.minecraft.entity.player.PlayerEntity.SleepResult;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RaidTimeManager {

	private World world;
	private RaidManager raidManager;
	
	private int timeUntilRaidInLastWarnPlayersOfRaidRun = -1;
	private long nextRaidGameTime = -1;
	private int activeRaidTicks = 0;
	private long daytimeBeforeRaid = 0;
	
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
	 * warned of the coming raid (approximated, in seconds).
	 * @see #warnPlayersOfRaid()
	 */
	private static final Set<Integer> TIMES_TO_WARN_PLAYERS_OF_RAID = Sets.newHashSet(4800, 3600, 2400, 1800, 1200, 900,
			600, 300, 120, 60, 30, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
	
	public RaidTimeManager(RaidManager raidManager, World world) {
		MinecraftForge.EVENT_BUS.register(this);
		this.raidManager = raidManager;
		this.world = world;
	}
	
	/**
	 * Warns all players in the world of an upcoming raid via chat messages and
	 * sounds. The times at which to warn are specified in the field
	 * {@link #TIMES_TO_WARN_PLAYERS_OF_RAID}.
	 */
	void warnPlayersOfRaid() {
		int timeUntilRaidInSec = getTimeUntilRaid().getSec();
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
		Baseraids.messageManager.sendStatusMessage("Time until next raid: " + getTimeUntilRaid().getDisplayString());
		if (timeUntilRaidInSec <= 5 && ConfigOptions.getEnableSoundCountdown()) {
			world.playSound(null, NexusBlock.getBlockPos(), Baseraids.SOUND_RAID_TICKING.get(), SoundCategory.BLOCKS,
					300F, 1.0F);
		}
	}
	
	/**
	 * Checks and returns whether a raid should start. A raid should start whenever
	 * the specified time between raids has passed and it is night and the nexus is
	 * placed.
	 * 
	 * @return a flag whether a raid should start or not
	 */
	boolean shouldStartRaid() {
		if (world.getGameTime() < nextRaidGameTime) {
			return false;
		}
		return NexusBlock.getState() == NexusState.BLOCK;
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
		if (ConfigOptions.getEnableTimeReductionFromSleeping()) {
			restrictSleepBeforeRaid(event);
		}
	}

	/**
	 * Forbids the player to sleep in bed when a raid is coming in less than
	 * {@link #SLEEP_RESTRICTION_TICKS}.
	 * 
	 * @param event the event of type {@link PlayerSleepInBedEvent} that may be
	 *              cancelled
	 */
	private void restrictSleepBeforeRaid(PlayerSleepInBedEvent event) {
		if (getTimeUntilRaid().getTicks() < SLEEP_RESTRICTION_TICKS) {
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
		if (raidManager.isRaidActive()) {
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
		if (!ConfigOptions.getEnableTimeReductionFromSleeping()) {
			return;
		}
		MCDuration subtractionTime = new MCDuration(event.getNewTime() - ((ServerWorld) event.getWorld()).getDayTime());
		subtractFromTimeUntilRaid(subtractionTime);
		Baseraids.messageManager.sendStatusMessage("Time until next raid: " + getTimeUntilRaid().getDisplayString(), true);
	}
	
	/**
	 * Gets the time in ticks until the next raid based on
	 * {@link #nextRaidGameTime}.
	 * 
	 * @return the number of ticks until the next raid
	 */
	public MCDuration getTimeUntilRaid() {
		return new MCDuration(nextRaidGameTime - world.getGameTime());
	}

	boolean isMaxRaidDurationOver() {
		return activeRaidTicks > ConfigOptions.getMaxRaidDuration();
	}
	
	void resetActiveRaidTicks() {
		activeRaidTicks = 0;
		markDirty();
	}

	void incrementActiveRaidTicks() {
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
	
	private long getNewNextRaidGameTime() {
		return world.getGameTime() + ConfigOptions.getTimeBetweenRaids();
	}

	void resetNextRaidGameTime() {
		setNextRaidGameTime(getNewNextRaidGameTime());
	}

	private void setNextRaidGameTime(long time) {
		if (time < world.getGameTime()) {
			return;
		}
		nextRaidGameTime = time;
		Baseraids.LOGGER.debug("Set next raid game time to {}", time);
		markDirty();
	}

	private void setTimeUntilRaid(long time) {
		if (time < 0) {
			return;
		}
		setNextRaidGameTime(world.getGameTime() + time);
	}
	
	public void setTimeUntilRaid(MCDuration time) {
		setTimeUntilRaid(time.getTicks());
	}

	public void addTimeUntilRaid(MCDuration addTime) {
		setTimeUntilRaid(getTimeUntilRaid().addDuration(addTime).getTicks());
	}
	
	public void subtractFromTimeUntilRaid(MCDuration subtractionTime) {
		setTimeUntilRaid(getTimeUntilRaid().subtractDuration(subtractionTime).getTicks());
	}
	
	void setTimeToNighttime() {
		setDaytimeBeforeRaid(world.getDayTime());
		((ServerWorld) world).setDayTime(START_RAID_DAY_TIME);
	}

	void resetDaytimeToDaytimeBeforeRaid() {
		((ServerWorld) world).setDayTime(daytimeBeforeRaid);
		resetDaytimeBeforeRaid();
	}
	
	private void markDirty() {
		raidManager.markDirty();
	}
	
	/**
	 * Saves data relevant for the RaidManager: Writes the necessary data to a
	 * {@link CompoundNBT} and returns the {@link CompoundNBT} object.
	 * 
	 * @return the adapted {@link CompoundNBT} that was written to
	 */
	public CompoundNBT write() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putLong("nextRaidGameTime", nextRaidGameTime);
		nbt.putInt("activeRaidTicks", activeRaidTicks);
		nbt.putLong("daytimeBeforeRaid", daytimeBeforeRaid);
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
	 */
	public void read(CompoundNBT nbt) {
		try {
			nextRaidGameTime = nbt.getLong("nextRaidGameTime");
			activeRaidTicks = nbt.getInt("activeRaidTicks");
			daytimeBeforeRaid = nbt.getLong("daytimeBeforeRaid");

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
		if (nextRaidGameTime == -1) {
			nextRaidGameTime = getNewNextRaidGameTime();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(activeRaidTicks, daytimeBeforeRaid, nextRaidGameTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RaidTimeManager other = (RaidTimeManager) obj;
		return activeRaidTicks == other.activeRaidTicks && daytimeBeforeRaid == other.daytimeBeforeRaid
				&& nextRaidGameTime == other.nextRaidGameTime;
	}
}
