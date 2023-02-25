package may.baseraids;

import java.util.Objects;

import com.mojang.brigadier.CommandDispatcher;

import may.baseraids.commands.BaseraidsCommands;
import may.baseraids.config.ConfigOptions;
import may.baseraids.entities.BaseraidsEntityManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class WorldManager {

	private BaseraidsSavedData baseraidsData;
	private final BaseraidsCommands commands;
	final BaseraidsEntityManager entityManager;

	WorldManager() {
		MinecraftForge.EVENT_BUS.register(this);
		commands = new BaseraidsCommands(this);
		entityManager = new BaseraidsEntityManager(this);

	}

	/**
	 * Registers the commands defined in {@link #commands}.
	 * 
	 * @param event the event of type {@link RegisterCommandsEvent} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onRegisterCommandEvent(final RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
		commands.register(commandDispatcher);
	}

	/**
	 * Registers the setups of AI goals for raid monsters.
	 * 
	 * @param event the event of type {@link FMLCommonSetupEvent} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onFMLCommonSetup(final FMLCommonSetupEvent event) {
		entityManager.registerSetups();
	}

	/**
	 * Initiates the loading process for this mod using the class
	 * {@link BaseraidsSavedData} when the world is loaded.
	 * 
	 * @param event the event of type {@link WorldEvent.Load} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldLoadedLoadBaseraidsWorldSavedData(final WorldEvent.Load event) {
		if (event.getWorld().isClientSide() || !((Level) event.getWorld()).dimension().equals(Level.OVERWORLD))
			return;

		if (event.getWorld()instanceof ServerLevel serverLevel) {
			baseraidsData = BaseraidsSavedData.get(this, serverLevel);
		}

	}

	/**
	 * Initiates the saving process for this mod using the class
	 * {@link BaseraidsSavedData} when the world is saved.
	 * 
	 * @param event the event of type {@link WorldEvent.Save} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldSavedSaveBaseraidsWorldSavedData(final WorldEvent.Save event) {
		if (event.getWorld().isClientSide() || !((Level) event.getWorld()).dimension().equals(Level.OVERWORLD))
			return;

		if (event.getWorld()instanceof ServerLevel serverLevel) {
			baseraidsData = BaseraidsSavedData.get(this, serverLevel);
		}
	}

	/**
	 * Called for potential spawns in the world.
	 * 
	 * @param event the event of type {@link LivingSpawnEvent.CheckSpawn} that calls
	 *              this function
	 */
	@SubscribeEvent
	public void onMonsterSpawn(final LivingSpawnEvent.CheckSpawn event) {
		if (event.getWorld().isClientSide()) {
			return;
		}

		if (onMonsterSpawnOutsideCaveShouldCancelSpawn(event)) {
			event.setResult(Result.DENY);
		}
	}

	/**
	 * Cancels a monster spawning event, if it is not inside a cave and the config
	 * option {@link ConfigOptions#deactivateMonsterNightSpawn} is true.
	 * 
	 * @param event the event of type {@link LivingSpawnEvent.CheckSpawn} that calls
	 *              this function
	 */
	private boolean onMonsterSpawnOutsideCaveShouldCancelSpawn(final LivingSpawnEvent.CheckSpawn event) {
		if (!ConfigOptions.getDeactivateMonsterNightSpawn()) {
			return false;
		}

		if (!(event.getWorld() instanceof Level)) {
			return false;
		}

		if (!((Level) event.getWorld()).dimension().equals(Level.OVERWORLD)) {
			return false;
		}

		if (event.getEntityLiving().getClassification(false) != MobCategory.MONSTER) {
			return false;
		}

		BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
		if (!event.getWorld().getBlockState(pos).equals(Blocks.CAVE_AIR.defaultBlockState())) {
			return true;
		}

		if (event.getWorld().getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) == pos.getY()) {
			return true;
		}

		return event.getWorld().canSeeSky(pos);
	}

	public void markDirty() {
		baseraidsData.setDirty(true);
	}

	public RaidManager getRaidManager() {
		return baseraidsData.raidManager;
	}

	public RaidTimeManager getRaidTimeManager() {
		return baseraidsData.raidManager.getRaidTimeManager();
	}

	public ServerLevel getServerLevel() {
		return baseraidsData.serverLevel;
	}

	@Override
	public int hashCode() {
		return Objects.hash(baseraidsData, commands, entityManager);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorldManager other = (WorldManager) obj;
		return Objects.equals(baseraidsData, other.baseraidsData) && Objects.equals(commands, other.commands)
				&& Objects.equals(entityManager, other.entityManager);
	}
}
