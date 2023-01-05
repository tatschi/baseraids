package may.baseraids;

import may.baseraids.nexus.NexusBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Mod;

/**
 * This class is the base of saving and loading data for this mod.
 * 
 * @author Natascha May
 */
//@Mod.EventBusSubscriber annotation automatically registers STATIC event handlers 
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BaseraidsWorldSavedData extends WorldSavedData {

	private static final String DATA_NAME = Baseraids.MODID + "_WorldSavedData";

	public RaidManager raidManager;
	public ServerWorld serverWorld;

	/**
	 * Creates a <code>WorldSavedData</code> object with the name in
	 * <code>DATA_NAME</code>, creates and sets a <code>RaidManager</code> and sets
	 * the world for which <code>this</code> saves data.
	 * 
	 * @param world the world for which <code>this</code> saves data
	 */
	public BaseraidsWorldSavedData(ServerWorld world, WorldManager worldManager) {
		super(DATA_NAME);
		raidManager = new RaidManager(world, worldManager);
		this.serverWorld = world;
	}

	/**
	 * Reads the data stored in the given <code>CompoundNBT</code> and calls
	 * functions from other classes that will read their custom data. This function
	 * assumes that the nbt was previously written by this class or to be precise,
	 * that the nbt includes certain elements.
	 * 
	 * @param nbt the nbt that will be read out. It is assumed to include certain
	 *            elements.
	 */
	@Override
	public void read(CompoundNBT nbt) {
		NexusBlock.readAdditional(nbt.getCompound("nexusBlock"));
		this.raidManager.readAdditional(nbt.getCompound("raidManager"), serverWorld);
	}

	/**
	 * Writes the necessary data to the given <code>CompoundNBT</code> and calls
	 * functions from other classes that will write their custom data.
	 * 
	 * @param nbt the nbt that will be written to
	 * @return the adapted CompoundNBT that was written to
	 */
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		nbt.put("raidManager", raidManager.writeAdditional());
		nbt.put("nexusBlock", NexusBlock.writeAdditional());
		return nbt;
	}

	/**
	 * Initiates the loading process that loads the data stored using this class.
	 * <p>
	 * More details on the loading process: An instance of type
	 * <code>DimensionSavedDataManager</code> is used to controls the loading and
	 * saving process for a particular world. Using the method
	 * <code>World#getSavedData()</code>, this instance is acquired. When the method
	 * <code>DimensionSavedDataManager#getOrCreate</code> is called and there is
	 * already an instance of <code>BaseraidsWorldSavedData</code>, the
	 * <code>DimensionSavedDataManager#get</code> method is called, in which
	 * <code>DimensionSavedDataManager#loadSavedData</code> is called. That method
	 * loads the data from the file by calling the
	 * <code>BaseraidsWorldSavedData#read</code> and eventually returns the instance
	 * which now contains the loaded data.
	 * 
	 * @param world the world for which data should be loaded
	 * @return an instance of this class which contains the loaded data
	 */
	public static BaseraidsWorldSavedData get(WorldManager worldManager, ServerWorld world) {
		Baseraids.LOGGER.info("collecting baseraidsSavedData");
		DimensionSavedDataManager manager = world.getSavedData();
		BaseraidsWorldSavedData worldSavedDataInstance = manager
				.getOrCreate(() -> new BaseraidsWorldSavedData(world, worldManager), DATA_NAME);
		return worldSavedDataInstance;
	}

}
