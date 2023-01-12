package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import may.baseraids.entities.ai.RaidArrow;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/**
 * This class defines the AI goal to attack blocks that are in the way towards
 * the nexus using a ranged attack.
 * 
 * @author Natascha May
 */
public class AttackBlockRangedGoal<T extends Monster & RangedAttackMob> extends AttackBlockGoal<T> {

	// cooldown in ticks
	private static final int RANGED_ATTACK_COOLDOWN = 20;	
	private int rangedAttackRemainingCooldown = 0;

	public AttackBlockRangedGoal(T entity, RaidManager raidManager) {
		super(entity, raidManager);
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
		AbstractArrow arrowEntity = createArrowEntity();		
		shootArrowEntityAtBlock(arrowEntity, targetBlock);
		entity.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
		entity.level.addFreshEntity(arrowEntity);		
	}
	
	/**
	 * Creates an {@link AbstractArrowEntity} from ammo held by the entity and adds potion, enchantment and other effects.
	 * @return the create arrow entity
	 */
	private AbstractArrow createArrowEntity() {
		InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(entity, BowItem.class::isInstance);
	    ItemStack itemstack = entity.getItemInHand(interactionhand);
		RaidArrow arrowEntity = new RaidArrow(entity.level, entity, raidManager);
		arrowEntity.setEffectsFromItem(itemstack);
		float distanceFactor = BowItem.getPowerForTime(entity.getUseItem().getMaxStackSize());
		arrowEntity.setEnchantmentEffectsFromEntity(entity, distanceFactor);
		
		if (entity.getMainHandItem().getItem() instanceof BowItem bowItem) {
			arrowEntity = (RaidArrow) bowItem.customArrow(arrowEntity);
		}
		
		return arrowEntity;
	}
	
	/**
	 * Takes an arrow entity and shoots the arrow towards the given target block.
	 * @param arrowEntity the arrow entity to be shot
	 * @param targetBlock the block position to be shot at
	 */
	private void shootArrowEntityAtBlock(AbstractArrow arrowEntity, BlockPos targetBlock) {
		double dX = targetBlock.getX() - entity.getX();
		double dY = targetBlock.getY() + 0.333333D - arrowEntity.getY();
		double dZ = targetBlock.getZ() - entity.getZ();
		double distInXZPlane = Math.sqrt(dX * dX + dZ * dZ);
		double dYWithDistanceAdjustment = dY + distInXZPlane * 0.2F;
		arrowEntity.shoot(dX, dYWithDistanceAdjustment, dZ, 1.6F,
				(14 - entity.level.getDifficulty().getId() * 4));
	}

}
