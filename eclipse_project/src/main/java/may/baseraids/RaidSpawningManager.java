package may.baseraids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import may.baseraids.entities.BaseraidsEntityManager;
import may.baseraids.entities.RaidEntitySpawnCountRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
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
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaidSpawningManager {

	private World world;
	private RaidManager raidManager;
	/** A list of all spawned mobs for managing active raids */
	private List<MobEntity> spawnedMobs = new ArrayList<MobEntity>();
	/**
	 * A list of UUIDs for all spawned mobs. Only used and updated when saving and
	 * loading.
	 */
	private List<UUID> spawnedMobsUUIDs = new ArrayList<UUID>();

	// spawning parameters
	private static final double SPAWN_ANGLE_INTERVAL = 2 * Math.PI / 100;
	private static final int SPAWN_RADIUS_MIN = 40;
	private static final int SPAWN_RADIUS_MAX = 60;

	public RaidSpawningManager(RaidManager raidManager, World world) {
		this.raidManager = raidManager;
		this.world = world;
		MinecraftForge.EVENT_BUS.register(this);
		RaidEntitySpawnCountRegistry.registerSpawnCounts();
	}

	/**
	 * Spawns the mobs for the current level specified by <code>amountOfMobs</code>
	 * and inputs the spawned entities into the list <code>spawnedMobs</code>.
	 */
	void spawnRaidMobs() {
		int raidLevel = raidManager.getRaidLevel();
		Set<EntityType<?>> entityTypesToSpawn = RaidEntitySpawnCountRegistry.getEntityTypesToSpawn();

		entityTypesToSpawn.forEach(type -> {
			int count = RaidEntitySpawnCountRegistry.getSpawnCountForEntityAndLevel(type, raidLevel-1);
			MobEntity[] spawnedMobsArray = spawnSpecificEntities(type, count);

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
		Random r = new Random();

		// select random radius between SPAWN_RADIUS_MIN and SPAWN_RADIUS_MAX
		int radius = r.nextInt(SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN) + SPAWN_RADIUS_MIN;

		// select random angle
		int randomAngleIndex = r.nextInt(100);
		double angle = randomAngleIndex * SPAWN_ANGLE_INTERVAL;

		// compute coordinates x and z for the radius and angle
		int x = (int) (radius * Math.cos(angle));
		int z = (int) (radius * Math.sin(angle));
		BlockPos centerSpawnPos = NexusBlock.getBlockPos().add(0, 1, 0);
		BlockPos spawnPosXZ = centerSpawnPos.add(x, 0, z);

		// find the right height at which this entity type can be spawned
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
	
	public boolean isEntityRaiding(LivingEntity entity) {
		return spawnedMobs.contains(entity);
	}

	/**
	 * Reads and stores the UUIDs of the mobs stored in the given
	 * <code>CompoundNBT</code>, so that they can be recovered when they are spawned
	 * into the world. This function assumes that the nbt was previously written by
	 * this class or to be precise, that the nbt includes certain elements.
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

		for (int index = 0; index < spawnedMobsList.size(); index++) {
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

	/**
	 * Recovers the spawned mobs after saving and loading by comparing the UUIDs
	 * when they are loaded by the game.
	 * 
	 * @param event the event of type <code>EntityJoinWorldEvent</code> that
	 *              triggers this function
	 */
	@SubscribeEvent
	public void onEntityJoinWorld(final EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote()) {
			return;
		}
		if (!event.getWorld().equals(world)) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof MobEntity)) {
			return;
		}

		UUID uuid = entity.getUniqueID();
		if (!spawnedMobsUUIDs.contains(uuid)) {
			return;
		}

		spawnedMobsUUIDs.remove(uuid);
		MobEntity mob = (MobEntity) entity;
		spawnedMobs.add(mob);
		BaseraidsEntityManager.setupGoals(mob);
		Baseraids.baseraidsData.setDirty(true);
	}

}
