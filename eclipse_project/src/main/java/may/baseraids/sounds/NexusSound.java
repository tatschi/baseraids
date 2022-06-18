package may.baseraids.sounds;

import may.baseraids.Baseraids;
import may.baseraids.NexusEffectsTileEntity;
import may.baseraids.RaidManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class NexusSound extends TileEntityTickableSound {

	RaidManager raidManager;
	boolean hasSwitchedSound;

	public NexusSound(SoundEvent sound, SoundCategory category, float volume, float pitch,
			NexusEffectsTileEntity entity) {
		super(sound, category, volume, pitch, entity);
		this.raidManager = Baseraids.baseraidsData.raidManager;
		this.repeat = true;
		this.hasSwitchedSound = false;
		Minecraft.getInstance().getSoundHandler().playOnNextTick(this);
	}

	private void playNextSound() {
		if (raidManager.isRaidActive()) {
			new NexusRaidSound((NexusEffectsTileEntity) entity);
		} else
			new NexusNoRaidSound((NexusEffectsTileEntity) entity);
	}

	public void tick() {
		super.tick();
		if (this.shouldSwitchSound()) {
			playNextSound();
			this.finishPlaying();
		}
	}

	protected abstract boolean shouldSwitchSound();
	
	protected boolean hasSwitchedSound() {
		return hasSwitchedSound;
	}

}
