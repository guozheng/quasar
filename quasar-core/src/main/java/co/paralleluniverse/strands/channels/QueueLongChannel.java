/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.BasicSingleConsumerLongQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class QueueLongChannel extends QueuePrimitiveChannel<Long> implements LongChannel {
    public QueueLongChannel(BasicSingleConsumerLongQueue queue, OverflowPolicy policy) {
        super(queue, policy);
    }

    @Override
    public long receiveLong() throws SuspendExecution, InterruptedException {
        if (isClosed())
            throw new EOFException();
        awaitItem();
        final long m = queue().pollLong();
        signalSenders();
        return m;
    }

    @Override
    public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        if (isClosed())
            throw new EOFException();
        if (!awaitItem(timeout, unit))
            throw new TimeoutException();
        final long m = queue().pollLong();
        signalSenders();
        return m;
    }

    @Override
    public boolean trySend(long message) {
        if (isSendClosed())
            return true;
        if (queue().enq(message)) {
            signalReceivers();
            return true;
        } else
            return false;
    }

    @Override
    public void send(long message) throws SuspendExecution, InterruptedException {
        if (isSendClosed())
            return;
        if (!queue().enq(message))
            super.send(message);
        else
            signalReceivers();
    }

    @Override
    public boolean send(long message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (isSendClosed())
            return true;
        if (!queue().enq(message))
            return super.send(message, timeout, unit);
        signalReceivers();
        return true;
    }

    @Override
    protected BasicSingleConsumerLongQueue queue() {
        return (BasicSingleConsumerLongQueue) queue;
    }
}
