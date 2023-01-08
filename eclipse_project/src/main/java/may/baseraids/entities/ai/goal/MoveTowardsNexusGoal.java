package may.baseraids.entities.ai.goal;

import java.util.EnumSet;

import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

/**
 * This class defines the AI goal to move toward the nexus.
 * 
 * @author Natascha May
 */
public class MoveTowardsNexusGoal<T extends MobEntity> extends Goal {

	protected T entity;
	protected RaidManager raidManager;
	protected int distanceReached = 2;

	public MoveTowardsNexusGoal(T entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	public boolean shouldExecute() {
		BlockPos nexusPos = NexusBlock.getBlockPos();

		if (entity.getAttackTarget() != null || !raidManager.isRaidActive()
				|| entity.getDistanceSq(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < distanceReached) {
			return false;
		}

		Path path = entity.getNavigator().getPath();
		return (path == null || path.isFinished());
	}

	@Override
	public boolean shouldContinueExecuting() {
		return shouldExecute();
	}

	@Override
	public void tick() {
		BlockPos nexusPos = NexusBlock.getBlockPos();
		entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);
	}

	@Override
	public void resetTask() {
		entity.getNavigator().clearPath();
	}

}
