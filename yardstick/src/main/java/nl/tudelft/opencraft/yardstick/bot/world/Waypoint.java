package nl.tudelft.opencraft.yardstick.bot.world;

import nl.tudelft.opencraft.yardstick.util.Vector3i;

public class Waypoint {

    private final double weight;
    private final int level;
    private int x;
    private int z;

    public Waypoint(int x, int z, double weight, int level) {
        this.x = x;
        this.z = z;
        this.weight = weight;
        this.level = level;
    }

    public Vector3i getHighestWalkTarget(World world) throws ChunkNotLoadedException {
        return new Vector3i(x, world.getHighestBlockAt(x, z).getY() + 1, z);
    }

    public double distance(Waypoint a) {
        return Math.sqrt(distanceSquared(a));
    }

    public double distanceSquared(Waypoint a) {
        double dx = x - a.x;
        double dz = z - a.z;
        return dx * dx + dz * dz;
    }

    public double getWeight() {
        return weight;
    }

    public int getLevel() {
        return level;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
}
