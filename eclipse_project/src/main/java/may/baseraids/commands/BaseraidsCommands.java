package may.baseraids.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import may.baseraids.RaidManager;
import may.baseraids.WorldManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

/**
 * This class defines the commands for this mod.
 * 
 * @author Natascha May
 */
public class BaseraidsCommands {

	private WorldManager worldManager;
	
	public BaseraidsCommands(WorldManager worldManager) {
		this.worldManager = worldManager;
	}
	
	public void register(CommandDispatcher<CommandSource> dispatcher) {
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

	private int giveNexusToPlayer(CommandSource source, ServerPlayerEntity target) {
		return NexusBlock.giveNexusToPlayer(target) ? 0 : 1;
	}

	private int winRaid(CommandSource source) {
		worldManager.getRaidManager().winRaid();
		return 0;
	}

	private int loseRaid(CommandSource source) {
		worldManager.getRaidManager().loseRaid();
		return 0;
	}

	private int getTimeUntilRaid(CommandSource source) {
		source.sendFeedback(new StringTextComponent(
				"Time until next raid: " + worldManager.getRaidManager().getTimeUntilRaidInDisplayString()),
				true);
		int timeUntilRaid = worldManager.getRaidManager().getTimeUntilRaidInSec();
		return timeUntilRaid;
	}
	
	private int setTimeUntilRaid(CommandSource source, int min) {
		worldManager.getRaidManager().setTimeUntilRaidInMin(min);
		source.sendFeedback(new StringTextComponent(
				"Time until next raid: " + worldManager.getRaidManager().getTimeUntilRaidInDisplayString()),
				true);
		int timeUntilRaid = worldManager.getRaidManager().getTimeUntilRaidInSec();
		return timeUntilRaid;
	}
	
	private int reduceTimeUntilRaid(CommandSource source, int min) {
		worldManager.getRaidManager().reduceTimeUntilRaidInMin(min);
		source.sendFeedback(new StringTextComponent(
				"Time until next raid: " + worldManager.getRaidManager().getTimeUntilRaidInDisplayString()),
				true);
		int timeUntilRaid = worldManager.getRaidManager().getTimeUntilRaidInSec();
		return timeUntilRaid;
	}

	private int getRaidLevel(CommandSource source) {
		int level = worldManager.getRaidManager().getRaidLevel();
		source.sendFeedback(new StringTextComponent("Raid level: " + level), true);
		return level;
	}
	
	private int setRaidLevel(CommandSource source, int level) {
		worldManager.getRaidManager().setRaidLevel(level);
		source.sendFeedback(new StringTextComponent("Raid level: " + level), true);
		return level;
	}

	private int startRaid(CommandSource source) {
		worldManager.getRaidManager().startRaid();
		return 0;
	}

	private int restoreDestroyedBlocks(CommandSource source) {
		worldManager.getRaidManager().restoreDestroyedBlocksMng.restoreAndClearSavedBlocks();
		return 0;
	}
}
