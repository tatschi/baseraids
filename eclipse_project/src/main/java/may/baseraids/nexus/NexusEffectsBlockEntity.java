package may.baseraids.nexus;

import java.util.List;

import may.baseraids.Baseraids;
import may.baseraids.config.ConfigOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * This class handles sound and effects from the nexus.
 * 
 * @author Natascha May
 */
public class NexusEffectsBlockEntity extends BlockEntity {

	static List<NexusEffects.NexusEffect> curEffects = null;
	static int lastWonRaidLevel = 0;
	private static final double EFFECT_DISTANCE = 30D;

	public NexusEffectsBlockEntity(BlockPos pos, BlockState state) {
		super(Baseraids.NEXUS_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState blockState, T type) {
		if (level.isClientSide)
			return;

		if (level.getGameTime() % 40L == 0L) {

			// add effects
			if (curEffects != null) {
				curEffects.forEach(x -> addEffectsToPlayers(level, pos, NexusEffects.getEffectInstance(x)));
			}

			// play sound
			if (Baseraids.worldManager.getRaidManager().isRaidActive()) {
				if (ConfigOptions.getEnableSoundRaidHeartbeat()) {
					playSoundWithPos(level, pos, Baseraids.SOUND_RAID_ACTIVE.get(), 300F, 1.0F);
				}
			} else {
				if (ConfigOptions.getEnableSoundNexusAmbient()) {
					playSoundWithPos(level, pos, SoundEvents.BEACON_AMBIENT, 0.25F, 0.5F);
				}
			}

		}
	}

	public static void setLastWonRaidLevel(int level) {
		lastWonRaidLevel = level;
		curEffects = NexusEffects.effects.get(level - 1);
	}

	public static void playSoundWithPos(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
		level.playSound((Player) null, pos, sound, SoundSource.BLOCKS, volume, pitch);
	}

	/**
	 * Adds the current effects {@link #curEffects} to all players in the distance
	 * {@link #EFFECT_DISTANCE}.
	 */
	public static void addEffectsToPlayers(Level level, BlockPos pos, MobEffectInstance mobEffectInstance) {
		if (level.isClientSide) {
			return;
		}
		if (mobEffectInstance == null) {
			return;
		}

		AABB axisalignedbb = (new AABB(pos)).inflate(EFFECT_DISTANCE).expandTowards(0.0D, level.getHeight(), 0.0D);

		List<Player> list = level.getEntitiesOfClass(Player.class, axisalignedbb);
		for (Player player : list) {
			player.addEffect(mobEffectInstance);
		}
	}

}
