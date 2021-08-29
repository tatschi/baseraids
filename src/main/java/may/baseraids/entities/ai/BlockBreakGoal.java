package may.baseraids.entities.ai;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

public class BlockBreakGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	
	
	protected int previousBreakProgress = -1;
 	protected BlockPos curFocusedBlock;
	private int maxDistance = 5;
 	
 	protected static ConcurrentHashMap<BlockPos, Integer> globalBreakingProgress = new ConcurrentHashMap<BlockPos, Integer>();
 	
 	// time to break the block in ticks
 	protected int timeToBreak;
	
 	
 	public BlockBreakGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.MOVE));
		curFocusedBlock = new BlockPos(0, 0, 0);
	}
 	
	@Override
	public boolean shouldExecute() {
		if(!raidManager.isRaidActive()) return false;
		if(entity.getNavigator().getPath().reachesTarget()) return false;
		
		// search for destroyable block in reach
		BlockPos testPosition = new BlockPos(entity.getPosition());
		testPosition.add(0, 1, 0);
		
		// IDEAS:
		// - check for collision
		// if(entity.collidedHorizontally)
		// - radially check nearest blocks
		
		
		// previous code, to be changed
		/*
		for(int i = 0; i < maxDistance; i++) {
			testPosition.add(x, y, z)
			if(!entity.world.getBlockState(testPosition).isSolid()) continue;
			if(!entity.world.getBlockState(testPosition).canEntityDestroy(entity.world, testPosition, entity)) continue;
		}
		*/
		
		// copy value of block pos
		curFocusedBlock = new BlockPos(testPosition.getX(), testPosition.getY(), testPosition.getZ());
		return true;
	}
	
	public void startExecuting() {
		
	}

}
