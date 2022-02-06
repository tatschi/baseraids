package may.baseraids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jline.utils.Log;

import com.google.common.collect.Sets;

import may.baseraids.NexusBlock.State;
import may.baseraids.config.ConfigOptions;
import may.baseraids.sounds.SoundEffect;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class controls everything concerning raids: spawning, timers, ending a raid, rewards and more.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1
 */
// @Mod.EventBusSubscriber annotation automatically registers STATIC event handlers 
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaidManager {

	// RUNTIME VARIABLES
	private World world = null;
	public boolean isInitialized = false;

	private boolean isRaidActive;
	private int tick = 0;
	private int lastTickGameTime = -1;
	private int curRaidLevel;
	private int lastRaidGameTime;

	private List<MobEntity> spawnedMobs = new ArrayList<MobEntity>();

	// RAID SETTINGS
	public static final int MAX_RAID_LEVEL = 5, MIN_RAID_LEVEL = 1;
	private static final int START_OF_NIGHT_IN_WORLD_DAY_TIME = 13000; // defines the world.daytime at which it starts
																		// to be night (one day == 24000)

	private static final int[][] AMOUNT_OF_MOBS_DEFAULT = { { 4, 0, 0 }, { 2, 4, 0 }, { 3, 3, 4 }, { 8, 4, 4 },
			{ 12, 6, 4 } };
	private static HashMap<Integer, HashMap<EntityType<?>, Integer>> amountOfMobs = new HashMap<Integer, HashMap<EntityType<?>, Integer>>();

	// stores the amount of mobs to spawn for each raid level and mob using <amount,
	// Entry<raidlevel, mobname>>
	// private HashMap<Entry<Integer, String>, Integer> amountOfMobsToSpawn = new
	// HashMap<Entry<Integer, String>, Integer>();
	// private HashMap<Integer, HashMap<EntityType<?>, Integer>>
	// amountOfMobsToSpawn;

	// sets the times (remaining time until raid) for when to warn all players of
	// the coming raid (approximated, in seconds)
	private Set<Integer> warnAllPlayersOfRaidTimes = Sets.newHashSet(18000, 6000, 1200, 600, 300, 60, 30, 10, 5, 4, 3,
			2, 1);

	private static final ResourceLocation[] LOOTTABLES = { new ResourceLocation(Baseraids.MODID, "chests/raid_level_1"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_2"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_3"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_4"),
			new ResourceLocation(Baseraids.MODID, "chests/raid_level_5") };
	
	private static final SoundEffect SOUND_DURING_RAID = new SoundEffect(SoundEvents.BLOCK_BELL_USE, SoundCategory.AMBIENT, 5.0F, 0.1F, 60);
	private static final SoundEffect SOUND_RAID_WARNING = new SoundEffect(SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F, 1, 0);

	public RaidManager() {
		MinecraftForge.EVENT_BUS.register(this);
		setDefaultWriteParameters();
		// amountOfMobsToSpawn = new HashMap<Integer, HashMap<EntityType<?>,
		// Integer>>();
		setAmountOfMobsToSpawn();
		Baseraids.LOGGER.info("RaidManager created");
	}

	private void setAmountOfMobsToSpawn() {
		final EntityType<?>[] ORDER_OF_MOBS_IN_ARRAY = { Baseraids.BASERAIDS_ZOMBIE_ENTITY_TYPE.get(),
				Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(), Baseraids.BASERAIDS_SPIDER_ENTITY_TYPE.get() };

		for (int curLevel = 0; curLevel < MAX_RAID_LEVEL; curLevel++) {
			HashMap<EntityType<?>, Integer> hashMapForCurLevel = new HashMap<EntityType<?>, Integer>();

			for (int curMob = 0; curMob < ORDER_OF_MOBS_IN_ARRAY.length; curMob++) {
				hashMapForCurLevel.put(ORDER_OF_MOBS_IN_ARRAY[curMob], AMOUNT_OF_MOBS_DEFAULT[curLevel][curMob]);
			}

			amountOfMobs.put(curLevel + 1, hashMapForCurLevel);
		}

	}

	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		if (!isInitialized)
			return;
		if (event.phase != TickEvent.Phase.START)
			return;
		if (world == null)
			world = event.world;
		if (world.isRemote())
			return;
		if (!world.getDimensionKey().equals(World.OVERWORLD))
			return;
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			if (isRaidActive()) {
				endRaid();
			}
			return;
		}

		if (lastTickGameTime == -1) {
			lastTickGameTime = (int) world.getGameTime();
		}
		
		warnPlayersOfRaid();
		if (shouldStartRaid()) {
			startRaid();
		}
		if (isRaidActive()) {
			activeRaidTick();
		}
		
		tick = (tick+1) % 1000;
	}

	// warn players at specified times
	private void warnPlayersOfRaid() {
		int timeUntilRaidInSec = getTimeUntilRaidInSec();
		if (!warnAllPlayersOfRaidTimes.stream().anyMatch(time -> time == timeUntilRaidInSec)) {
			return;
		}
		
		int displayTimeMin = (int) timeUntilRaidInSec / 60;
		int displayTimeSec = timeUntilRaidInSec % 60;
		String displayTime = "";
		if(displayTimeMin > 0) {
			displayTime += displayTimeMin + "min";
		}
		if(displayTimeSec > 0) {
			displayTime += displayTimeSec + "s";
		}
		Baseraids.sendChatMessage("Time until next raid: " + displayTime);
		if (timeUntilRaidInSec < 5) {
			SOUND_RAID_WARNING.playSound(world, null, NexusBlock.getBlockPos());
		}
	}

	private boolean shouldStartRaid() {
		if (getTimeSinceRaid() < ConfigOptions.timeBetweenRaids.get()) {
			return false;
		}
		if (world.getDayTime() % 24000 < START_OF_NIGHT_IN_WORLD_DAY_TIME) {
			return false;
		}
		if(NexusBlock.getState() != State.BLOCK) {
			return false;
		}
		return true;
	}
	
	private void activeRaidTick() {
		// HANDLE SOUND
		if (isRaidActive && tick % SOUND_DURING_RAID.intervalInTicks == 0) {
			SOUND_DURING_RAID.playSound(world, null, NexusBlock.getBlockPos());
		}

		// CHECK IF RAID IS WON
		if (isRaidWon()) {
			Baseraids.LOGGER.info("Raid ended: all mobs are dead");
			winRaid();
		}

		// END RAID AFTER MAX DURATION
		if (isRaidActive && getTimeSinceRaid() > ConfigOptions.maxRaidDuration.get()) {
			Baseraids.LOGGER.info("Raid ended: reached max duration");
			winRaid();
		}
	}

	public void startRaid() {
		if (world == null) {
			Baseraids.LOGGER.warn("Could not start raid because world == null");
			return;
		}
		if (!world.getDimensionKey().equals(World.OVERWORLD))
			return;

		
		Baseraids.sendChatMessage("You are being raided!");
		Baseraids.LOGGER.info("Initiating raid");

		setLastRaidGameTime((int) (world.getGameTime()));
		setRaidActive(true);

		// SPAWNING
		HashMap<EntityType<?>, Integer> amountOfMobsToSpawn = amountOfMobs.get(curRaidLevel);
		if (amountOfMobs == null) {
			Baseraids.LOGGER.error("Error while reading the amount of mobs to spawn: HashMap was null");
		}
		amountOfMobsToSpawn.forEach((type, num) -> spawnedMobs.addAll(Arrays.asList(spawnRaidMobs(type, num))));
		Baseraids.LOGGER.info("Spawned all entities for the raid");
	}

	private <T extends Entity> MobEntity[] spawnRaidMobs(EntityType<T> entityType, int numMobs) {
		int radius = 50;
		double angleInterval = 2 * Math.PI / 100;
		BlockPos centerSpawnPos = NexusBlock.getBlockPos().add(0, 1, 0);

		ILivingEntityData ilivingentitydata = null;
		MobEntity[] mobs = new MobEntity[numMobs];
		for (int i = 0; i < numMobs; i++) {

			// find random coordinates in a circle around the nexus to spawn the current mob
			Random r = new Random();
			int randomAngle = r.nextInt(100);
			double angle = randomAngle * angleInterval;

			int x = (int) (radius * Math.cos(angle));
			int z = (int) (radius * Math.sin(angle));
			BlockPos spawnPosXZ = centerSpawnPos.add(x, 0, z);

			// find the right height
			BlockPos spawnPos;
			if (EntitySpawnPlacementRegistry.getPlacementType(entityType)
					.equals((EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS))) {
				spawnPos = world.getHeight(Heightmap.Type.WORLD_SURFACE, spawnPosXZ).add(0, 5, 0);
			} else {
				spawnPos = world.getHeight(Heightmap.Type.WORLD_SURFACE, spawnPosXZ);
			}

			Baseraids.LOGGER.debug(
					"Spawn " + entityType.getName().getString() + " at radius " + radius + " and angle " + angle);

			if (EntitySpawnPlacementRegistry.canSpawnEntity(entityType, (IServerWorld) world, SpawnReason.MOB_SUMMONED,
					spawnPos, r)) {

				if (entityType.equals(Baseraids.BASERAIDS_PHANTOM_ENTITY_TYPE.get())) {
					mobs[i] = EntityType.PHANTOM.create(world);
					mobs[i].moveToBlockPosAndAngles(spawnPos, 0.0F, 0.0F);
					ilivingentitydata = mobs[i].onInitialSpawn((IServerWorld) world,
							world.getDifficultyForLocation(spawnPos), SpawnReason.NATURAL, ilivingentitydata,
							(CompoundNBT) null);
					((IServerWorld) world).func_242417_l(mobs[i]);
				} else {
					mobs[i] = (MobEntity) entityType.spawn((ServerWorld) world, null, null, spawnPos,
							SpawnReason.MOB_SUMMONED, false, false);
				}

			} else {
				Baseraids.LOGGER.error("Couldn't spawn entity");
			}

		}
		return mobs;
	}

	public void loseRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid lost");
		Baseraids.sendChatMessage("You have lost the raid!");
		// make sure the raid level is adjusted before endRaid() because endRaid() uses
		// the new level
		resetRaidLevel();
		endRaid();

	}

	public void winRaid() {
		if (world == null)
			return;
		Baseraids.LOGGER.info("Raid won");
		Baseraids.sendChatMessage("You have won the raid!");

		// PLACE LOOT CHEST
		BlockPos chestPos = NexusBlock.getBlockPos().add(ConfigOptions.lootChestPositionRelative);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
		if (world.getTileEntity(chestPos) instanceof ChestTileEntity) {
			ChestTileEntity chestEntity = (ChestTileEntity) world.getTileEntity(chestPos);

			chestEntity.setLootTable(LOOTTABLES[curRaidLevel - 1], world.getRandom().nextLong());
			chestEntity.fillWithLoot(null);

			Baseraids.LOGGER.info("Added loot to loot chest");
		} else {
			Baseraids.LOGGER.error("Could not add loot to loot chest");
		}

		// make sure the raid level is adjusted before endRaid() because endRaid() uses
		// the new level
		increaseRaidLevel();
		endRaid();

		// PLAY SOUND EFFECT
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				1.5F);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				1.5F);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		world.playSound(null, NexusBlock.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 5.0F,
				2);

	}

	private void endRaid() {
		Baseraids.sendChatMessage("Your next raid will have level " + curRaidLevel);
		setRaidActive(false);
		spawnedMobs.forEach(mob -> mob.onKillCommand());
		spawnedMobs.clear();
		world.sendBlockBreakProgress(-1, NexusBlock.getBlockPos(), -1);
	}

	public int getTimeUntilRaidInSec() {
		// Math.floorMod returns only positive values (for a positive modulus) while %
		// returns the actual remainder
		return (int) Math.max((ConfigOptions.timeBetweenRaids.get() - getTimeSinceRaid()),
				Math.floorMod(START_OF_NIGHT_IN_WORLD_DAY_TIME - (world.getDayTime() % 24000), 24000)) / 20;
	}

	private boolean isRaidWon() {
		if (spawnedMobs.isEmpty())
			return false;
		for (MobEntity mob : spawnedMobs) {
			if (mob.isAlive()) {
				return false;
			}
		}
		return true;
	}

	private void increaseRaidLevel() {
		curRaidLevel++;
		if (curRaidLevel > MAX_RAID_LEVEL)
			curRaidLevel = MAX_RAID_LEVEL;
		markDirty();
	}

	private void resetRaidLevel() {
		curRaidLevel = MIN_RAID_LEVEL;
		markDirty();
	}

	public CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putInt("curRaidLevel", curRaidLevel);
		nbt.putInt("lastRaidGameTime", lastRaidGameTime);
		nbt.putBoolean("isRaidActive", isRaidActive);

		// spawned mobs
		ListNBT spawnedMobsList = new ListNBT();
		int index = 0;
		for (MobEntity mob : spawnedMobs) {
			CompoundNBT compound = new CompoundNBT();
			compound.putUniqueId("ID" + index, mob.getUniqueID());
			Baseraids.LOGGER.debug("writing UUID " + mob.getUniqueID());
			spawnedMobsList.add(compound);
			index++;
		}

		nbt.put("spawnedMobs", spawnedMobsList);

		return nbt;
	}

	public void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		try {
			lastRaidGameTime = nbt.getInt("lastRaidGameTime");
			curRaidLevel = nbt.getInt("curRaidLevel");
			isRaidActive = nbt.getBoolean("isRaidActive");

			Minecraft.getInstance().enqueue(() -> readSpawnedMobsList(nbt, serverWorld));

			Baseraids.LOGGER.debug("Finished loading RaidManager");

		} catch (Exception e) {
			Log.warn("Exception while reading data for RaidManager. Setting parameters to default. Exception: " + e);
			setDefaultWriteParameters();
			markDirty();
		}
	}

	private void readSpawnedMobsList(CompoundNBT nbt, ServerWorld serverWorld) {
		ListNBT spawnedMobsList = nbt.getList("spawnedMobs", 10);
		spawnedMobs.clear();
		int index = 0;
		for (INBT compound : spawnedMobsList) {
			CompoundNBT compoundNBT = (CompoundNBT) compound;
			Entity entity = serverWorld.getEntityByUuid(compoundNBT.getUniqueId("ID" + index));
			if (entity == null) {
				Log.warn("Could not read entity from data");
				continue;
			}
			if (!(entity instanceof MobEntity)) {
				Log.warn("Error while reading data for RaidManager: Read entity not of type MobEntity");
				continue;
			}

			spawnedMobs.add((MobEntity) entity);
			index++;
		}
	}

	private void setDefaultWriteParameters() {
		lastRaidGameTime = 0;
		curRaidLevel = 1;
		isRaidActive = false;
	}

	private void setLastRaidGameTime(int time) {
		lastRaidGameTime = time;
		markDirty();
	}

	public void setRaidActive(boolean active) {
		isRaidActive = active;
		markDirty();
	}

	public boolean isRaidActive() {
		return isRaidActive;
	}

	public void markDirty() {
		Baseraids.baseraidsData.markDirty();
	}

	public int getTimeSinceRaid() {
		if (world == null)
			return -1;
		return (int) (world.getGameTime()) - lastRaidGameTime;
	}

	public int getRaidLevel() {
		return curRaidLevel;
	}
}
