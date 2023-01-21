package may.baseraids.entities.ai.goal;

import java.util.EnumSet;
import java.util.Random;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * This class defines an abstract AI goal to attack blocks that are in the way
 * towards the nexus.
 * 
 * @author Natascha May
 */
public abstract class AttackBlockGoal<T extends Mob> extends Goal {

	protected T entity;
	protected RaidManager raidManager;

	protected BlockPos target = null;
	protected int findTargetTicks = 0;

	private Random rand = new Random();

	protected static final int MELEE_DAMAGE = 1;
	protected static final float MELEE_ATTACK_RANGE = 3f;
	protected static final float RANGED_ATTACK_RANGE = 20f;
	protected static final int JITTER_FACTOR = 4;
	protected static final int FIND_TARGET_MAX_TICKS = 80;

	protected AttackBlockGoal(T entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!raidManager.isRaidActive()) {
			return false;
		}

		if (entity.getTarget() != null) {
			return false;
		}

		Path path = entity.getNavigation().getPath();
		if (path != null && !path.isDone()) {
			return false;
		}

		if (findTargetTicks > 2 * FIND_TARGET_MAX_TICKS) {
			findTargetTicks = 0;
			return false;
		}

		if (findTargetTicks > FIND_TARGET_MAX_TICKS) {
			findTargetTicks++;
			return false;
		}

		return true;
	}

	@Override
	public void tick() {
		if (target != null) {
			attackTarget();
			findTargetTicks = 0;
		} else {
			findTarget();
			findTargetTicks++;
		}
	}

	/**
	 * Attacks the block specified by the field {@link #target}. This method is
	 * expected to be extended by every implementing class.
	 */
	protected void attackTarget() {
		entity.setAggressive(true);
		entity.getLookControl().setLookAt(Baseraids.getVector3dFromBlockPos(target));
	}

	/**
	 * Attacks the given block with a melee attack including animation and sound
	 * effects.
	 * 
	 * @param targetBlock the block to be attacked
	 */
	protected void attackBlockMelee(BlockPos targetBlock) {
		swingArmAtRandom();

		boolean wasBroken = raidManager.globalBlockBreakProgressMng.addProgress(target, MELEE_DAMAGE);
		if (wasBroken) {
			entity.getNavigation().recomputePath();
			target = null;
		}
	}

	/**
	 * Attempts to find a target block and saves the result in the field
	 * {@link #target}. Prioritizes the nexus direction, otherwise jitters the
	 * look direction of the entity to find a possible target.
	 */
	protected void findTarget() {
		entity.getLookControl().setLookAt(Baseraids.getVector3dFromBlockPos(NexusBlock.getBlockPos()));
		BlockPos focusedBlock = getFocusedBlock();

		if (isAttackableBlock(focusedBlock)) {
			target = focusedBlock;
			return;
		}

		jitterLookPositionAroundNexus();
		focusedBlock = getFocusedBlock();

		if (isAttackableBlock(focusedBlock)) {
			target = focusedBlock;
			return;
		}
		target = null;
	}

	/**
	 * Gets the first block in the look direction of the entity.
	 * 
	 * @return the BlockPos of the first block in the look direction
	 */
	protected BlockPos getFocusedBlock() {
		Vec3 posVec = new Vec3(entity.getLookControl().getWantedX(), entity.getLookControl().getWantedY(),
				entity.getLookControl().getWantedZ());
		
		BlockHitResult rayTraceResult = entity.level.clip(new ClipContext(entity.getEyePosition(), posVec, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, entity));
		return rayTraceResult.getBlockPos();
	}

	/**
	 * Checks if a given block is in melee range for the entity.
	 * 
	 * @param pos the position of the block that is checked to be in melee range
	 * @return true, if the block is in melee range, otherwise false
	 */
	protected boolean isBlockInMeleeRange(BlockPos pos) {
		return entity
				.distanceToSqr(Baseraids.getVector3dFromBlockPos(pos)) <= (MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE);
	}

	/**
	 * Checks if a given block is attackable for the entity.
	 * 
	 * @param pos the position of the block that is checked to be attackable
	 * @return true, if the block is attackable, otherwise false
	 */
	protected boolean isAttackableBlock(BlockPos pos) {
		if (entity.level.getBlockState(pos).equals(Blocks.AIR.defaultBlockState())) {
			return false;
		}

		if (!isBlockCloserToNexusThanEntityToNexus(pos)) {
			return false;
		}

		return hasLineOfSight(pos);
	}

	@Override
	public void stop() {
		super.stop();
		this.entity.setAggressive(false);
		target = null;
	}

	/**
	 * Swings the active arm of the entity with a certain probability.
	 */
	private void swingArmAtRandom() {
		if (this.entity.getRandom().nextInt(20) == 0 && !this.entity.swinging) {
			this.entity.swing(entity.getUsedItemHand());
		}
	}

	/**
	 * Adds some randomized variation (jitter) to the nexus position and sets this
	 * new position as the look position of the entity.
	 */
	private void jitterLookPositionAroundNexus() {
		Vec3 jitter = new Vec3(rand.nextDouble(), rand.nextDouble(), rand.nextDouble()).scale(JITTER_FACTOR);
		Vec3 jitteredLookPos = jitter.add(Baseraids.getVector3dFromBlockPos(NexusBlock.getBlockPos()));
		entity.getLookControl().setLookAt(jitteredLookPos);
	}

	/**
	 * Checks if a given block is closer to the nexus than the entity.
	 * 
	 * @param pos the position of the block that is checked to be closer
	 * @return true, if the block is closer, otherwise false
	 */
	private boolean isBlockCloserToNexusThanEntityToNexus(BlockPos pos) {
		return pos.distSqr(NexusBlock.getBlockPos()) <= entity.position()
				.distanceToSqr(Baseraids.getVector3dFromBlockPos(NexusBlock.getBlockPos()));
	}

	/**
	 * Checks if a given block can be seen from the entities eye position.
	 * entity.getSensing().hasLineOfSight(pos);
	 * 
	 * @param pos	the position of the block that is checked to be visible
	 * @return true, if the block can be seen, otherwise false
	 */
	public boolean hasLineOfSight(BlockPos pos) {
		Vec3 posVec = Baseraids.getVector3dFromBlockPos(pos);
		if (posVec.distanceTo(entity.getEyePosition()) > 128.0D) {
			return false;
		} else {
			return entity.level.clip(new ClipContext(entity.getEyePosition(), posVec, ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS;
		}

	}

}
