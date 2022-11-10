package may.baseraids.entities.ai.goal;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;

/**
 * This class defines the AI goal to move toward the nexus.
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
		if(super.shouldExecute()) {
			if(!raidManager.isEntityRaiding(this.goalOwner.getRevengeTarget())) {
				Baseraids.LOGGER.debug("shouldExecute from custom goal");
				return true;
			}
		}
		
		return false;
	}

}
