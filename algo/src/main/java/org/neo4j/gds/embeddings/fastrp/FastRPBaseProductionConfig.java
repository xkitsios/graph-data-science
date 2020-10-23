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
package org.neo4j.gds.embeddings.fastrp;

import org.immutables.value.Value;
import org.neo4j.graphalgo.annotation.Configuration;

import java.util.List;

import static org.neo4j.graphalgo.utils.StringFormatting.formatWithLocale;

public interface FastRPBaseProductionConfig extends FastRPBaseConfig {

    @Override
    @Configuration.Ignore
    default List<String> featureProperties() {
        return List.of();
    }

    @Override
    @Configuration.Ignore
    default int propertyDimension() {
        return 0;
    }

    @Value.Check
    default void validate() {
        List<? extends Number> iterationWeights = iterationWeights();
        FastRPBaseConfig.validateCommon(iterationWeights);
        if (embeddingDimension() < 1) {
            throw new IllegalArgumentException(formatWithLocale(
                "The value of embeddingDimension is %s, but must be at least 1",
                embeddingDimension()
            ));
        }
    }

}