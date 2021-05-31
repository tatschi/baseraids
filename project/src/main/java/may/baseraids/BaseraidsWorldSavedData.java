package may.baseraids;

import java.util.Vector;

import may.baseraids.RaidManager.RaidManagerData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

public class BaseraidsWorldSavedData extends WorldSavedData{

	private static final String DATA_NAME = Baseraids.MODID + "_WorldSavedData";
	
	public BlockPos placedNexusBlockPos = new BlockPos(-1, -1, -1);
	public RaidManager raidManager = new RaidManager();
	public RaidManagerData raidManagerData = raidManager.data;
	
	
	public BaseraidsWorldSavedData() {
		super(DATA_NAME);
		Baseraids.LOGGER.info("LOGID:SAVEDDATA Constructing a BaseraidsWorldSavedData");
	}
	public BaseraidsWorldSavedData(String name) {
		super(name);
		Baseraids.LOGGER.info("LOGID:SAVEDDATA Constructing a BaseraidsWorldSavedData");
	}
	
	@Override
	public void read(CompoundNBT nbt) {
		
		this.placedNexusBlockPos = new BlockPos(
				nbt.getInt("placedNexusBlockPosX"),
				nbt.getInt("placedNexusBlockPosY"),
				nbt.getInt("placedNexusBlockPosZ")
				);
		this.raidManagerData = RaidManagerData.read(nbt.getCompound("raidManagerData"));
		raidManager.data = this.raidManagerData;
		Baseraids.LOGGER.info("Initialized raidManager");
		this.raidManager.isInitialized = true;
	}
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		nbt.putInt("placedNexusBlockPosX", this.placedNexusBlockPos.getX());
		nbt.putInt("placedNexusBlockPosY", this.placedNexusBlockPos.getY());
		nbt.putInt("placedNexusBlockPosZ", this.placedNexusBlockPos.getZ());
		nbt.put("raidManagerData", raidManagerData.write());
		return nbt;
	
	}
	
	
	public void setPlacedNexusBlock(BlockPos pos) {
		this.placedNexusBlockPos = pos;
		this.markDirty();
	}
	
	public void setRaidManagerData(RaidManagerData data) {
		this.raidManagerData = data;
		//Baseraids.LOGGER.info("BaseraidsWorldSavedData Set time since raid to :" + data.timeSinceRaid);
		this.markDirty();
	}
	

	public static BaseraidsWorldSavedData get(ServerWorld world) {
		//Baseraids.LOGGER.info("LOGID:SAVEDDATA get BaseraidsWorldSavedData");
		DimensionSavedDataManager manager = world.getSavedData();
		BaseraidsWorldSavedData worldSavedDataInstance = manager.getOrCreate(() -> new BaseraidsWorldSavedData(), DATA_NAME);
		
		return worldSavedDataInstance;
	}
}
