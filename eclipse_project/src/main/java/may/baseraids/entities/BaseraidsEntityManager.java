package may.baseraids.entities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import may.baseraids.WorldManager;
import may.baseraids.entities.ai.goal.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.IronGolem;

/**
 * This class is used for setting up the default monsters with custom AI for
 * raiding.
 * 
 * @author Natascha May
 */
public class BaseraidsEntityManager {

	private static Map<EntityType<?>, Consumer<Mob>> setupsRegistry = new HashMap<>();

	private WorldManager worldManager;
	
	public BaseraidsEntityManager(WorldManager worldManager) {
		this.worldManager = worldManager;
	}

	public void registerSetups() {
		setupsRegistry.put(EntityType.ZOMBIE, entity -> setupZombieGoals((Zombie) entity));
		setupsRegistry.put(EntityType.SPIDER, entity -> setupSpiderGoals((Spider) entity));
		setupsRegistry.put(EntityType.SKELETON, entity -> setupSkeletonGoals((Skeleton) entity));
		setupsRegistry.put(EntityType.PHANTOM, entity -> setupPhantomGoals((Phantom) entity));
		setupsRegistry.put(EntityType.ZOMBIFIED_PIGLIN,
				entity -> setupZombifiedPiglinGoals((ZombifiedPiglin) entity));
		setupsRegistry.put(EntityType.CAVE_SPIDER, entity -> setupCaveSpiderGoals((CaveSpider) entity));
		setupsRegistry.put(EntityType.WITHER_SKELETON,
				entity -> setupWitherSkeletonGoals((WitherSkeleton) entity));
	}

	public void setupGoals(Mob entity) {
		if (!setupsRegistry.containsKey(entity.getType()))
			return;
		setupsRegistry.get(entity.getType()).accept(entity);
	}

	public void setupZombieGoals(Zombie entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(RandomLookAroundGoal.class,
				Zombie.ZombieAttackTurtleEggGoal.class, LookAtPlayerGoal.class, MoveThroughVillageGoal.class,
				WaterAvoidingRandomStrollGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class, NearestAttackableTargetGoal.class));

		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, worldManager.getRaidManager()));
		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, worldManager.getRaidManager()).setAlertOthers(ZombifiedPiglin.class));
		entity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entity, Player.class, true));
		entity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(entity, IronGolem.class, true));

		entity.setPersistenceRequired();
	}

	public void setupSpiderGoals(Spider entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(FloatGoal.class, WaterAvoidingRandomStrollGoal.class, LookAtPlayerGoal.class, RandomLookAroundGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, worldManager.getRaidManager()));
		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, worldManager.getRaidManager()));

		entity.setPersistenceRequired();
	}

	public void setupSkeletonGoals(Skeleton entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtPlayerGoal.class, RandomLookAroundGoal.class,
				WaterAvoidingRandomStrollGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class, NearestAttackableTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, worldManager.getRaidManager()));
		entity.goalSelector.addGoal(1,
				new AttackBlockRangedGoal<Skeleton>(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entity, Player.class, true));
		entity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(entity, IronGolem.class, true));

		entity.setPersistenceRequired();
	}

	public void setupPhantomGoals(Phantom entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList();
		removeGoalsFromList(entity, goalClassesToRemove);

		entity.goalSelector.addGoal(1, new MoveTowardsNexusPhantomGoal(entity, worldManager.getRaidManager()));

		entity.setPersistenceRequired();
	}

	public void setupZombifiedPiglinGoals(ZombifiedPiglin entity) {
		// remove unwanted goals
		removeGoalsFromList(entity, Arrays.asList(WaterAvoidingRandomStrollGoal.class));
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class));

		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, worldManager.getRaidManager()));
		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(1,
				new HurtByNotRaidingTargetGoal(entity, worldManager.getRaidManager()).setAlertOthers(ZombifiedPiglin.class));

		entity.setPersistenceRequired();
	}

	public void setupCaveSpiderGoals(CaveSpider entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtPlayerGoal.class, RandomLookAroundGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, worldManager.getRaidManager()));
		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, worldManager.getRaidManager()));

		entity.setPersistenceRequired();
	}

	public void setupWitherSkeletonGoals(WitherSkeleton entity) {
		// remove unwanted goals
		final List<Class<? extends Goal>> goalClassesToRemove = Arrays.asList(LookAtPlayerGoal.class, RandomLookAroundGoal.class,
				WaterAvoidingRandomStrollGoal.class);
		removeGoalsFromList(entity, goalClassesToRemove);
		removeTargetsFromList(entity, Arrays.asList(HurtByTargetGoal.class, NearestAttackableTargetGoal.class));

		entity.goalSelector.addGoal(1, new MoveTowardsNexusGoal<>(entity, worldManager.getRaidManager()));
		entity.goalSelector.addGoal(1, new AttackBlockMeleeGoal<>(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(1, new HurtByNotRaidingTargetGoal(entity, worldManager.getRaidManager()));
		entity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(entity, Player.class, true));
		entity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(entity, IronGolem.class, true));

		entity.setPersistenceRequired();
	}

	/**
	 * Removes the specified AI goals from the given entity.
	 * 
	 * @param entity              the entity from which to remove the goals
	 * @param goalClassesToRemove a list of classes of the types of goals to remove
	 */
	private void removeGoalsFromList(Mob entity, final List<Class<? extends Goal>> goalClassesToRemove) {
		entity.goalSelector.getAvailableGoals().removeIf(goal -> goalClassesToRemove.contains(goal.getGoal().getClass()));
	}

	/**
	 * Removes the specified AI target goals from the given entity.
	 * 
	 * @param entity                the entity from which to remove the goals
	 * @param targetClassesToRemove a list of classes of the types of goals to
	 *                              remove
	 */
	private void removeTargetsFromList(Mob entity,
			final List<Class<? extends TargetGoal>> targetClassesToRemove) {
		entity.targetSelector.getAvailableGoals().removeIf(goal -> targetClassesToRemove.contains(goal.getGoal().getClass()));
	}
}
