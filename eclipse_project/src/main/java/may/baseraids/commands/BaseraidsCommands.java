package may.baseraids.commands;

import java.util.function.BiFunction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import may.baseraids.MCDuration;
import may.baseraids.RaidManager;
import may.baseraids.WorldManager;
import may.baseraids.nexus.NexusBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * This class defines the commands for this mod.
 * 
 * @author Natascha May
 */
public class BaseraidsCommands {

	private static final String NAME_ARG_TICKS = "ticks";
	private static final String NAME_ARG_LEVEL = "level";
	private WorldManager worldManager;

	public BaseraidsCommands(WorldManager worldManager) {
		this.worldManager = worldManager;
	}

	public void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher
				.register(Commands.literal("baseraids").requires(commandSource -> commandSource.hasPermission(2))
						.then(registerTimeUntilRaidCommand()).then(registerLevelCommand()).then(registerRaidCommand())
						.then(registerGiveNexusCommand()).then(registerRestoreDestroyedBlocksCommand()));
	}

	private LiteralArgumentBuilder<CommandSourceStack> registerTimeUntilRaidCommand() {
		return Commands.literal("timeUntilRaid")
				.then(Commands.literal("query").executes(commandSource -> getTimeUntilRaid(commandSource.getSource())))
				.then(registerTimeInputCommand("set", this::setTimeUntilRaid))
				.then(registerTimeInputCommand("subtract", this::subtractTimeUntilRaid))
				.then(registerTimeInputCommand("add", this::addTimeUntilRaid));
	}

	private LiteralArgumentBuilder<CommandSourceStack> registerLevelCommand() {
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

	private LiteralArgumentBuilder<CommandSourceStack> registerRaidCommand() {
		return Commands.literal("raid")
				.then(Commands.literal("start").executes(commandSource -> startRaid(commandSource.getSource())))
				.then(Commands.literal("win").executes(commandSource -> winRaid(commandSource.getSource())))
				.then(Commands.literal("lose").executes(commandSource -> loseRaid(commandSource.getSource())));
	}

	private LiteralArgumentBuilder<CommandSourceStack> registerGiveNexusCommand() {
		return Commands.literal("giveNexus")
				.then(Commands.argument("target", EntityArgument.players())
						.executes(commandSource -> giveNexusToPlayer(commandSource.getSource(),
								EntityArgument.getPlayer(commandSource, "target"))));
	}

	private LiteralArgumentBuilder<CommandSourceStack> registerRestoreDestroyedBlocksCommand() {
		return Commands.literal("restoreDestroyedBlocks")
				.executes(commandSource -> restoreDestroyedBlocks(commandSource.getSource()));
	}

	private LiteralArgumentBuilder<CommandSourceStack> registerTimeInputCommand(String leadingLiteral,
			BiFunction<CommandSourceStack, MCDuration, Integer> func) {
		return Commands.literal(leadingLiteral).then(Commands.literal("min")
				.then(Commands.argument("minutes", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
						.executes(commandSource -> func.apply(commandSource.getSource(),
								new MCDuration().setMin(IntegerArgumentType.getInteger(commandSource, "minutes"))))))
				.then(Commands.literal("sec").then(Commands
						.argument("seconds", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
						.executes(commandSource -> func.apply(commandSource.getSource(),
								new MCDuration().setSec(IntegerArgumentType.getInteger(commandSource, "seconds"))))))
				.then(Commands.literal(NAME_ARG_TICKS)
						.then(Commands.argument(NAME_ARG_TICKS, LongArgumentType.longArg(1, Long.MAX_VALUE))
								.executes(commandSource -> func.apply(commandSource.getSource(),
										new MCDuration(LongArgumentType.getLong(commandSource, NAME_ARG_TICKS))))));
	}

	private int giveNexusToPlayer(CommandSourceStack commandSourceStack, ServerPlayer target) {
		boolean success = NexusBlock.giveNexusToPlayer(target);
		if (success) {
			commandSourceStack.sendSuccess(Component.translatable("baseraids.success.add_nexus_to_inventory"), true);
		}
		return success ? 0 : 1;
	}

	private int winRaid(CommandSourceStack commandSourceStack) {
		worldManager.getRaidManager().winRaid();
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.raid_won"), true);
		return 0;
	}

	private int loseRaid(CommandSourceStack commandSourceStack) {
		worldManager.getRaidManager().loseRaid();
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.raid_lost"), true);
		return 0;
	}

	private int getTimeUntilRaid(CommandSourceStack commandSourceStack) {
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.time_until_raid",
				worldManager.getRaidTimeManager().getTimeUntilRaid().getDisplayString()), true);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int setTimeUntilRaid(CommandSourceStack commandSourceStack, MCDuration time) {
		worldManager.getRaidTimeManager().setTimeUntilRaid(time);
		sendTimeUntilRaidFeedback(commandSourceStack);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int addTimeUntilRaid(CommandSourceStack commandSourceStack, MCDuration time) {
		worldManager.getRaidTimeManager().addTimeUntilRaid(time);
		sendTimeUntilRaidFeedback(commandSourceStack);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int subtractTimeUntilRaid(CommandSourceStack commandSourceStack, MCDuration time) {
		worldManager.getRaidTimeManager().subtractFromTimeUntilRaid(time);
		sendTimeUntilRaidFeedback(commandSourceStack);
		return worldManager.getRaidTimeManager().getTimeUntilRaid().getSec();
	}

	private int getRaidLevel(CommandSourceStack commandSourceStack) {
		int level = worldManager.getRaidManager().getRaidLevel();
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.next_level", level), true);
		return level;
	}

	private int setRaidLevel(CommandSourceStack commandSourceStack, int level) {
		worldManager.getRaidManager().setRaidLevel(level);
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.next_level", level), true);
		return level;
	}

	private int startRaid(CommandSourceStack commandSourceStack) {
		worldManager.getRaidManager().startRaid();
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.raid_start"), true);
		return 0;
	}

	private int restoreDestroyedBlocks(CommandSourceStack commandSourceStack) {
		worldManager.getRaidManager().restoreDestroyedBlocksMng.restoreAndClearSavedBlocks();
		commandSourceStack.sendSuccess(Component.translatable("baseraids.success.restore_destroyed_blocks"), true);
		return 0;
	}

	private void sendTimeUntilRaidFeedback(CommandSourceStack commandSourceStack) {
		commandSourceStack.sendSuccess(Component.translatable("baseraids.subtitle.time_until_raid",
				worldManager.getRaidTimeManager().getTimeUntilRaid().getDisplayString()), true);
	}

}
