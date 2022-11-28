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

package nl.tudelft.opencraft.yardstick.model;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.graph.Graph;
import com.typesafe.config.Config;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskStatus;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

/**
 * Represents the SAMOVAR model.
 */
public class SAMOVARModel implements BotModel {

    private static Graph map;
    final Random rng = new Random(0);
    final Config samovarConfig;
    final int botsNumber;

    final Map<String, Object> assignedPath = new ConcurrentHashMap<>();

    public SAMOVARModel(Config samovarConfig, int botsNumber) {
        this.samovarConfig = samovarConfig;
        this.botsNumber = botsNumber;
    }

    public void onBefore() {
        generateMap();
        generatePath(botsNumber);
    }

    void generateMap() {
        // TODO
    }

    /**
     * Generate {@code pathNum} paths, they are not assigned to bots yet.
     */
    void generatePath(int pathNum) {
        // TODO
    }

    // Do all the updates as the paper descirbes here.
    @Override
    public TaskExecutor newTask(Bot bot) {
        if (assignedPath.containsKey(bot.getName())) {
            // ...
        } else {
            // ...
        }
        // return stayIdle(Math.round(getPauseDuration() * 1000));
        // return new WalkTaskExecutor(bot, nextTargetLocation(bot));
        return null;
    }

    TaskExecutor stayIdle(long idleDuration) {
        return new TaskExecutor() {
            long start = System.currentTimeMillis();
            long duration = idleDuration;

            TaskStatus getCurrentStatus() {
                return System.currentTimeMillis() - start >= duration ? TaskStatus.forSuccess() : TaskStatus.forInProgress();
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

    public Vector3i nextTargetLocation(Bot bot) {
        // TODO
        return null;
    }

    // TODO what's the unit?
    double getPauseDuration() {
        var dpConfig = samovarConfig.getConfig("pauseDurationDistribution");
        return lognormalDistribution(dpConfig.getDouble("avg"), dpConfig.getDouble("std"));
    }

    double getVelocity() {
        var vConfig = samovarConfig.getConfig("velocityDistribution");
        return lognormalDistribution(vConfig.getDouble("avg"), vConfig.getDouble("std"));
    }

    double getAreaPopularity() {
        var apConfig = samovarConfig.getConfig("areaPopularityDistribution");
        return lognormalDistribution(apConfig.getDouble("avg"), apConfig.getDouble("std"));
    }

    double getDistinctVisitedAreas() {
        var dvaConfig = samovarConfig.getConfig("distinctVisitedAreasDistribution");
        return lognormalDistribution(dvaConfig.getDouble("avg"), dvaConfig.getDouble("std"));
    }

    double getPersonalWeight() {
        var pwConfig = samovarConfig.getConfig("personalWeightDistribution");
        return zipfDistribution(pwConfig.getDouble("theta"));
    }

    // https://stackoverflow.com/questions/21674599/generating-a-lognormal-distribution-from-an-array-in-java
    double lognormalDistribution(double avg, double std) {
        return Math.exp(std * rng.nextGaussian() + avg);
    }

    // https://diveintodata.org/2009/09/13/zipf-distribution-generator-in-java/
    double zipfDistribution(double theta) {
        // TODO
        return 0.0;
    }
}
