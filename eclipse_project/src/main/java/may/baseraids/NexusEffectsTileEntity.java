package may.baseraids;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * This class handles sound and effects from the nexus. Effects are not
 * implemented as of 1.16.4-0.1.
 * 
 * @author Natascha May
 * @since 1.16.4-0.1
 */
public class NexusEffectsTileEntity extends TileEntity implements ITickableTileEntity {

	EffectInstance curEffect = null;
	double effectDistance = 50D;

	final static EffectInstance REGEN_EFFECT_AFTER_RAID_WIN = new EffectInstance(Effects.REGENERATION, 100, 0, false,
			true);

	public NexusEffectsTileEntity() {
		super(Baseraids.NEXUS_TILE_ENTITY_TYPE.get());
	}

	public void tick() {
		if(world.isRemote) return;
		addEffectsToPlayers();

		
		if (this.world.getGameTime() % 40L == 0L) {

			this.addEffectsToPlayers();
			if (Baseraids.baseraidsData.raidManager.isRaidActive()) {
				this.playSoundWithPos(Baseraids.SOUND_RAID_ACTIVE.get(), 0.5F, 1.0F);
			} else {								
				this.playSoundWithPos(SoundEvents.BLOCK_BEACON_AMBIENT, 0.25F, 0.5F);
			}

		}
	}

	public void playSoundWithPos(SoundEvent sound, float volume, float pitch) {
		this.playSound(sound, this.pos, volume, pitch);
	}
	
	public void playSound(SoundEvent sound, BlockPos pos, float volume, float pitch) {
		this.world.playSound((PlayerEntity) null, pos, sound, SoundCategory.BLOCKS, volume, pitch);
	}
	

	/**
	 * Adds the effect <code>curEffect</code> to all players in the distance
	 * <code>effectDistance</code>.
	 */
	private void addEffectsToPlayers() {
		if (world.isRemote) {
			return;
		}
		if (curEffect == null) {
			return;
		}
		if (world.getGameTime() % (curEffect.getDuration() / 2) != 0) {
			return;
		}

		AxisAlignedBB axisalignedbb = (new AxisAlignedBB(pos)).grow(effectDistance).expand(0.0D,
				(double) world.getHeight(), 0.0D);

		List<PlayerEntity> list = world.getEntitiesWithinAABB(PlayerEntity.class, axisalignedbb);
		for (PlayerEntity playerentity : list) {
			playerentity.addPotionEffect(curEffect);
		}
	}

	/**
	 * Retrieves packet to send to the client whenever this Tile Entity is resynced
	 * via World.notifyBlockUpdate. For modded TE's, this packet comes back to you
	 * clientside in {@link #onDataPacket}
	 */
	@Override
	@Nullable
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(this.pos, 3, this.getUpdateTag());
	}

	/**
	 * Called when you receive a TileEntityData packet for the location this
	 * TileEntity is currently in. On the client, the NetworkManager will always be
	 * the remote server. On the server, it will be whomever is responsible for
	 * sending the packet.
	 *
	 * @param net The NetworkManager the packet originated from
	 * @param pkt The data packet
	 */
	@Override
	public void onDataPacket(net.minecraft.network.NetworkManager net,
			net.minecraft.network.play.server.SUpdateTileEntityPacket pkt) {

	}

}
