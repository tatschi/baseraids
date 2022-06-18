package may.baseraids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jline.utils.Log;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

/**
 * This class controls everything concerning the spawning during raids: what and
 * how many mobs to spawn, where to spawn, despawning, saving and loading
 * spawned mobs and more.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1.1
 */
public class RaidSpawningManager {

	private World world;
	private RaidManager raidManager;
	/**
	 * Contains all mobs currently spawned by this manager.
	 */
	private List<MobEntity> spawnedMobs = new ArrayList<MobEntity>();;
	private boolean hasFinishedSpawning = false;

	// the distinction between AMOUNT_OF_MOBS_DEFAULT and amountOfMobs is because this is planned to be configurable
	private static final int[][] AMOUNT_OF_MOBS_DEFAULT = { { 4, 0, 0 }, { 2, 4, 0 }, { 3, 3, 4 }, { 8, 4, 4 },
			{ 12, 6, 4 } };
	/**
	 * Defines in which order the amounts in AMOUNT_OF_MOBS_DEFAULT are defined (i.e. which amount corresponds to which EntityType).
	 */
	private static final EntityType<?>[] ORDER_OF_MOBS_IN_ARRAY = { Baseraids.BASERAIDS_ZOMBIE_ENTITY_TYPE.get(),
			Baseraids.BASERAIDS_SKELETON_ENTITY_TYPE.get(), Baseraids.BASERAIDS_SPIDER_ENTITY_TYPE.get() };
	private static HashMap<Integer, HashMap<EntityType<?>, Integer>> amountOfMobs = new HashMap<Integer, HashMap<EntityType<?>, Integer>>();

	public RaidSpawningManager(RaidManager raidManager, World world) {
		this.raidManager = raidManager;
		this.world = world;
		setAmountOfMobsToSpawn();
	}

	/** 
	 * Fills the hashmap amountOfMobs according to the setting in AMOUNT_OF_MOBS_DEFAULT.
	 * This extra step is currently unnecessary but will be useful if the amount of mobs is configurable (which is planned). 
	 */
	private void setAmountOfMobsToSpawn() {
		for (int curLevel = 0; curLevel < RaidManager.MAX_RAID_LEVEL; curLevel++) {
			HashMap<EntityType<?>, Integer> hashMapForCurLevel = new HashMap<EntityType<?>, Integer>();
			for (int curMob = 0; curMob < ORDER_OF_MOBS_IN_ARRAY.length; curMob++) {
				hashMapForCurLevel.put(ORDER_OF_MOBS_IN_ARRAY[curMob], AMOUNT_OF_MOBS_DEFAULT[curLevel][curMob]);
			}
			amountOfMobs.put(curLevel + 1, hashMapForCurLevel);
		}
	}

	/**
	 * Starts the spawning process of raid mobs using the <code>amountOfMobs</code> and enters the spawned mobs into <code>spawnedMobs</code>.
	 */
	void spawnRaidMobs() {		
		HashMap<EntityType<?>, Integer> amountOfMobsToSpawn = amountOfMobs.get(raidManager.getRaidLevel());
		if (amountOfMobs == null) {
			Baseraids.LOGGER.error("Error while reading the amount of mobs to spawn: HashMap was null");
			return;
		}
		
		amountOfMobsToSpawn.forEach((type, num) -> spawnedMobs.addAll(Arrays.asList(spawnSpecificEntities(type, num))));
		Baseraids.LOGGER.info("Spawned all entities for the raid");
	}

	private <T extends Entity> MobEntity[] spawnSpecificEntities(EntityType<T> entityType, int numMobs) {
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

	boolean areAllSpawnedMobsDead() {
		if (spawnedMobs.isEmpty())
			return true;
		for (MobEntity mob : spawnedMobs) {
			if (mob.isAlive()) {
				return false;
			}
		}
		return true;
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

	CompoundNBT writeAdditional() {
		CompoundNBT nbt = new CompoundNBT();

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
		
		//nbt.putBoolean("hasFinishedSpawning", hasFinishedSpawning);
		return nbt;
	}

	void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		Minecraft.getInstance().enqueue(() -> readSpawnedMobsList(nbt, serverWorld));
	}

	void killAllMobs() {
		spawnedMobs.forEach(mob -> mob.onKillCommand());
		spawnedMobs.clear();
	}

}
