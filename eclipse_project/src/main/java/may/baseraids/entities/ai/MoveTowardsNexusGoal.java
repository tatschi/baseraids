package may.baseraids.entities.ai;

import java.util.EnumSet;

import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

/**
 * This class defines the AI goal to move toward the nexus.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
 */
public class MoveTowardsNexusGoal extends Goal {

	private MobEntity entity;
	private RaidManager raidManager;
	private BlockPos nexusPos;
	private int distanceReached = 2;

	public MoveTowardsNexusGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		nexusPos = NexusBlock.getBlockPos();
	}

	public boolean shouldExecute() {
		nexusPos = NexusBlock.getBlockPos();
		Path path = entity.getNavigator().getPath();
		return entity.getAttackTarget() == null && raidManager.isRaidActive()
				&& entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) >= distanceReached
				&& (path == null || !path.isFinished());
	}

	public boolean shouldContinueExecuting() {
		nexusPos = NexusBlock.getBlockPos();
		Path path = entity.getNavigator().getPath();
		;
		return raidManager.isRaidActive() && entity.world instanceof ServerWorld
				&& entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) >= distanceReached
				&& (path == null || !path.isFinished());
	}

	public void startExecuting() {
		nexusPos = NexusBlock.getBlockPos();
	}

	public void tick() {
		entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);

	}

	public void resetTask() {
		entity.getNavigator().clearPath();
		entity.getLookController().setLookPosition(new Vector3d(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()));
	}

}
