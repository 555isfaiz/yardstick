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

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskStatus;
import nl.tudelft.opencraft.yardstick.util.Vector2i;
import nl.tudelft.opencraft.yardstick.util.Vector3i;

/**
 * Represents the SAMOVAR model.
 */
public class SAMOVARModel implements BotModel {

    class Waypoint extends Vector2i {

        final double weight;
        final int level;

        Waypoint(int x, int z, double weight, int level) {
            super(x, z);
            this.weight = weight;
            this.level = level;
        }
    }

    private static MutableGraph<Waypoint> map;
    private static HashMap<Integer, ArrayList<Waypoint>> leveledList;

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
        sampleWaypoint();
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

    ArrayList<Double> getAreaPopularityList() {
        int waypointNumber = samovarConfig.getInt("waypointNumber");
        ArrayList<Double> areaPopularityList = new ArrayList<Double>();
        for (int i = 0; i < waypointNumber; i++) {
            areaPopularityList.add(getAreaPopularity());
        }
        Collections.sort(areaPopularityList);
        return areaPopularityList;
    }

    ArrayList<Integer> getAreaLevelList(ArrayList<Double> areaPopularityList) throws Exception {
        int level = samovarConfig.getInt("levelNumber");
        int logBase = samovarConfig.getInt("levelLogBase");
        ArrayList<Integer> areaLevelList = new ArrayList<Integer>();
        int totalBin = 0;
        for (int i = 0; i < level; i++) {
            totalBin += Math.pow(logBase, i);
        }
        int binWidth = areaPopularityList.size() / totalBin;
        if (binWidth <= 0) {
            throw new Exception("The number of waypoints must be greater then binWidth");
        }

        int tempLevel = 1;
        int tempBinUpper = binWidth;
        for (int i = 1; i <= areaPopularityList.size(); i++) {
            if (i > tempBinUpper && tempLevel < level) {
                tempBinUpper += binWidth * Math.pow(logBase, tempLevel);
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

    Graph<Waypoint> sampleWaypoint() {
        int worldWidth = samovarConfig.getInt("worldWidth");
        int worldLength = samovarConfig.getInt("worldLength");
        int waypointSize = samovarConfig.getInt("waypointSize");
        int waypointNumber = samovarConfig.getInt("waypointNumber");
        map = GraphBuilder.undirected().build();
        initLeveledList();

        int numAreaX = (int) Math.ceil((double) worldWidth / waypointSize);
        int numAreaZ = (int) Math.ceil((double) worldLength / waypointSize);

        int[] randomAreaIndex = new Random().ints(0, numAreaX * numAreaZ).distinct().limit(waypointNumber).toArray();
        int[] randomPopularityIndex = new Random().ints(0, waypointNumber).distinct().limit(waypointNumber).toArray();
        ArrayList<Double> areaPopularityList = getAreaPopularityList();
        try {
            ArrayList<Integer> areaLevelList = getAreaLevelList(areaPopularityList);
            for (int i = 0; i < waypointNumber; i++) {
                int areaX = randomAreaIndex[i] % numAreaX;
                int areaZ = randomAreaIndex[i] / numAreaX;
                int x =
                    areaX *
                    waypointSize +
                    (
                        (areaX == numAreaX - 1 && worldWidth % waypointSize != 0)
                            ? (worldWidth % waypointSize) / 2
                            : waypointSize / 2
                    );
                int z =
                    areaZ *
                    waypointSize +
                    (
                        (areaZ == numAreaZ - 1 && worldLength % waypointSize != 0)
                            ? (worldLength % waypointSize) / 2
                            : waypointSize / 2
                    );

                Double popularity = areaPopularityList.get(randomPopularityIndex[i]);
                int level = areaLevelList.get(randomPopularityIndex[i]);

                Waypoint waypoint = new Waypoint(x, z, popularity, level);
                leveledList.get(level).add(waypoint);
                map.addNode(waypoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

	void initLeveledList() {
        int level = samovarConfig.getInt("levelNumber");
        leveledList = new HashMap<Integer, ArrayList<Waypoint>>();
        for (int i = 1; i <= level; i++) {
            leveledList.put(i, new ArrayList<Waypoint>());
        }
    }

    void connectWaypoint() {
        int levelNumber = samovarConfig.getInt("levelNumber");
        int connectionRange = samovarConfig.getInt("connectionRange");
        for (int level = 1; level <= levelNumber; level++) {
            ArrayList<Waypoint> currentLvPointList = leveledList.get(level);
            if (level == 1) {
                for (int index1 = 0; index1 < currentLvPointList.size(); index1++) for (
                    int index2 = index1 + 1;
                    index2 < currentLvPointList.size();
                    index2++
                ) map.putEdge(currentLvPointList.get(index1), currentLvPointList.get(index2));
            } else {
                ArrayList<Waypoint> upLvList = leveledList.get(level - 1);
                HashMap<Waypoint, ArrayList<Waypoint>> connectedUpList = new HashMap<Waypoint, ArrayList<Waypoint>>();
                for (int index1 = 0; index1 < currentLvPointList.size(); index1++) {
                    Waypoint currentPt = currentLvPointList.get(index1);
                    double minDistance = Double.MAX_VALUE;
                    int minIndex = 0;
                    for (int index2 = 0; index2 < upLvList.size(); index2++) {
                        double distance = currentPt.distance(upLvList.get(index2));
                        if (minDistance > distance) {
                            minDistance = distance;
                            minIndex = index2;
                        }
                    }
                    Waypoint connectedPt = upLvList.get(minIndex);
                    map.putEdge(currentPt, connectedPt);

                    if (!connectedUpList.containsKey(connectedPt)) connectedUpList.put(
                        connectedPt,
                        new ArrayList<Waypoint>()
                    );
                    ArrayList<Waypoint> sameLvConnected = connectedUpList.get(connectedPt);
                    for (int i = 0; i < sameLvConnected.size(); i++) map.putEdge(currentPt, sameLvConnected.get(i));
                    connectedUpList.get(connectedPt).add(currentPt);
                }
            }
        }
        Iterator<Waypoint> iter1 = map.nodes().iterator();
        while (iter1.hasNext()) {
            Waypoint pt1 = iter1.next();
            Set<Waypoint> adjPt = map.adjacentNodes(pt1);
            Iterator<Waypoint> iter2 = adjPt.iterator();
            while (iter2.hasNext()) {
                Waypoint pt2 = iter2.next();
                if (pt1.distance(pt2) < connectionRange) map.putEdge(pt1, pt2);
            }
        }
    }
}
