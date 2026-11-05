package net.follis.goldenscarabmod.entity.swarm;

/**
 * Hooks the client plugs into so the (common) swarm code can trigger client-only effects without ever
 * referencing a client class. On a dedicated server nothing sets them, so nothing happens.
 */
public final class SwarmFx {

    public interface SlamHandler {
        /** Something hit the ground hard at (x, y, z): shake the screen of players within radius blocks. */
        void slam(double x, double y, double z, float strength, double radius);
    }

    /** Set by the renderer when the client starts. */
    public static SlamHandler slamHandler;

    private SwarmFx() {
    }
}