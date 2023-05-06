package may.baseraids.entities.ai.goal;

import java.util.EnumSet;

import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

/**
 * This class defines the AI goal to move toward the nexus.
 * 
 * @author Natascha May
 */
public class MoveTowardsNexusGoal<T extends Mob> extends Goal {

	protected T entity;
	protected RaidManager raidManager;
	protected int distanceReached = 2;

	public MoveTowardsNexusGoal(T entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	public boolean canUse() {
		BlockPos nexusPos = NexusBlock.getBlockPos();

		if (entity.getTarget() != null || !raidManager.isRaidActive()
				|| entity.distanceToSqr(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ()) < distanceReached) {
			return false;
		}

		Path path = entity.getNavigation().getPath();
		return (path == null || path.isDone());
	}

	@Override
	public void tick() {
		BlockPos nexusPos = NexusBlock.getBlockPos();
		entity.getNavigation().moveTo(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);
	}

}
