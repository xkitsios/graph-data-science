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
package org.neo4j.graphalgo.beta.k1coloring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.graphalgo.AlgoBaseProcTest;
import org.neo4j.graphalgo.BaseProcTest;
import org.neo4j.graphalgo.GdsCypher;
import org.neo4j.graphalgo.HeapControlTest;
import org.neo4j.graphalgo.IterationsConfigTest;
import org.neo4j.graphalgo.MemoryEstimateTest;
import org.neo4j.graphalgo.catalog.GraphCreateProc;
import org.neo4j.graphalgo.core.loading.GraphStoreCatalog;
import org.neo4j.graphalgo.core.utils.paged.HugeLongArray;
import org.neo4j.graphalgo.extension.Neo4jGraph;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

abstract class K1ColoringProcBaseTest<CONFIG extends K1ColoringConfig> extends BaseProcTest  implements
    AlgoBaseProcTest<K1Coloring, CONFIG, HugeLongArray>,
    IterationsConfigTest<K1Coloring, CONFIG, HugeLongArray>,
    MemoryEstimateTest<K1Coloring, CONFIG, HugeLongArray>,
    HeapControlTest<K1Coloring, CONFIG, HugeLongArray> {

    @Neo4jGraph
    public static final String DB_CYPHER = "CREATE" +
           " (a)" +
           ",(b)" +
           ",(c)" +
           ",(d)" +
           ",(a)-[:REL]->(b)" +
           ",(a)-[:REL]->(c)";

    @Override
    public GraphDatabaseAPI graphDb() {
        return db;
    }

    static final String K1COLORING_GRAPH = "myGraph";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
            getProcedureClazz(),
            GraphCreateProc.class
        );
        loadGraph(K1COLORING_GRAPH);
    }

    @AfterEach
    void tearDown() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

    @Override
    public void assertResultEquals(HugeLongArray result1, HugeLongArray result2) {
        assertArrayEquals(result1.toArray(), result2.toArray());
    }

    GdsCypher.ModeBuildStage algoBuildStage() {
        return GdsCypher.call()
            .explicitCreation(K1COLORING_GRAPH)
            .algo("gds", "beta", "k1coloring");
    }

}
