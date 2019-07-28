package com.mtc.tools.schedule.dangdang.starter.annotation;

import com.mtc.tools.schedule.dangdang.starter.autoconfigure.ElasticJobAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({ElasticJobAutoConfiguration.class})
public @interface EnableElasticJob {

}

