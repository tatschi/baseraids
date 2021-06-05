package may.baseraids.commands;

import com.mojang.brigadier.CommandDispatcher;

import may.baseraids.Baseraids;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class StartRaidCommand{

	public static void register(CommandDispatcher<CommandSource> dispatcher) {
	      dispatcher.register(
	    		Commands.literal("baseraids")
	    		.requires((commandSource) -> { return commandSource.hasPermissionLevel(2);})
	    		.then(Commands.literal("raid")
	    				.then(Commands.literal("start")
	    						.executes((commandSource) -> {return startRaid(commandSource.getSource());}))
	    				.then(Commands.literal("win")
	    						.executes((commandSource) -> {return winRaid(commandSource.getSource());})
	    		)
	      		.then(Commands.literal("get").then(Commands.literal("timeUntilRaid")
	      				.executes((commandSource) -> {return getTimeUntilRaid(commandSource.getSource());}))
	      		)));
	}

	
	private static int winRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.winRaid(source.getWorld());
		return 0;
	}


	private static int getTimeUntilRaid(CommandSource source) {
		int timeUntilRaid = Baseraids.baseraidsData.raidManager.getTimeUntilRaidInSec(source.getWorld());
		if(timeUntilRaid > 60) {
			source.sendFeedback(new StringTextComponent("Time until next raid: " + (int) timeUntilRaid / 60 + "min " + timeUntilRaid % 60 + "s"), true);
		}else {
			source.sendFeedback(new StringTextComponent("Time left: " + timeUntilRaid + "s"), true);
		}
		return timeUntilRaid;
	}


	private static int startRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.initiateRaid(source.getWorld());
		return 0;
	}
}
