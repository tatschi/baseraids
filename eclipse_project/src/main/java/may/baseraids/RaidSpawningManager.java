package may.baseraids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import may.baseraids.entities.RaidEntitySpawnCountRegistry;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
	private static final int SPAWN_ANGLE_STEPS = 100;
	private static final double SPAWN_ANGLE_INTERVAL = 2 * Math.PI / SPAWN_ANGLE_STEPS;
	private static final int SPAWN_RADIUS_MIN = 40;
	private static final int SPAWN_RADIUS_MAX = 60;
	private static final int MAX_SPAWN_TRIES = 5;

	private RandomSource rand = RandomSource.create();

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
		Set<EntityType<? extends Mob>> entityTypesToSpawn = RaidEntitySpawnCountRegistry.getEntityTypesToSpawn();

		int playerCount = level.players().size();

		entityTypesToSpawn.forEach(type -> {
			int count = RaidEntitySpawnCountRegistry.getSpawnCountForEntityAndLevelAndPlayerCount(type, raidLevel,
					playerCount);
			Mob[] spawnedMobsArray = spawnSpecificEntities(type, count);

			// remove nulls and convert to collection
			Collection<Mob> spawnedMobsNonNullCollection = Arrays.stream(spawnedMobsArray)
					.filter(Objects::nonNull).toList();

			spawnedMobs.addAll(spawnedMobsNonNullCollection);
		});

		raidManager.markDirty();
		Baseraids.LOGGER.info("Spawned all entities for the raid");
	}

	/**
	 * Spawns the given number of mobs of the given entity type.
	 * 
	 * @param <T>        extends {@link Mob} the entity class corresponding to
	 *                   the {@code entityType}
	 * @param entityType
	 * @param numMobs    the amount of mobs to spawn of this type
	 * @return an array of {@link Mob} containing the spawned entities
	 */
	private <T extends Mob> Mob[] spawnSpecificEntities(EntityType<T> entityType, int numMobs) {
		SpawnGroupData spawnGroupData = null;
		Mob[] mobs = new Mob[numMobs];
		for (int i = 0; i < numMobs; i++) {
			BlockPos spawnPos = findSpawnPos(entityType);

			if (entityType.equals(EntityType.PHANTOM)) {
				mobs[i] = EntityType.PHANTOM.create(level);
				mobs[i].moveTo(spawnPos, 0.0F, 0.0F);
				spawnGroupData = mobs[i].finalizeSpawn((ServerLevelAccessor) level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, spawnGroupData, (CompoundTag) null);
				((ServerLevelAccessor) level).addFreshEntityWithPassengers(mobs[i]);
			} else {
				mobs[i] = entityType.spawn((ServerLevel) level, spawnPos, MobSpawnType.MOB_SUMMONED);
			}

			if (mobs[i] != null) {
				if (Baseraids.LOGGER.isDebugEnabled()) {
					Baseraids.LOGGER.debug("Spawn %s at (%i, %i, %i)", entityType.getDescriptionId(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
				}
				
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
	private <T extends Mob> BlockPos findSpawnPos(EntityType<T> entityType) {
		int tries = 0;
		BlockPos spawnPos;
		do {
			spawnPos = generateRandomSpawnPos(entityType);
			tries++;
			if(tries >= MAX_SPAWN_TRIES) {
				break;
			}
		}while(!Mob.checkMobSpawnRules(entityType, level, MobSpawnType.MOB_SUMMONED, spawnPos, rand));		
		
		return spawnPos;
	}
	
	/**
	 * Generates an unchecked spawn position for the given entity type. The
	 * position is selected randomly on a circle around the nexus block.
	 * 
	 * @param entityType
	 * @return spawn position
	 */
	private <T extends Mob> BlockPos generateRandomSpawnPos(EntityType<T> entityType) {
		int radius = generateRadius();
		double angle = generateAngle();		
		BlockPos spawnPos = NexusBlock.getBlockPos().offset(convertRadiusAndAngleToVector(radius, angle));
		return new BlockPos(spawnPos.getX(), findSpawnHeight(entityType, spawnPos), spawnPos.getZ());
	}

	/**
	 * Selects a random radius between {@link #SPAWN_RADIUS_MIN} and {@link #SPAWN_RADIUS_MAX}. 
	 * @return the radius
	 */
	private int generateRadius() {
		return rand.nextInt(SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN) + SPAWN_RADIUS_MIN;
	}
	
	/**
	 * Converts the radius and angle to an offset vector for a {@link BlockPos}. 
	 * @param radius
	 * @param angle
	 * @return a {@link Vec3i} that offsets a {@link BlockPos} by the given radius and angle
	 */
	private Vec3i convertRadiusAndAngleToVector(int radius, double angle) {
		int x = (int) (radius * Math.cos(angle));
		int z = (int) (radius * Math.sin(angle));
		return new Vec3i(x, 0, z);
	}
	
	/**
	 * Selects a random angle from {@link #SPAWN_ANGLE_STEPS} number of uniformly distributed angles.
	 * @return the angle
	 */
	private double generateAngle() {
		int randomAngleIndex = rand.nextInt(SPAWN_ANGLE_STEPS);
		return randomAngleIndex * SPAWN_ANGLE_INTERVAL;
	}
	
	/**
	 * Finds the right height at which this entity type can be spawned at this xz-position.
	 * @param <T>	extends {@link Mob} the entity class corresponding to
	 *                   the {@code entityType}
	 * @param entityType
	 * @param spawnPos
	 * @return the y-coordinate corresponding to the height
	 */
	private <T extends Mob> int findSpawnHeight(EntityType<T> entityType, BlockPos spawnPos) {
		Type heightmapType = SpawnPlacements.getPlacementType(entityType);
		int surfaceHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawnPos.getX(), spawnPos.getZ());
		if (!heightmapType.equals(Type.ON_GROUND)) {
			return surfaceHeight + 20 + rand.nextInt(15);
		}
		return surfaceHeight;
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
			if (!mob.isDeadOrDying()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Kills all mobs that were spawned by this object.
	 */
	void killAllMobs() {
		spawnedMobs.forEach(mob -> mob.remove(RemovalReason.KILLED));
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
	public void onEntityJoinWorld(final EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		if (!event.getLevel().equals(level)) {
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
