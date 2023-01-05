package may.baseraids.nexus;

import java.util.List;

import may.baseraids.Baseraids;
import may.baseraids.config.ConfigOptions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * This class handles sound and effects from the nexus.
 * 
 * @author Natascha May
 */
public class NexusEffectsTileEntity extends TileEntity implements ITickableTileEntity {

	List<NexusEffects.NexusEffect> curEffects = null;
	int lastWonRaidLevel = 0;
	double effectDistance = 30D;

	public NexusEffectsTileEntity() {
		super(Baseraids.NEXUS_TILE_ENTITY_TYPE.get());
	}

	public void tick() {
		if (world.isRemote)
			return;

		if (this.world.getGameTime() % 40L == 0L) {

			// add effects
			if (curEffects != null) {
				curEffects.forEach(x -> this.addEffectsToPlayers(NexusEffects.getEffectInstance(x)));
			}

			// play sound
			if (Baseraids.baseraidsData.raidManager.isRaidActive()) {
				if (Boolean.TRUE.equals(ConfigOptions.enableSoundRaidHeartbeat.get())) {
					this.playSoundWithPos(Baseraids.SOUND_RAID_ACTIVE.get(), 300F, 1.0F);
				}
			} else {
				if (Boolean.TRUE.equals(ConfigOptions.enableSoundNexusAmbient.get())) {
					this.playSoundWithPos(SoundEvents.BLOCK_BEACON_AMBIENT, 0.25F, 0.5F);
				}
			}

		}
	}
	
	public void setLastWonRaidLevel(int level) {
		lastWonRaidLevel = level;
		curEffects = NexusEffects.effects.get(level-1);
	}

	public void playSoundWithPos(SoundEvent sound, float volume, float pitch) {
		this.playSound(sound, this.pos, volume, pitch);
	}

	public void playSound(SoundEvent sound, BlockPos pos, float volume, float pitch) {
		this.world.playSound((PlayerEntity) null, pos, sound, SoundCategory.BLOCKS, volume, pitch);
	}

	/**
	 * Adds the current effects {@link #curEffects} to all players in the distance
	 * {@link #effectDistance}.
	 */
	public void addEffectsToPlayers(EffectInstance effect) {
		if (world.isRemote) {
			return;
		}
		if (effect == null) {
			return;
		}

		AxisAlignedBB axisalignedbb = (new AxisAlignedBB(pos)).grow(effectDistance).expand(0.0D,
				(double) world.getHeight(), 0.0D);

		List<PlayerEntity> list = world.getEntitiesWithinAABB(PlayerEntity.class, axisalignedbb);
		for (PlayerEntity playerentity : list) {
			playerentity.addPotionEffect(effect);
		}
	}

	@Override
	public void read(BlockState state, CompoundNBT nbt) {
		super.read(state, nbt);
		lastWonRaidLevel = nbt.getInt("lastWonRaidLevel");
		curEffects = NexusEffects.effects.get(lastWonRaidLevel-1);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);
		compound.putInt("lastWonRaidLevel", lastWonRaidLevel);
		return compound;
	}


}
