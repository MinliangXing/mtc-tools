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

package com.mtc.tools.schedule.dangdang.spring.api;


import com.google.common.base.Optional;
import com.mtc.tools.schedule.dangdang.core.api.ElasticJob;
import com.mtc.tools.schedule.dangdang.core.api.JobScheduler;
import com.mtc.tools.schedule.dangdang.core.api.listener.ElasticJobListener;
import com.mtc.tools.schedule.dangdang.core.config.LiteJobConfiguration;
import com.mtc.tools.schedule.dangdang.core.event.JobEventConfiguration;
import com.mtc.tools.schedule.dangdang.core.reg.base.CoordinatorRegistryCenter;
import com.mtc.tools.schedule.dangdang.spring.job.util.AopTargetUtils;

/**
 * 基于Spring的作业启动器.
 *
 * @author caohao
 * @author zhangliang
 */
public final class SpringJobScheduler extends JobScheduler {
    
    private final ElasticJob elasticJob;
    
    public SpringJobScheduler(final ElasticJob elasticJob, final CoordinatorRegistryCenter regCenter, final LiteJobConfiguration jobConfig, final ElasticJobListener... elasticJobListeners) {
        super(regCenter, jobConfig, getTargetElasticJobListeners(elasticJobListeners));
        this.elasticJob = elasticJob;
    }
    
    public SpringJobScheduler(final ElasticJob elasticJob, final CoordinatorRegistryCenter regCenter, final LiteJobConfiguration jobConfig,
                              final JobEventConfiguration jobEventConfig, final ElasticJobListener... elasticJobListeners) {
        super(regCenter, jobConfig, jobEventConfig, getTargetElasticJobListeners(elasticJobListeners));
        this.elasticJob = elasticJob;
    }
    
    private static ElasticJobListener[] getTargetElasticJobListeners(final ElasticJobListener[] elasticJobListeners) {
        final ElasticJobListener[] result = new ElasticJobListener[elasticJobListeners.length];
        for (int i = 0; i < elasticJobListeners.length; i++) {
            result[i] = (ElasticJobListener) AopTargetUtils.getTarget(elasticJobListeners[i]);
        }
        return result;
    }
    
    @Override
    protected Optional<ElasticJob> createElasticJobInstance() {
        return Optional.fromNullable(elasticJob);
    }
}
