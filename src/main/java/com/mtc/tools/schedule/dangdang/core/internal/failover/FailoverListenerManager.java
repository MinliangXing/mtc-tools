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

package com.mtc.tools.schedule.dangdang.core.internal.failover;


import com.mtc.tools.schedule.dangdang.core.config.LiteJobConfiguration;
import com.mtc.tools.schedule.dangdang.core.internal.config.ConfigurationNode;
import com.mtc.tools.schedule.dangdang.core.internal.config.ConfigurationService;
import com.mtc.tools.schedule.dangdang.core.internal.config.LiteJobConfigurationGsonFactory;
import com.mtc.tools.schedule.dangdang.core.internal.instance.InstanceNode;
import com.mtc.tools.schedule.dangdang.core.internal.listener.AbstractJobListener;
import com.mtc.tools.schedule.dangdang.core.internal.listener.AbstractListenerManager;
import com.mtc.tools.schedule.dangdang.core.internal.schedule.JobRegistry;
import com.mtc.tools.schedule.dangdang.core.internal.sharding.ShardingService;
import com.mtc.tools.schedule.dangdang.core.reg.base.CoordinatorRegistryCenter;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

import java.util.List;

/**
 * 失效转移监听管理器.
 *
 * @author zhangliang
 */
public final class FailoverListenerManager extends AbstractListenerManager {

    private final String jobName;

    private final ConfigurationService configService;

    private final ShardingService shardingService;

    private final FailoverService failoverService;

    private final ConfigurationNode configNode;

    private final InstanceNode instanceNode;

    public FailoverListenerManager(final CoordinatorRegistryCenter regCenter, final String jobName) {
        super(regCenter, jobName);
        this.jobName = jobName;
        configService = new ConfigurationService(regCenter, jobName);
        shardingService = new ShardingService(regCenter, jobName);
        failoverService = new FailoverService(regCenter, jobName);
        configNode = new ConfigurationNode(jobName);
        instanceNode = new InstanceNode(jobName);
    }

    @Override
    public void start() {
        addDataListener(new JobCrashedJobListener());
        addDataListener(new FailoverSettingsChangedJobListener());
    }

    private boolean isFailoverEnabled() {
        LiteJobConfiguration jobConfig = configService.load(true);
        return null != jobConfig && jobConfig.isFailover();
    }

    class JobCrashedJobListener extends AbstractJobListener {

        @Override
        protected void dataChanged(final String path, final TreeCacheEvent.Type eventType, final String data) {
            if (isFailoverEnabled() && TreeCacheEvent.Type.NODE_REMOVED == eventType && instanceNode.isInstancePath(path)) {
                String jobInstanceId = path.substring(instanceNode.getInstanceFullPath().length() + 1);
                if (jobInstanceId.equals(JobRegistry.getInstance().getJobInstance(jobName).getJobInstanceId())) {
                    return;
                }
                List<Integer> failoverItems = failoverService.getFailoverItems(jobInstanceId);
                if (!failoverItems.isEmpty()) {
                    for (int each : failoverItems) {
                        failoverService.setCrashedFailoverFlag(each);
                        failoverService.failoverIfNecessary();
                    }
                } else {
                    for (int each : shardingService.getShardingItems(jobInstanceId)) {
                        failoverService.setCrashedFailoverFlag(each);
                        failoverService.failoverIfNecessary();
                    }
                }
            }
        }
    }

    class FailoverSettingsChangedJobListener extends AbstractJobListener {

        @Override
        protected void dataChanged(final String path, final TreeCacheEvent.Type eventType, final String data) {
            if (configNode.isConfigPath(path) && TreeCacheEvent.Type.NODE_UPDATED == eventType && !LiteJobConfigurationGsonFactory.fromJson(data).isFailover()) {
                failoverService.removeFailoverInfo();
            }
        }
    }
}
