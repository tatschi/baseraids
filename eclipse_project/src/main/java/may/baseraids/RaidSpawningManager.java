package may.baseraids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Objects;

import may.baseraids.entities.RaidEntitySpawnCountRegistry;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This class controls the spawning, saving and loading and other handling of
 * the raid mobs.
 * 
 * @author Natascha May
 */
public class RaidSpawningManager {

	private Level level;
	private RaidManager raidManager;
	private WorldManager worldManager;
	/** A list of all spawned mobs for managing active raids */
	private List<Mob> spawnedMobs = new ArrayList<>();
	/**
	 * A list of UUIDs for all spawned mobs. Only used and updated when saving and
	 * loading.
	 */
	private List<UUID> spawnedMobsUUIDs = new ArrayList<>();

	// spawning parameters
	private static final double SPAWN_ANGLE_INTERVAL = 2 * Math.PI / 100;
	private static final int SPAWN_RADIUS_MIN = 40;
	private static final int SPAWN_RADIUS_MAX = 60;

	private Random rand = new Random();

	public RaidSpawningManager(RaidManager raidManager, Level world, WorldManager worldManager) {
		this.raidManager = raidManager;
		this.level = world;
		this.worldManager = worldManager;
		MinecraftForge.EVENT_BUS.register(this);
		RaidEntitySpawnCountRegistry.registerSpawnCounts();
	}

	/**
	 * Spawns the mobs for the current level specified in the
	 * {@link RaidEntitySpawnCountRegistry} and saves the spawned entities into the
	 * list {@link #spawnedMobs}.
	 */
	void spawnRaidMobs() {
		int raidLevel = raidManager.getRaidLevel();
		Set<EntityType<?>> entityTypesToSpawn = RaidEntitySpawnCountRegistry.getEntityTypesToSpawn();

		int playerCount = level.players().size();

		entityTypesToSpawn.forEach(type -> {
			int count = RaidEntitySpawnCountRegistry.getSpawnCountForEntityAndLevelAndPlayerCount(type, raidLevel,
					playerCount);
			Mob[] spawnedMobsArray = spawnSpecificEntities(type, count);

			// remove nulls and convert to collection
			Collection<Mob> spawnedMobsNonNullCollection = Arrays.stream(spawnedMobsArray)
					.filter(Objects::nonNull).collect(Collectors.toList());

			spawnedMobs.addAll(spawnedMobsNonNullCollection);
		});

		raidManager.markDirty();
		Baseraids.LOGGER.info("Spawned all entities for the raid");
	}

	/**
	 * Spawns the given number of mobs of the given entity type.
	 * 
	 * @param <T>        extends {@link Entity} the entity class corresponding to
	 *                   the {@code entityType}
	 * @param entityType
	 * @param numMobs    the amount of mobs to spawn of this type
	 * @return an array of {@link Mob} containing the spawned entities
	 */
	private <T extends Entity> Mob[] spawnSpecificEntities(EntityType<T> entityType, int numMobs) {

		SpawnGroupData spawnGroupData = null;
		Mob[] mobs = new Mob[numMobs];
		for (int i = 0; i < numMobs; i++) {

			BlockPos spawnPos = findSpawnPos(entityType);

			if (entityType.equals(EntityType.PHANTOM)) {
				mobs[i] = EntityType.PHANTOM.create(level);
				mobs[i].moveTo(spawnPos, 0.0F, 0.0F);
				spawnGroupData = mobs[i].finalizeSpawn((ServerLevelAccessor) level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, spawnGroupData, (CompoundTag) null);
				((ServerLevelAccessor) level).func_242417_l(mobs[i]);
			} else {
				mobs[i] = (Mob) entityType.spawn((ServerLevel) level, null, null, spawnPos,
						MobSpawnType.MOB_SUMMONED, false, false);
			}

			if (mobs[i] != null) {
				worldManager.entityManager.setupGoals(mobs[i]);
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

		// select random radius between SPAWN_RADIUS_MIN and SPAWN_RADIUS_MAX
		int radius = rand.nextInt(SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN) + SPAWN_RADIUS_MIN;

		// select random angle
		int randomAngleIndex = rand.nextInt(100);
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
			spawnPos = spawnPosXZ.offset(0, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), 0);
		} else {
			spawnPos = spawnPosXZ.offset(0, level.getHeight(EntitySpawnPlacementRegistry.func_209342_b(entityType), x, z), 0);
		}

		Baseraids.LOGGER.debug("Spawn %s at radius %i and angle %d", entityType.getRegistryName().toDebugFileName(), radius, angle);
		return spawnPos;
	}

	/**
	 * @return {@code true} if and only if all mobs that were spawned by this object
	 *         are dead.
	 */
	boolean areAllSpawnedMobsDead() {
		if (spawnedMobs.isEmpty()) {
			raidManager.markDirty();
			return false;
		}

		for (Mob mob : spawnedMobs) {
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
		spawnedMobs.forEach(Entity::remove);
		spawnedMobs.clear();
		raidManager.markDirty();
	}

	public boolean isEntityRaiding(LivingEntity entity) {
		return spawnedMobs.contains(entity);
	}

	/**
	 * Reads and stores the UUIDs of the mobs stored in the given
	 * {@link CompoundTag} so that they can be recovered when they are spawned into
	 * the world. This function assumes that the nbt was previously written by this
	 * class or to be precise, that the nbt includes certain elements.
	 * 
	 * @param nbt the nbt that will be read out. It is assumed to include certain
	 *            elements.
	 */
	private void readSpawnedMobsList(CompoundTag nbt) {
		ListTag spawnedMobsList = nbt.getList("spawnedMobs", 10);
		spawnedMobs.clear();
		spawnedMobsUUIDs.clear();

		for (int index = 0; index < spawnedMobsList.size(); index++) {
			CompoundTag compoundTag = spawnedMobsList.getCompound(index);
			UUID entityUUID = compoundTag.getUUID("ID" + index);
			Baseraids.LOGGER.debug("reading entity with ID {}", entityUUID);
			spawnedMobsUUIDs.add(entityUUID);
		}
	}

	/**
	 * Saves the UUIDs of the spawned mobs to a {@link CompoundTag} and returns the
	 * {@link CompoundTag} object.
	 * 
	 * @return the adapted {@link CompoundTag} that was written to
	 */
	CompoundTag write() {

		CompoundTag nbt = new CompoundTag();

		ListTag spawnedMobsList = new ListTag();
		int index = 0;
		for (Mob mob : spawnedMobs) {
			CompoundTag compound = new CompoundTag();

			compound.putUUID("ID" + index, mob.getUUID());
			Baseraids.LOGGER.debug("writing entity with UUID {}", mob.getUUID());
			spawnedMobsList.add(compound);
			index++;
		}

		nbt.put("spawnedMobs", spawnedMobsList);
		return nbt;
	}

	void read(CompoundTag nbt) {
		readSpawnedMobsList(nbt);
	}

	/**
	 * Recovers the spawned mobs after saving and loading by comparing the UUIDs
	 * when they are loaded by the game.
	 * 
	 * @param event the event of type {@link EntityJoinWorldEvent} that triggers
	 *              this function
	 */
	@SubscribeEvent
	public void onEntityJoinWorld(final EntityJoinWorldEvent event) {
		if (event.getWorld().isClientSide) {
			return;
		}
		if (!event.getWorld().equals(level)) {
			return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof Mob)) {
			return;
		}

		UUID uuid = entity.getUUID();
		if (!spawnedMobsUUIDs.contains(uuid)) {
			return;
		}

		spawnedMobsUUIDs.remove(uuid);
		Mob mob = (Mob) entity;
		spawnedMobs.add(mob);
		worldManager.entityManager.setupGoals(mob);
		raidManager.markDirty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(spawnedMobs, level);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RaidSpawningManager other = (RaidSpawningManager) obj;
		return Objects.equals(spawnedMobs, other.spawnedMobs) && Objects.equals(level, other.level);
	}

}
