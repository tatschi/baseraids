package may.baseraids.sounds;

import net.minecraft.client.audio.TickableSound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TileEntityTickableSound extends TickableSound{
	protected final TileEntity entity;
	
	public TileEntityTickableSound(SoundEvent sound, SoundCategory category, TileEntity entity) {
	      this(sound, category, 1.0F, 1.0F, entity);
	   }

	   public TileEntityTickableSound(SoundEvent sound, SoundCategory category, float volume, float pitch, TileEntity entity) {
	      super(sound, category);
	      this.volume = volume;
	      this.pitch = pitch;
	      this.entity = entity;
	      this.x = (double)((float)this.entity.getPos().getX());
	      this.y = (double)((float)this.entity.getPos().getY());
	      this.z = (double)((float)this.entity.getPos().getZ());
	   }


	   public void tick() {
	      if (this.entity.isRemoved()) {
	         this.finishPlaying();
	      } else {
	    	  this.x = (double)((float)this.entity.getPos().getX());
		      this.y = (double)((float)this.entity.getPos().getY());
		      this.z = (double)((float)this.entity.getPos().getZ());
	      }
	   }
}
