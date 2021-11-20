package may.baseraids.commands;

import com.mojang.brigadier.CommandDispatcher;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.entity.player.ServerPlayerEntity;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
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
	    		.then(Commands.literal("raid")
	    				.then(Commands.literal("start")
	    						.executes((commandSource) -> {return startRaid(commandSource.getSource());}))
	    				.then(Commands.literal("win")
	    						.executes((commandSource) -> {return winRaid(commandSource.getSource());}))
						.then(Commands.literal("lose")
	    						.executes((commandSource) -> {return loseRaid(commandSource.getSource());}))
	    		)
	      		.then(Commands.literal("get")
	      				.then(Commands.literal("timeUntilRaid")
	      						.executes((commandSource) -> {return getTimeUntilRaid(commandSource.getSource());}))
	      				.then(Commands.literal("level")
	      						.executes((commandSource) -> {return getRaidLevel(commandSource.getSource());}))
	      		)
	      		.then(Commands.literal("give")
	      				.then(Commands.literal("nexus")
	      						.then(Commands.argument("target", EntityArgument.players())
	      								.executes((commandSource) -> {return giveNexusToPlayer(commandSource.getSource(), EntityArgument.getPlayer(commandSource, "target"));})))
	    		 ));
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
		int timeUntilRaid = Baseraids.baseraidsData.raidManager.getTimeUntilRaidInSec();
		if(timeUntilRaid > 60) {
			source.sendFeedback(new StringTextComponent("Time until next raid: " + (int) timeUntilRaid / 60 + "min " + timeUntilRaid % 60 + "s"), true);
		}else {
			source.sendFeedback(new StringTextComponent("Time left: " + timeUntilRaid + "s"), true);
		}
		return timeUntilRaid;
	}
	
	private static int getRaidLevel(CommandSource source) {
		int level = Baseraids.baseraidsData.raidManager.getRaidLevel();
		source.sendFeedback(new StringTextComponent("Raid level: " + level), true);
		return level;
	}


	private static int startRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.initiateRaid();
		return 0;
	}
}
