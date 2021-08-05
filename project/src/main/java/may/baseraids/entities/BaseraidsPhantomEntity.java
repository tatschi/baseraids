package may.baseraids.entities;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.world.World;

public class BaseraidsPhantomEntity extends PhantomEntity{

	
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
		
		super.registerGoals();
	}
	
	/*
	public static AttributeModifierMap.MutableAttribute registerAttributes() {
	      return PhantomEntity.registerAttributes().createMutableAttribute(Attributes.FOLLOW_RANGE);
	}
	*/
}
