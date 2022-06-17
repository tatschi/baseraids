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
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;

public class BaseraidsEntityManager {

	public void setupZombieGoals(ZombieEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(ZombieEntity.AttackTurtleEggGoal.class,
				LookRandomlyGoal.class, MoveThroughVillageGoal.class, WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(3, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
	}

	public void setupSpiderGoals(SpiderEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookRandomlyGoal.class, SpiderEntity.AttackGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		
		entity.goalSelector.addGoal(0, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(2, new SpiderEntity.AttackGoal(entity));		
	}

	public void setupSkeletonGoals(SkeletonEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookRandomlyGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(2, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
	}

	public void setupPhantomGoals(PhantomEntity entity) {
		// 
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(PhantomEntity.OrbitPointGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		
		entity.goalSelector.addGoal(0, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
	}
	
	/**
	 * Removes the specified AI goals from the given entity.
	 * 
	 * @param entity              the entity from which to remove the goals
	 * @param goalClassesToRemove a list of classes of the types of goals to remove
	 */
	private void removeGoalsFromList(MonsterEntity entity, final List<Class<? extends Goal>> goalClassesToRemove) {
		entity.goalSelector.getRunningGoals().filter((goal) -> goalClassesToRemove.contains(goal.getClass()))
				.forEach((goal) -> entity.goalSelector.removeGoal(goal));
	}
}
