package com.a.eye.skywalking.collector.worker;

import com.a.eye.skywalking.collector.actor.*;
import com.a.eye.skywalking.collector.queue.EndOfBatchCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author pengys5
 */
public abstract class AnalysisMember extends AbstractLocalAsyncWorker {

    private Logger logger = LogManager.getFormatterLogger(AnalysisMember.class);

    public AnalysisMember(Role role, ClusterWorkerContext clusterContext, LocalWorkerContext selfContext) {
        super(role, clusterContext, selfContext);
    }

    public abstract void analyse(Object message) throws Exception;

    @Override
    public void preStart() throws ProviderNotFoundException {

    }

    @Override
    final public void onWork(Object message) throws Exception {
        if (message instanceof EndOfBatchCommand) {
            aggregation();
        } else {
            try {
                analyse(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract void aggregation() throws Exception;
}
