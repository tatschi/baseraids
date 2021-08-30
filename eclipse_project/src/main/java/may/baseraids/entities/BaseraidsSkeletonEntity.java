package may.baseraids.entities;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.DestroyNexusGoal;
import may.baseraids.entities.ai.MoveTowardsNexusGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.FleeSunGoal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.RestrictSunGoal;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class BaseraidsSkeletonEntity extends SkeletonEntity {

	
	public BaseraidsSkeletonEntity(EntityType<? extends SkeletonEntity> type, World worldIn) {
		super(type, worldIn);
	}
	
	public BaseraidsSkeletonEntity(World worldIn) {
		this(Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(), worldIn);
		
		
	}
	
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new MoveTowardsNexusGoal(this, Baseraids.baseraidsData.raidManager));
		this.goalSelector.addGoal(0, new DestroyNexusGoal(this, Baseraids.baseraidsData.raidManager));
		
		this.goalSelector.addGoal(2, new RestrictSunGoal(this));
	    this.goalSelector.addGoal(3, new FleeSunGoal(this, 1.0D));
	    this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, WolfEntity.class, 6.0F, 1.0D, 1.2D));
	    this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
	    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
	    this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolemEntity.class, true));
	    this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, TurtleEntity.class, 10, true, false, TurtleEntity.TARGET_DRY_BABY));
		
	}

}