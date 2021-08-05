package may.baseraids.entities.ai;

import java.util.EnumSet;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

public class MoveTowardsNexusGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	private Vector3d nexusPos;
	private int distanceReached = 15;
	
	public MoveTowardsNexusGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		nexusPos = new Vector3d(Baseraids.baseraidsData.placedNexusBlockPos.getX(), Baseraids.baseraidsData.placedNexusBlockPos.getY(), Baseraids.baseraidsData.placedNexusBlockPos.getZ());
	}
	
	
	public boolean shouldExecute() {
		nexusPos = new Vector3d(Baseraids.baseraidsData.placedNexusBlockPos.getX(), Baseraids.baseraidsData.placedNexusBlockPos.getY(), Baseraids.baseraidsData.placedNexusBlockPos.getZ());
		return entity.getAttackTarget() == null
				&& raidManager.isRaidActive()
				&& entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) >= distanceReached;
	}
	
	public boolean shouldContinueExecuting() {
		nexusPos = new Vector3d(Baseraids.baseraidsData.placedNexusBlockPos.getX(), Baseraids.baseraidsData.placedNexusBlockPos.getY(), Baseraids.baseraidsData.placedNexusBlockPos.getZ());
		return raidManager.isRaidActive()
				&& entity.world instanceof ServerWorld
				&& entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) >= distanceReached;
	}
	
	public void startExecuting() {
	}
	
	public void tick() {
		nexusPos = new Vector3d(Baseraids.baseraidsData.placedNexusBlockPos.getX(), Baseraids.baseraidsData.placedNexusBlockPos.getY(), Baseraids.baseraidsData.placedNexusBlockPos.getZ());
		
		entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);
		
		
	}
	
	public void resetTask() {
		entity.getNavigator().clearPath();
	}

	

}
