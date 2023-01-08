package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

/**
 * This class defines the AI goal to attack blocks that are in the way towards
 * the nexus using a melee attack.
 * 
 * @author Natascha May
 */
public class AttackBlockMeleeGoal<T extends Mob> extends AttackBlockGoal<T> {

	public AttackBlockMeleeGoal(T entity, RaidManager raidManager) {
		super(entity, raidManager);
	}
	
	@Override
	protected void attackTarget() {
		super.attackTarget();
		attackBlockMelee(target);
	}
	
	@Override
	protected boolean isAttackableBlock(BlockPos pos) {
		if(!super.isAttackableBlock(pos)) {
			return false;
		}	
		return isBlockInMeleeRange(pos);
	}

}
