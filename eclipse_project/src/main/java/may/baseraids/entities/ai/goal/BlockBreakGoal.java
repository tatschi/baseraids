package may.baseraids.entities.ai.goal;

import java.util.EnumSet;
import java.util.Random;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;

/**
 * This class defines the AI goal to break blocks that are in the way towards
 * the nexus.
 * 
 * @author Natascha May
 */
public class BlockBreakGoal extends Goal {

	private MobEntity entity;
	private RaidManager raidManager;
	
	private static final int DAMAGE = 1;
	private static final float DISTANCE_TO_ALLOW_MELEE_BREAKING = 2.5f;

	protected BlockPos target = null;

	final static Vector3i[] focusableBlocksAroundEntity = {

			new Vector3i(0, 0, 0), new Vector3i(0, 0, 1), new Vector3i(0, 0, -1),

			new Vector3i(1, 0, 0), new Vector3i(1, 0, 1), new Vector3i(1, 0, -1),

			new Vector3i(-1, 0, 0), new Vector3i(-1, 0, 1), new Vector3i(-1, 0, -1),

			new Vector3i(-1, -1, 0), new Vector3i(-1, -1, 1), new Vector3i(-1, -1, -1),

			new Vector3i(0, 1, 0), new Vector3i(0, 1, 1), new Vector3i(0, 1, -1),

			new Vector3i(0, -1, 0), new Vector3i(0, -1, 1), new Vector3i(0, -1, -1),

			new Vector3i(1, 1, 0), new Vector3i(1, 1, 1), new Vector3i(1, 1, -1),

			new Vector3i(1, -1, 0), new Vector3i(1, -1, 1), new Vector3i(1, -1, -1),

			new Vector3i(-1, 1, 0), new Vector3i(-1, 1, 1), new Vector3i(-1, 1, -1),

	};

	public BlockBreakGoal(MobEntity entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.LOOK));
	}

	@Override
	public boolean shouldExecute() {
		if (!raidManager.isRaidActive()) {
			return false;
		}		
		
		if (entity.getAttackTarget() != null) {
			return false;
		}
		
		Path path = entity.getNavigator().getPath();
		if (path != null && !path.isFinished()) {
			return false;
		}
		
		return true;
	}

	public boolean shouldContinueExecuting() {
		if (!raidManager.isRaidActive()) {
			return false;
		}
		
		if (entity.getAttackTarget() != null) {
			return false;
		}
		
		Path path = entity.getNavigator().getPath();
		if (path != null && !path.isFinished()) {
			return false;
		}
		return true;
	}
	
	public void startExecuting() {
		
	}

	public void tick() {		
		if(target != null) {
			attackTarget();
		}else {
			findTarget();
		}	
	}
	
	private void attackTarget() {
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(Baseraids.getVector3dFromBlockPos(target));

		// swing arm at random
		if (this.entity.getRNG().nextInt(20) == 0) {
			if (!this.entity.isSwingInProgress) {
				this.entity.swingArm(this.entity.getActiveHand());
			}
		}

		boolean wasBroken = raidManager.blockBreakProgressMng.addProgress(entity, target, DAMAGE);		
		if(wasBroken) {
			entity.getNavigator().clearPath();
		}
	}
	
	private void findTarget() {		
		BlockPos nexusPos = NexusBlock.getBlockPos();
		
		// Prioritize nexus if possible
		if (isAttackableBlock(nexusPos)) {
			target = nexusPos;
			entity.getLookController().setLookPosition(Baseraids.getVector3dFromBlockPos(target));
			return;
		}		
		
		Random r = new Random();
		Vector3d jitter = new Vector3d(r.nextDouble(), r.nextDouble(), r.nextDouble()).mul(4, 4, 4);
		Vector3d jitteredLookVec = jitter.add(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());
		entity.getLookController().setLookPosition(jitteredLookVec);
		
		BlockPos focusedBlock = getFocusedBlock();
		if(isAttackableBlock(focusedBlock)) {
			target = focusedBlock;
		}
	}
	
	private BlockPos getFocusedBlock() {
		return new BlockPos(entity.getLookController().getLookPosX(), entity.getLookController().getLookPosY(), entity.getLookController().getLookPosZ());
	}
	
	private boolean isAttackableBlock(BlockPos pos) {
		if (!entity.world.getBlockState(pos).isSolid()) {
			return false;
		}
		
		Vector3d nexusPos = Baseraids.getVector3dFromBlockPos(NexusBlock.getBlockPos());
		if(entity.getDistanceSq(nexusPos) <= pos.distanceSq(NexusBlock.getBlockPos())){
			return false;
		}
		
		Vector3d posVec = Baseraids.getVector3dFromBlockPos(pos);
		
		// check if the entity can see the block
		BlockRayTraceResult rayTraceResult = entity.world.rayTraceBlocks(new RayTraceContext(entity.getEyePosition(0), posVec, BlockMode.COLLIDER, FluidMode.ANY, entity));
		if(rayTraceResult.getType() == RayTraceResult.Type.MISS) {
			return false;
		}		
		
		
		Vector3d lookVec = posVec.subtract(entity.getEyePosition(0)).normalize();		
		Vector3d vecToNexus = nexusPos.subtract(entity.getEyePosition(0)).normalize();
		double dotProduct = lookVec.dotProduct(vecToNexus); 
		
		if(dotProduct < 0.7) {
			return false;
		}
		
		if(entity.getDistanceSq(posVec) > DISTANCE_TO_ALLOW_MELEE_BREAKING) {
			return false;
		}
		
		return true;
	}
	
	public void resetTask() {
		entity.setAggroed(false);
		target = null;
	}

}
