/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.gds.ml.linkmodels;

import org.neo4j.gds.ml.batch.Batch;
import org.neo4j.gds.ml.batch.BatchQueue;
import org.neo4j.gds.ml.linkmodels.logisticregression.LinkLogisticRegressionPredictor;
import org.neo4j.graphalgo.Algorithm;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.core.utils.ProgressLogger;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.LongStream;

public class LinkPredictionPredict extends Algorithm<LinkPredictionPredict, LinkPredictionResult> {

    private final LinkLogisticRegressionPredictor predictor;
    private final Graph graph;
    private final int batchSize;
    private final int concurrency;
    private final int topN;
    private final double threshold;
    private final AllocationTracker tracker;

    LinkPredictionPredict(
        LinkLogisticRegressionPredictor predictor,
        Graph graph,
        int batchSize,
        int concurrency,
        int topN,
        AllocationTracker tracker,
        ProgressLogger progressLogger,
        double threshold
    ) {
        this.predictor = predictor;
        this.graph = graph;
        this.concurrency = concurrency;
        this.batchSize = batchSize;
        this.topN = topN;
        this.tracker = tracker;
        this.threshold = threshold;
        this.progressLogger = progressLogger;
    }

    @Override
    public LinkPredictionResult compute() {
        progressLogger.reset(graph.nodeCount());
        progressLogger.logStart();
        var result = new LinkPredictionResult(topN);
        var batchQueue = new BatchQueue(graph.nodeCount(), batchSize);
        batchQueue.parallelConsume(concurrency, ignore -> new LinkPredictionScoreByIdsConsumer(
            graph.concurrentCopy(),
            predictor,
            result,
            progressLogger
        ));
        progressLogger.logFinish();
        return result;
    }

    @Override
    public LinkPredictionPredict me() {
        return this;
    }

    @Override
    public void release() {

    }

    private class LinkPredictionScoreByIdsConsumer implements Consumer<Batch> {
        private final Graph graph;
        private final LinkLogisticRegressionPredictor predictor;
        private final LinkPredictionResult predictedLinks;
        private final ProgressLogger progressLogger;

        private LinkPredictionScoreByIdsConsumer(
            Graph graph,
            LinkLogisticRegressionPredictor predictor,
            LinkPredictionResult predictedLinks,
            ProgressLogger progressLogger
        ) {
            this.graph = graph;
            this.predictor = predictor;
            this.predictedLinks = predictedLinks;
            this.progressLogger = progressLogger;
        }

        @Override
        public void accept(Batch batch) {
            for (long sourceId : batch.nodeIds()) {
                var neighbors = new HashSet<Long>();
                graph.forEachRelationship(
                    sourceId, (src, trg) -> {
                        neighbors.add(trg);
                        return true;
                    }
                );
                // since graph is undirected, only process pairs where sourceId < targetId
                LongStream.range(sourceId + 1, graph.nodeCount()).forEach(targetId -> {
                        if (neighbors.contains(targetId)) return;
                        var probability = predictor.predictedProbability(graph, sourceId, targetId);
                        if (probability < threshold) return;
                        predictedLinks.add(sourceId, targetId, probability);
                    }
                );
            }
            progressLogger.logProgress(batch.size());
        }
    }
}