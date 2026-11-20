package net.follis.goldenscarabmod.pattern;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/** The swords that levitate around a boss's body. A pattern takes them from here, so they visibly leave the body. */
public interface SwordPool {
    /** How many swords are on the body right now. */
    int swordsAvailable();

    /** Takes one sword off the body and returns where it hung in the world, or null if none is left. */
    @Nullable Vec3d takeSwordPosition();

    /** Height above the entity's feet of the middle of the body, where patterns centre themselves. */
    double bodyCenterHeight();

    /** A sword that left the body means to come back: the body does not regrow it in the meantime. */
    void lendSword();

    /** A lent sword is gone for good (it timed out, or was removed). */
    void forgetLent();

    /** Where a returning sword should fly to: the place on the body where the next sword will hang. */
    Vec3d returnPosition();

    /** A lent sword arrived: it hangs on the body again. */
    boolean takeBack();

    /** True for this slot when `taken` of the `total` swords of a pattern come from the body: they are spread evenly. */
    static boolean detaches(int slot, int total, int taken) {
        return total > 0 && (slot * taken) / total != ((slot + 1) * taken) / total;
    }

    /** A view of a pool that gives at most `max` swords: each pattern says how many it may take. */
    static SwordPool limit(SwordPool pool, int max) {
        int[] left = {max};
        return new SwordPool() {
            @Override
            public int swordsAvailable() {
                return Math.min(left[0], pool.swordsAvailable());
            }

            @Override
            @Nullable
            public Vec3d takeSwordPosition() {
                if (left[0] <= 0) return null;
                Vec3d p = pool.takeSwordPosition();
                if (p != null) left[0]--;
                return p;
            }

            @Override
            public double bodyCenterHeight() {
                return pool.bodyCenterHeight();
            }

            @Override
            public void lendSword() {
                pool.lendSword();
            }

            @Override
            public void forgetLent() {
                pool.forgetLent();
            }

            @Override
            public Vec3d returnPosition() {
                return pool.returnPosition();
            }

            @Override
            public boolean takeBack() {
                return pool.takeBack();
            }
        };
    }
}