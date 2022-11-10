package may.baseraids.entities.ai;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;

/**
 * This class defines the AI goal to move toward the nexus.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
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
