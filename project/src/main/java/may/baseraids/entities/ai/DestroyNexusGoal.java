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
	private int distanceToTriggerGoal = 15;
	private int distanceToAllowBreaking = 2;
	
 	protected static AtomicInteger previousBreakProgress = new AtomicInteger(-1);
 	
 	protected static AtomicInteger globalBreakingProgress = new AtomicInteger(0);
 	
 	// time to break the block in ticks
 	protected int timeToBreak = 500;
	
	
	public DestroyNexusGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.MOVE));
		nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
	}
	
	
	public boolean shouldExecute() {
		return raidManager.isRaidActive() && entity.isAlive() && entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < distanceToTriggerGoal;
	}
	
	public boolean shouldContinueExecuting() {
		return raidManager.isRaidActive() && entity.isAlive() && entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < distanceToTriggerGoal && entity.world instanceof ServerWorld;
	}
	
	public void startExecuting() {
		
	}
	
	public void tick() {
		nexusPos = Baseraids.baseraidsData.placedNexusBlockPos;
		
		
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());
		
		entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);
		
		
		
		if(entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < distanceToAllowBreaking) {
			globalBreakingProgress.getAndIncrement();
			entity.swingArm(entity.getActiveHand());
			// send progress every time i was increased (so every timeToBreak / 10 ticks)
			int i = (int)((float)globalBreakingProgress.get() / (float)timeToBreak * 10.0F);		
			if (i != previousBreakProgress.get()) {
				Baseraids.LOGGER.info("Send Block break progress");
				
				entity.world.sendBlockBreakProgress(entity.getEntityId(), nexusPos, i);
				
			}
			previousBreakProgress.set(i);
			
			
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
		
		
		
		
	}
	
	public void resetTask() {
		entity.setAggroed(false);
		entity.getNavigator().clearPath();
		if(!raidManager.isRaidActive()) {
			synchronized(raidManager) {
				this.entity.world.sendBlockBreakProgress(-1, nexusPos, -1);
			}
			previousBreakProgress.set(-1);
		}
	}
	
}


