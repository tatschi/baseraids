package may.baseraids.entities.ai;

import java.util.EnumSet;

import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class MoveTowardsNexusGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	private BlockPos nexusPos;
	private int distanceReached = 15;
	
	public MoveTowardsNexusGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		nexusPos = NexusBlock.getInstance().curBlockPos;
	}
	
	
	public boolean shouldExecute() {
		nexusPos = NexusBlock.getInstance().curBlockPos;
		return entity.getAttackTarget() == null
				&& raidManager.isRaidActive()
				&& entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) >= distanceReached;
	}
	
	public boolean shouldContinueExecuting() {
		nexusPos = NexusBlock.getInstance().curBlockPos;
		return raidManager.isRaidActive()
				&& entity.world instanceof ServerWorld
				&& entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) >= distanceReached;
	}
	
	public void startExecuting() {
		nexusPos = NexusBlock.getInstance().curBlockPos;
	}
	
	public void tick() {		
		entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);		
	}
	
	public void resetTask() {
		entity.getNavigator().clearPath();
	}

	

}
