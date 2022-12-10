package may.baseraids.entities.ai.goal;

import java.util.Random;

import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

public class MoveTowardsNexusPhantomGoal extends MoveTowardsNexusGoal<PhantomEntity>{
	
	private int tickDelay;
	protected int distanceReached = 25;

	public MoveTowardsNexusPhantomGoal(PhantomEntity entity, RaidManager raidManager) {
		super(entity, raidManager);
	}
	
	public boolean shouldExecute() {		
		if(entity.getAttackTarget() != null || !raidManager.isRaidActive()) {
			return false;
		}
		
		return !isOrbitPositionCloseEnoughToNexus();
	}
	
	public void startExecuting() {
		tickDelay = 20;
	}
	
	public void tick() {
		if(tickDelay == 20) {
			setOrbitPositionAtNexus();
		}
		tickDelay--;
		if(tickDelay == 0) {
			tickDelay = 20;
		}
		
    }
	
	public void resetTask() {
	}
	
	/**
	 * Determines whether the entities orbitPosition is within the {@link MoveTowardsNexusPhantomGoal#distanceReached} from the nexus while neglecting the height difference.
	 * @return
	 */
	private boolean isOrbitPositionCloseEnoughToNexus() {
		BlockPos nexusPos = NexusBlock.getBlockPos();
		Vector3i orbitPosAtNexusHeight = new Vector3i(entity.orbitPosition.getX(), nexusPos.getY(), entity.orbitPosition.getZ());
		Vector3i nexusPosVec = new Vector3i(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());
		return orbitPosAtNexusHeight.distanceSq(nexusPosVec) < distanceReached;
	}
	
	/**
	 * Sets the entities orbitPosition to a position in randomly varying height above the nexus.
	 */
	private void setOrbitPositionAtNexus() {
		entity.orbitPosition = NexusBlock.getBlockPos().up(20 + (new Random()).nextInt(20));
		if (entity.orbitPosition.getY() < entity.world.getSeaLevel()) {
			entity.orbitPosition = new BlockPos(entity.orbitPosition.getX(), entity.world.getSeaLevel() + 1, entity.orbitPosition.getZ());
		}
	}
}