package may.baseraids.entities.ai;

import org.jline.utils.Log;

import may.baseraids.config.ConfigOptions;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

/**
 * This class manages all information on breaking a block for a given BlockPos
 * during a raid.
 * 
 * @author Natascha May
 */
public class BlockBreakProgressManager {

	private final Level level;

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

	public BlockBreakProgressManager(Level world, BlockPos pos, int breakBlockId) {
		this.level = world;
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
			SoundType soundtype = level.getBlockState(pos).getSoundType(level, pos, null);
			Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F, pos));
			
			level.destroyBlockProgress(breakBlockId, pos, -1);
			level.destroyBlockProgress(breakBlockId, pos, breakProgressRelative);
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
		float hardness = level.getBlockState(pos).getBlock().defaultDestroyTime();
		return ConfigOptions.getMonsterBlockBreakingTimeMultiplier() * (int) Math.round(
				3 * (hardness + 80 * Math.log10(hardness + 1)) - 60 * Math.exp(-Math.pow(hardness - 2.5, 2) / 6) + 50);
	}

	/**
	 * Saves data relevant for the this class: Writes the necessary data to a
	 * {@link CompoundTag} and returns the {@link CompoundTag} object.
	 * 
	 * @return the adapted {@link CompoundTag} that was written to
	 */
	public CompoundTag write() {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("breakProgressAbsolute", breakProgressAbsolute);
		nbt.putInt("breakProgressRelative", breakProgressRelative);
		nbt.putInt("breakBlockId", breakBlockId);		
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
	public static BlockBreakProgressManager read(CompoundTag nbt, ServerLevel serverLevel, BlockPos pos) {
		try {
			int breakBlockId = nbt.getInt("breakBlockId");
			BlockBreakProgressManager mng = new BlockBreakProgressManager(serverLevel, pos, breakBlockId);
			mng.breakProgressAbsolute = nbt.getInt("breakProgressAbsolute");
			mng.breakProgressRelative = nbt.getInt("breakProgressRelative");
			return mng;

		} catch (Exception e) {
			Log.warn("Exception while reading data for BlockBreakProgressManager. Setting parameters to default. Exception: " + e);
			return null;
		}
	}
}
