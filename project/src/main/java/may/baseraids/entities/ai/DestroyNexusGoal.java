package may.baseraids.entities.ai;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.block.Block;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class DestroyNexusGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	private BlockPos nexusPos;
	private int minDistanceToNexus;
	
 	protected int previousBreakProgress = -1;
 	
 	protected static AtomicInteger globalBreakingProgress = new AtomicInteger(0);
 	
 	// time to break the block in ticks
 	protected int timeToBreak = 500;
	
	public DestroyNexusGoal(MobEntity entity, RaidManager raidManager, BlockPos nexusPos) {
		this(entity, raidManager, nexusPos, 10);
	}
	
	public DestroyNexusGoal(MobEntity entity, RaidManager raidManager, BlockPos nexusPos, int minDistanceToNexus) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
		this.nexusPos = nexusPos;
		this.minDistanceToNexus = minDistanceToNexus;
	}
	
	
	public boolean shouldExecute() {
		return raidManager.isRaidActive() && entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < minDistanceToNexus;
	}
	
	public boolean shouldContinueExecuting() {
		return raidManager.isRaidActive() && entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < minDistanceToNexus && entity.world instanceof ServerWorld;
	}
	
	public void startExecuting() {
		
	}
	
	public void tick() {
		globalBreakingProgress.getAndIncrement();
		
		
		// send progress every time i was increased (so every timeToBreak / 10 ticks)
		int i = (int)((float)globalBreakingProgress.get() / (float)timeToBreak * 10.0F);		
		if (i != previousBreakProgress) {
			Baseraids.LOGGER.info("Send Block break progress");
			
			entity.world.sendBlockBreakProgress(entity.getEntityId(), nexusPos, i);
		}
		previousBreakProgress = i;
		
		
		synchronized(raidManager) {
			if (globalBreakingProgress.get() == timeToBreak) {
				// break the block after timeToBreak ticks (block should stay though)
				
				Baseraids.LOGGER.info("Break block");
				globalBreakingProgress.set(0);
				this.entity.world.sendBlockBreakProgress(-1, nexusPos, -1);
				//entity.world.removeBlock(nexusPos, false);
				entity.world.playEvent(1021, nexusPos, 0);
				entity.world.playEvent(2001, nexusPos, Block.getStateId(entity.world.getBlockState(nexusPos)));
				// trigger raid end
				raidManager.loseRaid();
			}
		}
		
		
	}
	
	public void resetTask() {
		
	}
	
}


