package may.baseraids.commands;

import java.util.function.BiFunction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import may.baseraids.MCDuration;
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

	private static final String NAME_ARG_TICKS = "ticks";
	private static final String TEXT_TIME_UNTIL_NEXT_RAID = "Set time until next raid to ";
	private static final String NAME_ARG_LEVEL = "level";
	private WorldManager worldManager;

	public BaseraidsCommands(WorldManager worldManager) {
		this.worldManager = worldManager;
	}

	public void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("baseraids").requires(commandSource -> commandSource.hasPermissionLevel(2))
				.then(registerTimeUntilRaidCommand()).then(registerLevelCommand()).then(registerRaidCommand())
				.then(registerGiveNexusCommand()).then(registerRestoreDestroyedBlocksCommand()));
	}

	private LiteralArgumentBuilder<CommandSource> registerTimeUntilRaidCommand() {
		return Commands.literal("timeUntilRaid")
				.then(Commands.literal("query").executes(commandSource -> getTimeUntilRaid(commandSource.getSource())))
				.then(registerTimeInputCommand("set", this::setTimeUntilRaid))
				.then(registerTimeInputCommand("subtract", this::subtractTimeUntilRaid))
				.then(registerTimeInputCommand("add", this::addTimeUntilRaid));
	}

	private LiteralArgumentBuilder<CommandSource> registerLevelCommand() {
		return Commands.literal(NAME_ARG_LEVEL)
				.then(Commands.literal("query").executes(commandSource -> getRaidLevel(commandSource.getSource())))
				.then(Commands.literal("set")
						.then(Commands
								.argument(NAME_ARG_LEVEL,
										IntegerArgumentType.integer(RaidManager.MIN_RAID_LEVEL,
												RaidManager.MAX_RAID_LEVEL))
								.executes(commandSource -> setRaidLevel(commandSource.getSource(),
										IntegerArgumentType.getInteger(commandSource, NAME_ARG_LEVEL)))));
	}

	private LiteralArgumentBuilder<CommandSource> registerRaidCommand() {
		return Commands.literal("raid")
				.then(Commands.literal("start").executes(commandSource -> startRaid(commandSource.getSource())))
				.then(Commands.literal("win").executes(commandSource -> winRaid(commandSource.getSource())))
				.then(Commands.literal("lose").executes(commandSource -> loseRaid(commandSource.getSource())));
	}

	private LiteralArgumentBuilder<CommandSource> registerGiveNexusCommand() {
		return Commands.literal("giveNexus")
				.then(Commands.argument("target", EntityArgument.players())
						.executes(commandSource -> giveNexusToPlayer(commandSource.getSource(),
								EntityArgument.getPlayer(commandSource, "target"))));
	}

	private LiteralArgumentBuilder<CommandSource> registerRestoreDestroyedBlocksCommand() {
		return Commands.literal("restoreDestroyedBlocks")
				.executes(commandSource -> restoreDestroyedBlocks(commandSource.getSource()));
	}

	private LiteralArgumentBuilder<CommandSource> registerTimeInputCommand(String leadingLiteral,
			BiFunction<CommandSource, MCDuration, Integer> func) {
		return Commands.literal(leadingLiteral)
				.then(Commands.literal("min")
						.then(Commands.argument("minutes", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
								.executes(commandSource -> func.apply(commandSource.getSource(),
										new MCDuration().setMin(IntegerArgumentType.getInteger(commandSource, "minutes"))))))
				.then(Commands.literal("sec")
						.then(Commands.argument("seconds", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
								.executes(commandSource -> func.apply(commandSource.getSource(),
										new MCDuration().setSec(IntegerArgumentType.getInteger(commandSource, "seconds"))))))
				.then(Commands.literal(NAME_ARG_TICKS)
						.then(Commands.argument(NAME_ARG_TICKS, LongArgumentType.longArg(1, Long.MAX_VALUE))
								.executes(commandSource -> func.apply(commandSource.getSource(),
										new MCDuration(LongArgumentType.getLong(commandSource, NAME_ARG_TICKS))))));
	}

	private int giveNexusToPlayer(CommandSource source, ServerPlayerEntity target) {
		boolean success = NexusBlock.giveNexusToPlayer(target);
		if (success) {
			sendFeedback(source, "Added nexus block to inventory");
		}
		return success ? 0 : 1;
	}

	private int winRaid(CommandSource source) {
		worldManager.getRaidManager().winRaid();
		sendFeedback(source, "Raid won");
		return 0;
	}

	private int loseRaid(CommandSource source) {
		worldManager.getRaidManager().loseRaid();
		sendFeedback(source, "Raid lost");
		return 0;
	}

	private int getTimeUntilRaid(CommandSource source) {
		sendFeedback(source,
				"Time until raid: " + worldManager.getRaidTimeManager().getTimeUntilRaid().getDisplayString());
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int setTimeUntilRaid(CommandSource source, MCDuration time) {
		worldManager.getRaidTimeManager().setTimeUntilRaid(time);
		sendTimeUntilRaidFeedback(source);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int addTimeUntilRaid(CommandSource source, MCDuration time) {
		worldManager.getRaidTimeManager().addTimeUntilRaid(time);
		sendTimeUntilRaidFeedback(source);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}
	
	private int subtractTimeUntilRaid(CommandSource source, MCDuration time) {
		worldManager.getRaidTimeManager().subtractFromTimeUntilRaid(time);
		sendTimeUntilRaidFeedback(source);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int getRaidLevel(CommandSource source) {
		int level = worldManager.getRaidManager().getRaidLevel();
		sendFeedback(source, "Raid level: " + level);
		return level;
	}

	private int setRaidLevel(CommandSource source, int level) {
		worldManager.getRaidManager().setRaidLevel(level);
		sendFeedback(source, "Raid level: " + level);
		return level;
	}

	private int startRaid(CommandSource source) {
		worldManager.getRaidManager().startRaid();
		sendFeedback(source, "Raid started");
		return 0;
	}

	private int restoreDestroyedBlocks(CommandSource source) {
		worldManager.getRaidManager().restoreDestroyedBlocksMng.restoreAndClearSavedBlocks();
		sendFeedback(source, "Restored destroyed blocks");
		return 0;
	}

	private void sendTimeUntilRaidFeedback(CommandSource source) {
		sendFeedback(source,
				TEXT_TIME_UNTIL_NEXT_RAID + worldManager.getRaidTimeManager().getTimeUntilRaid().getDisplayString());
	}

	private void sendFeedback(CommandSource source, String text) {
		source.sendFeedback(new StringTextComponent(text), true);
	}
}
