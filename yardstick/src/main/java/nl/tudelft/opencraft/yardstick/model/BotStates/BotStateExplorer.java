package nl.tudelft.opencraft.yardstick.model.BotStates;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

public class BotStateExplorer extends BotState {
    public boolean isExploring = false;
    public Vector3i exploreLocation = null;

    public boolean isWaiting = false;
    public long waitDuration = 200;

    public BotStateExplorer(Bot bot, Vector3i location) {
        super(bot);

        this.exploreLocation = location;
    } 
}
