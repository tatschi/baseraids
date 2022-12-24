package may.baseraids.config;

import may.baseraids.Baseraids;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * This class defines all options that are available in the config file and their initial values.
 * 
 * @author Natascha May
 */
public class ConfigOptions {

	// RAID MANAGER
	public static ForgeConfigSpec.IntValue timeBetweenRaids;
	public static ForgeConfigSpec.IntValue maxRaidDuration;	
	public static ForgeConfigSpec.BooleanValue deactivateMonsterNightSpawn;
	public static Vector3i lootChestPositionRelative;
	
	public static ForgeConfigSpec.IntValue monsterBlockBreakingTimeMultiplier;
	public static ForgeConfigSpec.BooleanValue restoreDestroyedBlocks;
	
	// SOUNDS
	public static ForgeConfigSpec.BooleanValue enableSoundWinLose;
	public static ForgeConfigSpec.BooleanValue enableSoundRaidHeartbeat;
	public static ForgeConfigSpec.BooleanValue enableSoundCountdown;
	public static ForgeConfigSpec.BooleanValue enableSoundNexusAmbient;
	
	protected final static int MONSTER_BLOCK_BREAKING_TIME_MULTIPLIER_DEFAULT = 2;
	public static final int[] LOOT_CHEST_POS_RELATIVE_DEFAULT = {0, 1, 0};
	
	
	public static void init(ForgeConfigSpec.Builder builder) {
		builder.comment("Baseraids Config");
		
		timeBetweenRaids = builder
				.comment("Time between two raids in ticks. 10min is 12000, 1min is 1200.")
				.defineInRange(Baseraids.MODID + ".timeBetweenRaids", 72000, 0, (int) 1e7);
		
		maxRaidDuration = builder
				.comment("Maximum time a raid can last in ticks. When this limit is reached, the raid is won, because the nexus survived! 1min is 1200")
				.defineInRange(Baseraids.MODID + ".maxRaidDuration", 3600, 0, (int) 11000);
		
		deactivateMonsterNightSpawn = builder
				.comment("If set to true, the mod deactivates monster spawning at night. They will still spawn in caves.")
				.define(Baseraids.MODID + ".deactivateMonsterNightSpawn", true);
		
		monsterBlockBreakingTimeMultiplier = builder
				.comment("Time it takes the monsters to break a block. Doubling the value will double the time. ")
				.defineInRange(Baseraids.MODID + ".monsterBlockBreakingTimeMultiplier", MONSTER_BLOCK_BREAKING_TIME_MULTIPLIER_DEFAULT, 0, 100);
		
		restoreDestroyedBlocks = builder
				.comment("If set to true, the mod restores all blocks that were broken during the raid."
						+ "No matter the value, you can always restore the blocks using a command.")
				.define(Baseraids.MODID + ".restoreDestroyedBlocks", false);
		
		enableSoundWinLose = builder
				.comment("If set to true, the sounds after winning or losing a raid will be played.")
				.define(Baseraids.MODID + ".enableSoundWinLose", true);
		
		enableSoundRaidHeartbeat = builder
				.comment("If set to true, the heartbeat sound during a raid will be played.")
				.define(Baseraids.MODID + ".enableSoundRaidHeartbeat", true);
		
		enableSoundCountdown = builder
				.comment("If set to true, the sounds during the countdown before a raid will be played.")
				.define(Baseraids.MODID + ".enableSoundCountdown", true);
		
		enableSoundNexusAmbient = builder
				.comment("If set to true, the ambient sound of the nexus will be played.")
				.define(Baseraids.MODID + ".enableSoundNexusAmbient", true);
		
		lootChestPositionRelative = new Vector3i(LOOT_CHEST_POS_RELATIVE_DEFAULT[0], LOOT_CHEST_POS_RELATIVE_DEFAULT[1], LOOT_CHEST_POS_RELATIVE_DEFAULT[2]);
	}	
	
}

