package may.baseraids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import may.baseraids.entities.BaseraidsEntityManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class controls the spawning, saving and loading and other handling of
 * the raid mobs.
 * 
 * @author Natascha May
 * @since 1.16.4-0.0.0.1
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaidSpawningManager {

	private World world;
	private RaidManager raidManager;
	private List<MobEntity> spawnedMobs = new ArrayList<MobEntity>();
	/** UUIDs only used for saving and loading */
	private List<UUID> spawnedMobsUUIDs = new ArrayList<UUID>();

	private static final int[][] AMOUNT_OF_MOBS_DEFAULT = { { 10, 0, 0 }, { 10, 3, 0 }, { 10, 3, 2 }, { 12, 5, 4 },
			{ 15, 6, 5 }, { 25, 10, 8 }, { 30, 15, 10 } };
	private static HashMap<Integer, HashMap<EntityType<?>, Integer>> amountOfMobs = new HashMap<Integer, HashMap<EntityType<?>, Integer>>();

	public RaidSpawningManager(RaidManager raidManager, World world) {
		this.raidManager = raidManager;
		this.world = world;
		MinecraftForge.EVENT_BUS.register(this);
		setAmountOfMobsToSpawn();
	}

	/**
	 * Sets the amount of mobs to spawn at each raid level.
	 */
	void setAmountOfMobsToSpawn() {
		final EntityType<?>[] ORDER_OF_MOBS_IN_ARRAY = { EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER };

		for (int curLevel = 0; curLevel < RaidManager.MAX_RAID_LEVEL; curLevel++) {
			HashMap<EntityType<?>, Integer> hashMapForCurLevel = new HashMap<EntityType<?>, Integer>();

			for (int curMob = 0; curMob < ORDER_OF_MOBS_IN_ARRAY.length; curMob++) {
				hashMapForCurLevel.put(ORDER_OF_MOBS_IN_ARRAY[curMob], AMOUNT_OF_MOBS_DEFAULT[curLevel][curMob]);
			}

			amountOfMobs.put(curLevel + 1, hashMapForCurLevel);
		}

	}

	/**
	 * Spawns the mobs for the current level specified by <code>amountOfMobs</code>
	 * and inputs the spawned entities into the list <code>spawnedMobs</code>.
	 */
	void spawnRaidMobs() {
		HashMap<EntityType<?>, Integer> amountOfMobsToSpawn = amountOfMobs.get(raidManager.getRaidLevel());
		if (amountOfMobs == null) {
			Baseraids.LOGGER.error("Error while reading the amount of mobs to spawn: HashMap was null");
		}
		amountOfMobsToSpawn.forEach((type, num) -> {

			MobEntity[] spawnedMobsArray = spawnSpecificEntities(type, num);

			// remove nulls and convert to collection
			Collection<MobEntity> spawnedMobsNonNullCollection = Arrays.stream(spawnedMobsArray)
					.filter((entity) -> entity != null).collect(Collectors.toList());

			spawnedMobs.addAll(spawnedMobsNonNullCollection);

		});
		Baseraids.baseraidsData.setDirty(true);
		Baseraids.LOGGER.info("Spawned all entities for the raid");
	}

	/**
	 * Spawns <code>numMobs</code> mobs of the type <code>entityType</code>.
	 * 
	 * @param <T>        extends <code>Entity</code> the entity class corresponding
	 *                   to the <code>entityType</code>
	 * @param entityType
	 * @param numMobs    the amount of mobs to spawn of this type
	 * @return an array of <code>MobEntity</code> containing the spawned entities
	 */
	private <T extends Entity> MobEntity[] spawnSpecificEntities(EntityType<T> entityType, int numMobs) {

		ILivingEntityData ilivingentitydata = null;
		MobEntity[] mobs = new MobEntity[numMobs];
		for (int i = 0; i < numMobs; i++) {

			BlockPos spawnPos = findSpawnPos(entityType);

			if (entityType.equals(EntityType.PHANTOM)) {
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

			if (mobs[i] != null) {
				BaseraidsEntityManager.setupGoals(mobs[i]);
			}
		}
		return mobs;
	}

	/**
	 * Finds and returns a compatible spawn position for the given entity type. The
	 * position is selected randomly on a circle around the nexus block.
	 * 
	 * @param entityType
	 * @return compatible spawn position
	 */
	private BlockPos findSpawnPos(EntityType<?> entityType) {
		int radius = 50;
		double angleInterval = 2 * Math.PI / 100;
		BlockPos centerSpawnPos = NexusBlock.getBlockPos().add(0, 1, 0);

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
			spawnPos = world.getHeight(EntitySpawnPlacementRegistry.func_209342_b(entityType), spawnPosXZ);
		}

		Baseraids.LOGGER
				.debug("Spawn " + entityType.getName().getString() + " at radius " + radius + " and angle " + angle);
		return spawnPos;
	}

	/**
	 * @return <code>true</code>, if and only if all mobs that were spawned by this
	 *         object are dead.
	 */
	boolean areAllSpawnedMobsDead() {
		if (spawnedMobs.isEmpty()) {
			Baseraids.baseraidsData.setDirty(true);
			return false;
		}

		for (MobEntity mob : spawnedMobs) {
			if (mob.isAlive()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Kills all mobs that were spawned by this object.
	 */
	void killAllMobs() {
		spawnedMobs.forEach(mob -> {
			mob.remove();
		});
		spawnedMobs.clear();
		Baseraids.baseraidsData.setDirty(true);
	}

	/**
	 * Reads and stores the UUIDs of the mobs stored in the given <code>CompoundNBT</code>,
	 * so that they can be recovered when they are spawned into the world.
	 * This function assumes that the
	 * nbt was previously written by this class or to be precise, that the nbt
	 * includes certain elements.
	 * 
	 * @param nbt         the nbt that will be read out. It is assumed to include
	 *                    certain elements.
	 * @param serverWorld the world that is loaded. It is used in the
	 *                    <code>RaidSpawningManager</code> to get references to
	 *                    previously spawned mobs.
	 */
	private void readSpawnedMobsList(CompoundNBT nbt, ServerWorld serverWorld) {		
		ListNBT spawnedMobsList = nbt.getList("spawnedMobs", 10);
		spawnedMobs.clear();
		spawnedMobsUUIDs.clear();
		
		for(int index = 0; index < spawnedMobsList.size(); index++) {
			CompoundNBT compoundNBT = (CompoundNBT) spawnedMobsList.getCompound(index);
			UUID entityUUID = compoundNBT.getUniqueId("ID" + index);
			Baseraids.LOGGER.debug("reading entity with ID " + entityUUID);
			spawnedMobsUUIDs.add(entityUUID);			
		}
	}

	/**
	 * Saves the UUIDs of the spawned mobs to a <code>CompoundNBT</code> and returns
	 * the <code>CompoundNBT</code> object.
	 * 
	 * @return the adapted <code>CompoundNBT</code> that was written to
	 */
	CompoundNBT writeAdditional() {
		
		CompoundNBT nbt = new CompoundNBT();

		ListNBT spawnedMobsList = new ListNBT();
		int index = 0;
		for (MobEntity mob : spawnedMobs) {
			CompoundNBT compound = new CompoundNBT();
			
			compound.putUniqueId("ID" + index, mob.getUniqueID());
			Baseraids.LOGGER.debug("writing entity with UUID " + mob.getUniqueID());
			spawnedMobsList.add(compound);
			index++;
		}

		nbt.put("spawnedMobs", spawnedMobsList);
		return nbt;
	}

	
	
	void readAdditional(CompoundNBT nbt, ServerWorld serverWorld) {
		readSpawnedMobsList(nbt, serverWorld);
	}
	
	@SubscribeEvent
	public void onEntityJoinWorld(final EntityJoinWorldEvent event) {
		if(event.getWorld().isRemote()) {
			return;
		}
		if(!event.getWorld().equals(world)) {
			return;
		}
		
		Entity entity = event.getEntity();
		if(!(entity instanceof MobEntity)) {
			return;
		}
		
		UUID uuid = entity.getUniqueID();
		if(!spawnedMobsUUIDs.contains(uuid)) {
			return;
		}
		
		spawnedMobsUUIDs.remove(uuid);
		MobEntity mob = (MobEntity) entity;
		spawnedMobs.add(mob);		
		BaseraidsEntityManager.setupGoals(mob);
		Baseraids.baseraidsData.setDirty(true);
	}

}
