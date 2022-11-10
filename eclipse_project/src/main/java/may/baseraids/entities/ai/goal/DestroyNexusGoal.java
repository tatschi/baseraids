package may.baseraids.entities.ai.goal;

import java.util.EnumSet;

import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

/**
 * This class defines the AI goal to break the nexus.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
 */
public class DestroyNexusGoal extends Goal {

	private MobEntity entity;
	private RaidManager raidManager;
	private BlockPos nexusPos;
	private int distanceToTriggerGoal = 2;
	private int distanceToAllowBreaking = 2;

	
	private static final int DAMAGE = 1;

	public DestroyNexusGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.MOVE));
		nexusPos = NexusBlock.getBlockPos();
	}

	public boolean shouldExecute() {
		nexusPos = NexusBlock.getBlockPos();
		return raidManager.isRaidActive() && entity.isAlive()
				&& nexusPos.withinDistance(entity.getPositionVec(), distanceToTriggerGoal);
	}

	public boolean shouldContinueExecuting() {
		return raidManager.isRaidActive() && entity.isAlive()
				&& nexusPos.withinDistance(entity.getPositionVec(), distanceToTriggerGoal)
				&& entity.world instanceof ServerWorld;
	}

	public void startExecuting() {
		nexusPos = NexusBlock.getBlockPos();
	}

	public void tick() {
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());

		entity.getNavigator().tryMoveToXYZ(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ(), 1);

		if (nexusPos.withinDistance(entity.getPositionVec(), distanceToAllowBreaking)) {
			// swing arm at random
			if (this.entity.getRNG().nextInt(20) == 0) {
				this.entity.world.playEvent(1019, nexusPos, 0);
				if (!this.entity.isSwingInProgress) {
					this.entity.swingArm(this.entity.getActiveHand());
				}
			}

			raidManager.blockBreakProgressMng.addProgress(entity, nexusPos, DAMAGE);
		}

	}

	public void resetTask() {
		entity.setAggroed(false);
		entity.getNavigator().clearPath();
	}

}
