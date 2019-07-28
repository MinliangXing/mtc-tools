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

package com.mtc.tools.schedule.dangdang.console.restful.config;


import com.mtc.tools.schedule.dangdang.console.service.JobAPIService;
import com.mtc.tools.schedule.dangdang.console.service.impl.JobAPIServiceImpl;
import com.mtc.tools.schedule.dangdang.lifecycle.domain.JobSettings;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * 作业配置的RESTful API.
 *
 * @author caohao
 */
@Path("/jobs/config")
public final class LiteJobConfigRestfulApi {

    private JobAPIService jobAPIService = new JobAPIServiceImpl();

    /**
     * 获取作业配置.
     *
     * @param jobName 作业名称
     * @return 作业配置
     */
    @GET
    @Path("/{jobName}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobSettings getJobSettings(@PathParam("jobName") final String jobName) {
        return jobAPIService.getJobSettingsAPI().getJobSettings(jobName);
    }

    /**
     * 修改作业配置.
     *
     * @param jobSettings 作业配置
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateJobSettings(final JobSettings jobSettings) {
        jobAPIService.getJobSettingsAPI().updateJobSettings(jobSettings);
    }

    /**
     * 删除作业配置.
     *
     * @param jobName 作业名称
     */
    @DELETE
    @Path("/{jobName}")
    public void removeJob(@PathParam("jobName") final String jobName) {
        jobAPIService.getJobSettingsAPI().removeJobSettings(jobName);
    }
}
