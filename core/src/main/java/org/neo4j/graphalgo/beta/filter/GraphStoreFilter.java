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
package org.neo4j.graphalgo.beta.filter;

import org.jetbrains.annotations.NotNull;
import org.neo4j.graphalgo.annotation.ValueClass;
import org.neo4j.graphalgo.api.GraphStore;
import org.neo4j.graphalgo.beta.filter.expression.Expression;
import org.neo4j.graphalgo.beta.filter.expression.ExpressionParser;
import org.neo4j.graphalgo.beta.filter.expression.SemanticErrors;
import org.neo4j.graphalgo.beta.filter.expression.ValidationContext;
import org.neo4j.graphalgo.config.GraphCreateFromGraphConfig;
import org.neo4j.graphalgo.core.loading.CSRGraphStore;
import org.neo4j.graphalgo.core.utils.BatchingProgressLogger;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;
import org.neo4j.logging.Log;
import org.opencypher.v9_0.parser.javacc.ParseException;

import java.util.concurrent.ExecutorService;

import static org.neo4j.graphalgo.ElementProjection.PROJECT_ALL;

public final class GraphStoreFilter {

    @NotNull
    public static GraphStore filter(
        GraphStore graphStore,
        GraphCreateFromGraphConfig config,
        ExecutorService executorService,
        Log log,
        AllocationTracker tracker
    ) throws ParseException, SemanticErrors {
        var expressions = parseAndValidate(graphStore, config.nodeFilter(), config.relationshipFilter());

        var inputNodes = graphStore.nodes();

        var progressLogger = new BatchingProgressLogger(
            log,
            inputNodes.nodeCount(),
            "GraphStore Filter",
            config.concurrency()
        );

        progressLogger.logStart();

        var filteredNodes = NodesFilter.filterNodes(
            graphStore,
            expressions.nodeExpression(),
            config.concurrency(),
            executorService,
            progressLogger,
            tracker
        );

        var filteredRelationships = RelationshipsFilter.filterRelationships(
            graphStore,
            expressions.relationshipExpression(),
            inputNodes,
            filteredNodes.nodeMapping(),
            config.concurrency(),
            executorService,
            progressLogger,
            tracker
        );

        progressLogger.logFinish();

        return CSRGraphStore.of(
            graphStore.databaseId(),
            filteredNodes.nodeMapping(),
            filteredNodes.propertyStores(),
            filteredRelationships.topology(),
            filteredRelationships.propertyStores(),
            config.concurrency(),
            tracker
        );
    }

    @ValueClass
    interface Expressions {
        Expression nodeExpression();

        Expression relationshipExpression();
    }

    private static Expressions parseAndValidate(
        GraphStore graphStore,
        String nodeFilter,
        String relationshipFilter
    ) throws ParseException, SemanticErrors {
        var nodeExpression = ExpressionParser.parse(replaceStarWithTrue(nodeFilter));
        var relationshipExpression = ExpressionParser.parse(replaceStarWithTrue(relationshipFilter));

        nodeExpression.validate(ValidationContext.forNodes(graphStore)).validate();
        relationshipExpression.validate(ValidationContext.forRelationships(graphStore)).validate();

        return ImmutableExpressions.of(nodeExpression, relationshipExpression);
    }

    private static String replaceStarWithTrue(String filter) {
        return filter.equals(PROJECT_ALL) ? "true" : filter;
    }

    private GraphStoreFilter() {}
}
