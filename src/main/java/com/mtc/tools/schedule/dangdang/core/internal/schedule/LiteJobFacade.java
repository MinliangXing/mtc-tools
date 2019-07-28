/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.mtc.tools.schedule.dangdang.core.internal.schedule;

import com.google.common.base.Strings;
import com.mtc.tools.schedule.dangdang.core.api.listener.ElasticJobListener;
import com.mtc.tools.schedule.dangdang.core.config.LiteJobConfiguration;
import com.mtc.tools.schedule.dangdang.core.config.dataflow.DataflowJobConfiguration;
import com.mtc.tools.schedule.dangdang.core.context.TaskContext;
import com.mtc.tools.schedule.dangdang.core.event.JobEventBus;
import com.mtc.tools.schedule.dangdang.core.event.type.JobExecutionEvent;
import com.mtc.tools.schedule.dangdang.core.event.type.JobStatusTraceEvent;
import com.mtc.tools.schedule.dangdang.core.exception.JobExecutionEnvironmentException;
import com.mtc.tools.schedule.dangdang.core.executor.JobFacade;
import com.mtc.tools.schedule.dangdang.core.executor.ShardingContexts;
import com.mtc.tools.schedule.dangdang.core.internal.config.ConfigurationService;
import com.mtc.tools.schedule.dangdang.core.internal.failover.FailoverService;
import com.mtc.tools.schedule.dangdang.core.internal.sharding.ExecutionContextService;
import com.mtc.tools.schedule.dangdang.core.internal.sharding.ExecutionService;
import com.mtc.tools.schedule.dangdang.core.internal.sharding.ShardingService;
import com.mtc.tools.schedule.dangdang.core.reg.base.CoordinatorRegistryCenter;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * 为作业提供内部服务的门面类.
 * 
 * @author zhangliang
 */
@Slf4j
public final class LiteJobFacade implements JobFacade {
    
    private final ConfigurationService configService;
    
    private final ShardingService shardingService;
    
    private final ExecutionContextService executionContextService;
    
    private final ExecutionService executionService;
    
    private final FailoverService failoverService;
    
    private final List<ElasticJobListener> elasticJobListeners;
    
    private final JobEventBus jobEventBus;
    
    public LiteJobFacade(final CoordinatorRegistryCenter regCenter, final String jobName, final List<ElasticJobListener> elasticJobListeners, final JobEventBus jobEventBus) {
        configService = new ConfigurationService(regCenter, jobName);
        shardingService = new ShardingService(regCenter, jobName);
        executionContextService = new ExecutionContextService(regCenter, jobName);
        executionService = new ExecutionService(regCenter, jobName);
        failoverService = new FailoverService(regCenter, jobName);
        this.elasticJobListeners = elasticJobListeners;
        this.jobEventBus = jobEventBus;
    }
    
    @Override
    public LiteJobConfiguration loadJobRootConfiguration(final boolean fromCache) {
        return configService.load(fromCache);
    }
    
    @Override
    public void checkJobExecutionEnvironment() throws JobExecutionEnvironmentException {
        configService.checkMaxTimeDiffSecondsTolerable();
    }
    
    @Override
    public void failoverIfNecessary() {
        if (configService.load(true).isFailover()) {
            failoverService.failoverIfNecessary();
        }
    }
    
    @Override
    public void registerJobBegin(final ShardingContexts shardingContexts) {
        executionService.registerJobBegin(shardingContexts);
    }
    
    @Override
    public void registerJobCompleted(final ShardingContexts shardingContexts) {
        executionService.registerJobCompleted(shardingContexts);
        if (configService.load(true).isFailover()) {
            failoverService.updateFailoverComplete(shardingContexts.getShardingItemParameters().keySet());
        }
    }
    
    @Override
    public ShardingContexts getShardingContexts() {
        boolean isFailover = configService.load(true).isFailover();
        if (isFailover) {
            List<Integer> failoverShardingItems = failoverService.getLocalFailoverItems();
            if (!failoverShardingItems.isEmpty()) {
                return executionContextService.getJobShardingContext(failoverShardingItems);
            }
        }
        shardingService.shardingIfNecessary();
        List<Integer> shardingItems = shardingService.getLocalShardingItems();
        if (isFailover) {
            shardingItems.removeAll(failoverService.getLocalTakeOffItems());
        }
        shardingItems.removeAll(executionService.getDisabledItems(shardingItems));
        return executionContextService.getJobShardingContext(shardingItems);
    }
    
    @Override
    public boolean misfireIfRunning(final Collection<Integer> shardingItems) {
        return executionService.misfireIfHasRunningItems(shardingItems);
    }
    
    @Override
    public void clearMisfire(final Collection<Integer> shardingItems) {
        executionService.clearMisfire(shardingItems);
    }
    
    @Override
    public boolean isExecuteMisfired(final Collection<Integer> shardingItems) {
        return isEligibleForJobRunning() && configService.load(true).getTypeConfig().getCoreConfig().isMisfire() && !executionService.getMisfiredJobItems(shardingItems).isEmpty();
    }
    
    @Override
    public boolean isEligibleForJobRunning() {
        LiteJobConfiguration liteJobConfig = configService.load(true);
        if (liteJobConfig.getTypeConfig() instanceof DataflowJobConfiguration) {
            return !shardingService.isNeedSharding() && ((DataflowJobConfiguration) liteJobConfig.getTypeConfig()).isStreamingProcess();    
        }
        return !shardingService.isNeedSharding();
    }
    
    @Override
    public boolean isNeedSharding() {
        return shardingService.isNeedSharding();
    }
    
    @Override
    public void beforeJobExecuted(final ShardingContexts shardingContexts) {
        for (ElasticJobListener each : elasticJobListeners) {
            each.beforeJobExecuted(shardingContexts);
        }
    }
    
    @Override
    public void afterJobExecuted(final ShardingContexts shardingContexts) {
        for (ElasticJobListener each : elasticJobListeners) {
            each.afterJobExecuted(shardingContexts);
        }
    }
    
    @Override
    public void postJobExecutionEvent(final JobExecutionEvent jobExecutionEvent) {
        jobEventBus.post(jobExecutionEvent);
    }
    
    @Override
    public void postJobStatusTraceEvent(final String taskId, final JobStatusTraceEvent.State state, final String message) {
        TaskContext taskContext = TaskContext.from(taskId);
        jobEventBus.post(new JobStatusTraceEvent(taskContext.getMetaInfo().getJobName(), taskContext.getId(),
                taskContext.getSlaveId(), JobStatusTraceEvent.Source.LITE_EXECUTOR, taskContext.getType(), taskContext.getMetaInfo().getShardingItems().toString(), state, message));
        if (!Strings.isNullOrEmpty(message)) {
            log.trace(message);
        }
    }
}
