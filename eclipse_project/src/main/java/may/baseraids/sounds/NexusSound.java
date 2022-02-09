package may.baseraids.sounds;

import may.baseraids.Baseraids;
import may.baseraids.NexusEffectsTileEntity;
import may.baseraids.RaidManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class NexusSound extends TileEntityTickableSound {

	RaidManager raidManager;

	public NexusSound(SoundEvent sound, SoundCategory category, float volume, float pitch,
			NexusEffectsTileEntity entity) {
		super(sound, category, volume, pitch, entity);
		this.raidManager = Baseraids.baseraidsData.raidManager;
		Minecraft.getInstance().getSoundHandler().playOnNextTick(this.getNextSound());
	}

	private ITickableSound getNextSound() {
		if (raidManager.isRaidActive()) {
			return new NexusRaidSound((NexusEffectsTileEntity) entity);
		} else
			return new NexusNoRaidSound((NexusEffectsTileEntity) entity);
	}

	public void tick() {
		if (this.shouldSwitchSound() && !this.isDonePlaying()) {
			Minecraft.getInstance().getSoundHandler().playOnNextTick(this.getNextSound());
			this.finishPlaying();
		}
	}

	protected abstract boolean shouldSwitchSound();

}
