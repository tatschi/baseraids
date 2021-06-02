package may.baseraids.entities;

import java.util.EnumSet;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

public class MoveTowardsNexusGoal extends Goal{

	private MobEntity entity;
	private RaidManager raidManager;
	
	public MoveTowardsNexusGoal(MobEntity entity, RaidManager raidManager) {
		Baseraids.LOGGER.info("goal created");
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		
	}
	
	
	public boolean shouldExecute() {
		Baseraids.LOGGER.info("should Execute: " + entity.getAttackTarget() == null && raidManager.data.isRaidActive());
		return entity.getAttackTarget() == null && raidManager.data.isRaidActive();
	}
	
	public boolean shouldContinueExecuting() {
		Baseraids.LOGGER.info("should continue executing: " + entity.getAttackTarget() == null && raidManager.data.isRaidActive());
		return raidManager.data.isRaidActive() && entity.world instanceof ServerWorld;
	}
	
	public void startExecuting() {
		Baseraids.LOGGER.info("start execute");
	}
	
	public void tick() {
		Baseraids.LOGGER.info("tick goal");
		if (!entity.getNavigator().hasPath()) {
			Baseraids.LOGGER.info("has no path");
			Vector3d nexusPos = new Vector3d(Baseraids.baseraidsData.placedNexusBlockPos.getX(), Baseraids.baseraidsData.placedNexusBlockPos.getY(), Baseraids.baseraidsData.placedNexusBlockPos.getZ());
			Vector3d vector3d = RandomPositionGenerator.findRandomTargetBlockTowards((CreatureEntity) entity, 15, 4, nexusPos);
			// null pointer exception in next line? vector3d == null?
			entity.getNavigator().tryMoveToXYZ(vector3d.getX(), vector3d.getY(), vector3d.getZ(), 1.0D);
		}
		
	}
	
	public void resetTask() {
		Baseraids.LOGGER.info("reset goal");
		entity.getNavigator().clearPath();
	}

	

}
