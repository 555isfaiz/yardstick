package nl.tudelft.opencraft.yardstick.model.BotStates;

import java.util.ArrayList;
import java.util.List;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.world.Block;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

public class BotStateBuilder extends BotState{

    // work area
    public boolean isAtWorkLocation = false;
    public Vector3i workLocation = null;
    
    // mining
    public boolean isAtMiningLocation = false;
    public Vector3i miningLocation = null;

    public boolean needToMine = true;
    public boolean needMiningLocation = true;
    public long miningDuration = 10; // nb blocks to mine

    public boolean needToWait = false; 
    public long waitDuration = 20; // default value 20

    public List<Block> miningBlocks = new ArrayList<Block>();

    // building
    public boolean isAtBuildLocation = false;
    public Vector3i buildingLocation = null;

    public long buildingDuration = 10; // nb of block to place (approximation)

    public List<Vector3i> buildingBlocks = new ArrayList<Vector3i>();

    // blocked bot
    public Vector3i previousLocation = null;
    public long previousLocationDuration = 3;

    // timeout in walktastexecutor to unblock characters
    public BotStateBuilder(Bot bot,
                            Vector3i workLocation, 
                            Vector3i location) {
        super(bot);

        this.workLocation = workLocation;
        this.previousLocation = new Vector3i(location.getX(), 0, location.getZ());
        this.buildingLocation = new Vector3i(workLocation.getX(), 0, workLocation.getZ());
    }

}
