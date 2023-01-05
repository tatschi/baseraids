package may.baseraids;

import java.util.concurrent.ConcurrentHashMap;

import org.jline.utils.Log;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This class remembers which blocks were destroyed during a raid and can restore them at any point.
 * @author Natascha May
 *
 */
public class RestoreDestroyedBlocksManager {

	private World world;
	private RaidManager raidManager;
	private ConcurrentHashMap<BlockPos, BlockState> savedBlocks = new ConcurrentHashMap<BlockPos, BlockState>();
	
	public RestoreDestroyedBlocksManager(RaidManager raidManager, World world) {
		this.raidManager = raidManager;
		this.world = world;
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	/**
	 * Saves all blocks that are destroyed during a raid to
	 * {@link BaseraidsWorldSavedData}.
	 * 
	 * @param event the event of type {@link BlockEvent.BreakEvent} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onBlockDestroyedDuringRaid_saveBlock(final BlockEvent.BreakEvent event) {
		if(event.getWorld().isRemote()) {
			return;
		}
		if(!event.getWorld().equals(world)) {
			return;
		}
		if(!raidManager.isRaidActive()) {
			return;
		}

		savedBlocks.put(event.getPos(), event.getState());
	}
	
	/**
	 * Restores all currently saved blocks.
	 */
	public void restoreSavedBlocks() {
		savedBlocks.forEach((pos, state) -> {
			world.setBlockState(pos, state);
		});
	}
	
	/**
	 * Restores and then clears all currently saved blocks.
	 */
	public void restoreAndClearSavedBlocks() {
		savedBlocks.forEach((pos, state) -> {
			world.setBlockState(pos, state);
		});
		savedBlocks.clear();
	}
	
	/**
	 * Clears all currently saved blocks.
	 */
	public void clearSavedBlocks() {
		savedBlocks.clear();
	}
	
	/**
	 * Saves data relevant for the this class: Writes the necessary data to a
	 * <code>CompoundNBT</code> and returns the <code>CompoundNBT</code> object.
	 * 
	 * @return the adapted <code>CompoundNBT</code> that was written to
	 */
	public CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		
		ListNBT savedBlocksList = new ListNBT();
		savedBlocks.forEach((key, value) -> {
			CompoundNBT keyValuePairNBT = new CompoundNBT();
			keyValuePairNBT.put("BlockPos", NBTUtil.writeBlockPos(key));
			keyValuePairNBT.put("BlockState", NBTUtil.writeBlockState(value));
			savedBlocksList.add(keyValuePairNBT);
		});		
		
		nbt.put("savedBlocks", savedBlocksList);
		return nbt;
	}

	/**
	 * Reads the data stored in the given <code>CompoundNBT</code>. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements. If an exception was thrown during the
	 * reading process (this could very well happen for incompatible versions), the
	 * parameters that were not set are given a default value using
	 * <code>setDefaultWriteParametersIfNotSet()</code>.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 * @param serverWorld the world that is loaded. It is used in the
	 *                    <code>RaidSpawningManager</code> to get references to
	 *                    previously spawned mobs.
	 */
	public void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		try {
			savedBlocks.clear();
			ListNBT savedBlocksList = nbt.getList("savedBlocks", 10);
			savedBlocksList.forEach(c -> {
				CompoundNBT com = (CompoundNBT) c;
				BlockPos key = NBTUtil.readBlockPos(com.getCompound("BlockPos"));
				BlockState value = NBTUtil.readBlockState(com.getCompound("BlockState"));
				savedBlocks.put(key, value);
			});

			Baseraids.LOGGER.debug("Finished loading RestoreDestroyedBlocksManager");

		} catch (Exception e) {
			Log.warn("Exception while reading data for RestoreDestroyedBlocksManager. Setting parameters to default. Exception: " + e);
		}
	}

}
