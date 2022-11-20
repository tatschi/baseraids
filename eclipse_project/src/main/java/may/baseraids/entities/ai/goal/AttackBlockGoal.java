package may.baseraids.entities.ai.goal;

import java.util.EnumSet;
import java.util.Random;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.vector.Vector3d;

/**
 * This class defines an abstract AI goal to attack blocks that are in the way towards
 * the nexus.
 * 
 * @author Natascha May
 */
public abstract class AttackBlockGoal<T extends MobEntity> extends Goal{

	protected T entity;
	protected RaidManager raidManager;
	
	protected BlockPos target = null;
	protected int findTargetTicks = 0;
	
	protected static final int MELEE_DAMAGE = 1;
	protected static final float MELEE_ATTACK_RANGE = 3f;
	protected static final float RANGED_ATTACK_RANGE = 20f;
	protected static final int JITTER_FACTOR = 4;
	protected static final int FIND_TARGET_MAX_TICKS = 80;
	
	
	protected AttackBlockGoal(T entity, RaidManager raidManager) {
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
		
		if(findTargetTicks > 2 * FIND_TARGET_MAX_TICKS) {
			findTargetTicks = 0;
			return false;
		}
		
		if(findTargetTicks > FIND_TARGET_MAX_TICKS) {
			findTargetTicks++;
			return false;
		}
		
		return true;
	}
	
	public boolean shouldContinueExecuting() {
		return shouldExecute();
	}
	
	public void tick() {		
		if(target != null) {
			attackTarget();
			findTargetTicks = 0;
		}else {
			findTarget();
			findTargetTicks++;
		}	
	}
	
	/**
	 * Attacks the block specified by the field <code>target</code>.
	 * This method is expected to be extended by every implementing class.
	 */
	protected void attackTarget() {
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(Baseraids.getVector3dFromBlockPos(target));
	}
	
	/**
	 * Attacks the block specified by the field <code>target</code> with a melee attack.
	 */
	protected void attackBlockMelee(BlockPos targetBlock) {
		swingArmAtRandom();

		boolean wasBroken = raidManager.blockBreakProgressMng.addProgress(entity, target, MELEE_DAMAGE);		
		if(wasBroken) {
			entity.getNavigator().clearPath();
			target = null;
		}
	}
	
	/**
	 * Attempts to find a target block and saves the result in the field <code>target</code>.
	 * Prioritizes the nexus direction, otherwise jitters the look direction of the entity to find a possible target.
	 */
	protected void findTarget() {				
		entity.getLookController().setLookPosition(Baseraids.getVector3dFromBlockPos(NexusBlock.getBlockPos()));
		BlockPos focusedBlock = getFocusedBlock();
		
		if(isAttackableBlock(focusedBlock)) {
			target = focusedBlock;
			return;
		}
		
		jitterLookPositionAroundNexus();
		focusedBlock = getFocusedBlock();
		
		if(isAttackableBlock(focusedBlock)) {
			target = focusedBlock;
			return;
		}
		target = null;
	}
	
	/**
	 * Gets the first block in the look direction of the entity.
	 * @return the BlockPos of the first block in the look direction
	 */
	protected BlockPos getFocusedBlock() {
		Vector3d posVec = new Vector3d(entity.getLookController().getLookPosX(), entity.getLookController().getLookPosY(), entity.getLookController().getLookPosZ());
		BlockRayTraceResult rayTraceResult = entity.world.rayTraceBlocks(new RayTraceContext(entity.getEyePosition(1), posVec, BlockMode.COLLIDER, FluidMode.NONE, entity));
		return rayTraceResult.getPos();
	}
	
	/**
	 * Checks if a given block is in melee range for the entity.
	 * @param pos the position of the block that is checked to be in melee range
	 * @return true, if the block is in melee range, otherwise false
	 */
	protected boolean isBlockInMeleeRange(BlockPos pos) {
		return entity.getDistanceSq(Baseraids.getVector3dFromBlockPos(pos)) <= (MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE);
	}
	
	/**
	 * Checks if a given block is attackable for the entity.
	 * @param pos the position of the block that is checked to be attackable
	 * @return true, if the block is attackable, otherwise false
	 */
	protected boolean isAttackableBlock(BlockPos pos) {
		if(entity.world.getBlockState(pos).equals(Blocks.AIR.getDefaultState())){
			return false;
		}
		
		if(!isBlockCloserToNexusThanEntityToNexus(pos)) {
			return false;
		}		
		
		if(!canEntitySeeBlock(pos)) {
			return false;
		}
		
		return true;
	}
	
	public void resetTask() {
		super.resetTask();
		this.entity.setAggroed(false);
		target = null;
	}
	
	/**
	 * Swings the active arm of the entity with a certain probability. 
	 */
	private void swingArmAtRandom() {
		if (this.entity.getRNG().nextInt(20) == 0) {
			if (!this.entity.isSwingInProgress) {
				this.entity.swingArm(this.entity.getActiveHand());
			}
		}
	}
	
	/**
	 * Adds some randomized variation (jitter) to the nexus position and sets this new position as the look position of the entity.
	 */
	private void jitterLookPositionAroundNexus() {
		Random r = new Random();
		Vector3d jitter = new Vector3d(r.nextDouble(), r.nextDouble(), r.nextDouble()).mul(JITTER_FACTOR, JITTER_FACTOR, JITTER_FACTOR);
		Vector3d jitteredLookPos = jitter.add(Baseraids.getVector3dFromBlockPos(NexusBlock.getBlockPos()));
		entity.getLookController().setLookPosition(jitteredLookPos);
	}
	
	/**
	 * Checks if a given block is closer to the nexus than the entity.
	 * @param pos the position of the block that is checked to be closer
	 * @return true, if the block is closer, otherwise false
	 */
	private boolean isBlockCloserToNexusThanEntityToNexus(BlockPos pos) {
		return pos.distanceSq(NexusBlock.getBlockPos()) <= entity.getPosition().distanceSq(NexusBlock.getBlockPos());
	}
	
	/**
	 * Checks if a given block can be seen from the entities eye position.
	 * @param pos the position of the block that is checked to be visible
	 * @return true, if the block can be seen, otherwise false
	 */
	private boolean canEntitySeeBlock(BlockPos pos) {
		Vector3d posVec = Baseraids.getVector3dFromBlockPos(pos);
		BlockRayTraceResult rayTraceResult = entity.world.rayTraceBlocks(new RayTraceContext(entity.getEyePosition(1), posVec, BlockMode.COLLIDER, FluidMode.NONE, entity));
		return rayTraceResult.getPos().equals(pos);
	}

}
