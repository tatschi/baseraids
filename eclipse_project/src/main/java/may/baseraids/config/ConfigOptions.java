package may.baseraids.config;

import java.util.List;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigOptions {

	// RAID MANAGER
	public static ForgeConfigSpec.IntValue timeBetweenRaids;
	public static ForgeConfigSpec.IntValue maxRaidDuration;
	public static List<ForgeConfigSpec.ConfigValue<int[]>> amountOfMobs;
	public static ForgeConfigSpec.BooleanValue deactivateMonsterNightSpawn;
	public static ForgeConfigSpec.ConfigValue<Vector3i> lootChestPositionRelative;
	
	
	public static final int[][] AMOUNT_OF_MOBS_DEFAULT = {{3, 1, 0}, {5, 2, 2}, {8, 4, 4}};
	public static final EntityType<?>[] ORDER_OF_MOBS_IN_ARRAY =
		{	Baseraids.BASERAIDS_ZOMBIE_ENTITY_TYPE.get(),
			Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(),
			Baseraids.BASERAIDS_SPIDER_ENTITY_TYPE.get()};	
	public static final String ORDER_OF_MOBS_IN_ARRAY_AS_STRING = "Zombies, Skeletons, Spiders";
	
	
	
	
	public static void init(ForgeConfigSpec.Builder builder) {
		builder.comment("Baseraids Config");
		
		timeBetweenRaids = builder
				.comment("Time between two raids in ticks. 10min is 12000, 1min is 1200.")
				.defineInRange(Baseraids.MODID + ".timeBetweenRaids", 24000, 0, (int) 1e7);
		
		maxRaidDuration = builder
				.comment("Maximum time a raid can last in ticks. When this limit is reached, the raid is won, because the nexus survived! 1min is 1200")
				.defineInRange(Baseraids.MODID + ".maxRaidDuration", 3600, 0, (int) 1e7);
		
		
		for (int i = 0; i < RaidManager.MAX_RAID_LEVEL; i++) {
			amountOfMobs.add(i, builder
					.comment("Amount of mobs to spawn in a raid of level " + i + ". List in this order: " + ORDER_OF_MOBS_IN_ARRAY_AS_STRING)
					.define(Baseraids.MODID + ".amountOfMobsLevel" + i, AMOUNT_OF_MOBS_DEFAULT[i]));
		}
		
		deactivateMonsterNightSpawn = builder
				.comment("If set to true, the mod deactivates monster spawning at night. They will still spawn in caves.")
				.define(Baseraids.MODID + ".deactivateMonsterNightSpawn", true);
		
		lootChestPositionRelative = builder
				.comment("Set the position of the loot chest that will be spawned after winning a raid. Give coordinates relative to the nexus position in (x, y, z).")
				.define(Baseraids.MODID + ".lootChestPositionRelative", new Vector3i(0, 1, 0));
	}
}
