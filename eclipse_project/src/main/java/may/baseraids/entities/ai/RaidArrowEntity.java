package may.baseraids.entities.ai;

import javax.annotation.Nullable;

import may.baseraids.RaidManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

public class RaidArrowEntity extends ArrowEntity {

	Entity shooter;
	RaidManager raidManager;
	private static final int DAMAGE = 25;

	public RaidArrowEntity(World world, LivingEntity shooter, RaidManager raidManager) {
		super(world, shooter);
		this.raidManager = raidManager;
	}

	// collision with block
	protected void func_230299_a_(BlockRayTraceResult p_230299_1_) {
		super.func_230299_a_(p_230299_1_);
		if (!raidManager.isRaidActive()) {
			return;
		}
		raidManager.blockBreakProgressMng.addProgress(shooter, p_230299_1_.getPos(), DAMAGE);
	}

	public void setShooter(@Nullable Entity entityIn) {
		super.setShooter(entityIn);
		shooter = entityIn;
	}
	
}
