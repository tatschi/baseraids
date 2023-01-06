package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

public class MoveTowardsNexusPhantomGoal extends MoveTowardsNexusGoal<PhantomEntity> {

	private int tickDelay;

	public MoveTowardsNexusPhantomGoal(PhantomEntity entity, RaidManager raidManager) {
		super(entity, raidManager);
		distanceReached = 25;
	}

	@Override
	public boolean shouldExecute() {
		if (entity.getAttackTarget() != null || !raidManager.isRaidActive()) {
			return false;
		}

		return !isOrbitPositionCloseEnoughToNexus();
	}

	@Override
	public void startExecuting() {
		tickDelay = 20;
	}

	@Override
	public void tick() {
		if (tickDelay == 20) {
			setOrbitPositionAtNexus();
		}
		tickDelay--;
		if (tickDelay == 0) {
			tickDelay = 20;
		}

	}

	@Override
	public void resetTask() {
		// override parent method to do nothing instead
	}

	/**
	 * Determines whether the entities orbitPosition is within the
	 * {@link MoveTowardsNexusPhantomGoal#distanceReached} from the nexus while
	 * neglecting the height difference.
	 * 
	 * @return
	 */
	private boolean isOrbitPositionCloseEnoughToNexus() {
		BlockPos nexusPos = NexusBlock.getBlockPos();
		Vector3i orbitPosAtNexusHeight = new Vector3i(entity.orbitPosition.getX(), nexusPos.getY(),
				entity.orbitPosition.getZ());
		Vector3i nexusPosVec = new Vector3i(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());
		return orbitPosAtNexusHeight.distanceSq(nexusPosVec) < distanceReached;
	}

	/**
	 * Sets the entities orbitPosition to a position in randomly varying height
	 * above the nexus.
	 */
	private void setOrbitPositionAtNexus() {
		entity.orbitPosition = NexusBlock.getBlockPos().up(20 + entity.getRNG().nextInt(20));
		if (entity.orbitPosition.getY() < entity.world.getSeaLevel()) {
			entity.orbitPosition = new BlockPos(entity.orbitPosition.getX(), entity.world.getSeaLevel() + 1,
					entity.orbitPosition.getZ());
		}
	}
}