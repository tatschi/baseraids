package may.baseraids.entities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.BlockBreakGoal;
import may.baseraids.entities.ai.BlockBreakRangedGoal;
import may.baseraids.entities.ai.DestroyNexusGoal;
import may.baseraids.entities.ai.HurtByNotRaidingTargetGoal;
import may.baseraids.entities.ai.MoveTowardsNexusGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;

/**
 * This class is used for setting up the default monsters with custom AI for raiding.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
 */
public class BaseraidsEntityManager {
	
	private static Map<EntityType<?>, Consumer<MobEntity>> setupsRegistry = new HashMap<EntityType<?>, Consumer<MobEntity>>();
	
	public static void registerSetups() {
		setupsRegistry.put(EntityType.ZOMBIE, (entity) -> setupZombieGoals((ZombieEntity) entity));
		setupsRegistry.put(EntityType.SPIDER, (entity) -> setupSpiderGoals((SpiderEntity) entity));
		setupsRegistry.put(EntityType.SKELETON, (entity) -> setupSkeletonGoals((SkeletonEntity) entity));
		setupsRegistry.put(EntityType.PHANTOM, (entity) -> setupPhantomGoals((PhantomEntity) entity));
		setupsRegistry.put(EntityType.ZOMBIFIED_PIGLIN, (entity) -> setupZombifiedPiglinGoals((ZombifiedPiglinEntity) entity));
	}
	
	public static void setupGoals(MobEntity entity) {
		if(!setupsRegistry.containsKey(entity.getType())) return;
		setupsRegistry.get(entity.getType()).accept(entity);
	}

	public static void setupZombieGoals(ZombieEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(ZombieEntity.AttackTurtleEggGoal.class,
				LookRandomlyGoal.class, MoveThroughVillageGoal.class, WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		final List<Class<? extends TargetGoal>> targetClassesToRemove = Arrays.asList(HurtByTargetGoal.class);
		removeTargetsFromList(entity, targetClassesToRemove);
		
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(3, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager).setCallsForHelp(ZombifiedPiglinEntity.class));
	}

	public static void setupSpiderGoals(SpiderEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookRandomlyGoal.class, SpiderEntity.AttackGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		final List<Class<? extends TargetGoal>> targetClassesToRemove = Arrays.asList(HurtByTargetGoal.class);
		removeTargetsFromList(entity, targetClassesToRemove);
		
		entity.goalSelector.addGoal(0, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(2, new SpiderEntity.AttackGoal(entity));		
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager));
	}

	public static void setupSkeletonGoals(SkeletonEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtGoal.class, LookRandomlyGoal.class, WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		final List<Class<? extends TargetGoal>> targetClassesToRemove = Arrays.asList(HurtByTargetGoal.class);
		removeTargetsFromList(entity, targetClassesToRemove);
		
		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakRangedGoal<SkeletonEntity>(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager));
		
	}

	public static void setupPhantomGoals(PhantomEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(PhantomEntity.OrbitPointGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		
		entity.goalSelector.addGoal(0, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
	}
	
	public static void setupZombifiedPiglinGoals(ZombifiedPiglinEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		final List<Class<? extends TargetGoal>> targetClassesToRemove = Arrays.asList(HurtByTargetGoal.class);
		removeTargetsFromList(entity, targetClassesToRemove);
		
		entity.goalSelector.addGoal(0, new DestroyNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new BlockBreakGoal(entity, Baseraids.baseraidsData.raidManager));				
		entity.goalSelector.addGoal(3, new MoveTowardsNexusGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager).setCallsForHelp());
		
	}
	
	/**
	 * Removes the specified AI goals from the given entity.
	 * 
	 * @param entity              the entity from which to remove the goals
	 * @param goalClassesToRemove a list of classes of the types of goals to remove
	 */
	private static void removeGoalsFromList(MobEntity entity, final List<Class<? extends Goal>> goalClassesToRemove) {		
		entity.goalSelector.getRunningGoals().filter((goal) -> goalClassesToRemove.contains(goal.getClass()))
				.forEach((goal) -> entity.goalSelector.removeGoal(goal));
	}
	
	/**
	 * Removes the specified AI target goals from the given entity.
	 * 
	 * @param entity              the entity from which to remove the goals
	 * @param targetClassesToRemove a list of classes of the types of goals to remove
	 */
	private static void removeTargetsFromList(MobEntity entity, final List<Class<? extends TargetGoal>> targetClassesToRemove) {
		entity.targetSelector.goals.removeIf((goal) -> targetClassesToRemove.contains(goal.getGoal().getClass()));
	}
}
