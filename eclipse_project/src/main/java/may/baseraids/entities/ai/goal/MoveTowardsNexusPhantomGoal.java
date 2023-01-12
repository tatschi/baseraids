package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.monster.Phantom;

public class MoveTowardsNexusPhantomGoal extends MoveTowardsNexusGoal<Phantom> {

	private int tickDelay;

	public MoveTowardsNexusPhantomGoal(Phantom entity, RaidManager raidManager) {
		super(entity, raidManager);
		distanceReached = 25;
	}

	@Override
	public boolean canUse() {
		if (entity.getTarget() != null || !raidManager.isRaidActive()) {
			return false;
		}

		return !isOrbitPositionCloseEnoughToNexus();
	}

	@Override
	public void start() {
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
	public void stop() {
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
		Vec3i orbitPosAtNexusHeight = new Vec3i(entity.orbitPosition.getX(), nexusPos.getY(),
				entity.orbitPosition.getZ());
		Vec3i nexusPosVec = new Vec3i(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());
		return orbitPosAtNexusHeight.distSqr(nexusPosVec) < distanceReached;
	}

	/**
	 * Sets the entities orbitPosition to a position in randomly varying height
	 * above the nexus.
	 */
	private void setOrbitPositionAtNexus() {
		entity.orbitPosition = NexusBlock.getBlockPos().up(20 + entity.getRandom().nextInt(20));
		if (entity.orbitPosition.getY() < entity.level.getSeaLevel()) {
			entity.orbitPosition = new BlockPos(entity.orbitPosition.getX(), entity.level.getSeaLevel() + 1,
					entity.orbitPosition.getZ());
		}
	}
}