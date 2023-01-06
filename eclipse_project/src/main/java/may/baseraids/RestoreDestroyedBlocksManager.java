package may.baseraids;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jline.utils.Log;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This class remembers which blocks were destroyed during a raid and can
 * restore them at any point.
 * 
 * @author Natascha May
 *
 */
public class RestoreDestroyedBlocksManager {

	private World world;
	private RaidManager raidManager;
	private ConcurrentHashMap<BlockPos, BlockState> savedBlocks = new ConcurrentHashMap<>();

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
	public void onBlockDestroyedDuringRaidSaveBlock(final BlockEvent.BreakEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (!event.getWorld().equals(world)) {
			return;
		}
		if (!raidManager.isRaidActive()) {
			return;
		}

		savedBlocks.put(event.getPos(), event.getState());
	}

	/**
	 * Restores all currently saved blocks.
	 */
	public void restoreSavedBlocks() {
		savedBlocks.forEach((pos, state) -> world.setBlockState(pos, state));
	}

	/**
	 * Restores and then clears all currently saved blocks.
	 */
	public void restoreAndClearSavedBlocks() {
		restoreSavedBlocks();
		clearSavedBlocks();
	}

	/**
	 * Clears all currently saved blocks.
	 */
	public void clearSavedBlocks() {
		savedBlocks.clear();
	}

	/**
	 * Saves data relevant for the this class: Writes the necessary data to a
	 * {@link CompoundNBT} and returns the {@link CompoundNBT} object.
	 * 
	 * @return the adapted {@link CompoundNBT} that was written to
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
	 * Reads the data stored in the given {@link CompoundNBT}. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 */
	public void readAdditional(CompoundNBT nbt) {
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
			Log.warn(
					"Exception while reading data for RestoreDestroyedBlocksManager. Setting parameters to default. Exception: "
							+ e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(savedBlocks, world);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestoreDestroyedBlocksManager other = (RestoreDestroyedBlocksManager) obj;
		return Objects.equals(savedBlocks, other.savedBlocks) && Objects.equals(world, other.world);
	}

}
