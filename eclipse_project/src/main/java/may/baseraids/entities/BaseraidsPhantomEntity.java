package may.baseraids.entities;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.DestroyNexusGoal;
import may.baseraids.entities.ai.MoveTowardsNexusGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class BaseraidsPhantomEntity extends PhantomEntity {

	public static final String CONFIG_NAME = "Phantom";

	public BaseraidsPhantomEntity(EntityType<? extends PhantomEntity> type, World worldIn) {
		super(type, worldIn);
	}

	public BaseraidsPhantomEntity(World worldIn) {
		this(Baseraids.BASERAIDS_PHANTOM_ENTITY_TYPE.get(), worldIn);

	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new MoveTowardsNexusGoal(this, Baseraids.baseraidsData.raidManager));
		this.goalSelector.addGoal(0, new DestroyNexusGoal(this, Baseraids.baseraidsData.raidManager));

		// the constructors to the following goals are private inside PhantomEntity
		// therefore the code for these goals was copied from PhantomEntity, pasted in this class and
		// adjusted to not use private members from PhantomEntity which are no longer of use for this child class
		this.goalSelector.addGoal(1, new BaseraidsPhantomEntity.PickAttackGoal());
		this.goalSelector.addGoal(2, new BaseraidsPhantomEntity.SweepAttackGoal());
		this.targetSelector.addGoal(1, new BaseraidsPhantomEntity.AttackPlayerGoal());
	}

	class AttackPlayerGoal extends Goal {
		private final EntityPredicate field_220842_b = (new EntityPredicate()).setDistance(64.0D);
		private int tickDelay = 20;

		private AttackPlayerGoal() {
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state
		 * necessary for execution in this method as well.
		 */
		public boolean shouldExecute() {
			if (this.tickDelay > 0) {
				--this.tickDelay;
				return false;
			} else {
				this.tickDelay = 60;
				List<PlayerEntity> list = BaseraidsPhantomEntity.this.world.getTargettablePlayersWithinAABB(
						this.field_220842_b, BaseraidsPhantomEntity.this,
						BaseraidsPhantomEntity.this.getBoundingBox().grow(16.0D, 64.0D, 16.0D));
				if (!list.isEmpty()) {
					list.sort(Comparator.<Entity, Double>comparing(Entity::getPosY).reversed());

					for (PlayerEntity playerentity : list) {
						if (BaseraidsPhantomEntity.this.canAttack(playerentity, EntityPredicate.DEFAULT)) {
							BaseraidsPhantomEntity.this.setAttackTarget(playerentity);
							return true;
						}
					}
				}

				return false;
			}
		}

		/**
		 * Returns whether an in-progress EntityAIBase should continue executing
		 */
		public boolean shouldContinueExecuting() {
			LivingEntity livingentity = BaseraidsPhantomEntity.this.getAttackTarget();
			return livingentity != null ? BaseraidsPhantomEntity.this.canAttack(livingentity, EntityPredicate.DEFAULT)
					: false;
		}
	}

	class PickAttackGoal extends Goal {
		private int tickDelay;

		private PickAttackGoal() {
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state
		 * necessary for execution in this method as well.
		 */
		public boolean shouldExecute() {
			LivingEntity livingentity = BaseraidsPhantomEntity.this.getAttackTarget();
			return livingentity != null
					? BaseraidsPhantomEntity.this.canAttack(BaseraidsPhantomEntity.this.getAttackTarget(),
							EntityPredicate.DEFAULT)
					: false;
		}

		/**
		 * Execute a one shot task or start executing a continuous task
		 */
		public void startExecuting() {
			this.tickDelay = 10;
		}

		/**
		 * Reset the task's internal state. Called when this task is interrupted by
		 * another one
		 */
		public void resetTask() {
		}

		/**
		 * Keep ticking a continuous task that has already been started
		 */
		public void tick() {
			--this.tickDelay;
			if (this.tickDelay <= 0) {
				this.tickDelay = (8 + BaseraidsPhantomEntity.this.rand.nextInt(4)) * 20;
				BaseraidsPhantomEntity.this.playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 10.0F,
						0.95F + BaseraidsPhantomEntity.this.rand.nextFloat() * 0.1F);
			}

		}

	}

	abstract class MoveGoal extends Goal {
		public MoveGoal() {
			this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
		}
	}

	class SweepAttackGoal extends BaseraidsPhantomEntity.MoveGoal {
		private SweepAttackGoal() {
		}

		/**
		 * Returns whether execution should begin. You can also read and cache any state
		 * necessary for execution in this method as well.
		 */
		public boolean shouldExecute() {
			return BaseraidsPhantomEntity.this.getAttackTarget() != null;
		}

		/**
		 * Returns whether an in-progress EntityAIBase should continue executing
		 */
		public boolean shouldContinueExecuting() {
			LivingEntity livingentity = BaseraidsPhantomEntity.this.getAttackTarget();
			if (livingentity == null) {
				return false;
			} else if (!livingentity.isAlive()) {
				return false;
			} else if (!(livingentity instanceof PlayerEntity)
					|| !((PlayerEntity) livingentity).isSpectator() && !((PlayerEntity) livingentity).isCreative()) {
				if (!this.shouldExecute()) {
					return false;
				} else {
					if (BaseraidsPhantomEntity.this.ticksExisted % 20 == 0) {
						List<CatEntity> list = BaseraidsPhantomEntity.this.world.getEntitiesWithinAABB(CatEntity.class,
								BaseraidsPhantomEntity.this.getBoundingBox().grow(16.0D), EntityPredicates.IS_ALIVE);
						if (!list.isEmpty()) {
							for (CatEntity catentity : list) {
								catentity.func_213420_ej();
							}

							return false;
						}
					}

					return true;
				}
			} else {
				return false;
			}
		}

		/**
		 * Execute a one shot task or start executing a continuous task
		 */
		public void startExecuting() {
		}

		/**
		 * Reset the task's internal state. Called when this task is interrupted by
		 * another one
		 */
		public void resetTask() {
			BaseraidsPhantomEntity.this.setAttackTarget((LivingEntity) null);
		}

		/**
		 * Keep ticking a continuous task that has already been started
		 */
		public void tick() {
			LivingEntity livingentity = BaseraidsPhantomEntity.this.getAttackTarget();
			if (BaseraidsPhantomEntity.this.getBoundingBox().grow((double) 0.2F).intersects(livingentity.getBoundingBox())) {
				BaseraidsPhantomEntity.this.attackEntityAsMob(livingentity);
				if (!BaseraidsPhantomEntity.this.isSilent()) {
					BaseraidsPhantomEntity.this.world.playEvent(1039, BaseraidsPhantomEntity.this.getPosition(), 0);
				}
			}

		}
	}

}
