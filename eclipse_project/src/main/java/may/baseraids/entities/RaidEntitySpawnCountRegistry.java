package may.baseraids.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityType;

public class RaidEntitySpawnCountRegistry {

	private static Map<EntityType<?>, int[]> entitySpawnCountRegistry = new HashMap<EntityType<?>, int[]>();
	
	
	private static final int[] SPAWN_COUNT_ZOMBIES = 			{10, 10, 9, 7, 5, 15, 20, 20};
	private static final int[] SPAWN_COUNT_SKELETONS = 			{0, 3, 3, 5, 6, 10, 15, 15};
	private static final int[] SPAWN_COUNT_SPIDERS = 			{0, 0, 2, 4, 5, 8, 10, 10};
	private static final int[] SPAWN_COUNT_PHANTOMS = 			{0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] SPAWN_COUNT_ZOMBIFIED_PIGLINS = 	{0, 0, 4, 8, 12, 16, 16, 16};
	private static final int[] SPAWN_COUNT_WITHER_SKELETONS = 	{0, 0, 0, 0, 0, 0, 0, 1};
	private static final int[] SPAWN_COUNT_CAVE_SPIDERS = 		{0, 0, 0, 0, 0, 1, 1, 1};
	
	public static void registerSpawnCounts() {
		entitySpawnCountRegistry.put(EntityType.ZOMBIE, SPAWN_COUNT_ZOMBIES);
		entitySpawnCountRegistry.put(EntityType.SKELETON, SPAWN_COUNT_SKELETONS);
		entitySpawnCountRegistry.put(EntityType.SPIDER, SPAWN_COUNT_SPIDERS);
		entitySpawnCountRegistry.put(EntityType.PHANTOM, SPAWN_COUNT_PHANTOMS);
		entitySpawnCountRegistry.put(EntityType.ZOMBIFIED_PIGLIN, SPAWN_COUNT_ZOMBIFIED_PIGLINS);
		entitySpawnCountRegistry.put(EntityType.WITHER_SKELETON, SPAWN_COUNT_WITHER_SKELETONS);
		entitySpawnCountRegistry.put(EntityType.CAVE_SPIDER, SPAWN_COUNT_CAVE_SPIDERS);
	}
	
	public static Set<EntityType<?>> getEntityTypesToSpawn(){
		return entitySpawnCountRegistry.keySet();
	}
	
	public static int getSpawnCountForEntityAndLevel(EntityType<?> type, int level) {
		if(!entitySpawnCountRegistry.containsKey(type)) {
			return 0;
		}
		return entitySpawnCountRegistry.get(type)[level];
	}
	
	public static int getSpawnCountForEntityAndLevelAndPlayerCount(EntityType<?> type, int level, int playerCount) {
		return getSpawnCountForEntityAndLevel(type, level) * playerCount;
	}
}
