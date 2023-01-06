package may.baseraids;

import com.mojang.brigadier.CommandDispatcher;

import may.baseraids.commands.BaseraidsCommands;
import may.baseraids.config.ConfigOptions;
import may.baseraids.entities.BaseraidsEntityManager;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityClassification;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class WorldManager {

	private BaseraidsWorldSavedData baseraidsData;
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
		CommandDispatcher<CommandSource> commandDispatcher = event.getDispatcher();
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
	 * {@link BaseraidsWorldSavedData} when the world is loaded.
	 * 
	 * @param event the event of type {@link WorldEvent.Load} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldLoadedLoadBaseraidsWorldSavedData(final WorldEvent.Load event) {
		if (event.getWorld().isRemote() || !((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD))
			return;

		if (event.getWorld() instanceof ServerWorld) {
			baseraidsData = BaseraidsWorldSavedData.get(this, (ServerWorld) event.getWorld());
		}

	}

	/**
	 * Initiates the saving process for this mod using the class
	 * {@link BaseraidsWorldSavedData} when the world is saved.
	 * 
	 * @param event the event of type {@link WorldEvent.Save} that calls this
	 *              function
	 */
	@SubscribeEvent
	public void onWorldSavedSaveBaseraidsWorldSavedData(final WorldEvent.Save event) {
		if (event.getWorld().isRemote() || !((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD))
			return;
		if (event.getWorld() instanceof ServerWorld) {
			baseraidsData = BaseraidsWorldSavedData.get(this, (ServerWorld) event.getWorld());
		}
	}

	/**
	 * Called for potential spawns in the world.
	 * 
	 * @param event the event of type {@link WorldEvent.PotentialSpawns} that calls
	 *              this function
	 */
	@SubscribeEvent
	public void onMonsterSpawn(final WorldEvent.PotentialSpawns event) {
		World world = (World) event.getWorld();
		if (world.isRemote())
			return;

		if (!event.isCancelable()) {
			return;
		}

		if (onMonsterSpawnOutsideCaveShouldCancelSpawn(event)) {
			event.setCanceled(true);
		}
	}

	/**
	 * Cancels a monster spawning event, if it is not inside a cave and the config
	 * option {@link ConfigOptions#deactivateMonsterNightSpawn} is true.
	 * 
	 * @param event the event of type {@link WorldEvent.PotentialSpawns} that calls
	 *              this function
	 */
	private boolean onMonsterSpawnOutsideCaveShouldCancelSpawn(final WorldEvent.PotentialSpawns event) {
		if (Boolean.FALSE.equals(ConfigOptions.deactivateMonsterNightSpawn.get())) {
			return false;
		}

		if (!((World) event.getWorld()).getDimensionKey().equals(World.OVERWORLD)) {
			return false;
		}

		if (event.getType() != EntityClassification.MONSTER) {
			return false;
		}

		if (!event.getWorld().getBlockState(event.getPos()).equals(Blocks.CAVE_AIR.getDefaultState())) {
			return true;
		}

		if (event.getWorld().getHeight(Heightmap.Type.WORLD_SURFACE, event.getPos()).equals(event.getPos())) {
			return true;
		}

		return event.getWorld().canSeeSky(event.getPos());
	}

	public void markDirty() {
		baseraidsData.setDirty(true);
	}

	public RaidManager getRaidManager() {
		return baseraidsData.raidManager;
	}

	public ServerWorld getServerWorld() {
		return baseraidsData.serverWorld;
	}
}
