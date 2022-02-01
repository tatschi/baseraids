package may.baseraids.entities.ai;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.block.Block;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

public class BlockBreakGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	
	
 	protected BlockPos curFocusedBlock;
	private float maxDistance = 2.25f;
 	
 	protected static ConcurrentHashMap<BlockPos, Integer> globalBreakingProgress = new ConcurrentHashMap<BlockPos, Integer>();
 	protected static ConcurrentHashMap<BlockPos, Integer> previousBreakProgress = new ConcurrentHashMap<BlockPos, Integer>();
 	
 	// time to break the block in ticks
 	protected int timeToBreak = 200;
 	
 	
 	final static Vector3i[] focusableBlocksAroundEntity = {
 			
 			new Vector3i(0, 0, 0),
 			new Vector3i(0, 0, 1),
 			new Vector3i(0, 0, -1),
 			
 			new Vector3i(0, 1, 0),
 			new Vector3i(0, 1, 1),
 			new Vector3i(0, 1, -1),
 			
 			new Vector3i(0, -1, 0),
 			new Vector3i(0, -1, 1),
 			new Vector3i(0, -1, -1),
 			
 			new Vector3i(1, 0, 0),
 			new Vector3i(1, 0, 1),
 			new Vector3i(1, 0, -1),
 			
 			new Vector3i(1, 1, 0),
 			new Vector3i(1, 1, 1),
 			new Vector3i(1, 1, -1),
 			
 			new Vector3i(1, -1, 0),
 			new Vector3i(1, -1, 1),
 			new Vector3i(1, -1, -1),
 			
 			new Vector3i(-1, 0, 0),
 			new Vector3i(-1, 0, 1),
 			new Vector3i(-1, 0, -1),
 			
 			new Vector3i(-1, 1, 0),
 			new Vector3i(-1, 1, 1),
 			new Vector3i(-1, 1, -1),
 			
 			new Vector3i(-1, -1, 0),
 			new Vector3i(-1, -1, 1),
 			new Vector3i(-1, -1, -1)
 			
 	};    
	
 	
 	public BlockBreakGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
		curFocusedBlock = new BlockPos(0, 0, 0);
	}
 	
	@Override
	public boolean shouldExecute() {
		if(!raidManager.isRaidActive()) return false;
		
		if(entity.getAIMoveSpeed() > 0) {
			// cycle through possible blocks to destroy around the entity
			BlockPos defaultFocusedBlock = this.entity.getPosition();
			curFocusedBlock = defaultFocusedBlock;
			
			for(Vector3i vec : focusableBlocksAroundEntity){
				curFocusedBlock = curFocusedBlock.add(vec);
				if(entity.world.getBlockState(curFocusedBlock).isSolid()) {
					return true;
				}
				curFocusedBlock = defaultFocusedBlock;
			}
		}
		return false;
		
	}
	
	public void startExecuting() {
		
	}
	
	public void tick() {		
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(curFocusedBlock.getX(), curFocusedBlock.getY(), curFocusedBlock.getZ());
		
		//entity.getNavigator().tryMoveToXYZ(curFocusedBlock.getX(), curFocusedBlock.getY(), curFocusedBlock.getZ(), 1);
		
		// swing arm at random
		if (this.entity.getRNG().nextInt(20) == 0) {
			
			if (!this.entity.isSwingInProgress) {
				this.entity.swingArm(this.entity.getActiveHand());
			}
		}
		Baseraids.LOGGER.info("BlockBreakGoal#tick tick");

		
		// initialize the value if there is none for this block
		globalBreakingProgress.putIfAbsent(curFocusedBlock, 0);
		// increment breaking progress
		globalBreakingProgress.compute(curFocusedBlock, (k, V) -> V+1);
		
		
		// send progress every time i was increased (so every timeToBreak / 10 ticks)
		
		int i = (int)((float)globalBreakingProgress.get(curFocusedBlock) / (float)timeToBreak * 10.0F);		
		if (i != previousBreakProgress.getOrDefault(curFocusedBlock, -1)) {
			Baseraids.LOGGER.info("BlockBreakGoal#tick Send Block break progress");
			if(i % 150 == 0) {
				this.entity.world.playEvent(1019, curFocusedBlock, 0);
			}
			entity.world.sendBlockBreakProgress(entity.getEntityId(), curFocusedBlock, i);

		}
		previousBreakProgress.replace(curFocusedBlock, i);


		synchronized(raidManager) {
			if (globalBreakingProgress.get(curFocusedBlock) == timeToBreak) {
				// break the block after timeToBreak ticks (block should stay though)

				Baseraids.LOGGER.info("BlockBreakGoal#tick Break block");
				globalBreakingProgress.remove(curFocusedBlock);
				this.entity.world.sendBlockBreakProgress(-1, curFocusedBlock, -1);
				entity.world.removeBlock(curFocusedBlock, false);
				// TODO change played sound
				entity.world.playEvent(1021, curFocusedBlock, 0);
				entity.world.playEvent(2001, curFocusedBlock, Block.getStateId(entity.world.getBlockState(curFocusedBlock)));
				entity.getNavigator().clearPath();
			}
		}
		
	}

}
