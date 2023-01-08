package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import may.baseraids.entities.ai.RaidArrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * This class defines the AI goal to attack blocks that are in the way towards
 * the nexus using a ranged attack.
 * 
 * @author Natascha May
 */
public class AttackBlockRangedGoal<T extends Monster & IRangedAttackMob> extends AttackBlockGoal<T> {

	// cooldown in ticks
	private static final int RANGED_ATTACK_COOLDOWN = 20;	
	private int rangedAttackRemainingCooldown = 0;

	public AttackBlockRangedGoal(T entity, RaidManager raidManager) {
		super(entity, raidManager);
	}

	@Override
	public void resetTask() {
		super.resetTask();
		this.entity.resetActiveHand();
	}
	
	@Override
	public void tick() {
		super.tick();
		if(rangedAttackRemainingCooldown != 0) {
			rangedAttackRemainingCooldown--;
		}
	}
	
	@Override
	protected void attackTarget() {
		super.attackTarget();
		
		if(isBlockInMeleeRange(target)) {
			// MELEE ATTACK
			attackBlockMelee(getFocusedBlock());
		}else {
			// RANGED ATTACK
			if(rangedAttackRemainingCooldown == 0) {
				rangedAttackRemainingCooldown = RANGED_ATTACK_COOLDOWN;			
				attackBlockWithRangedAttack(getFocusedBlock());
				target = null;
			}
		}		
	}

	/**
	 * Attacks the given block with a ranged attack including animation and sound effects.
	 * @param targetBlock the block to be attacked
	 */
	private void attackBlockWithRangedAttack(BlockPos targetBlock) {
		AbstractArrowEntity arrowEntity = createArrowEntity();		
		shootArrowEntityAtBlock(arrowEntity, targetBlock);
		entity.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (entity.getRNG().nextFloat() * 0.4F + 0.8F));
		entity.world.addEntity(arrowEntity);		
	}
	
	/**
	 * Creates an {@link AbstractArrowEntity} from ammo held by the entity and adds potion, enchantment and other effects.
	 * @return the create arrow entity
	 */
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
	
	/**
	 * Takes an arrow entity and shoots the arrow towards the given target block.
	 * @param arrowEntity the arrow entity to be shot
	 * @param targetBlock the block position to be shot at
	 */
	private void shootArrowEntityAtBlock(AbstractArrowEntity arrowEntity, BlockPos targetBlock) {
		double dX = targetBlock.getX() - entity.getPosX();
		double dY = targetBlock.getY() + 0.333333D - arrowEntity.getPosY();
		double dZ = targetBlock.getZ() - entity.getPosZ();
		double distInXZPlane = MathHelper.sqrt(dX * dX + dZ * dZ);
		double dYWithDistanceAdjustment = dY + distInXZPlane * 0.2F;
		arrowEntity.shoot(dX, dYWithDistanceAdjustment, dZ, 1.6F,
				(14 - entity.world.getDifficulty().getId() * 4));
	}

}
