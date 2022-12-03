package may.baseraids.entities.ai;

import java.util.concurrent.ConcurrentHashMap;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
	private World world;

	/**
	 * Holds the {@link BlockBreakProgressManager} for each block that has been
	 * damaged.
	 */
	private ConcurrentHashMap<BlockPos, BlockBreakProgressManager> breakProgress = new ConcurrentHashMap<>();

	public GlobalBlockBreakProgressManager(RaidManager raidManager, World world) {
		this.world = world;
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
				p -> new BlockBreakProgressManager(world, pos, breakProgress.size()));
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

		world.playEvent(1021, pos, 0);
		world.playEvent(2001, pos, Block.getStateId(world.getBlockState(pos)));

		if (NexusBlock.getBlockPos().equals(pos)) {
			raidManager.loseRaid();
			return;
		}
		world.removeBlock(pos, false);
	}

	/**
	 * Resets all progress and parameters that are recorded by this class.
	 */
	public synchronized void resetAllProgress() {
		breakProgress.forEach(0, (k, V) -> world.sendBlockBreakProgress(V.breakBlockId, k, -1));
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
			world.sendBlockBreakProgress(mng.breakBlockId, pos, -1);
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
	public void onBreakBlock_resetAllInfoForThisBlockPos(final BlockEvent.BreakEvent event) {
		if (event.getPlayer().world.isRemote()) {
			return;
		}
		if (!Baseraids.baseraidsData.raidManager.isRaidActive()) {
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
	public void onPlaceBlock_resetAllInfoForThisBlockPos(final BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (!Baseraids.baseraidsData.raidManager.isRaidActive()) {
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
	public void onPlaceFluid_resetAllInfoForThisBlockPos(final BlockEvent.FluidPlaceBlockEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (!Baseraids.baseraidsData.raidManager.isRaidActive()) {
			return;
		}
		resetProgress(event.getPos());
	}
}
