package may.baseraids.entities.ai;

import java.util.concurrent.ConcurrentHashMap;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import may.baseraids.config.ConfigOptions;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This class collects and manages all information on breaking blocks during a raid.
 * 
 * @author Natascha May
 */
public class BlockBreakProgressManager {

	private RaidManager raidManager;
	private World world;

	/**
	 * Absolute break progress is the summed up absolute damage that has been added
	 * to each block
	 */
	public ConcurrentHashMap<BlockPos, Integer> breakProgressAbsolute = new ConcurrentHashMap<BlockPos, Integer>();
	/** Break progress relative to the timeToBreak of each block */
	public ConcurrentHashMap<BlockPos, Integer> breakProgressRelative = new ConcurrentHashMap<BlockPos, Integer>();
	/** The absolute damage it takes each block to break */
	public ConcurrentHashMap<BlockPos, Integer> damageUntilBlockBreaks = new ConcurrentHashMap<BlockPos, Integer>();
	/**
	 * The id that will be passed as breakerId into world#sendBlockBreakProgress.
	 * Keep distinct ids for each block to allow the progress to persist even when
	 * entities start breaking other blocks
	 */
	public ConcurrentHashMap<BlockPos, Integer> breakBlockId = new ConcurrentHashMap<BlockPos, Integer>();

	public BlockBreakProgressManager(RaidManager raidManager, World world) {
		this.world = world;
		this.raidManager = raidManager;
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * Adds breaking progress to the given block.
	 * 
	 * @param pos    the position of the block that the progress is added to
	 * @param damage the amount of damage made by the attack
	 * @return returns true if the block at BlockPos was removed/broken
	 */
	public boolean addProgress(BlockPos pos, int damage) {
		breakProgressAbsolute.putIfAbsent(pos, 0);
		breakProgressAbsolute.compute(pos, (k, V) -> V + damage);

		int damageUntilBlockBreaks = this.damageUntilBlockBreaks.computeIfAbsent(pos,
				(p) -> computeDamageToBreakBlock(p));
		int breakBlockId = this.breakBlockId.computeIfAbsent(pos, p -> breakProgressAbsolute.size());
		sendProgressToWorldIfRelativeProgressChanged(breakBlockId, pos, damageUntilBlockBreaks);
		return breakBlockIfEnoughDamageMade(pos, damageUntilBlockBreaks);
	}

	/**
	 * Sends the breaking progress to the world if enough damage has been made, i.e.
	 * if the relative progress increased.
	 * 
	 * @param breakBlockId           the Id of the block in the context of this
	 *                               class, @see breakBlockId
	 * @param pos                    the position of the block
	 * @param damageUntilBlockBreaks the damage that is required to break the block
	 */
	private synchronized void sendProgressToWorldIfRelativeProgressChanged(int breakBlockId, BlockPos pos,
			int damageUntilBlockBreaks) {
		int prev = breakProgressRelative.computeIfAbsent(pos, p -> 0);
		// note that this divides two integers which results in a "rounded down" integer
		// and requires the correct order of computation
		int cur = breakProgressRelative.compute(pos,
				(k, V) -> breakProgressAbsolute.get(k) * 10 / damageUntilBlockBreaks);
		if (prev != cur) {
			world.playEvent(1019, pos, 0);
			world.sendBlockBreakProgress(breakBlockId, pos, -1);
			world.sendBlockBreakProgress(breakBlockId, pos, cur);
		}
	}

	/**
	 * Breaks the block if enough damage has been made.
	 * 
	 * @param pos                    the position of the block
	 * @param damageUntilBlockBreaks the damage that is required to break the block
	 * @return true, if the block was removed/broken
	 */
	private synchronized boolean breakBlockIfEnoughDamageMade(BlockPos pos, int damageUntilBlockBreaks) {
		if (breakProgressAbsolute.get(pos) >= damageUntilBlockBreaks) {
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
		Baseraids.LOGGER.debug("BlockBreakProgressManager#breakBlock");
		world.sendBlockBreakProgress(breakBlockId.get(pos), pos, -1);
		breakProgressAbsolute.remove(pos);
		breakProgressRelative.remove(pos);
		damageUntilBlockBreaks.remove(pos);
		breakBlockId.remove(pos);

		world.playEvent(1021, pos, 0);
		world.playEvent(2001, pos, Block.getStateId(world.getBlockState(pos)));

		if (NexusBlock.getBlockPos().equals(pos)) {
			raidManager.loseRaid();
			return;
		}
		world.removeBlock(pos, false);
	}

	/**
	 * Computes the damage that is required to break a block based on a formula that
	 * considers the hardness of the block non-linearly.
	 * A value can be set in the config that is multiplied to this time to control the difficulty.
	 * @param pos the position of the block
	 * @return the damage required to break the block
	 */
	private int computeDamageToBreakBlock(BlockPos pos) {
		if (NexusBlock.getBlockPos().equals(pos)) {
			return 500;
		}
		float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
		return ConfigOptions.monsterBlockBreakingTimeMultiplier.get() * (int) Math.round(
				3 * (hardness + 80 * Math.log10(hardness + 1)) - 60 * Math.exp(-Math.pow(hardness - 2.5, 2) / 6) + 50);
	}

	/**
	 * Resets all progress and parameters that are recorded by this class.
	 */
	public void resetAllProgress() {
		breakProgressAbsolute.forEachKey(0, pos -> world.sendBlockBreakProgress(breakBlockId.get(pos), pos, -1));
		breakProgressAbsolute.clear();
		breakProgressRelative.clear();
		damageUntilBlockBreaks.clear();
		breakBlockId.clear();
	}

	/**
	 * Attempts to directly give the nexus to the player that broke the block, when
	 * a nexus block is broken. If it is not successful or the event happens during
	 * a raid, the breaking event is cancelled.
	 * 
	 * @param event the event of type <code>BlockEvent.BreakEvent</code> that
	 *              triggers this method
	 */
	@SubscribeEvent
	public void onBreakBlock_resetAllInfoForThisBlockPos(final BlockEvent.BreakEvent event) {
		if (event.getPlayer().world.isRemote()) {
			return;
		}
		if (!Baseraids.baseraidsData.raidManager.isRaidActive()) {
			return;
		}
		breakProgressAbsolute.remove(event.getPos());
		breakProgressRelative.remove(event.getPos());
		damageUntilBlockBreaks.remove(event.getPos());
		breakBlockId.remove(event.getPos());
	}

}
