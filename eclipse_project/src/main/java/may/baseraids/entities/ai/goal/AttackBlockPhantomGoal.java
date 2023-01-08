package may.baseraids.entities.ai.goal;

import java.util.Random;

import com.mojang.math.Vector3d;

import may.baseraids.RaidManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Phantom;

public class AttackBlockPhantomGoal extends AttackBlockGoal<Phantom> {

	private int tickDelay;
	private Random rand = new Random();
	protected static final int PHANTOM_DAMAGE = 10;

	public AttackBlockPhantomGoal(Phantom entity, RaidManager raidManager) {
		super(entity, raidManager);
	}

	@Override
	public boolean shouldExecute() {
		if (!raidManager.isRaidActive()) {
			return false;
		}

		if (entity.getAttackTarget() != null) {
			return false;
		}

		if (findTargetTicks > 2 * FIND_TARGET_MAX_TICKS) {
			findTargetTicks = 0;
			return false;
		}

		if (findTargetTicks > FIND_TARGET_MAX_TICKS) {
			findTargetTicks++;
			return false;
		}

		return true;
	}

	@Override
	public void startExecuting() {
		entity.attackPhase = Phantom.AttackPhase.CIRCLE;
	}

	public void attackBlockPhantom() {
		if(entity.attackPhase == Phantom.AttackPhase.CIRCLE) {
			pickAttack();
		}
		if(entity.attackPhase == Phantom.AttackPhase.SWOOP) {
			sweepAttack();
		}		
	}
	
	private void pickAttack() {
		if (entity.attackPhase == Phantom.AttackPhase.CIRCLE) {
			--this.tickDelay;
			if (this.tickDelay <= 0) {
				entity.attackPhase = Phantom.AttackPhase.SWOOP;
				this.setOrbitPositionForSwoopPhase();
				this.tickDelay = 80 + rand.nextInt(1200);
				entity.playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 10.0F, 0.95F + rand.nextFloat() * 0.1F);
			}
		}
	}
	
	private void sweepAttack() {		
        entity.orbitOffset = new Vector3d(target.getX(), target.getY(), target.getZ());
        AxisAlignedBB originalBB = entity.getBoundingBox();
        entity.setBoundingBox(originalBB.grow(0.2F));
        if (entity.collidedHorizontally && isBlockInMeleeRange(target)) {
           
           boolean wasBroken = raidManager.globalBlockBreakProgressMng.addProgress(target, PHANTOM_DAMAGE);		
			if(wasBroken) {
				target = null;
			}				
			entity.attackPhase = Phantom.AttackPhase.CIRCLE;
           if (!entity.isSilent()) {
        	   entity.world.playEvent(1039, entity.getPosition(), 0);
           }
           resetAttack();
           return;           
        }
        entity.setBoundingBox(originalBB);
        if (entity.hurtTime > 0) {
        	entity.attackPhase = Phantom.AttackPhase.CIRCLE;
        }
	}
	
	private void resetAttack() {
		entity.orbitPosition = entity.getHomePosition();
		entity.orbitOffset = Vector3d.ZERO;
	}
	
	@Override
	protected void attackTarget() {
		attackBlockPhantom();
	}

	// copied from PickAttackGoal#func_203143_f
	private void setOrbitPositionForSwoopPhase() {
		entity.orbitPosition = target.up(20 + rand.nextInt(20));
		if (entity.orbitPosition.getY() < entity.world.getSeaLevel()) {
			entity.orbitPosition = new BlockPos(entity.orbitPosition.getX(), entity.world.getSeaLevel() + 1,
					entity.orbitPosition.getZ());
		}

	}

}
