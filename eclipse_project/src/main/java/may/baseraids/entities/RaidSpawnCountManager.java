package may.baseraids.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import may.baseraids.RaidSpawnCountPerWaveRegistry;

public class RaidSpawnCountManager {

	private static Map<EntityType<? extends Mob>, int[]> entitySpawnCountPerPlayerRegistry = new HashMap<>();
	
	private static final int[] SPAWN_COUNT_ZOMBIES = 			{8, 8, 7, 5, 5, 5, 5, 5, 3, 3};
	private static final int[] SPAWN_COUNT_SKELETONS = 			{0, 2, 2, 4, 5, 5, 8, 5, 5, 5};
	private static final int[] SPAWN_COUNT_SPIDERS = 			{0, 0, 2, 5, 5, 5, 5, 8, 5, 5};
	private static final int[] SPAWN_COUNT_PHANTOMS = 			{0, 0, 0, 0, 0, 1, 1, 3, 3, 5};
	private static final int[] SPAWN_COUNT_ZOMBIFIED_PIGLINS = 	{0, 0, 0, 0, 2, 3, 5, 8, 8, 8};
	private static final int[] SPAWN_COUNT_WITHER_SKELETONS = 	{0, 0, 0, 0, 0, 0, 0, 1, 2, 3};
	private static final int[] SPAWN_COUNT_CAVE_SPIDERS = 		{0, 0, 0, 0, 0, 0, 1, 1, 2, 3};
	
	private static final int MAX_MOB_AMOUNT_PER_WAVE = 20;
	/**
	 * Given a wave number, returns a map that contains the count to spawn for each entity type.
	 * The values must be registered at the start of each raid and emptied at the end of each raid with {@link registerSpawnCountsForLevelAndPlayerCount}.
	 */
	private static RaidSpawnCountPerWaveRegistry entitySpawnCountPerWaveRegistry = new RaidSpawnCountPerWaveRegistry();
	
	private RaidSpawnCountManager() {
		throw new IllegalStateException("Utility class");
	}
	
	public static void registerSpawnCounts() {
		entitySpawnCountPerPlayerRegistry.put(EntityType.ZOMBIE, SPAWN_COUNT_ZOMBIES);
		entitySpawnCountPerPlayerRegistry.put(EntityType.SKELETON, SPAWN_COUNT_SKELETONS);
		entitySpawnCountPerPlayerRegistry.put(EntityType.SPIDER, SPAWN_COUNT_SPIDERS);
		entitySpawnCountPerPlayerRegistry.put(EntityType.PHANTOM, SPAWN_COUNT_PHANTOMS);
		entitySpawnCountPerPlayerRegistry.put(EntityType.ZOMBIFIED_PIGLIN, SPAWN_COUNT_ZOMBIFIED_PIGLINS);
		entitySpawnCountPerPlayerRegistry.put(EntityType.WITHER_SKELETON, SPAWN_COUNT_WITHER_SKELETONS);
		entitySpawnCountPerPlayerRegistry.put(EntityType.CAVE_SPIDER, SPAWN_COUNT_CAVE_SPIDERS);
	}
		
	public static Set<EntityType<? extends Mob>> getEntityTypesToSpawn(){ // NOSONAR
		return entitySpawnCountPerPlayerRegistry.keySet();
	}
	
	public static int getSpawnCountForEntityAndLevel(EntityType<?> type, int level) {
		if(!entitySpawnCountPerPlayerRegistry.containsKey(type)) {
			return 0;
		}
		return entitySpawnCountPerPlayerRegistry.get(type)[level-1];
	}
	
	public static int getSpawnCountForEntityAndLevelAndPlayerCount(EntityType<?> type, int level, int playerCount) {
		return getSpawnCountForEntityAndLevel(type, level) * playerCount;
	}
	
	public static void registerSpawnCountsForLevelAndPlayerCount(int level, int playerCount) {
		Set<EntityType<? extends Mob>> entityTypesToSpawn = RaidSpawnCountManager.getEntityTypesToSpawn();
		var totalMobsToSpawn = new HashMap<EntityType<? extends Mob>, Integer>();
		entityTypesToSpawn.forEach(t -> {
			int count = RaidSpawnCountManager.getSpawnCountForEntityAndLevelAndPlayerCount(t, level,
					playerCount);
			totalMobsToSpawn.put(t, count);
		});
		
		int totalMobCount = 0;
		for(var val : totalMobsToSpawn.values()){
			totalMobCount += val;
		}
		
		int numOfWaves = totalMobCount / MAX_MOB_AMOUNT_PER_WAVE + 1;
		for(var t : entityTypesToSpawn) {
			for(int i = 1; i < numOfWaves; i++) {
				int count = totalMobsToSpawn.get(t) / numOfWaves;
				entitySpawnCountPerWaveRegistry.put(i, t, count);
			}
			int count = totalMobsToSpawn.get(t) % numOfWaves;
			entitySpawnCountPerWaveRegistry.put(numOfWaves, t, count);
		}				
	}
	
	/**
	 * Gets the amount of mobs to spawn for the given entity type and wave number that is registered in {@link RaidSpawnCountManager#entitySpawnCountPerWaveRegistry}.
	 * @param type
	 * @param wave
	 * @return The amount of mobs to spawn or null if there is none registered for the type and wave
	 */
	public static Integer getSpawnCountForEntityAndWave(EntityType<?> type, int wave) {		
		return entitySpawnCountPerWaveRegistry.get(wave).get(type);
	}
	
	public static int getMaxWave() {
		return entitySpawnCountPerWaveRegistry.getMaxWave();
	}

}
