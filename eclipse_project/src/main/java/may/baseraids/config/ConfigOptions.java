package may.baseraids.config;

import java.util.HashMap;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import may.baseraids.entities.BaseraidsSkeletonEntity;
import may.baseraids.entities.BaseraidsSpiderEntity;
import may.baseraids.entities.BaseraidsZombieEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/**
 * Defines all options that are available in the config file and their initial values.
 * 
 *
 */
public class ConfigOptions {

	// RAID MANAGER
	public static ForgeConfigSpec.IntValue timeBetweenRaids;
	public static ForgeConfigSpec.IntValue maxRaidDuration;
	public static HashMap<Integer, ArrayToAmountOfMobsHashMap> amountOfMobs = new HashMap<Integer, ArrayToAmountOfMobsHashMap>();
	public static ForgeConfigSpec.BooleanValue deactivateMonsterNightSpawn;
	public static ArrayToVector lootChestPositionRelative;
	
	
	
	public static final int[][] AMOUNT_OF_MOBS_DEFAULT = {{3, 1, 0}, {5, 2, 2}, {8, 4, 4}};
	
	// TODO registry objects not available during config loading
	public static final String[] ORDER_OF_ENTITY_TYPES_IN_CONFIG_ARRAY =
		{	BaseraidsZombieEntity.CONFIG_NAME,
			BaseraidsSkeletonEntity.CONFIG_NAME,
			BaseraidsSpiderEntity.CONFIG_NAME};	
	public static final String ORDER_OF_ENTITY_TYPES_IN_CONFIG_ARRAY_AS_STRING = String.join(", ", ORDER_OF_ENTITY_TYPES_IN_CONFIG_ARRAY);
	
	public static final int[] LOOT_CHEST_POS_RELATIVE_DEFAULT = {0, 1, 0};
	
	
	public static void init(ForgeConfigSpec.Builder builder) {
		builder.comment("Baseraids Config");
		
		timeBetweenRaids = builder
				.comment("Time between two raids in ticks. 10min is 12000, 1min is 1200.")
				.defineInRange(Baseraids.MODID + ".timeBetweenRaids", 24000, 0, (int) 1e7);
		
		maxRaidDuration = builder
				.comment("Maximum time a raid can last in ticks. When this limit is reached, the raid is won, because the nexus survived! 1min is 1200")
				.defineInRange(Baseraids.MODID + ".maxRaidDuration", 3600, 0, (int) 1e7);
		
		
		for (int i = 0; i < RaidManager.MAX_RAID_LEVEL; i++) {
			/*
			amountOfMobs.add(i, builder
					.comment("Amount of mobs to spawn in a raid of level " + i + ". List in this order: " + ORDER_OF_MOBS_IN_ARRAY_AS_STRING)
					.define(Baseraids.MODID + ".amountOfMobsLevel" + i, AMOUNT_OF_MOBS_DEFAULT[i]));
			*/
			
			
			amountOfMobs.put(i,
					ArrayToAmountOfMobsHashMap
					.define(builder, Baseraids.MODID + ".amountOfMobsLevel" + i, AMOUNT_OF_MOBS_DEFAULT[i],
					"Amount of mobs to spawn in a raid of level " + i + ". List in this order: " + ORDER_OF_ENTITY_TYPES_IN_CONFIG_ARRAY_AS_STRING));
			
			
		}
		
		
		
		
		
		deactivateMonsterNightSpawn = builder
				.comment("If set to true, the mod deactivates monster spawning at night. They will still spawn in caves.")
				.define(Baseraids.MODID + ".deactivateMonsterNightSpawn", true);
		
		lootChestPositionRelative = ArrayToVector
				.define(builder, Baseraids.MODID + ".lootChestPositionRelative", LOOT_CHEST_POS_RELATIVE_DEFAULT,
						"Set the position of the loot chest that will be spawned after winning a raid. Give coordinates relative to the nexus position in (x, y, z).");
	}
	
	/**
	 * Defines a custom config value that creates an integer array as a config option, but returns a HashMap of EntityTypes over Integer upon reading with get().
	 * The usage here is to define the amount of mobs to spawn for a raid by using an array in the config file, but reading the values from a map.
	 *
	 */
	public static class ArrayToAmountOfMobsHashMap{
		
		ForgeConfigSpec.ConfigValue<int[]> array;
		
		public static ArrayToAmountOfMobsHashMap define(Builder parent, String path, int[] defaultValue, String comment) {
			ConfigValue<int[]> array = parent
					.comment(comment)
					.define(path, defaultValue);
			ArrayToAmountOfMobsHashMap map = new ArrayToAmountOfMobsHashMap();
			map.array = array;
			return map;
		}
		
		public HashMap<EntityType<?>, Integer> get(){
			return convertArrayToMapOfEntityTypeOverNumber(array.get());
		}
		
		
		/**
		 * Converts an integer array into a HashMap that maps an EntityType to an integer. The corresponding EntityType can be found using the ORDER_OF_ENTITY_TYPES_IN_CONFIG_ARRAY and the Map Baseraids.configRegister.  
		 * @param array The array to be converted into a HashMap
		 * @return null if Baseraids.configRegister is empty
		 */
		private HashMap<EntityType<?>, Integer> convertArrayToMapOfEntityTypeOverNumber(int[] array){
			if(Baseraids.configRegister.isEmpty()) return null;
			
			HashMap<EntityType<?>, Integer> map = new HashMap<EntityType<?>, Integer>();
			
			for (int i = 0; i < array.length; i++) {
				map.put(Baseraids.configRegister.get(ORDER_OF_ENTITY_TYPES_IN_CONFIG_ARRAY[i]), array[i]);
			}
			return map;
		}
		
	}
	
	/**
	 * Defines a custom config value that creates an integer array as a config option, but returns a Vector3i upon reading with get().
	 *
	 */
	public static class ArrayToVector{
		
		ForgeConfigSpec.ConfigValue<int[]> array;
		
		public static ArrayToVector define(Builder parent, String path, int[] defaultValue, String comment) {
			ConfigValue<int[]> array = parent
					.comment(comment)
					.define(path, defaultValue);
			ArrayToVector vec = new ArrayToVector();
			vec.array = array;
			return vec;
		}
		
		public Vector3i get(){
			int[] array = this.array.get();
			if(array.length != 3) {
				throw new RuntimeException("Failed retrieving config value: array not of correct length");
			}
			return new Vector3i(array[0], array[1], array[2]);
		}
		
	}
	
	
}

