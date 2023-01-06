package may.baseraids.entities.ai.goal;

import may.baseraids.RaidManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;

/**
 * This class replaces the vanilla {@link HurtByTargetGoal} for raid monsters so that they do not attack other raid monsters.
 * 
 * @author Natascha May
 */
public class HurtByNotRaidingTargetGoal extends HurtByTargetGoal {

	private RaidManager raidManager;
	
	public HurtByNotRaidingTargetGoal(CreatureEntity creatureIn, RaidManager raidManager) {
		super(creatureIn);
		this.raidManager = raidManager;
	}

	@Override
	public boolean shouldExecute() {
		if(!super.shouldExecute()) {
			return false;
		}
		
		return !raidManager.isEntityRaiding(this.goalOwner.getRevengeTarget());
	}

}
