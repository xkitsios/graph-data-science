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
package org.neo4j.graphalgo.core.huge;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.Orientation;
import org.neo4j.graphalgo.RelationshipType;
import org.neo4j.graphalgo.api.CompositeRelationshipIterator;
import org.neo4j.graphalgo.api.GraphStore;
import org.neo4j.graphalgo.api.ImmutableProperties;
import org.neo4j.graphalgo.api.Relationships;
import org.neo4j.graphalgo.beta.generator.PropertyProducer;
import org.neo4j.graphalgo.beta.generator.RandomGraphGenerator;
import org.neo4j.graphalgo.beta.generator.RelationshipDistribution;
import org.neo4j.graphalgo.extension.GdlExtension;
import org.neo4j.graphalgo.extension.GdlGraph;
import org.neo4j.graphalgo.extension.IdFunction;
import org.neo4j.graphalgo.extension.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import static org.assertj.core.api.Assertions.assertThat;

@GdlExtension
class CSRCompositeRelationshipIteratorTest {
    @GdlGraph
    private static final String DB =
        "  (a), (b), (c)" +
        ", (a)-[:T1 {prop1: 42.0D, prop2: 84.0D, prop3: 1337.0D}]->(b)" +
        ", (b)-[:T1 {prop1: 1.0D, prop2: 2.0D, prop3: 3.0D}]->(a)" +
        ", (b)-[:T1 {prop1: 4.0D, prop2: 5.0D, prop3: 6.0D}]->(c)" +
        ", (a)-[:T2 {prop4: 0.0D}]->(b)";

    @Inject
    GraphStore graphStore;

    @Inject
    IdFunction idFunction;

    @Test
    void canIterateSingleProperty() {
        var iterator = graphStore.getCompositeRelationshipIterator(
            RelationshipType.of("T1"),
            List.of("prop1")
        );

        assertIteration(
            iterator,
            idFunction.of("a"),
            Map.of(
                idFunction.of("b"), List.of(42.0D)
            )
        );

        assertIteration(
            iterator,
            idFunction.of("b"),
            Map.of(
                idFunction.of("a"), List.of(1.0D),
                idFunction.of("c"), List.of(4.0D)
            )
        );
    }

    @Test
    void canIterateMultipleProperty() {
        var iterator = graphStore.getCompositeRelationshipIterator(
            RelationshipType.of("T1"),
            List.of("prop1", "prop2", "prop3")
        );

        assertIteration(
            iterator,
            idFunction.of("a"),
            Map.of(
                idFunction.of("b"), List.of(42.0D, 84.0D, 1337.0D)
            )
        );

        assertIteration(
            iterator,
            idFunction.of("b"),
            Map.of(
                idFunction.of("a"), List.of(1.0D, 2.0D, 3.0D),
                idFunction.of("c"), List.of(4.0D, 5.0D, 6.0D)
            )
        );
    }

    @Test
    void respectInGivenPropertyOrder() {
        var iterator = graphStore.getCompositeRelationshipIterator(
            RelationshipType.of("T1"),
            List.of("prop3", "prop2", "prop1")
        );

        assertIteration(
            iterator,
            idFunction.of("a"),
            Map.of(
                idFunction.of("b"), List.of(1337.0D, 84.0D, 42.0D)
            )
        );

        assertIteration(
            iterator,
            idFunction.of("b"),
            Map.of(
                idFunction.of("a"), List.of(3.0D, 2.0D, 1.0D),
                idFunction.of("c"), List.of(6.0D, 5.0D, 4.0D)
            )
        );
    }

    @Test
    void worksWithoutRelationshipsPresent() {
        var iterator = graphStore.getCompositeRelationshipIterator(
            RelationshipType.of("T2"),
            List.of("prop4")
        );

        assertIteration(
            iterator,
            idFunction.of("b"),
            Map.of()
        );
    }

    @Test
    void abortWhenConsumerReturnsFalse() {
        var iterator = graphStore.getCompositeRelationshipIterator(
            RelationshipType.of("T1"),
            List.of("prop1")
        );

        var callCounter = new MutableInt(0);
        iterator.forEachRelationship(idFunction.of("b"), (s, t, p) -> {
            callCounter.increment();
            return false;
        });

        assertThat(callCounter.intValue()).isEqualTo(1);
    }

    @Test
    void multiThread() {
        var nodeCount = 4;
        var averageDegree = 100000;

        var graph = RandomGraphGenerator
            .builder()
            .nodeCount(nodeCount)
            .averageDegree(averageDegree)
            .relationshipDistribution(RelationshipDistribution.UNIFORM)
            .relationshipPropertyProducer(PropertyProducer.fixed("prop", 1.0D))
            .build()
            .generate();

        String[] propertyKeys = {"prop"};
        Relationships.Properties[] properties = new Relationships.Properties[1];
        properties[0] = ImmutableProperties
            .builder()
            .degrees(graph.adjacencyDegrees)
            .offsets(graph.propertyOffsets)
            .list(graph.properties)
            .elementCount(nodeCount)
            .orientation(Orientation.NATURAL)
            .isMultiGraph(true)
            .defaultPropertyValue(Double.NaN)
            .build();

        var iterator = new CSRCompositeRelationshipIterator(
            graph.relationshipTopology(),
            propertyKeys,
            properties
        );

        var phaser = new Phaser(5);
        var tasks = List.of(
            new IterationTask(iterator, 0, phaser),
            new IterationTask(iterator, 1, phaser),
            new IterationTask(iterator, 2, phaser),
            new IterationTask(iterator, 3, phaser)
        );

        var pool = Executors.newCachedThreadPool();
        tasks.forEach(pool::submit);

        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndAwaitAdvance();

        assertThat(tasks.stream().mapToDouble(IterationTask::sum).sum()).isEqualTo(nodeCount * averageDegree);
    }

    private void assertIteration(
        CompositeRelationshipIterator iterator,
        long nodeId,
        Map<Long, List<Double>> expected
    ) {
        List<Long> seenTargets = new ArrayList<>();
        iterator.forEachRelationship(nodeId, (source, target, properties) -> {
            assertThat(source).isEqualTo(nodeId);

            seenTargets.add(target);
            assertThat(expected).containsKey(target);
            assertThat(properties).containsExactly(expected.get(target).toArray(new Double[0]));

            return true;
        });

        assertThat(seenTargets).containsExactlyInAnyOrder(expected.keySet().toArray(new Long[0]));
    }

    private static class IterationTask implements Runnable {

        private final CompositeRelationshipIterator iterator;
        private final long nodeId;
        private final Phaser phaser;
        private double sum;

        IterationTask(CompositeRelationshipIterator iterator, long nodeId, Phaser phaser) {
            this.iterator = iterator.concurrentCopy();
            this.nodeId = nodeId;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arriveAndAwaitAdvance();
            iterator.forEachRelationship(nodeId, (source, target, properties) -> {
                sum += properties[0];
                return true;
            });
            phaser.arrive();
        }

        double sum() {
            return sum;
        }
    }
}