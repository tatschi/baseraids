package may.baseraids;

import may.baseraids.RaidManager.RaidManagerData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

public class BaseraidsWorldSavedData extends WorldSavedData{

	private static final String DATA_NAME = Baseraids.MODID + "_WorldSavedData";
	
	public BlockPos placedNexusBlockPos;
	public RaidManager raidManager;
	public RaidManagerData raidManagerData;
	
	public boolean isNewWorld;
	
	public BaseraidsWorldSavedData() {
		this(DATA_NAME);
	}
	public BaseraidsWorldSavedData(String name) {
		super(name);
		raidManager = new RaidManager();
		raidManagerData = raidManager.data;
		placedNexusBlockPos = new BlockPos(-1, -1, -1);
		isNewWorld = true;
		raidManager.isInitialized = true;
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
		
		isNewWorld = nbt.getBoolean("isNewWorld");
		this.raidManager.isInitialized = true;
	}
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		nbt.putInt("placedNexusBlockPosX", this.placedNexusBlockPos.getX());
		nbt.putInt("placedNexusBlockPosY", this.placedNexusBlockPos.getY());
		nbt.putInt("placedNexusBlockPosZ", this.placedNexusBlockPos.getZ());
		nbt.put("raidManagerData", raidManagerData.write());
		nbt.putBoolean("isNewWorld", isNewWorld);
		return nbt;
	
	}
	
	
	public void setPlacedNexusBlock(BlockPos pos) {
		this.placedNexusBlockPos = pos;
		this.markDirty();
	}
	
	public void setRaidManagerData(RaidManagerData data) {
		this.raidManagerData = data;
		this.markDirty();
	}
	

	public static BaseraidsWorldSavedData get(ServerWorld world) {
		DimensionSavedDataManager manager = world.getSavedData();
		BaseraidsWorldSavedData worldSavedDataInstance = manager.getOrCreate(() -> new BaseraidsWorldSavedData(), DATA_NAME);
		
		return worldSavedDataInstance;
	}
}
