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
import com.typesafe.config.Config;

import nl.tudelft.opencraft.yardstick.bot.Bot;
import nl.tudelft.opencraft.yardstick.bot.ai.task.TaskExecutor;
import nl.tudelft.opencraft.yardstick.bot.world.ChunkNotLoadedException;

/**
 * Represents the SAMOVAR model.
 */
public class SAMOVARModel implements BotModel {

    private static Graph map;
    //
    private final BotModel interact = new SimpleInteractionModel();
    private final BotModel movement = new SimpleMovementModel();

    public SAMOVARModel(Config mapConfig) {

    }

    @Override
    public TaskExecutor newTask(Bot bot) throws ChunkNotLoadedException {

        return taskExecutor;
    }

}
