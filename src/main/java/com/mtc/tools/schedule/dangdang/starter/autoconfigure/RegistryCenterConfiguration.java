package com.mtc.tools.schedule.dangdang.starter.autoconfigure;


import com.mtc.tools.schedule.dangdang.core.api.ElasticJob;
import com.mtc.tools.schedule.dangdang.core.reg.zookeeper.ZookeeperConfiguration;
import com.mtc.tools.schedule.dangdang.core.reg.zookeeper.ZookeeperRegistryCenter;
import com.mtc.tools.schedule.dangdang.starter.ZookeeperRegistryProperties;
import com.mtc.tools.schedule.dangdang.starter.annotation.ElasticJobConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//@Configuration
@ConditionalOnClass(ElasticJob.class)
@ConditionalOnBean(annotation = ElasticJobConfig.class)
@EnableConfigurationProperties(ZookeeperRegistryProperties.class)
public class RegistryCenterConfiguration {

    private final ZookeeperRegistryProperties regCenterProperties;

    /**
     * Instantiates a new Registry center configuration.
     *
     * @param regCenterProperties the reg center properties
     */
    @Autowired
    public RegistryCenterConfiguration(ZookeeperRegistryProperties regCenterProperties) {
        this.regCenterProperties = regCenterProperties;
    }

    /**
     * Reg center zookeeper registry center.
     *
     * @return the zookeeper registry center
     */
    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    public ZookeeperRegistryCenter regCenter() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(regCenterProperties.getZkAddressList(), regCenterProperties.getNamespace());
        zookeeperConfiguration.setBaseSleepTimeMilliseconds(regCenterProperties.getBaseSleepTimeMilliseconds());
        zookeeperConfiguration.setConnectionTimeoutMilliseconds(regCenterProperties.getConnectionTimeoutMilliseconds());
        zookeeperConfiguration.setMaxSleepTimeMilliseconds(regCenterProperties.getMaxSleepTimeMilliseconds());
        zookeeperConfiguration.setSessionTimeoutMilliseconds(regCenterProperties.getSessionTimeoutMilliseconds());
        zookeeperConfiguration.setMaxRetries(regCenterProperties.getMaxRetries());
        zookeeperConfiguration.setDigest(regCenterProperties.getDigest());
        return new ZookeeperRegistryCenter(zookeeperConfiguration);
    }

}
