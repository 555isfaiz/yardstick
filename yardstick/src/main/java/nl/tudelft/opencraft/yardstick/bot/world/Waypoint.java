package nl.tudelft.opencraft.yardstick.bot.world;

import nl.tudelft.opencraft.yardstick.util.Vector2i;

public class Waypoint extends Vector2i {

    final double weight;
    final int level;

    public Waypoint(int x, int z, double weight, int level) {
        super(x, z);
        this.weight = weight;
        this.level = level;
    }
}
