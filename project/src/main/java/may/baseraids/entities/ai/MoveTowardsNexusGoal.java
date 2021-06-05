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
	
	public MoveTowardsNexusGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		
	}
	
	
	public boolean shouldExecute() {
		return entity.getAttackTarget() == null && raidManager.data.isRaidActive();
	}
	
	public boolean shouldContinueExecuting() {
		return raidManager.data.isRaidActive() && entity.world instanceof ServerWorld;
	}
	
	public void startExecuting() {
	}
	
	public void tick() {
		if (!entity.getNavigator().hasPath()) {
			Vector3d nexusPos = new Vector3d(Baseraids.baseraidsData.placedNexusBlockPos.getX(), Baseraids.baseraidsData.placedNexusBlockPos.getY(), Baseraids.baseraidsData.placedNexusBlockPos.getZ());
			entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1.0D);
		}
		
	}
	
	public void resetTask() {
		entity.getNavigator().clearPath();
	}

	

}
