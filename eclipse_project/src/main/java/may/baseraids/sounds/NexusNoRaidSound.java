package may.baseraids.sounds;

import may.baseraids.NexusEffectsTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;

public class NexusNoRaidSound extends NexusSound {

	public NexusNoRaidSound(NexusEffectsTileEntity entity) {
		super(SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 2.0F, 0.5F, entity);
	      this.repeat = true;
	      this.repeatDelay = 0;
	}

	@Override
	protected boolean shouldSwitchSound() {
		return raidManager.isRaidActive();
	}

}
