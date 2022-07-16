package may.baseraids.commands;

import com.mojang.brigadier.CommandDispatcher;

import may.baseraids.Baseraids;
import may.baseraids.NexusBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
				.then(Commands.literal("startRaid")
						.executes((commandSource) -> {return startRaid(commandSource.getSource());}))
				.then(Commands.literal("winRaid")
						.executes((commandSource) -> {return winRaid(commandSource.getSource());}))
				.then(Commands.literal("loseRaid")
						.executes((commandSource) -> {return loseRaid(commandSource.getSource());}))
	    		
	      		.then(Commands.literal("getTimeUntilRaid")
	      				.executes((commandSource) -> {return getTimeUntilRaid(commandSource.getSource());}))
	      		.then(Commands.literal("getLevel")
	      				.executes((commandSource) -> {return getRaidLevel(commandSource.getSource());}))
	      		.then(Commands.literal("sendTestMessage")
	      				.executes((commandSource) -> {return sendTestMessage(commandSource.getSource());}))
	      		.then(Commands.literal("giveNexus")
	      				.then(Commands.argument("target", EntityArgument.players())
	      						.executes((commandSource) -> {return giveNexusToPlayer(commandSource.getSource(), EntityArgument.getPlayer(commandSource, "target"));})))
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
		source.sendFeedback(new StringTextComponent("Time until next raid: " + Baseraids.baseraidsData.raidManager.getTimeUntilRaidInDisplayString()), true);
		int timeUntilRaid = Baseraids.baseraidsData.raidManager.getTimeUntilRaidInSec();
		return timeUntilRaid;
	}
	
	private static int getRaidLevel(CommandSource source) {
		int level = Baseraids.baseraidsData.raidManager.getRaidLevel();
		source.sendFeedback(new StringTextComponent("Raid level: " + level), true);
		return level;
	}


	private static int startRaid(CommandSource source) {
		Baseraids.baseraidsData.raidManager.startRaid();
		return 0;
	}
	
	private static int sendTestMessage(CommandSource source) {
		Baseraids.sendChatMessage("Test Message");
		return 0;
	}
}
