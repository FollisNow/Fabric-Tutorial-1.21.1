package net.follis.goldenscarabmod.entity.swarm;

/**
 * A formation that plays a finite attack animation (shape throw, burst, ...).
 * SwarmState starts one when the server announces a pattern, and goes back to the
 * humanoid formation once isFinished() returns true.
 */
public interface PatternFormation extends SwarmFormation {
    boolean isFinished();
}