package may.baseraids.entities.ai;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;

public class BlockBreakGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	
	
 	protected BlockPos curFocusedBlock;
	private float maxDistance = 2.25f;
 	
 	protected static ConcurrentHashMap<BlockPos, Integer> globalBreakingProgress = new ConcurrentHashMap<BlockPos, Integer>();
 	protected static ConcurrentHashMap<BlockPos, Integer> previousBreakProgress = new ConcurrentHashMap<BlockPos, Integer>();
 	
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
		Path path = entity.getNavigator().getPath();
		if(path == null) return false;
		if(path.reachesTarget()) return false;
		if(path.isFinished()) return false;
		
		if(!this.entity.collidedHorizontally) return false;
		
		// !! copied and adjusted from InteractDoorGoal
		for(int i = 0; i < Math.min(path.getCurrentPathIndex() + 2, path.getCurrentPathLength()); ++i) {
			PathPoint pathpoint = path.getPathPointFromIndex(i);
	        curFocusedBlock = new BlockPos(pathpoint.x, pathpoint.y + 1, pathpoint.z);	
	        if (!(this.entity.getDistanceSq((double)curFocusedBlock.getX(), this.entity.getPosY(), (double)curFocusedBlock.getZ()) > maxDistance)) {
	        	return entity.world.getBlockState(curFocusedBlock).isSolid();
            }
		}
		
		curFocusedBlock = this.entity.getPosition().up();
		return entity.world.getBlockState(curFocusedBlock).isSolid();
		
		
		
		// previous code, to be changed
		/*
		for(int i = 0; i < maxDistance; i++) {
			testPosition.add(x, y, z)
			if(!entity.world.getBlockState(testPosition).isSolid()) continue;
			if(!entity.world.getBlockState(testPosition).canEntityDestroy(entity.world, testPosition, entity)) continue;
		}
		*/
		
		// copy value of block pos
		//curFocusedBlock = new BlockPos(testPosition.getX(), testPosition.getY(), testPosition.getZ());
	}
	
	public void startExecuting() {
		
	}
	
	public void tick() {		
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(curFocusedBlock.getX(), curFocusedBlock.getY(), curFocusedBlock.getZ());
		
		entity.getNavigator().tryMoveToXYZ(curFocusedBlock.getX(), curFocusedBlock.getY(), curFocusedBlock.getZ(), 1);
		entity.swingArm(entity.getActiveHand());
		

		
		// initialize the value if there is none for this block
		globalBreakingProgress.putIfAbsent(curFocusedBlock, 0);
		// increment breaking progress
		globalBreakingProgress.compute(curFocusedBlock, (k, V) -> V+1);
		
		
		// send progress every time i was increased (so every timeToBreak / 10 ticks)
		int i = (int)((float)globalBreakingProgress.get(curFocusedBlock) / (float)timeToBreak * 10.0F);		
		if (i != previousBreakProgress.get(curFocusedBlock)) {
			Baseraids.LOGGER.info("Send Block break progress");

			entity.world.sendBlockBreakProgress(entity.getEntityId(), curFocusedBlock, i);

		}
		previousBreakProgress.replace(curFocusedBlock, i);


		synchronized(raidManager) {
			if (globalBreakingProgress.get(curFocusedBlock) == timeToBreak) {
				// break the block after timeToBreak ticks (block should stay though)

				Baseraids.LOGGER.info("Break block");
				globalBreakingProgress.remove(curFocusedBlock);
				this.entity.world.sendBlockBreakProgress(-1, curFocusedBlock, -1);
				entity.world.removeBlock(curFocusedBlock, false);
				// TODO change played sound
				entity.world.playEvent(1021, curFocusedBlock, 0);
				entity.world.playEvent(2001, curFocusedBlock, Block.getStateId(entity.world.getBlockState(curFocusedBlock)));
				// trigger raid end
				raidManager.loseRaid();
			}
		}
		
	}

}
