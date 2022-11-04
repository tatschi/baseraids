package may.baseraids.entities.ai;

import java.util.concurrent.ConcurrentHashMap;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import may.baseraids.config.ConfigOptions;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBreakProgressManager {

	private RaidManager raidManager;
	private World world;

	public ConcurrentHashMap<BlockPos, Integer> globalBreakingProgress = new ConcurrentHashMap<BlockPos, Integer>();
	public ConcurrentHashMap<BlockPos, Integer> previousBreakProgress = new ConcurrentHashMap<BlockPos, Integer>();

	public BlockBreakProgressManager(RaidManager raidManager, World world) {
		this.world = world;
		this.raidManager = raidManager;
	}

	/**
	 * 
	 * @param breakSource
	 * @param pos
	 * @return returns true if the block at BlockPos was removed/broken
	 */
	public boolean addProgress(Entity breakSource, BlockPos pos, int damage) {
		globalBreakingProgress.putIfAbsent(pos, 0);
		globalBreakingProgress.compute(pos, (k, V) -> V + damage);
		
		int timeToBreak = timeToBreakBlock(breakSource, pos);
		sendProgressToWorldAfterEnoughProgress(breakSource, pos, timeToBreak);
		return breakBlockAfterEnoughProgress(pos, timeToBreak);

	}

	private void sendProgressToWorldAfterEnoughProgress(Entity breakSource, BlockPos pos, int timeToBreak) {
		// send progress every time i was increased (so every timeToBreak / 10 progress steps)
		int i = (int) ((float) globalBreakingProgress.get(pos) / (float) timeToBreak * 10.0F);
		if (i != previousBreakProgress.getOrDefault(pos, -1)) {
			// TODO sound design
			world.sendBlockBreakProgress(breakSource.getEntityId(), pos, i);
		}
		previousBreakProgress.replace(pos, i);
	}

	private synchronized boolean breakBlockAfterEnoughProgress(BlockPos pos, int timeToBreak) {
		if (globalBreakingProgress.get(pos) >= timeToBreak) {
			breakBlock(pos);
			return true;
		}
		return false;
	}
	
	private synchronized void breakBlock(BlockPos pos) {
		Baseraids.LOGGER.info("BlockBreakGoal#tick Break block");
		globalBreakingProgress.remove(pos);
		world.sendBlockBreakProgress(-1, pos, -1);		
		
		// TODO sound design
		world.playEvent(1021, pos, 0);
		world.playEvent(2001, pos, Block.getStateId(world.getBlockState(pos)));
		
		
		if(NexusBlock.getBlockPos().equals(pos)) {
			raidManager.loseRaid();
			return;
		}
		world.removeBlock(pos, false);		
	}

	private int timeToBreakBlock(Entity breakSource, BlockPos pos) {
		if(NexusBlock.getBlockPos().equals(pos)) {
			return 500;
		}
		float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
		return ConfigOptions.monsterBlockBreakingTimeMultiplier.get() * (int) Math.round(
				3 * (hardness + 80 * Math.log10(hardness + 1)) - 60 * Math.exp(-Math.pow(hardness - 2.5, 2) / 6) + 50);
	}
	
	public void resetAllProgress() {
		globalBreakingProgress.forEachKey(0, pos -> world.sendBlockBreakProgress(-1, pos, -1));
		globalBreakingProgress.clear();
		previousBreakProgress.clear();
	}
}
