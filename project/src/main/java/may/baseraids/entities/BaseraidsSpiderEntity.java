package may.baseraids.entities;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.DestroyNexusGoal;
import may.baseraids.entities.ai.MoveTowardsNexusGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class BaseraidsSpiderEntity extends SpiderEntity{

	
	public BaseraidsSpiderEntity(EntityType<? extends SpiderEntity> type, World worldIn) {
		super(type, worldIn);
	}
	
	public BaseraidsSpiderEntity(World worldIn) {
		this(Baseraids.BASERAIDS_SPIDER_ENTITY_TYPE.get(), worldIn);
		
		
	}
	
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MoveTowardsNexusGoal(this, Baseraids.baseraidsData.raidManager));
		this.goalSelector.addGoal(0, new DestroyNexusGoal(this, Baseraids.baseraidsData.raidManager, Baseraids.baseraidsData.placedNexusBlockPos));
		

		this.goalSelector.addGoal(1, new SwimGoal(this));
		this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
		this.goalSelector.addGoal(4, new BaseraidsSpiderEntity.AttackGoal(this));
		this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.addGoal(6, new LookRandomlyGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new BaseraidsSpiderEntity.TargetGoal<>(this, PlayerEntity.class));
		this.targetSelector.addGoal(3, new BaseraidsSpiderEntity.TargetGoal<>(this, IronGolemEntity.class));
		
	}
	
	/**
	 * 
	 * AttackGoal copied from SpiderEntity (because of visibility)!
	 *
	 */
	static class AttackGoal extends MeleeAttackGoal {
	      public AttackGoal(SpiderEntity spider) {
	         super(spider, 1.0D, true);
	      }

	      /**
	       * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	       * method as well.
	       */
	      public boolean shouldExecute() {
	         return super.shouldExecute() && !this.attacker.isBeingRidden();
	      }

	      /**
	       * Returns whether an in-progress EntityAIBase should continue executing
	       */
	      public boolean shouldContinueExecuting() {
	         float f = this.attacker.getBrightness();
	         if (f >= 0.5F && this.attacker.getRNG().nextInt(100) == 0) {
	            this.attacker.setAttackTarget((LivingEntity)null);
	            return false;
	         } else {
	            return super.shouldContinueExecuting();
	         }
	      }

	      protected double getAttackReachSqr(LivingEntity attackTarget) {
	         return (double)(4.0F + attackTarget.getWidth());
	      }
	   }

	/**
	 * 
	 * TargetGoal copied from SpiderEntity (because of visibility)!
	 *
	 */
	
	static class TargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
	      public TargetGoal(SpiderEntity spider, Class<T> classTarget) {
	         super(spider, classTarget, true);
	      }

	      /**
	       * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	       * method as well.
	       */
	      public boolean shouldExecute() {
	         float f = this.goalOwner.getBrightness();
	         return f >= 0.5F ? false : super.shouldExecute();
	      }
	   }

}
