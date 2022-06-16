package may.baseraids.entities;

import java.util.Arrays;
import java.util.List;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.BlockBreakGoal;
import may.baseraids.entities.ai.DestroyNexusGoal;
import may.baseraids.entities.ai.MoveTowardsNexusGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;

public class BaseraidsEntityManager {

	public void setupZombieGoals(ZombieEntity entity) {
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(3, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));

		// remove unwanted goals
		List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(ZombieEntity.AttackTurtleEggGoal.class,
				LookRandomlyGoal.class, MoveThroughVillageGoal.class, WaterAvoidingRandomWalkingGoal.class);

		entity.goalSelector.getRunningGoals().filter((goal) -> goalClassesToRemove.contains(goal.getClass()))
				.forEach((goal) -> entity.goalSelector.removeGoal(goal));
	}

	public void setupSpiderGoals(SpiderEntity entity) {

	}

	public void setupSkeletonGoals(SkeletonEntity entity) {

	}
}
