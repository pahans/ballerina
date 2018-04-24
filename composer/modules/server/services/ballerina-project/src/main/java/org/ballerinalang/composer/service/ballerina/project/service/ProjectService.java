/*
 * Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.composer.service.ballerina.project.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.ballerinalang.composer.server.core.ServerConfig;
import org.ballerinalang.composer.server.core.ServerConstants;
import org.ballerinalang.composer.server.spi.ComposerService;
import org.ballerinalang.composer.server.spi.ServiceInfo;
import org.ballerinalang.composer.server.spi.ServiceType;
import org.ballerinalang.composer.service.ballerina.project.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.msf4j.Request;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *  Micro service that exposes the file system to composer.
 */
@Path(ServerConstants.CONTEXT_ROOT + "/" + Constants.SERVICE_PATH)
public class ProjectService implements ComposerService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private static final String STATUS = "status";
    private static final String SUCCESS = "success";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String MIME_APPLICATION_JSON = "application/json";

    private ServerConfig serverConfig;

    /**
     * Initializing service.
     */
    public ProjectService(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @OPTIONS
    @Path("/create")
    public Response readOptions() {
        return createCORSResponse();
    }

    @POST
    @Path("/create")
    @Consumes(MIME_APPLICATION_JSON)
    @Produces(MIME_APPLICATION_JSON)
    public Response create(@Context Request request, @PathParam("protocol") String protocol, String args) {
        try {
            List<String> commandList = new ArrayList<>();
            // path to ballerina
            String ballerinaExecute = System.getProperty("ballerina.home") + File.separator + "bin" + File.separator +
                    "ballerina";

            if (isWindows()) {
                ballerinaExecute += ".bat";
            }
            commandList.add(ballerinaExecute);
            commandList.add("init");
            String projectPath = new JsonParser().parse(args)
                    .getAsJsonObject().get(Constants.PROJECT_PATH).getAsString();
            Runtime.getRuntime().exec(commandList.toArray(new String[0]), null, new File(projectPath));
            JsonObject entity = new JsonObject();
            entity.addProperty("success", true);
            return createOKResponse(entity);
        } catch (Throwable throwable) {
            logger.error("/create service error", throwable.getMessage(), throwable);
            return createErrorResponse(throwable);
        }
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        return (os.contains("win"));
    }

    /**
     * Creates the JSON response for given entity.
     *
     * @param entity Response
     * @return Response
     */
    private Response createOKResponse(Object entity) {
        return Response.status(Response.Status.OK)
                .entity(entity)
                .header(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, '*')
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Creates an error response for the given IO Exception.
     *
     * @param ex Thrown Exception
     * @return Error Message
     */
    private Response createErrorResponse(Throwable ex) {
        JsonObject entity = new JsonObject();
        String errMsg = ex.getMessage();
        entity.addProperty("Error", errMsg);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(entity)
                .header(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, '*')
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Create CORS Response allowing all origins.
     *
     * TODO: Find a better solution to handle CORS in a global manner
     * and to avoid redundant logic for CORS in each service.
     *
     * @return CORS Response
     */
    public Response createCORSResponse() {
        return Response.ok()
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "*")
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(), "true")
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(), "POST, GET, OPTIONS ")
                .header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                        HttpHeaderNames.CONTENT_TYPE.toString() + ", " + HttpHeaderNames.ACCEPT.toString() +
                                ", X-Requested-With")
                .build();
    }

    @Override
    public ServiceInfo getServiceInfo() {
        return new ServiceInfo(Constants.SERVICE_NAME, Constants.SERVICE_PATH, ServiceType.HTTP);
    }
}
