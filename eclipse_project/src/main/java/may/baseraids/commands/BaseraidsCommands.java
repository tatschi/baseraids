package may.baseraids.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import may.baseraids.Baseraids;
import may.baseraids.RaidManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class defines the commands for this mod.
 * 
 * @author Natascha May
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BaseraidsCommands {

	@SubscribeEvent
	public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSource> commandDispatcher = event.getDispatcher();
		BaseraidsCommands.register(commandDispatcher);
	}
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
	      dispatcher.register(
	    		Commands.literal("baseraids")
	    		.requires((commandSource) -> { return commandSource.hasPermissionLevel(2);})
	    		
	    		.then(Commands.literal("timeUntilRaid")
	    			.then(Commands.literal("query")
	    				.executes((commandSource) -> {return getTimeUntilRaid(commandSource.getSource());}))
	    			.then(Commands.literal("set")
	    				.then(Commands.argument("min", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
	    						.executes((commandSource) -> {return setTimeUntilRaid(commandSource.getSource(), IntegerArgumentType.getInteger(commandSource, "min"));})))
		    		.then(Commands.literal("reduce")
		    				.then(Commands.argument("min", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
		    						.executes((commandSource) -> {return reduceTimeUntilRaid(commandSource.getSource(), IntegerArgumentType.getInteger(commandSource, "min"));}))))
	    			
	    		.then(Commands.literal("level")
	    				.then(Commands.literal("query")
	    					.executes((commandSource) -> {return getRaidLevel(commandSource.getSource());}))
	    				.then(Commands.literal("set")
	    						.then(Commands.argument("level", IntegerArgumentType.integer(RaidManager.MIN_RAID_LEVEL, RaidManager.MAX_RAID_LEVEL))
	    								.executes((commandSource) -> {return setRaidLevel(commandSource.getSource(), IntegerArgumentType.getInteger(commandSource, "level"));}))))
	    		
	    		
	    		.then(Commands.literal("raid")
	    				.then(Commands.literal("start")
	    						.executes((commandSource) -> {return startRaid(commandSource.getSource());}))
	    				.then(Commands.literal("win")
	    						.executes((commandSource) -> {return winRaid(commandSource.getSource());}))
	    				.then(Commands.literal("lose")
	    						.executes((commandSource) -> {return loseRaid(commandSource.getSource());})))
	    		
				.then(Commands.literal("giveNexus")
	      				.then(Commands.argument("target", EntityArgument.players())
	      						.executes((commandSource) -> {return giveNexusToPlayer(commandSource.getSource(), EntityArgument.getPlayer(commandSource, "target"));})))
	      		.then(Commands.literal("restoreDestroyedBlocks")
	      				.executes((commandSource) -> {return restoreDestroyedBlocks(commandSource.getSource());}))
	    		 );
	}

	private static int giveNexusToPlayer(CommandSource source, ServerPlayerEntity target) {
		return NexusBlock.giveNexusToPlayer(target) ? 0 : 1;
	}

	private static int winRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.winRaid();
		return 0;
	}

	private static int loseRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.loseRaid();
		return 0;
	}

	private static int getTimeUntilRaid(CommandSource source) {
		source.sendFeedback(new StringTextComponent(
				"Time until next raid: " + Baseraids.baseraidsData.raidManager.getTimeUntilRaidInDisplayString()),
				true);
		int timeUntilRaid = Baseraids.baseraidsData.raidManager.getTimeUntilRaidInSec();
		return timeUntilRaid;
	}
	
	private static int setTimeUntilRaid(CommandSource source, int min) {
		Baseraids.baseraidsData.raidManager.setTimeUntilRaidInMin(min);
		source.sendFeedback(new StringTextComponent(
				"Time until next raid: " + Baseraids.baseraidsData.raidManager.getTimeUntilRaidInDisplayString()),
				true);
		int timeUntilRaid = Baseraids.baseraidsData.raidManager.getTimeUntilRaidInSec();
		return timeUntilRaid;
	}
	
	private static int reduceTimeUntilRaid(CommandSource source, int min) {
		Baseraids.baseraidsData.raidManager.reduceTimeUntilRaidInMin(min);
		source.sendFeedback(new StringTextComponent(
				"Time until next raid: " + Baseraids.baseraidsData.raidManager.getTimeUntilRaidInDisplayString()),
				true);
		int timeUntilRaid = Baseraids.baseraidsData.raidManager.getTimeUntilRaidInSec();
		return timeUntilRaid;
	}

	private static int getRaidLevel(CommandSource source) {
		int level = Baseraids.baseraidsData.raidManager.getRaidLevel();
		source.sendFeedback(new StringTextComponent("Raid level: " + level), true);
		return level;
	}
	
	private static int setRaidLevel(CommandSource source, int level) {
		Baseraids.baseraidsData.raidManager.setRaidLevel(level);
		source.sendFeedback(new StringTextComponent("Raid level: " + level), true);
		return level;
	}

	private static int startRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.startRaid();
		return 0;
	}

	private static int restoreDestroyedBlocks(CommandSource source) {
		Baseraids.baseraidsData.raidManager.restoreDestroyedBlocksMng.restoreAndClearSavedBlocks();
		return 0;
	}
}
