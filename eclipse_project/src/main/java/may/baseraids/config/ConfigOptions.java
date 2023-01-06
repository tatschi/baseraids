package may.baseraids.config;

import org.apache.commons.lang3.BooleanUtils;

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
	private static ForgeConfigSpec.IntValue timeBetweenRaids;
	private static ForgeConfigSpec.IntValue maxRaidDuration;	
	private static ForgeConfigSpec.BooleanValue deactivateMonsterNightSpawn;
	private static Vector3i lootChestPositionRelative;
	
	private static ForgeConfigSpec.IntValue monsterBlockBreakingTimeMultiplier;
	private static ForgeConfigSpec.BooleanValue restoreDestroyedBlocks;
	
	// SOUNDS
	private static ForgeConfigSpec.BooleanValue enableSoundWinLose;
	private static ForgeConfigSpec.BooleanValue enableSoundRaidHeartbeat;
	private static ForgeConfigSpec.BooleanValue enableSoundCountdown;
	private static ForgeConfigSpec.BooleanValue enableSoundNexusAmbient;
	private static ForgeConfigSpec.BooleanValue enableTimeReductionFromSleeping;
	
	private static final int MONSTER_BLOCK_BREAKING_TIME_MULTIPLIER_DEFAULT = 2;
	private static final int[] LOOT_CHEST_POS_RELATIVE_DEFAULT = {0, 1, 0};
	
	
	private ConfigOptions() {
		throw new IllegalStateException("Utility class");
	}
	
	public static void init(ForgeConfigSpec.Builder builder) {
		builder.comment("Baseraids Config");
		
		timeBetweenRaids = builder
				.comment("Time between two raids in ticks. 10min is 12000, 1min is 1200.")
				.defineInRange(Baseraids.MODID + ".timeBetweenRaids", 72000, 0, (int) 1e7);
		
		maxRaidDuration = builder
				.comment("Maximum time a raid can last in ticks. When this limit is reached, the raid is won, because the nexus survived! 1min is 1200")
				.defineInRange(Baseraids.MODID + ".maxRaidDuration", 3600, 0, 11000);
		
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
		
		enableTimeReductionFromSleeping = builder
				.comment("If set to true, the time until the next raid is reduced when you sleep in a bed.")
				.define(Baseraids.MODID + ".enableTimeReductionFromSleeping", false);
		
		lootChestPositionRelative = new Vector3i(LOOT_CHEST_POS_RELATIVE_DEFAULT[0], LOOT_CHEST_POS_RELATIVE_DEFAULT[1], LOOT_CHEST_POS_RELATIVE_DEFAULT[2]);
	}

	public static int getTimeBetweenRaids() {
		return timeBetweenRaids.get();
	}

	public static int getMaxRaidDuration() {
		return maxRaidDuration.get();
	}

	public static boolean getDeactivateMonsterNightSpawn() {
		return BooleanUtils.toBoolean(deactivateMonsterNightSpawn.get());
	}

	public static Vector3i getLootChestPositionRelative() {
		return lootChestPositionRelative;
	}

	public static int getMonsterBlockBreakingTimeMultiplier() {
		return monsterBlockBreakingTimeMultiplier.get();
	}

	public static boolean getRestoreDestroyedBlocks() {		
		return BooleanUtils.toBoolean(restoreDestroyedBlocks.get());
	}

	public static boolean getEnableSoundWinLose() {
		return BooleanUtils.toBoolean(enableSoundWinLose.get());
	}

	public static boolean getEnableSoundRaidHeartbeat() {
		return BooleanUtils.toBoolean(enableSoundRaidHeartbeat.get());
	}

	public static boolean getEnableSoundCountdown() {
		return BooleanUtils.toBoolean(enableSoundCountdown.get());
	}

	public static boolean getEnableSoundNexusAmbient() {
		return BooleanUtils.toBoolean(enableSoundNexusAmbient.get());
	}

	public static boolean getEnableTimeReductionFromSleeping() {
		return BooleanUtils.toBoolean(enableTimeReductionFromSleeping.get());
	}	
	
}

