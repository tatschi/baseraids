package may.baseraids.entities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import may.baseraids.Baseraids;
import may.baseraids.entities.ai.goal.AttackBlockMeleeGoal;
import may.baseraids.entities.ai.goal.AttackBlockRangedGoal;
import may.baseraids.entities.ai.goal.HurtByNotRaidingTargetGoal;
import may.baseraids.entities.ai.goal.MoveTowardsNexusGoal;
import may.baseraids.entities.ai.goal.MoveTowardsNexusPhantomGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.CaveSpiderEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.WitherSkeletonEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * This class is used for setting up the default monsters with custom AI for
 * raiding.
 * 
 * @author Natascha May
 */
public class BaseraidsEntityManager {

	private static Map<EntityType<?>, Consumer<MobEntity>> setupsRegistry = new HashMap<EntityType<?>, Consumer<MobEntity>>();

	public static void registerSetups() {
		setupsRegistry.put(EntityType.ZOMBIE, (entity) -> setupZombieGoals((ZombieEntity) entity));
		setupsRegistry.put(EntityType.SPIDER, (entity) -> setupSpiderGoals((SpiderEntity) entity));
		setupsRegistry.put(EntityType.SKELETON, (entity) -> setupSkeletonGoals((SkeletonEntity) entity));
		setupsRegistry.put(EntityType.PHANTOM, (entity) -> setupPhantomGoals((PhantomEntity) entity));
		setupsRegistry.put(EntityType.ZOMBIFIED_PIGLIN,
				(entity) -> setupZombifiedPiglinGoals((ZombifiedPiglinEntity) entity));
		setupsRegistry.put(EntityType.CAVE_SPIDER, (entity) -> setupCaveSpiderGoals((CaveSpiderEntity) entity));
		setupsRegistry.put(EntityType.WITHER_SKELETON,
				(entity) -> setupWitherSkeletonGoals((WitherSkeletonEntity) entity));
	}

	public static void setupGoals(MobEntity entity) {
		if (!setupsRegistry.containsKey(entity.getType()))
			return;
		setupsRegistry.get(entity.getType()).accept(entity);
	}

	public static void setupZombieGoals(ZombieEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtGoal.class,
				ZombieEntity.AttackTurtleEggGoal.class, LookRandomlyGoal.class, MoveThroughVillageGoal.class,
				WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class, NearestAttackableTargetGoal.class));

		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager)
				.setCallsForHelp(ZombifiedPiglinEntity.class));
		entity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entity, PlayerEntity.class, true));
		entity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(entity, IronGolemEntity.class, true));

		entity.enablePersistence();
	}

	public static void setupSpiderGoals(SpiderEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtGoal.class, LookRandomlyGoal.class,
				SpiderEntity.AttackGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(2, new SpiderEntity.AttackGoal(entity));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager));

		entity.enablePersistence();
	}

	public static void setupSkeletonGoals(SkeletonEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtGoal.class, LookRandomlyGoal.class,
				WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class, NearestAttackableTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1,
				new AttackBlockRangedGoal<SkeletonEntity>(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entity, PlayerEntity.class, true));
		entity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(entity, IronGolemEntity.class, true));

		entity.enablePersistence();
	}

	public static void setupPhantomGoals(PhantomEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList();
		removeGoalsFromList(entity, goalClassesToRemove);

		entity.goalSelector.addGoal(1, new MoveTowardsNexusPhantomGoal(entity, Baseraids.baseraidsData.raidManager));
		// entity.goalSelector.addGoal(2, new AttackBlockPhantomGoal(entity,
		// Baseraids.baseraidsData.raidManager));

		entity.enablePersistence();
	}

	public static void setupZombifiedPiglinGoals(ZombifiedPiglinEntity entity) {
		// remove unwanted goals
		removeGoalsFromList(entity, Arrays.asList(WaterAvoidingRandomWalkingGoal.class));
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class));

		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1,
				new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager).setCallsForHelp());

		entity.enablePersistence();
	}

	public static void setupCaveSpiderGoals(CaveSpiderEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtGoal.class, LookRandomlyGoal.class,
				SpiderEntity.AttackGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(2, new SpiderEntity.AttackGoal(entity));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager));

		entity.enablePersistence();
	}

	public static void setupWitherSkeletonGoals(WitherSkeletonEntity entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtGoal.class, LookRandomlyGoal.class,
				WaterAvoidingRandomWalkingGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class, NearestAttackableTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, Baseraids.baseraidsData.raidManager));
		entity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entity, PlayerEntity.class, true));
		entity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(entity, IronGolemEntity.class, true));

		entity.enablePersistence();
	}

	/**
	 * Removes the specified AI goals from the given entity.
	 * 
	 * @param entity              the entity from which to remove the goals
	 * @param goalClassesToRemove a list of classes of the types of goals to remove
	 */
	private static void removeGoalsFromList(MobEntity entity, final List<Class<? extends Goal>> goalClassesToRemove) {
		entity.goalSelector.goals.removeIf((goal) -> goalClassesToRemove.contains(goal.getGoal().getClass()));
	}

	/**
	 * Removes the specified AI target goals from the given entity.
	 * 
	 * @param entity                the entity from which to remove the goals
	 * @param targetClassesToRemove a list of classes of the types of goals to
	 *                              remove
	 */
	private static void removeTargetsFromList(MobEntity entity,
			final List<Class<? extends TargetGoal>> targetClassesToRemove) {
		entity.targetSelector.goals.removeIf((goal) -> targetClassesToRemove.contains(goal.getGoal().getClass()));
	}
}
