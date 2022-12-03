package may.baseraids.entities.ai;

import may.baseraids.config.ConfigOptions;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * This class manages all information on breaking a block for a given BlockPos
 * during a raid.
 * 
 * @author Natascha May
 */
public class BlockBreakProgressManager {

	private final World world;

	private final BlockPos pos;

	/**
	 * Absolute break progress is the summed up absolute damage that has been added
	 * to this block
	 */
	private int breakProgressAbsolute;
	/**
	 * Break progress is the progress relative to the
	 * {@link BlockBreakProgressManager#damageUntilBlockBreaks}
	 */
	private int breakProgressRelative;
	/** The absolute damage it takes for the block to break */
	private final int damageUntilBlockBreaks;
	/**
	 * The id that will be passed as breakerId into
	 * {@link World#sendBlockBreakProgress}. Keep distinct ids for each block to
	 * allow the progress to persist even when entities start breaking other blocks.
	 */
	public final int breakBlockId;

	public BlockBreakProgressManager(World world, BlockPos pos, int breakBlockId) {
		this.world = world;
		this.pos = pos;

		breakProgressAbsolute = 0;
		breakProgressRelative = 0;
		this.breakBlockId = breakBlockId;
		damageUntilBlockBreaks = computeDamageToBreakBlock();
	}

	/**
	 * Adds breaking progress to the block.
	 * 
	 * @param damage the amount of damage made by the attack
	 * @return returns true, if the block at {@link BlockPos}  should be removed/broken
	 */
	public synchronized boolean addProgress(int damage) {
		breakProgressAbsolute += damage;
		sendProgressToWorldIfRelativeProgressChanged();
		return shouldBreakBlock();
	}

	/**
	 * Sends the breaking progress to the world if enough damage has been made, i.e.
	 * if the relative progress increased.
	 */
	private synchronized void sendProgressToWorldIfRelativeProgressChanged() {
		int prev = breakProgressRelative;
		// note that this divides two integers which results in a "rounded down" integer
		// and requires the correct order of computation
		breakProgressRelative = breakProgressAbsolute * 10 / damageUntilBlockBreaks;
		if (prev != breakProgressRelative) {
			world.playEvent(1019, pos, 0);
			world.sendBlockBreakProgress(breakBlockId, pos, -1);
			world.sendBlockBreakProgress(breakBlockId, pos, breakProgressRelative);
		}
	}

	/**
	 * Decides whether the block was damaged enough to be broken.
	 * 
	 * @return true, if the block should be removed/broken
	 */
	private synchronized boolean shouldBreakBlock() {
		return breakProgressAbsolute >= damageUntilBlockBreaks;
	}

	/**
	 * Computes the damage that is required to break the block based on a formula
	 * that considers the hardness of the block non-linearly. In the config, a value
	 * can be set that is multiplied to this time to control the difficulty.
	 * 
	 * @return the damage required to break the block
	 */
	private int computeDamageToBreakBlock() {
		if (NexusBlock.getBlockPos().equals(pos)) {
			return 500;
		}
		float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
		return ConfigOptions.monsterBlockBreakingTimeMultiplier.get() * (int) Math.round(
				3 * (hardness + 80 * Math.log10(hardness + 1)) - 60 * Math.exp(-Math.pow(hardness - 2.5, 2) / 6) + 50);
	}

}
