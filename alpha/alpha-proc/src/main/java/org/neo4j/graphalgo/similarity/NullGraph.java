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
package org.neo4j.graphalgo.similarity;

import org.neo4j.collection.primitive.PrimitiveLongIterable;
import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.api.IdMapGraph;
import org.neo4j.graphalgo.api.NodeProperties;
import org.neo4j.graphalgo.api.RelationshipConsumer;
import org.neo4j.graphalgo.api.RelationshipIntersect;
import org.neo4j.graphalgo.api.RelationshipWithPropertyConsumer;
import org.neo4j.graphalgo.core.loading.IdMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.LongPredicate;

/**
 * The NullGraph is used for non-product algos that don't use a graph.
 * It makes it a bit easier to adapt those algorithms to the new API,
 * as we can override graph creation and inject a NullGraph.
 */
public class NullGraph implements Graph, IdMapGraph {

    /*
     * The NullGraph doesn't have any nodes or rels, but it isn't empty because then the algo will not be run.
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void releaseTopology() {}

    @Override
    public long relationshipCount() {
        return 0L;
    }

    @Override
    public boolean isUndirected() {
        return false;
    }

    @Override
    public boolean hasRelationshipProperty() {
        return false;
    }

    @Override
    public void canRelease(boolean canRelease) {}

    @Override
    public RelationshipIntersect intersection() {
        return null;
    }

    @Override
    public IdMap idMap() {
        return null;
    }

    @Override
    public IdMapGraph concurrentCopy() {
        return this;
    }

    @Override
    public Collection<PrimitiveLongIterable> batchIterables(int batchSize) {
        return Collections.emptySet();
    }

    @Override
    public int degree(long nodeId) {
        return 0;
    }

    @Override
    public long toMappedNodeId(long nodeId) {
        throw new NullGraphStore.NullGraphException();
    }

    @Override
    public long toOriginalNodeId(long nodeId) {
        throw new NullGraphStore.NullGraphException();
    }

    @Override
    public boolean contains(long nodeId) {
        return false;
    }

    @Override
    public long nodeCount() {
        return 0L;
    }

    @Override
    public void forEachNode(LongPredicate consumer) {}

    @Override
    public PrimitiveLongIterator nodeIterator() {
        throw new NullGraphStore.NullGraphException();
    }

    @Override
    public NodeProperties nodeProperties(String propertyKey) {
        throw new NullGraphStore.NullGraphException();
    }

    @Override
    public Set<String> availableNodeProperties() {
        return Collections.emptySet();
    }

    @Override
    public long getTarget(long nodeId, long index) {
        throw new NullGraphStore.NullGraphException();
    }

    @Override
    public void forEachRelationship(long nodeId, RelationshipConsumer consumer) {}

    @Override
    public void forEachRelationship(long nodeId, double fallbackValue, RelationshipWithPropertyConsumer consumer) {}

    @Override
    public boolean exists(long sourceNodeId, long targetNodeId) {
        return false;
    }

    @Override
    public double relationshipProperty(long sourceNodeId, long targetNodeId, double fallbackValue) {
        throw new NullGraphStore.NullGraphException();
    }

    @Override
    public double relationshipProperty(long sourceNodeId, long targetNodeId) {
        throw new NullGraphStore.NullGraphException();
    }
}