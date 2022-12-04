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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Network;
import com.typesafe.config.Config;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskStatus;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

/**
 * Represents the SAMOVAR model.
 */
public class SAMOVARModel implements BotModel {
	
	class Waypoint extends Point{
		final double weight;
		final int level;
		Waypoint(int x, int y, double weight, int level){
			super(x, y);
			this.weight = weight;
			this.level = level;
		}
	}
    private static Graph<Waypoint> map;
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
    
    ArrayList<Double> getAreaPopularityList(int waypointNumber) {
    	ArrayList<Double> areaPopularityList = new ArrayList<Double>();
    	for(int i = 0; i < waypointNumber; i++) {
    		areaPopularityList.add(getAreaPopularity());
    	}
    	Collections.sort(areaPopularityList);
    	return areaPopularityList;
    	
    }
    
    ArrayList<Integer> getAreaLevelList(ArrayList<Double> areaPopularityList, int level, int logBase) throws Exception {
    	ArrayList<Integer> areaLevelList = new ArrayList<Integer>();
    	int numBinScale = 0;
    	for (int i = 0; i < level; i++) {
    		numBinScale += Math.pow(logBase, i);
    	}
    	int numBinBase = areaPopularityList.size()/numBinScale;
    	if (numBinBase <= 0) {
    		throw new Exception("The number of waypoints must be greater then number of bins");
    	}
    	
    	int tempLevel = 0;
    	int numBin = numBinBase;
    	for (int i = 1; i <= areaPopularityList.size(); i++) {
    		if (i > numBin && tempLevel < level) {
    			numBin += Math.pow(logBase, tempLevel);
    			tempLevel++; 
    		}
    		areaLevelList.add(tempLevel);
    	}
    	return areaLevelList;
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
    
    Graph<Waypoint> sampleWaypoint(int worldWidth, int worldLength, int waypointSize, int waypointNumber) {
    	MutableGraph<Waypoint> map = GraphBuilder.undirected().build();
    	return map;
    }
}
