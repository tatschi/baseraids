package may.baseraids;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

public class BaseraidsWorldSavedData extends WorldSavedData{

	private static final String DATA_NAME = Baseraids.MODID + "_WorldSavedData";
	
	public NexusBlock nexusBlock;
	public RaidManager raidManager;
	public ServerWorld serverWorld;
	
	public BaseraidsWorldSavedData(ServerWorld world) {
		this(DATA_NAME);
		this.serverWorld = world;
	}
	private BaseraidsWorldSavedData(String name) {
		super(name);
		raidManager = new RaidManager();
		raidManager.isInitialized = true;
		nexusBlock = NexusBlock.getInstance();
	}
	
	@Override
	public void read(CompoundNBT nbt) {
		
		this.nexusBlock.readAdditional(nbt.getCompound("nexusBlock"));
		this.raidManager.readAdditional(nbt.getCompound("raidManager"), serverWorld);
		
		this.raidManager.isInitialized = true;
	}
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		nbt.put("raidManager", raidManager.writeAdditional());
		nbt.put("nexusBlock", nexusBlock.writeAdditional());
		return nbt;
	
	}
	

	public static BaseraidsWorldSavedData get(ServerWorld world) {
		DimensionSavedDataManager manager = world.getSavedData();
		BaseraidsWorldSavedData worldSavedDataInstance = manager.getOrCreate(() -> new BaseraidsWorldSavedData(world), DATA_NAME);
		
		return worldSavedDataInstance;
	}
}
