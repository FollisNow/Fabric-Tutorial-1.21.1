package net.follis.goldenscarabmod.world.structure;

public enum Dir {
    N(0, 0, -1), E(1, 0, 0), S(0, 0, 1), W(-1, 0, 0), UP(0, 1, 0), DOWN(0, -1, 0);
    public final int dx, dy, dz;
    Dir(int dx, int dy, int dz) { this.dx = dx; this.dy = dy; this.dz = dz; }
    public Dir opposite() {
        return switch (this) { case N -> S; case S -> N; case E -> W; case W -> E; case UP -> DOWN; case DOWN -> UP; };
    }
    public Dir rotateCW() {
        return switch (this) { case N -> E; case E -> S; case S -> W; case W -> N; default -> this; };
    }
}


