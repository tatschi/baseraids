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
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BlockBreakProgressManager {

	private RaidManager raidManager;
	private World world;

	/** Absolute break progress is the summed up absolute damage that has been added to each block */
	public ConcurrentHashMap<BlockPos, Integer> breakProgressAbsolute = new ConcurrentHashMap<BlockPos, Integer>();
	/** Break progress relative to the timeToBreak of each block */
	public ConcurrentHashMap<BlockPos, Integer> breakProgressRelative = new ConcurrentHashMap<BlockPos, Integer>();
	/** The absolute damage it takes each block to break */
	public ConcurrentHashMap<BlockPos, Integer> damageUntilBlockBreaks = new ConcurrentHashMap<BlockPos, Integer>();
	/** The id that will be passed as breakerId into world#sendBlockBreakProgress. Keep distinct ids for each block to allow the progress to persist even when entities start breaking other blocks */
	public ConcurrentHashMap<BlockPos, Integer> breakBlockId = new ConcurrentHashMap<BlockPos, Integer>();

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
		breakProgressAbsolute.putIfAbsent(pos, 0);
		breakProgressAbsolute.compute(pos, (k, V) -> V + damage);
		
		int damageUntilBlockBreaks = this.damageUntilBlockBreaks.computeIfAbsent(pos, (p) -> computeDamageToBreakBlock(p));
		int breakBlockId = this.breakBlockId.computeIfAbsent(pos, p -> breakProgressAbsolute.size());
		sendProgressToWorldIfRelativeProgressChanged(breakBlockId, pos, damageUntilBlockBreaks);
		return breakBlockIfEnoughDamageMade(pos, damageUntilBlockBreaks);

	}

	private synchronized void sendProgressToWorldIfRelativeProgressChanged(int breakBlockId, BlockPos pos, int damageUntilBlockBreaks) {
		int prev = breakProgressRelative.computeIfAbsent(pos, p -> 0);
		// note that this divides two integers which results in a "rounded down" integer and requires the correct order of computation
		int cur = breakProgressRelative.compute(pos, (k, V) -> breakProgressAbsolute.get(k) * 10 / damageUntilBlockBreaks);		
		if(prev != cur) {
			// TODO sound design
			world.playEvent(1019, pos, 0);
			world.sendBlockBreakProgress(breakBlockId, pos, -1);
			world.sendBlockBreakProgress(breakBlockId, pos, cur);
		}
	}

	private synchronized boolean breakBlockIfEnoughDamageMade(BlockPos pos, int damageUntilBlockBreaks) {
		if (breakProgressAbsolute.get(pos) >= damageUntilBlockBreaks) {
			breakBlock(pos);
			return true;
		}
		return false;
	}
	
	private synchronized void breakBlock(BlockPos pos) {
		Baseraids.LOGGER.debug("BlockBreakProgressManager#breakBlock");
		world.sendBlockBreakProgress(breakBlockId.get(pos), pos, -1);
		breakProgressAbsolute.remove(pos);
		breakProgressRelative.remove(pos);
		damageUntilBlockBreaks.remove(pos);
		breakBlockId.remove(pos);
		
		// TODO sound design
		world.playEvent(1021, pos, 0);
		world.playEvent(2001, pos, Block.getStateId(world.getBlockState(pos)));
		
		
		if(NexusBlock.getBlockPos().equals(pos)) {
			raidManager.loseRaid();
			return;
		}
		world.removeBlock(pos, false);		
	}

	private int computeDamageToBreakBlock(BlockPos pos) {
		if(NexusBlock.getBlockPos().equals(pos)) {
			return 500;
		}
		float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
		return ConfigOptions.monsterBlockBreakingTimeMultiplier.get() * (int) Math.round(
				3 * (hardness + 80 * Math.log10(hardness + 1)) - 60 * Math.exp(-Math.pow(hardness - 2.5, 2) / 6) + 50);
	}
	
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
		if(!Baseraids.baseraidsData.raidManager.isRaidActive()) {
			return;
		}
		breakProgressAbsolute.remove(event.getPos());
		breakProgressRelative.remove(event.getPos());
		damageUntilBlockBreaks.remove(event.getPos());
		breakBlockId.remove(event.getPos());
	}
}
