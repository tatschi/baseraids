package may.baseraids;

import java.util.List;
import may.baseraids.sounds.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;

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

	final static SoundEffect NEXUS_AMBIENT_SOUND = new SoundEffect(SoundEvents.BLOCK_BEACON_AMBIENT,
			SoundCategory.BLOCKS, 2.0F, 0.5F, 80);

	public NexusEffectsTileEntity() {
		super(Baseraids.NEXUS_TILE_ENTITY_TYPE.get());
	}

	public void tick() {
		addEffectsToPlayers();
		if (world.getGameTime() % (NEXUS_AMBIENT_SOUND.intervalInTicks - 5) == 0L) {
			NEXUS_AMBIENT_SOUND.playSound(world, null, pos);
		}
	}

	/**
	 * Adds the effect <code>curEffect</code> to all players in the distance <code>effectDistance</code>.
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

}
