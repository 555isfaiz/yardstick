package nl.tudelft.opencraft.yardstick.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.task.BreakBlocksTaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.PlaceBlocksTaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskStatus;
import nl.tudelft.opencraft.yardstick.bot.ai.task.WalkTaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.world.Block;
import nl.tudelft.opencraft.yardstick.bot.world.BlockFace;
import nl.tudelft.opencraft.yardstick.bot.world.ChunkNotLoadedException;
import nl.tudelft.opencraft.yardstick.bot.world.Material;
import nl.tudelft.opencraft.yardstick.model.BotStates.BotState;
import nl.tudelft.opencraft.yardstick.model.BotStates.BotStateBuilder;
import nl.tudelft.opencraft.yardstick.model.BotStates.BotStateDestroyer;
import nl.tudelft.opencraft.yardstick.model.BotStates.BotStateExplorer;
import nl.tudelft.opencraft.yardstick.util.Vector3d;
import nl.tudelft.opencraft.yardstick.util.Vector3i;
import nl.tudelft.opencraft.yardstick.util.ZigZagRange;

public class MyRealisticModel implements BotModel{

    private static final Random RANDOM = new Random(System.nanoTime());

    private final Logger logger = LoggerFactory.getLogger(MyRealisticModel.class);

    private Map<Integer, BotState> botStates = new HashMap<Integer, BotState>();

    private int counter = 0;

    private int workMaxDistance = 50;
    private int waitForMaterial = 50;
    private int buildingDuration = 10;
    private int miningDuration = 15;

    private int exploreMaxDistance = 50;
    private int destroyDuration = 25;

    @Override
    public TaskExecutor newTask(Bot bot) {
        if (botStates.get(bot.getPlayer().getId()) == null) {
            // int r = RANDOM.nextInt(100);
            // if (counter == 0 || r < 75) {
            //     initBotStateBuilder(bot);
            //     counter += 1;
            // } else if (r < 95){
            //     initBotStateExplorer(bot);
            // } else {
            //     initBotStateDestroyer(bot);
            // }
            initBotStateExplorer(bot);
        }

        return taskManager(bot);
    }


    public TaskExecutor taskManager(Bot bot) {
        BotState state = botStates.get(bot.getPlayer().getId());

        if (state instanceof BotStateBuilder) {
            return builderTask(bot, (BotStateBuilder) state);
        } else if (state instanceof BotStateExplorer) {
            return explorerTask(bot, (BotStateExplorer) state);
        } else {
            return destroyerTask(bot, (BotStateDestroyer) state);
        }
    }


    void initBotStateBuilder(Bot bot) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();
            
        int workX = currentLocation.getX() + RANDOM.nextInt(workMaxDistance * 2) - workMaxDistance;
        int workZ = currentLocation.getZ() + RANDOM.nextInt(workMaxDistance * 2) - workMaxDistance;

        Vector3i workLocation = getTargetAt(bot, workX, workZ);

        botStates.putIfAbsent(bot.getPlayer().getId(),
                                new BotStateBuilder(bot,
                                                    workLocation, 
                                                    currentLocation));
    }


    void initBotStateExplorer(Bot bot) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int exploreX = currentLocation.getX() + RANDOM.nextInt(exploreMaxDistance * 2) - exploreMaxDistance;
        int exploreZ = currentLocation.getZ() + RANDOM.nextInt(exploreMaxDistance * 2) - exploreMaxDistance;

        Vector3i exploreLocation = getTargetAt(bot, exploreX, exploreZ);

        botStates.putIfAbsent(bot.getPlayer().getId(),
                                new BotStateExplorer(bot, 
                                                    exploreLocation));
    }


    void initBotStateDestroyer(Bot bot) {
        botStates.putIfAbsent(bot.getPlayer().getId(),
                                new BotStateDestroyer(bot));
    }
    

    ///////////////////////////
    /////// DESTROYER /////////
    /////////////////////////// 


    private TaskExecutor destroyerTask(Bot bot, BotStateDestroyer state) {
        if (!state.hasTarget) {
            state.hasTarget = true;
            state.changeTargetDuration = RANDOM.nextInt(25) + destroyDuration;

            state.targetLocation = getTargetLocation(bot);
        }
        
        if (!state.isAtTargetLocation) {
            state.isAtTargetLocation = isAtApproximateLocation(bot, state.targetLocation);

            if(state.isAtTargetLocation) {
                state.isAtDestroyLocation = false;
                state.destroyLocation = getDestroyLocation(bot, state.targetLocation);

                return destroyerTask(bot, state);
            } 
            else {
                return new WalkTaskExecutor(bot, state.targetLocation);
            }
        }

        if (!state.isAtDestroyLocation) {
            state.isAtDestroyLocation = isAtExactLocation(bot, state.destroyLocation);
            
            if (state.isAtDestroyLocation) {
                state.changeTargetDuration -= 1;
                state.isAtDestroyLocation = false;
                state.destroyLocation = getDestroyLocation(bot, state.targetLocation);

                state.destroyBlock = getDestroyBlock(bot);

                return new BreakBlocksTaskExecutor(bot, state.destroyBlock);
            }
        }

        if (state.isWaiting) {
            state.waitDuration -= 1;

            if (state.waitDuration <= 0) {
                state.waitDuration = waitForMaterial;
                state.isWaiting = false;

                return destroyerTask(bot, state);
            }

            return idle(bot, state.waitDuration);       
        }

        if (state.changeTargetDuration <= 0) {
            state.hasTarget = false;
            state.isAtTargetLocation = false;
            state.isAtDestroyLocation = false;

            return destroyerTask(bot, state);
        }

        if (RANDOM.nextInt(10) < 2) {
            state.isWaiting = true;
            state.waitDuration = waitForMaterial;
        }

        return new WalkTaskExecutor(bot, state.destroyLocation); 
    }

    
    public List<Block> getDestroyBlock(Bot bot) {
        List<Block> randomDestroyBlock = new ArrayList<Block>();

        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int x = currentLocation.getX();
        int y = (RANDOM.nextInt(2) == 0)? currentLocation.getY(): currentLocation.getY() - 1;
        int z = currentLocation.getZ();

        int r = RANDOM.nextInt(5);

        try {
            if (r == 0) {
                randomDestroyBlock.add(bot.getWorld().getBlockAt(x + 1, y, z));
            } else if (r == 1) {
                randomDestroyBlock.add(bot.getWorld().getBlockAt(x - 1, y, z));
            } else if (r == 2) {
                randomDestroyBlock.add(bot.getWorld().getBlockAt(x, y, z + 1));
            } else if (r == 3){
                randomDestroyBlock.add(bot.getWorld().getBlockAt(x, y, z - 1));
            } else {
                randomDestroyBlock.add(bot.getWorld().getBlockAt(x, y, z));
            }
        }

        catch (ChunkNotLoadedException ex) {
            logger.warn("Bot target not loaded: ({},{},{})", x, y, z);
            return randomDestroyBlock;
        }

        return randomDestroyBlock;
    }


    private Vector3i getDestroyLocation(Bot bot, Vector3i targetLocation) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int destroyX = currentLocation.getX() + RANDOM.nextInt(2 + 2) - 2;
        int destroyZ = currentLocation.getZ() + RANDOM.nextInt(2 + 2) - 2;

        int closeEnoughX = Math.abs(destroyX - targetLocation.getX());
        int closeEnoughZ = Math.abs(destroyZ - targetLocation.getZ());

        while (closeEnoughX > 10 || closeEnoughZ > 10) {
            destroyX = currentLocation.getX() + RANDOM.nextInt(2 + 2) - 2;
            destroyZ = currentLocation.getZ() + RANDOM.nextInt(2 + 2) - 2;

            closeEnoughX = Math.abs(destroyX - targetLocation.getX());
            closeEnoughZ = Math.abs(destroyZ - targetLocation.getZ());
        }

        return getTargetAt(bot, destroyX, destroyZ);
    }


    private Vector3i getTargetLocation(Bot bot) {
        List<Integer> keysAsArray = new ArrayList<Integer>(botStates.keySet());

        BotState randomState = null;

        while (true) {
            randomState = botStates.get(keysAsArray.get(RANDOM.nextInt(keysAsArray.size())));
            
            if (randomState instanceof BotStateBuilder) {
                Vector3i targetLocation = ((BotStateBuilder) randomState).workLocation;
                return getTargetAt(bot, targetLocation.getX(), targetLocation.getZ());
            }
        }
    }

    ///////////////////////////
    //////// BUILDER //////////
    /////////////////////////// 


    private TaskExecutor builderTask(Bot bot, BotStateBuilder state) {
        if (!state.isAtWorkLocation) {
            state.isAtWorkLocation = isAtApproximateLocation(bot, state.workLocation);
            return new WalkTaskExecutor(bot, state.workLocation);
        }

        if (!state.needToMine) {
            return build(bot, state);
        }

        return mining(bot, state);
    }


    private TaskExecutor build(Bot bot, BotStateBuilder state) {
        if (!state.isAtBuildLocation) {
            state.isAtBuildLocation = isAtExactLocation(bot, state.buildingLocation);
            
            if (state.isAtBuildLocation) {
                state.buildingBlocks = getBuildBlockLocation(bot, state);
                state.buildingLocation = getBuildingLocation(bot, state);

                state.isAtBuildLocation = false;
                state.buildingDuration -= 1;

                return new PlaceBlocksTaskExecutor(bot, state.buildingBlocks, Material.DIRT);
            }
        }

        if (state.buildingDuration <= 0) {
            state.isAtMiningLocation = false;

            state.needToMine = true;
            state.needMiningLocation = true;

            state.buildingDuration = RANDOM.nextInt(5) + buildingDuration;

            return builderTask(bot, state);
        }


        return new WalkTaskExecutor(bot, state.buildingLocation);
    }


    private Vector3i getBuildingLocation(Bot bot, BotStateBuilder state) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int mineX = currentLocation.getX() + RANDOM.nextInt(2 * 2 + 1) - 2;
        int mineZ = currentLocation.getZ() + RANDOM.nextInt(2 * 2 + 1) - 2;

        return getTargetAt(bot, mineX, mineZ);
    }


    private List<Vector3i> getBuildBlockLocation(Bot bot, BotStateBuilder state) {
        List<Vector3i> buildBlockLocation = new ArrayList<Vector3i>();

        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        Vector3i buildLocation = new Vector3i(currentLocation.getX() + RANDOM.nextInt(1 * 2 + 1) + 1, 
                                            currentLocation.getY(),
                                            currentLocation.getZ() + RANDOM.nextInt(1 * 2 + 1) + 1);
        buildBlockLocation.add(buildLocation);

        return buildBlockLocation;
    }


    private boolean isAtExactLocation(Bot bot, Vector3i location) {
        Vector3i botLocation = bot.getPlayer().getLocation().intVector();
        
        int isSameX = Math.abs(botLocation.getX() - location.getX());
        int isSameY = Math.abs(botLocation.getZ() - location.getZ());

        if (isSameX != 0 || isSameY != 0) {
            return false;
        }

        return true;        
    }


    private void updatePreviousLocation(Bot bot, BotStateBuilder state) {
        Vector3i location = bot.getPlayer().getLocation().intVector();
        state.previousLocation = new Vector3i(location.getX(), 0, location.getZ());
    }


    private TaskExecutor mining(Bot bot, BotStateBuilder state) {

        if (state.needMiningLocation) {
            state.needMiningLocation = false;
            state.isAtMiningLocation = false;
            state.miningDuration = RANDOM.nextInt(10) + miningDuration;
            
            state.miningLocation = getMiningLocationFar(bot, state);
        }

        if (!state.isAtMiningLocation) {
            state.isAtMiningLocation = isAtExactLocation(bot, state.miningLocation);
            
            if (state.isAtMiningLocation) {
                updatePreviousLocation(bot, state);

                state.isAtMiningLocation = false;
                state.miningDuration -= 1;
                state.miningLocation = getMiningLocationShort(bot, state);
                
                state.needToWait = true;

                state.miningBlocks = getMiningBlocks(bot, state);

                return new BreakBlocksTaskExecutor(bot, state.miningBlocks);
            }
        }

        if (state.needToWait) {
            state.waitDuration -= 1;

            if (state.waitDuration <= 0) {
                state.needToWait = false;
                state.waitDuration = 20; // default val
            }

            return idle(bot, state.waitDuration);
        }


        if (isAtExactLocation(bot, state.previousLocation)) {

            if  (state.previousLocationDuration <= 0) {
                state.previousLocationDuration = 3; // default value to wait
                // modified walktaskexecutor
                state.isAtMiningLocation = false;
                state.miningLocation = getMiningLocationShort(bot, state);

                state.miningBlocks = getRandomMiningBlock(bot, state);

                return new BreakBlocksTaskExecutor(bot, state.miningBlocks);
            } else {
                state.previousLocationDuration -= 1;
            }
        } 
        
        else {
            state.previousLocationDuration = 3;
            updatePreviousLocation(bot, state);
        }

        if (state.miningDuration <= 0) {
            state.isAtWorkLocation = false;
            state.needToMine = false;

            state.needMiningLocation = true;
            state.isAtMiningLocation = false;

            return builderTask(bot, state);
        }

        return new WalkTaskExecutor(bot, state.miningLocation);
    }


    public List<Block> getRandomMiningBlock(Bot bot, BotStateBuilder state) {
        List<Block> randomMiningBlockLocation = new ArrayList<Block>();

        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int x = currentLocation.getX();
        int y = currentLocation.getY();
        int z = currentLocation.getZ();

        int r = RANDOM.nextInt(3);

        try {
            if (r == 0) {
                randomMiningBlockLocation.add(bot.getWorld().getBlockAt(x + 1, y, z));
            } else if (r == 1) {
                randomMiningBlockLocation.add(bot.getWorld().getBlockAt(x - 1, y, z));
            } else if (r == 2) {
                randomMiningBlockLocation.add(bot.getWorld().getBlockAt(x, y, z + 1));
            } else {
                randomMiningBlockLocation.add(bot.getWorld().getBlockAt(x, y, z - 1));
            }
        }

        catch (ChunkNotLoadedException ex) {
            logger.warn("Bot target not loaded: ({},{},{})", x, y, z);
            return randomMiningBlockLocation;
        }

        return randomMiningBlockLocation;
    }


    public List<Block> getMiningBlocks(Bot bot, BotStateBuilder state) {
        List<Block> miningBlockLocation = new ArrayList<Block>();

        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int x = currentLocation.getX();
        int y = currentLocation.getY() - 1;
        int z = currentLocation.getZ();

        try {
            miningBlockLocation.add(bot.getWorld().getBlockAt(currentLocation.getX(),
                                                            currentLocation.getY() - 1,
                                                            currentLocation.getZ()));
        }

        catch (ChunkNotLoadedException ex) {
            logger.warn("Bot target not loaded: ({},{},{})", x, y, z);
            return miningBlockLocation;
        }

        return miningBlockLocation;
    }


    private Vector3i getMiningLocationFar(Bot bot, BotStateBuilder state) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int mineX = currentLocation.getX() + RANDOM.nextInt(15) + 10;
        int mineZ = currentLocation.getZ() + RANDOM.nextInt(15) + 10;

        int negativeX = RANDOM.nextInt(2);
        int negativeZ = RANDOM.nextInt(2);

        mineX = (negativeX == 0)? mineX: mineX * -1;
        mineZ = (negativeZ == 0)? mineZ: mineZ * -1;

        return getTargetAt(bot, mineX, mineZ);
    }


    private Vector3i getMiningLocationShort(Bot bot, BotStateBuilder state) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int mineX = currentLocation.getX() + RANDOM.nextInt(2 * 2 + 1) - 2;
        int mineZ = currentLocation.getZ() + RANDOM.nextInt(2 * 2 + 1) - 2;

        return getTargetAt(bot, mineX, mineZ);
    }
    

    private boolean isAtApproximateLocation(Bot bot, Vector3i location) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();
        
        int closeToX = Math.abs(currentLocation.getX() - location.getX());
        int closeToZ = Math.abs(currentLocation.getZ() - location.getZ());

        if (closeToX > 5 || closeToZ > 5) {
            return false;
        }

        return true;        
    }

    ///////////////////////////
    ////// EXPLORATION ////////
    ///////////////////////////

    private TaskExecutor explorerTask(Bot bot, BotStateExplorer state) {
        if (!state.isExploring && !state.isWaiting) {

            if (RANDOM.nextInt(10) < 10) {
                int distance = RANDOM.nextInt(exploreMaxDistance) + 5;
                state.exploreLocation = getExploreLocation(bot, distance);
                return new WalkTaskExecutor(bot, state.exploreLocation);
            } 
            
            // else {
            //     state.waitDuration = RANDOM.nextInt(200) + 50;
            //     return idle(bot, state.waitDuration);
            // }
        }

        // if (state.isWaiting) {
        //     state.waitDuration -= 1;

        //     if (state.waitDuration <= 0) {
        //         state.isWaiting = false;

        //         return explorerTask(bot, state);
        //     }

        //     return idle(bot, state.waitDuration);
        // }

        // if (isAtApproximateLocation(bot, state.exploreLocation)) {
        //     state.isExploring = false;

        //     return explorerTask(bot, state);
        // }

        return new WalkTaskExecutor(bot, state.exploreLocation);
    }


    private Vector3i getExploreLocation(Bot bot, int distance) {
        Vector3i currentLocation = bot.getPlayer().getLocation().intVector();

        int exploreX = currentLocation.getX() + RANDOM.nextInt(distance * 2) - distance;
        int exploreZ = currentLocation.getZ() + RANDOM.nextInt(distance * 2) - distance;

        return new Vector3i(exploreX, currentLocation.getY(), exploreZ);//getTargetAt(bot, exploreX, exploreZ);
    }


    private Vector3i getTargetAt(Bot bot, int x, int z) {
        Vector3d botLoc = bot.getPlayer().getLocation();

        int y = -1;

        try {
            for (ZigZagRange it = new ZigZagRange(0, 255, (int) botLoc.getY()); it.hasNext(); ) {
                y = it.next();
                Block test = bot.getWorld().getBlockAt(x, y, z);
                if (test.getMaterial().isTraversable()
                        && !test.getRelative(BlockFace.BOTTOM).getMaterial().isTraversable()) {
                    break;
                }
            }

            if (y < 0 || y > 255) {
                return botLoc.intVector();
            }

            return new Vector3i(x, y, z);
        } catch (ChunkNotLoadedException ex) {
            logger.warn("Bot target not loaded: ({},{},{})", x, y, z);
            return botLoc.intVector();
        }
    }


    private TaskExecutor idle(Bot bot, long idleDuration) {

        return new TaskExecutor() {
            long start = System.currentTimeMillis();
            long duration = idleDuration;

            TaskStatus getCurrentStatus() {
                return System.currentTimeMillis() - start >= duration
                    ? TaskStatus.forSuccess()
                    : TaskStatus.forInProgress();
            }

            @Override
            public String getShortName() {
                return "IdleTask";
            }

            @Override
            public TaskStatus getStatus() {
                return getCurrentStatus();
            }

            @Override
            public TaskStatus tick() {
                return getCurrentStatus();
            }

            @Override
            public void stop() {}
        };
    }


    public Vector3i computeTargetLocation(Bot bot) {
        return longDistanceTarget(bot);
    }

    
    private Vector3i longDistanceTarget(Bot bot) {
        Vector3d currentLocation = bot.getPlayer().getLocation();

        return currentLocation.intVector();
    }

}
