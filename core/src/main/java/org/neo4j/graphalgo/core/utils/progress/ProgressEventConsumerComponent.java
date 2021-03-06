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
package org.neo4j.graphalgo.core.utils.progress;

import org.jctools.queues.MpscLinkedQueue;
import org.neo4j.function.ThrowingFunction;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.Context;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.Log;
import org.neo4j.monitoring.Monitors;
import org.neo4j.scheduler.JobScheduler;

import java.util.Queue;

final class ProgressEventConsumerComponent extends LifecycleAdapter implements ThrowingFunction<Context, ProgressEventTracker, ProcedureException> {

    private final Log log;
    private final JobScheduler jobScheduler;
    private final Monitors globalMonitors;
    private final ProgressEventConsumer.Monitor monitor;
    private final LoggingProgressEventMonitor loggingMonitor;
    private final Queue<LogEvent> messageQueue;
    private volatile ProgressEventConsumer progressEventConsumer;

    ProgressEventConsumerComponent(
        Log log,
        JobScheduler jobScheduler,
        Monitors globalMonitors
    ) {
        this.log = log;
        this.jobScheduler = jobScheduler;
        this.globalMonitors = globalMonitors;
        this.monitor = globalMonitors.newMonitor(ProgressEventConsumer.Monitor.class);
        this.loggingMonitor = new LoggingProgressEventMonitor(log);
        this.messageQueue = new MpscLinkedQueue<>();
    }

    @Override
    public void start() {
        globalMonitors.addMonitorListener(loggingMonitor);
        progressEventConsumer = new ProgressEventConsumer(monitor, jobScheduler, messageQueue);
        progressEventConsumer.start();
        this.log.info("GDS Progress event tracking is enabled");
    }

    @Override
    public void stop() {
        progressEventConsumer.stop();
        progressEventConsumer = null;
        globalMonitors.removeMonitorListener(loggingMonitor);
    }

    ProgressEventConsumer progressEventConsumer() {
        return progressEventConsumer;
    }

    @Override
    public ProgressEventTracker apply(Context context) throws ProcedureException {
        var progressEventConsumer = this.progressEventConsumer;
        if (progressEventConsumer == null) {
            throw new ProcedureException(Status.Database.Unknown, "The " + getClass().getSimpleName() + " is stopped");
        }
        var username = context.securityContext().subject().username();
        return new ProgressEventQueueTracker(messageQueue, username);
    }
}
