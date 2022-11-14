package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.BlockPos;

/**
 * This class defines the AI goal to attack blocks that are in the way towards
 * the nexus using a melee attack.
 * 
 * @author Natascha May
 */
public class AttackBlockMeleeGoal<T extends MobEntity> extends AttackBlockGoal<T> {

	public AttackBlockMeleeGoal(T entity, RaidManager raidManager) {
		super(entity, raidManager);
	}
	
	protected void attackTarget() {
		super.attackTarget();
		attackBlockMelee(target);
	}
	
	protected boolean isAttackableBlock(BlockPos pos) {
		if(!super.isAttackableBlock(pos)) {
			return false;
		}	
		return isBlockInMeleeRange(pos);
	}

}
