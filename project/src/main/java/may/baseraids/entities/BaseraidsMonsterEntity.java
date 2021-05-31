package may.baseraids.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.world.World;

public class BaseraidsMonsterEntity extends MonsterEntity{
// Interface instead??
	
	
	protected BaseraidsMonsterEntity(EntityType<? extends MonsterEntity> type, World worldIn) {
		super(type, worldIn);
	}

	
		
		
}
