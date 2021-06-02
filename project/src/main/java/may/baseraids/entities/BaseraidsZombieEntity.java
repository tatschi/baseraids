package may.baseraids.entities;

import may.baseraids.Baseraids;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class BaseraidsZombieEntity extends ZombieEntity {

	//public static final EntityType<ZombieEntity> ZOMBIE = EntityType.register("zombie", EntityType.Builder.<ZombieEntity>create(ZombieEntity::new, EntityClassification.MONSTER).size(0.6F, 1.95F).trackingRange(8));
	
	public BaseraidsZombieEntity(EntityType<? extends ZombieEntity> type, World worldIn) {
		super(type, worldIn);
		Baseraids.LOGGER.info("Zombie created");
	}
	
	public BaseraidsZombieEntity(World worldIn) {
		this(Baseraids.BASERAIDS_ZOMBIE_TYPE.get(), worldIn);
		
		
	}
	
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new MoveTowardsNexusGoal(this, Baseraids.baseraidsData.raidManager));
		//this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0D, false));
		/*
		this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setCallsForHelp(ZombifiedPiglinEntity.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillagerEntity.class, false));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolemEntity.class, true));
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, TurtleEntity.class, 10, true, false, TurtleEntity.TARGET_DRY_BABY));
		*/
	}

}
