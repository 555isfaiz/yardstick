/*
 * Yardstick: A Benchmark for Minecraft-like Services
 * Copyright (C) 2020 AtLarge Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package nl.tudelft.opencraft.yardstick.experiment;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import nl.tudelft.opencraft.yardstick.Yardstick;
import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.BotManager;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskStatus;
import nl.tudelft.opencraft.yardstick.game.GameArchitecture;
import nl.tudelft.opencraft.yardstick.model.SAMOVARModel;

// TODO remove this class once we have a good BotModel interface.
public class Experiment13SAMOVAR extends Experiment {

    private final Config behaviorConfig;
    private final Config samovarConfig;

    private SAMOVARModel samovar;

    private long startMillis;
    private Duration experimentDuration;
    private BotManager botManager;
    private ScheduledFuture<?> runningBotManager;

    public Experiment13SAMOVAR(int nodeID, GameArchitecture game, Config behaviorConfig, Config samovarConfig, Duration experimentDuration) {
        super(13, nodeID, game, "latency and walk experiment with SAMOVAR model");
        this.behaviorConfig = behaviorConfig;
        this.samovarConfig = samovarConfig;
    }
    
    @Override
    protected void before() {
        this.experimentDuration = behaviorConfig.getDuration("duration");
        int botsTotal = behaviorConfig.getInt("bots");
        this.samovar = new SAMOVARModel(samovarConfig, botsTotal);
        Duration timeBetweenJoins = behaviorConfig.getDuration("joininterval");
        int numberOfBotsPerJoin = behaviorConfig.getInt("numbotsperjoin");
        this.startMillis = System.currentTimeMillis();
        
        samovar.onBefore();
        
        botManager = new BotManager(game);
        botManager.setPlayerStepIncrease(numberOfBotsPerJoin);
        botManager.setPlayerCountTarget(botsTotal);
        runningBotManager = Yardstick.THREAD_POOL.scheduleAtFixedRate(botManager, 0, timeBetweenJoins.getSeconds(),
                TimeUnit.SECONDS);
    }

    @Override
    protected void tick() {
        botManager.getConnectedBots().stream()
                .filter(Bot::isJoined)
                .forEach(this::botTick);
    }

    private void botTick(Bot bot) {
        TaskExecutor t = bot.getTaskExecutor();
        if (t == null || t.getStatus().getType() != TaskStatus.StatusType.IN_PROGRESS) {
            var futureTaskExecutor = samovar.newTask(bot);
            bot.setTaskExecutor(futureTaskExecutor);
        }
    }

    @Override
    protected boolean isDone() {
        return System.currentTimeMillis() - this.startMillis > this.experimentDuration.toMillis();
    }

    @Override
    protected void after() {
        runningBotManager.cancel(false);
        List<Bot> botList = botManager.getConnectedBots();
        for (Bot bot : botList) {
            bot.disconnect("disconnect");
        }
    }
}
