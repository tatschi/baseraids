package may.baseraids;

import java.util.Objects;

import org.jline.utils.Log;

import may.baseraids.config.ConfigOptions;
import may.baseraids.entities.ai.GlobalBlockBreakProgressManager;
import may.baseraids.nexus.NexusBlock;
import may.baseraids.nexus.NexusEffects;
import may.baseraids.nexus.NexusEffectsTileEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This class controls everything concerning raids: spawning, timers, starting
 * and ending a raid, rewards and more.
 * 
 * @author Natascha May
 */
public class RaidManager {

	private World world = null;
	private WorldManager worldManager;
	private boolean isInitialized = false;

	private Boolean isRaidActive;
	private int curRaidLevel = -1;

	public static final int MAX_RAID_LEVEL = 10;
	public static final int MIN_RAID_LEVEL = 1;

	private RaidSpawningManager raidSpawningMng;
	private RaidTimeManager raidTimeMng;
	public final GlobalBlockBreakProgressManager globalBlockBreakProgressMng;
	public final RestoreDestroyedBlocksManager restoreDestroyedBlocksMng;

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
		raidTimeMng = new RaidTimeManager(this, world);
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
		if (event.phase != TickEvent.Phase.START)
			return;
		if (world.isRemote())
			return;
		if (!world.getDimensionKey().equals(World.OVERWORLD))
			return;

		raidTimeMng.warnPlayersOfRaid();
		if (raidTimeMng.shouldStartRaid()) {
			startRaid();
		}
		if (isRaidActive()) {
			activeRaidTick();
		}
	}
	
	/**
	 * Ticks during raids and checks if the raid is won or the max duration is over.
	 */
	private void activeRaidTick() {
		if (Boolean.FALSE.equals(isRaidActive)) {
			return;
		}
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			Baseraids.messageManager.sendStatusMessage("Raid was ended because difficulty is peaceful", true);
			endRaid();
		}
		raidTimeMng.incrementActiveRaidTicks();

		if (raidSpawningMng.areAllSpawnedMobsDead()) {
			Baseraids.LOGGER.info("Raid ended: all mobs are dead");
			winRaid();
			return;
		}
		if (raidTimeMng.isMaxRaidDurationOver()) {
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

		raidTimeMng.resetActiveRaidTicks();
		raidTimeMng.resetNextRaidGameTime();
		restoreDestroyedBlocksMng.clearSavedBlocks();

		raidTimeMng.setTimeToNighttime();
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
		if (ConfigOptions.getEnableSoundWinLose()) {
			world.playSound(null, NexusBlock.getBlockPos(), Baseraids.SOUND_RAID_WON.get(), SoundCategory.BLOCKS, 300F,
					1.0F);
		}
	}

	private void playLoseSound() {
		if (ConfigOptions.getEnableSoundWinLose()) {
			world.playSound(null, NexusBlock.getBlockPos(), Baseraids.SOUND_RAID_LOST.get(), SoundCategory.BLOCKS, 300F,
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
		if (ConfigOptions.getRestoreDestroyedBlocks()) {
			restoreDestroyedBlocksMng.restoreSavedBlocks();
		}
		raidTimeMng.resetDaytimeToDaytimeBeforeRaid();
	}

	/**
	 * Spawns a chest at the position specified by
	 * {@link ConfigOptions#lootChestPositionRelative} relative form the nexus and
	 * fills it with loot defined in the loottables referenced in the field
	 * {@link RaidManager#REWARD_CHEST_LOOTTABLES}.
	 */
	private void spawnAndFillRewardChest() {
		// spawn chest
		BlockPos chestPos = NexusBlock.getBlockPos().add(ConfigOptions.getLootChestPositionRelative());
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
	 * Increases the raid level by one, unless {@link #MAX_RAID_LEVEL} is reached.
	 */
	private void increaseRaidLevel() {
		int newLevel = Math.min(curRaidLevel + 1, MAX_RAID_LEVEL);
		setRaidLevel(newLevel);
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
	public CompoundNBT write() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putBoolean("isRaidActive", isRaidActive);
		nbt.putInt("curRaidLevel", curRaidLevel);

		CompoundNBT raidSpawning = raidSpawningMng.write();
		nbt.put("raidSpawningManager", raidSpawning);
		CompoundNBT raidTime = raidTimeMng.write();
		nbt.put("raidTimeManager", raidTime);
		CompoundNBT restoreDestroyedBlocksMngNBT = restoreDestroyedBlocksMng.write();
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
	public void read(CompoundNBT nbt, ServerWorld serverWorld) {
		try {
			isRaidActive = nbt.getBoolean("isRaidActive");
			curRaidLevel = nbt.getInt("curRaidLevel");

			CompoundNBT raidSpawningNBT = nbt.getCompound("raidSpawningManager");
			raidSpawningMng.read(raidSpawningNBT);
			CompoundNBT raidTimeNBT = nbt.getCompound("raidTimeManager");
			raidTimeMng.read(raidTimeNBT);
			CompoundNBT restoreDestroyedBlocksMngNBT = nbt.getCompound("restoreDestroyedBlocksManager");
			restoreDestroyedBlocksMng.read(restoreDestroyedBlocksMngNBT);

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
	}

	public boolean isRaidActive() {
		return isRaidActive;
	}

	public void setRaidActive(boolean active) {
		isRaidActive = active;
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

	public RaidTimeManager getRaidTimeManager() {
		return raidTimeMng;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(curRaidLevel, globalBlockBreakProgressMng, isInitialized, isRaidActive, raidSpawningMng,
				raidTimeMng, restoreDestroyedBlocksMng, world);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RaidManager other = (RaidManager) obj;
		return curRaidLevel == other.curRaidLevel
				&& Objects.equals(globalBlockBreakProgressMng, other.globalBlockBreakProgressMng)
				&& isInitialized == other.isInitialized && Objects.equals(isRaidActive, other.isRaidActive)
				&& Objects.equals(raidSpawningMng, other.raidSpawningMng)
				&& Objects.equals(raidTimeMng, other.raidTimeMng)
				&& Objects.equals(restoreDestroyedBlocksMng, other.restoreDestroyedBlocksMng)
				&& Objects.equals(world, other.world);
	}

}
