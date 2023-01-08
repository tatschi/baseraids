package may.baseraids;

import java.util.Objects;

import may.baseraids.nexus.NexusBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * This class is the base of saving and loading data for this mod.
 * 
 * @author Natascha May
 */
public class BaseraidsSavedData extends SavedData {

	private static final String DATA_NAME = Baseraids.MODID + "_WorldSavedData";

	public final RaidManager raidManager;
	public final ServerLevel serverLevel;

	/**
	 * Creates a {@link WorldSavedData} object with the name {@link #DATA_NAME},
	 * creates and sets the {@link #raidManager} and {@link #serverLevel}.
	 * 
	 * @param level        the world for which this instance saves data
	 * @param worldManager the {@link WorldManager} that created this instance
	 */
	public BaseraidsSavedData(ServerLevel level, WorldManager worldManager) {
		super(DATA_NAME);
		raidManager = new RaidManager(level, worldManager);
		this.serverLevel = level;
	}

	/**
	 * Reads the data stored in the given {@link CompoundTag} and calls functions
	 * from other classes that will read their custom data. This function assumes
	 * that the nbt was previously written by this class or to be precise, that the
	 * nbt includes certain elements.
	 * 
	 * @param nbt the nbt that will be read out. It is assumed to include certain
	 *            elements.
	 */
	@Override
	public void read(CompoundTag nbt) {
		NexusBlock.read(nbt.getCompound("nexusBlock"));
		this.raidManager.read(nbt.getCompound("raidManager"), serverLevel);
	}

	/**
	 * Writes the necessary data to the given {@link CompoundTag} and calls
	 * functions from other classes that will write their custom data.
	 * 
	 * @param nbt the nbt that will be written to
	 * @return the adapted {@link CompoundTag} that was written to
	 */
	@Override
	public CompoundTag write(CompoundTag nbt) {
		nbt.put("raidManager", raidManager.write());
		nbt.put("nexusBlock", NexusBlock.write());
		return nbt;
	}

	/**
	 * Initiates the loading process that loads the data stored using this class.
	 * <p>
	 * More details on the loading process: An instance of type
	 * {@link DimensionSavedDataManager} is used to controls the loading and saving
	 * process for a particular world. Using the method
	 * {@link ServerLevel#getSavedData()}, this instance is acquired. When the
	 * method
	 * {@link DimensionSavedDataManager#getOrCreate(java.util.function.Supplier, String)}
	 * is called and there is already an instance of
	 * {@link BaseraidsSavedData}, the method
	 * {@link DimensionSavedDataManager#get(java.util.function.Supplier, String)} is
	 * called, in which the private method {@code DimensionSavedDataManager#loadSavedData} is
	 * called. That method loads the data from the file by calling the
	 * {@link WorldSavedData#read(CompoundTag)} and eventually returns the instance
	 * which now contains the loaded data.
	 * 
	 * @param worldManager	the {@link WorldManager} that created this instance
	 * @param world	the world for which data should be loaded
	 * @return an instance of this class which contains the loaded data
	 */
	public static BaseraidsSavedData get(WorldManager worldManager, ServerLevel world) {
		Baseraids.LOGGER.info("collecting baseraidsSavedData");
		DimensionSavedDataManager manager = world.getSavedData();
		return manager.getOrCreate(() -> new BaseraidsSavedData(world, worldManager), DATA_NAME);
	}

	@Override
	public int hashCode() {
		return Objects.hash(raidManager, serverLevel);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseraidsSavedData other = (BaseraidsSavedData) obj;
		return Objects.equals(raidManager, other.raidManager) && Objects.equals(serverLevel, other.serverLevel);
	}

}
