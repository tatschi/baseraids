package may.baseraids;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jline.utils.Log;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

	private Level level;
	private RaidManager raidManager;
	private ConcurrentHashMap<BlockPos, BlockState> savedBlocks = new ConcurrentHashMap<>();

	public RestoreDestroyedBlocksManager(RaidManager raidManager, Level world) {
		this.raidManager = raidManager;
		this.level = world;
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * Saves all blocks that are destroyed during a raid to
	 * {@link BaseraidsSavedData}.
	 * 
	 * @param event the event of type {@link BlockEvent.BreakEvent} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onBlockDestroyedDuringRaidSaveBlock(final BlockEvent.BreakEvent event) {
		if (event.getWorld().isClientSide()) {
			return;
		}
		if (!event.getWorld().equals(level)) {
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
		savedBlocks.forEach((pos, state) -> level.setBlockAndUpdate(pos, state));
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
	 * {@link CompoundTag} and returns the {@link CompoundTag} object.
	 * 
	 * @return the adapted {@link CompoundTag} that was written to
	 */
	public CompoundTag write() {
		CompoundTag nbt = new CompoundTag();

		ListTag savedBlocksList = new ListTag();
		savedBlocks.forEach((key, value) -> {
			CompoundTag keyValuePairNBT = new CompoundTag();
			keyValuePairNBT.put("BlockPos", NbtUtils.writeBlockPos(key));
			keyValuePairNBT.put("BlockState", NbtUtils.writeBlockState(value));
			savedBlocksList.add(keyValuePairNBT);
		});

		nbt.put("savedBlocks", savedBlocksList);
		return nbt;
	}

	/**
	 * Reads the data stored in the given {@link CompoundTag}. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 */
	public void read(CompoundTag nbt) {
		try {
			savedBlocks.clear();
			ListTag savedBlocksList = nbt.getList("savedBlocks", 10);
			savedBlocksList.forEach(c -> {
				CompoundTag com = (CompoundTag) c;
				BlockPos key = NbtUtils.readBlockPos(com.getCompound("BlockPos"));
				BlockState value = NbtUtils.readBlockState(com.getCompound("BlockState"));
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
		return Objects.hash(savedBlocks, level);
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
		return Objects.equals(savedBlocks, other.savedBlocks) && Objects.equals(level, other.level);
	}

}
