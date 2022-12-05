package nl.tudelft.opencraft.yardstick.bot.ai.task;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.pathfinding.PathNode;
import nl.tudelft.opencraft.yardstick.bot.entity.BotPlayer;
import nl.tudelft.opencraft.yardstick.bot.world.Block;
import nl.tudelft.opencraft.yardstick.bot.world.ChunkNotLoadedException;
import nl.tudelft.opencraft.yardstick.bot.world.Material;
import nl.tudelft.opencraft.yardstick.util.Vector3d;
import nl.tudelft.opencraft.yardstick.util.Vector3i;
import java.util.Map;

public class SamovarTaskExecutor extends AbstractTaskExecutor {

    private static double speed = 0.15, jumpFactor = 3, fallFactor = 4, liquidFactor = 0.5;
    private PathNode nextStep;
    private int ticksSinceStepChange = 0;
    private final Map<Vector3i, Long> waypoints;
    private long start = -1;

    public SamovarTaskExecutor(Bot bot, final PathNode nextStep, Map<Vector3i, Long> waypoints) {
        super(bot);
        this.nextStep = nextStep;
        this.waypoints = waypoints;
    }

    @Override
    protected TaskStatus onTick() {

        if (waypoints.containsKey(nextStep.getLocation())) {
            if (start == -1)
                start = System.currentTimeMillis();
            if(System.currentTimeMillis() - start < waypoints.get(nextStep.getLocation()))
                return TaskStatus.forInProgress();
        }
        this.start = -1;

        // If we have no more steps to do, we're done
        if (nextStep == null) {
            return TaskStatus.forSuccess();
        }

        BotPlayer player = bot.getPlayer();

        // Skip the step if the next step is close by
        if (nextStep.getNext() != null && player.getLocation().distanceSquared(nextStep.getNext().getLocation().doubleVector()) < 0.05) {
            nextStep = nextStep.getNext();
            ticksSinceStepChange = 0;
        }

        // If the player is too far away from the next step
        // Abort for now
        if (player.getLocation().distanceSquared(nextStep.getLocation().doubleVector()) > 5.0) {
            logger.info(String.format("Strayed from path. %s -> %s", player.getLocation(), nextStep.getLocation()));
            // TODO: Fix later
            //TaskStatus status = TaskStatus.forInProgress();
            //pathFuture = service.submit(task);
            return TaskStatus.forFailure("Strayed from path. %s -> %s");
        }

        // Keep track of how many ticks a step takes
        // If a step takes too many ticks, abort
        ticksSinceStepChange++;
        if (ticksSinceStepChange > 80) {
            nextStep = null;
            return TaskStatus.forFailure("Too many ticks since step change");
        }

        // Get locations
        Vector3d moveLoc = player.getLocation();
        Vector3i blockLoc = moveLoc.intVector().add(new Vector3i(0, -1, 0));
        Block thisBlock;

        try {
            thisBlock = bot.getWorld().getBlockAt(blockLoc);
        } catch (ChunkNotLoadedException e) {
            // TODO: Fix: Wait until chunk is loaded.
            logger.warn("Block under player: {}", blockLoc);
            logger.warn("Player at {}", moveLoc);
            return TaskStatus.forFailure(e.getMessage());
        }

        // Step
        Vector3i stepTargetBlock = nextStep.getLocation();
        if (stepTargetBlock == null) {
            return TaskStatus.forFailure("No next step");
        }
        Vector3d stepTarget = stepTargetBlock.doubleVector();

        // Stand on the center of a block
        stepTarget = stepTarget.add(0.5, 0, 0.5);

        // Calculate speed
        double moveSpeed = this.speed;
        boolean inLiquid = false; // TODO: player.isInLiquid();
        if (Material.getById(thisBlock.getTypeId()) == Material.SOUL_SAND) {
            if (Material.getById(thisBlock.getTypeId()) == Material.SOUL_SAND) {
                // Soulsand makes us shorter 8D
                stepTarget = stepTarget.add(0, -0.12, 0);
            }
            moveSpeed *= liquidFactor;
        } else if (inLiquid) {
            moveSpeed *= liquidFactor;
        }

        double stepX = stepTarget.getX(), stepY = stepTarget.getY(), stepZ = stepTarget.getZ();

        // See if we're climbing, or jumping
        if (moveLoc.getY() != stepY) {
            boolean canClimbBlock = false;
            try {
                canClimbBlock = bot.getPathFinder().getWorldPhysics().canClimb(moveLoc.intVector());
            } catch (ChunkNotLoadedException e) {
                return TaskStatus.forInProgress();
            }
            if (!inLiquid && !canClimbBlock) {
                if (moveLoc.getY() < stepY) {
                    moveSpeed *= jumpFactor;
                } else {
                    moveSpeed *= fallFactor;
                }
            }

            // Set new Y-coord
            double offsetY = moveLoc.getY() < stepY ? Math.min(moveSpeed, stepY - moveLoc.getY()) : Math.max(-moveSpeed, stepY - moveLoc.getY());
            moveLoc = moveLoc.add(new Vector3d(0, offsetY, 0));
        }

        if (moveLoc.getX() != stepX) {
            double offsetX = moveLoc.getX() < stepX ? Math.min(moveSpeed, stepX - moveLoc.getX()) : Math.max(-moveSpeed, stepX - moveLoc.getX());
            moveLoc = moveLoc.add(new Vector3d(offsetX, 0, 0));
        }

        if (moveLoc.getZ() != stepZ) {
            double offsetZ = moveLoc.getZ() < stepZ ? Math.min(moveSpeed, stepZ - moveLoc.getZ()) : Math.max(-moveSpeed, stepZ - moveLoc.getZ());
            moveLoc = moveLoc.add(new Vector3d(0, 0, offsetZ));
        }

        // Send new player location to server
        bot.getController().updateLocation(moveLoc);

        if (moveLoc.equals(stepTarget)) {
            nextStep = nextStep.getNext();
            ticksSinceStepChange = 0;
        }

        return TaskStatus.forInProgress();
    }

    @Override
    protected void onStop() {
        nextStep = null;
    }
}
