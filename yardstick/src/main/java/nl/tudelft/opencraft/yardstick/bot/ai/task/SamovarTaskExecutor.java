package nl.tudelft.opencraft.yardstick.bot.ai.task;

import nl.tudelft.opencraft.yardstick.Yardstick;
import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.pathfinding.PathNode;
import nl.tudelft.opencraft.yardstick.bot.entity.BotPlayer;
import nl.tudelft.opencraft.yardstick.bot.world.Block;
import nl.tudelft.opencraft.yardstick.bot.world.ChunkNotLoadedException;
import nl.tudelft.opencraft.yardstick.bot.world.Material;
import nl.tudelft.opencraft.yardstick.util.Vector3d;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SamovarTaskExecutor extends AbstractTaskExecutor {

    private final static double speed = 0.15;
    private final static int defaultTimeout = 6000;
    private final static int timeToAbort = 100;
    private final long startTime;
    private final Vector3i target;
    private Future<PathNode> pathFuture;
    private PathNode nextStep;
    private int ticksSinceStepChange = 0;
    private int timeout = defaultTimeout;

    private Callable<PathNode> task = new Callable<>() {
        @Override
        public PathNode call() {
            BotPlayer player = bot.getPlayer();
            return search(player.getLocation().intVector(), target);
        }
    };

    public SamovarTaskExecutor(Bot bot, final Vector3i target) {
        super(bot);
        this.target = target;
        if (bot.getPlayer().getLocation().intVector().equals(target)) {
            logger.warn("Useless walk task. Bot and given target location equal.");
        }
        pathFuture = Yardstick.THREAD_POOL.submit(task);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected TaskStatus onTick() {
        if (pathFuture != null && !pathFuture.isDone()) {
            // If we're still calculating the path:
            // Check timeout
            if (timeout > 0 && System.currentTimeMillis() - startTime > timeout) {
                pathFuture.cancel(true);
                nextStep = null;
                return TaskStatus.forFailure(String.format("Path search from %s to %s timed out (%s ms)", bot.getPlayer().getLocation(), target, timeout));
            } else {
                return TaskStatus.forInProgress();
            }

        } else if (pathFuture != null && pathFuture.isDone() && !pathFuture.isCancelled()) {
            // If we've found a path successfully
            try {
                nextStep = pathFuture.get();
                ticksSinceStepChange = 0;
                logger.info(MessageFormat.format("bot {0} walking towards {1}", bot.getName(), target));
            } catch (InterruptedException e) {
                return TaskStatus.forFailure(e.getMessage(), e);
            } catch (ExecutionException e) {
                return TaskStatus.forFailure(e.getMessage(), e.getCause());
            } finally {
                pathFuture = null;
            }
        }

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
        if (ticksSinceStepChange > timeToAbort) {
            nextStep = null;
            return TaskStatus.forFailure("Too many ticks since step change");
        }

        // Get locations
        Vector3d moveLoc = player.getLocation();

        // Step
        Vector3i stepTargetBlock = nextStep.getLocation();
        if (stepTargetBlock == null) {
            return TaskStatus.forFailure("No next step");
        }
        Vector3d stepTarget = stepTargetBlock.doubleVector();

        // Stand on the center of a block
        stepTarget = stepTarget.add(0.5, 0, 0.5);

        double stepX = stepTarget.getX(), stepY = stepTarget.getY(), stepZ = stepTarget.getZ();

        if (moveLoc.getX() != stepX) {
            double offsetX = moveLoc.getX() < stepX ? Math.min(this.speed, stepX - moveLoc.getX()) : Math.max(-this.speed, stepX - moveLoc.getX());
            moveLoc = moveLoc.add(new Vector3d(offsetX, 0, 0));
        }

        if (moveLoc.getZ() != stepZ) {
            double offsetZ = moveLoc.getZ() < stepZ ? Math.min(this.speed, stepZ - moveLoc.getZ()) : Math.max(-this.speed, stepZ - moveLoc.getZ());
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
        if (pathFuture != null && !pathFuture.isDone()) {
            pathFuture.cancel(true);
        }
        nextStep = null;
    }

    private PathNode search(Vector3i start, Vector3i end) {
        PathNode tmpNode = new PathNode(start);
        Vector3i difVector;
        PathNode nextNode;

        int xDiff = Math.abs(start.getX() - end.getX());
        int zDiff = Math.abs(start.getZ() - end.getZ());

        int xShift = (start.getX() > end.getX())? -1 : 1;
        int zShift = (start.getZ() > end.getZ())? -1 : 1;

        for (int i = 0; i < Math.min(xDiff, zDiff); i++) {

            difVector = new Vector3i(xShift, 0, zShift);

            nextNode = new PathNode(tmpNode.getLocation().add(difVector));
            tmpNode.setNext(nextNode);
            nextNode.setPrevious(tmpNode);
            tmpNode = nextNode;
        }

        for (int i = 0; i < Math.abs(xDiff - zDiff); i++) {

            difVector = (xDiff > zDiff)? new Vector3i(xShift, 0, 0): new Vector3i(0,0,zShift);
            nextNode = new PathNode(tmpNode.getLocation().add(difVector));

            tmpNode.setNext(nextNode);
            nextNode.setPrevious(tmpNode);
            tmpNode = nextNode;
        }

        return buildPath(tmpNode);
    }

    private PathNode buildPath(PathNode end) {
        PathNode pointer = end;
        while (pointer.getPrevious() != null) {
            pointer.getPrevious().setNext(pointer);
            pointer = pointer.getPrevious();
        }
        return pointer;
    }
}
