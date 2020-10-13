/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.embeddings.graphsage.weighted;

import org.neo4j.gds.embeddings.graphsage.Aggregator;
import org.neo4j.gds.embeddings.graphsage.Layer;
import org.neo4j.gds.embeddings.graphsage.NeighborhoodSampler;
import org.neo4j.gds.embeddings.graphsage.UniformNeighborhoodSampler;
import org.neo4j.gds.embeddings.graphsage.ddl4j.Variable;
import org.neo4j.gds.embeddings.graphsage.ddl4j.functions.Weights;
import org.neo4j.gds.embeddings.graphsage.ddl4j.tensor.Matrix;
import org.neo4j.graphalgo.api.Graph;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class WeightedAggregatingLayer implements Layer {

    private final UniformNeighborhoodSampler sampler;
    private final Graph graph;
    private final long sampleSize;
    private final Weights<Matrix> weights;
    private long randomState;
    private final Function<Variable<Matrix>, Variable<Matrix>> activationFunction;

    public WeightedAggregatingLayer(Graph graph, Weights<Matrix> weights, long sampleSize, Function<Variable<Matrix>, Variable<Matrix>> activationFunction) {
        this.graph = graph;
        this.sampleSize = sampleSize;
        this.weights = weights;
        this.activationFunction = activationFunction;
        this.randomState = ThreadLocalRandom.current().nextLong();
        this.sampler = new UniformNeighborhoodSampler();
    }

    @Override
    public Aggregator aggregator() {
        return new WeightedAggregator(graph, weights, activationFunction);
    }

    @Override
    public NeighborhoodSampler sampler() {
        return sampler;
    }

    @Override
    public long sampleSize() {
        return sampleSize;
    }

    @Override
    public long randomState() {
        return randomState;
    }

    @Override
    public void generateNewRandomState() {
        randomState = ThreadLocalRandom.current().nextLong();
    }
}
