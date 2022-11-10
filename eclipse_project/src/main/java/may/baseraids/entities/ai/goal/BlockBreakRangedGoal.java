package may.baseraids.entities.ai.goal;

import java.util.EnumSet;
import java.util.Random;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import may.baseraids.RaidManager;
import may.baseraids.entities.ai.RaidArrowEntity;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/**
 * This class defines the AI goal to break blocks that are in the way towards
 * the nexus.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.2.0
 */
public class BlockBreakRangedGoal<T extends MonsterEntity & IRangedAttackMob> extends Goal {

	private T entity;
	private RaidManager raidManager;


	// cooldown in ticks
	private static final int ATTACK_COOLDOWN = 10;
	
	private int remainingCooldown = 0;
	//private float maxAttackDistance = 20;
	private BlockPos target = null;
	private int seeTime;

	public BlockBreakRangedGoal(T entity, RaidManager raidManager) {
		this.entity = entity;
		this.raidManager = raidManager;
		this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.LOOK));
	}

	@Override
	public boolean shouldExecute() {
		if (!raidManager.isRaidActive())
			return false;

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

	public void resetTask() {
		super.resetTask();
		this.entity.setAggroed(false);
		this.seeTime = 0;
		this.entity.resetActiveHand();
		target = null;
	}

	public void tick() {
		

		if(target != null) {
			attackTarget();
		}else {
			findTarget();
		}		
		
	}
	
	private void attackTarget() {
		if(remainingCooldown == 0) {
			remainingCooldown = ATTACK_COOLDOWN;
			
			attackBlockWithRangedAttack(getFocusedBlock());
		}else {
			remainingCooldown--;
		}
	}
	
	private void findTarget() {
		BlockPos nexusPos = NexusBlock.getBlockPos();
		Random r = new Random();
		Vector3d jitter = new Vector3d(r.nextDouble(), r.nextDouble(), r.nextDouble()).mul(2, 2, 2);
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
		Vector3d lookVec = posVec.subtract(entity.getEyePosition(0)).normalize();
		Vector3d vecToNexus = nexusPos.subtract(entity.getEyePosition(0)).normalize();
		double dotProduct = lookVec.dotProduct(vecToNexus); 
		
		if(dotProduct < 0.7) {
			return false;
		}
		
		return true;
	}

	private void attackBlockWithRangedAttack(BlockPos targetBlock) {
		entity.setAggroed(true);
		entity.getLookController().setLookPosition(Baseraids.getVector3dFromBlockPos(targetBlock));
		
		AbstractArrowEntity arrowEntity = createArrowEntity();
		
		double d0 = targetBlock.getX() - entity.getPosX();
		double d1 = targetBlock.getY() + 0.333333D - arrowEntity.getPosY();
		double d2 = targetBlock.getZ() - entity.getPosZ();
		double d3 = (double) MathHelper.sqrt(d0 * d0 + d2 * d2);
		arrowEntity.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F,
				(float) (14 - entity.world.getDifficulty().getId() * 4));
		entity.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (entity.getRNG().nextFloat() * 0.4F + 0.8F));
		entity.world.addEntity(arrowEntity);		
	}
	
	private AbstractArrowEntity createArrowEntity() {
		ItemStack itemstack = entity.findAmmo(entity.getHeldItem(ProjectileHelper.getHandWith(entity, Items.BOW)));
		RaidArrowEntity arrowEntity = new RaidArrowEntity(entity.world, entity, raidManager);
		arrowEntity.setPotionEffect(itemstack);
		float distanceFactor = BowItem.getArrowVelocity(entity.getItemInUseMaxCount());
		arrowEntity.setEnchantmentEffectsFromEntity(entity, distanceFactor);
		
		if (entity.getHeldItemMainhand().getItem() instanceof net.minecraft.item.BowItem) {
			arrowEntity = (RaidArrowEntity) ((net.minecraft.item.BowItem) entity.getHeldItemMainhand().getItem())
					.customArrow(arrowEntity);
		}
		
		return arrowEntity;
	}

}
