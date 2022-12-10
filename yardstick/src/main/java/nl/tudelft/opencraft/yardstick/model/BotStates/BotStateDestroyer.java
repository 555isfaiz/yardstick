package nl.tudelft.opencraft.yardstick.model.BotStates;

import java.util.ArrayList;
import java.util.List;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.world.Block;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

public class BotStateDestroyer extends BotState{

    public boolean hasTarget = false;
    public boolean isAtTargetLocation = false;

    public Vector3i targetLocation = null;

    public boolean isAtDestroyLocation = false;
    public Vector3i destroyLocation = null;

    public List<Block> destroyBlock = new ArrayList<Block>(); 

    public boolean isWaiting = false;
    public long waitDuration = 200;

    public long changeTargetDuration = 15;
    
    public BotStateDestroyer(Bot bot) {
        super(bot);
    }
}
