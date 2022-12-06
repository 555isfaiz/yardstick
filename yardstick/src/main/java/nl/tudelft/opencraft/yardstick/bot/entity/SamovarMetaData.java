package nl.tudelft.opencraft.yardstick.bot.entity;

import nl.tudelft.opencraft.yardstick.bot.world.Waypoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SamovarMetaData {
    private AtomicBoolean wasInStartedWayPoint = new AtomicBoolean(false);
    private final Waypoint startWayPoint;
    private Map<Waypoint, Double> waypoints;
    private AtomicBoolean wasIdleTime = new AtomicBoolean(false);

    public SamovarMetaData(Waypoint startWayPoint, Map<Waypoint, Double> waypoints) {
        this.startWayPoint = startWayPoint;
        this.waypoints = waypoints;
    }

    public AtomicBoolean getWasIdleTime() {
        return wasIdleTime;
    }

    public AtomicBoolean getWasInStartedWayPoint() {
        return wasInStartedWayPoint;
    }

    public Waypoint getStartWayPoint() {
        return startWayPoint;
    }

    public Map<Waypoint, Double> getWaypoints() {
        return waypoints;
    }
}
