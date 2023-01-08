package may.baseraids.entities.ai;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jline.utils.Log;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This class collects and manages all information on breaking blocks during a
 * raid.
 * 
 * @author Natascha May
 */
public class GlobalBlockBreakProgressManager {

	private RaidManager raidManager;
	private Level level;

	/**
	 * Holds the {@link BlockBreakProgressManager} for each block that has been
	 * damaged.
	 */
	private ConcurrentHashMap<BlockPos, BlockBreakProgressManager> breakProgress = new ConcurrentHashMap<>();

	public GlobalBlockBreakProgressManager(RaidManager raidManager, Level level) {
		this.level = level;
		this.raidManager = raidManager;
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * Adds breaking progress to the given block.
	 * 
	 * @param pos    the position of the block that the progress is added to
	 * @param damage the amount of absolute damage made by the attack
	 * @return returns true, if the block at BlockPos was removed/broken
	 */
	public boolean addProgress(BlockPos pos, int damage) {
		BlockBreakProgressManager mng = breakProgress.computeIfAbsent(pos,
				p -> new BlockBreakProgressManager(level, pos, breakProgress.size()));
		if (mng.addProgress(damage)) {
			breakBlock(pos);
			return true;
		}
		return false;
	}

	/**
	 * Breaks a given block by removing it and resetting all relevant parameters for
	 * this block. If the block was the nexus, it initiates the loss of the raid.
	 * 
	 * @param pos the position of the block
	 */
	private synchronized void breakBlock(BlockPos pos) {
		if (!raidManager.isRaidActive()) {
			return;
		}
		Baseraids.LOGGER.debug("GlobalBlockBreakProgressManager#breakBlock");
		resetProgress(pos);

		level.playEvent(1021, pos, 0);
		level.playEvent(2001, pos, Block.getStateId(level.getBlockState(pos)));

		if (NexusBlock.getBlockPos().equals(pos)) {
			raidManager.loseRaid();
			return;
		}
		level.removeBlock(pos, false);
	}

	/**
	 * Resets all progress and parameters that are recorded by this class.
	 */
	public synchronized void resetAllProgress() {
		breakProgress.forEach(0, (k, v) -> level.sendBlockBreakProgress(v.breakBlockId, k, -1));
		breakProgress.clear();
	}

	/**
	 * Resets the progress and parameters for this BlockPos.
	 * 
	 * @param pos the position of the block
	 */
	public synchronized void resetProgress(BlockPos pos) {
		BlockBreakProgressManager mng = breakProgress.get(pos);
		if (mng != null) {
			level.sendBlockBreakProgress(mng.breakBlockId, pos, -1);
			breakProgress.remove(pos);
		}
	}

	/**
	 * Resets all progress and parameters for the position where a block was broken.
	 * 
	 * @param event the event of type {@link BlockEvent.BreakEvent} that triggers
	 *              this method
	 */
	@SubscribeEvent
	public void onBreakBlockResetAllInfoForThisBlockPos(final BlockEvent.BreakEvent event) {
		if (event.getPlayer().world.isRemote()) {
			return;
		}
		if (!raidManager.isRaidActive()) {
			return;
		}
		resetProgress(event.getPos());
	}

	/**
	 * Resets all progress and parameters for the position where a block was placed.
	 * 
	 * @param event the event of type {@link BlockEvent.EntityPlaceEvent} that
	 *              triggers this method
	 */
	@SubscribeEvent
	public void onPlaceBlockResetAllInfoForThisBlockPos(final BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (!raidManager.isRaidActive()) {
			return;
		}
		resetProgress(event.getPos());
	}

	/**
	 * Resets all progress and parameters for the position where a fluid was placed.
	 * 
	 * @param event the event of type {@link BlockEvent.FluidPlaceBlockEvent} that
	 *              triggers this method
	 */
	@SubscribeEvent
	public void onPlaceFluidResetAllInfoForThisBlockPos(final BlockEvent.FluidPlaceBlockEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (!raidManager.isRaidActive()) {
			return;
		}
		resetProgress(event.getPos());
	}
	
	/**
	 * Saves data relevant for the this class: Writes the necessary data to a
	 * {@link CompoundTag} and returns the {@link CompoundTag} object.
	 * 
	 * @return the adapted {@link CompoundTag} that was written to
	 */
	public CompoundTag write() {
		CompoundTag nbt = new CompoundTag();
		
		ListTag breakProgressList = new ListTag();
		breakProgress.forEach((key, value) -> {
			CompoundTag keyValuePairNBT = new CompoundTag();
			keyValuePairNBT.put("BlockPos", NbtUtils.writeBlockPos(key));
			keyValuePairNBT.put("BlockBreakProgressManager", value.write());
			breakProgressList.add(keyValuePairNBT);
		});		
		
		nbt.put("breakProgress", breakProgressList);
		return nbt;
	}

	/**
	 * Reads the data stored in the given {@link CompoundTag}. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 * @param serverLevel the world that is loaded
	 */
	public void read(CompoundTag nbt, ServerLevel serverLevel) {
		try {
			breakProgress.clear();
			ListTag breakProgressList = nbt.getList("breakProgress", 10);
			breakProgressList.forEach(c -> {
				CompoundTag com = (CompoundTag) c;
				BlockPos key = NbtUtils.readBlockPos(com.getCompound("BlockPos"));
				BlockBreakProgressManager value = BlockBreakProgressManager.read(com.getCompound("BlockBreakProgressManager"), serverLevel, key);
				if(value != null) {
					breakProgress.put(key, value);					
				}
			});

			Baseraids.LOGGER.debug("Finished loading GlobalBlockBreakProgressManager");

		} catch (Exception e) {
			Log.warn("Exception while reading data for GlobalBlockBreakProgressManager. Setting parameters to default. Exception: " + e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(breakProgress, level);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GlobalBlockBreakProgressManager other = (GlobalBlockBreakProgressManager) obj;
		return Objects.equals(breakProgress, other.breakProgress) && Objects.equals(level, other.level);
	}
}
