package io.slingr.endpoints.skeleton;

import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.datastores.DataStore;
import io.slingr.endpoints.services.datastores.DataStoreResponse;
import io.slingr.endpoints.services.exchange.Parameter;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.Strings;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import io.slingr.endpoints.ws.exchange.WebServiceRequest;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * <p>Sample endpoint
 *
 * <p>Created by lefunes on 01/12/16.
 */
@SlingrEndpoint(name = "skeleton")
public class SkeletonEndpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(SkeletonEndpoint.class);

    @ApplicationLogger
    private AppLogs appLogger;

    @EndpointProperty
    private String token;

    private final Random random = new Random();

    public SkeletonEndpoint() {
    }

    @Override
    public void endpointStarted() {
        // the loggers, endpoint properties, data stores, etc. are initialized at this point. the endpoint is ready to be used
        logger.info("Endpoint is started");
    }

    @Override
    public void endpointStopped(String cause) {
        logger.info(String.format("Endpoint is stopping - cause [%s]", cause));
    }

    @EndpointFunction(name = "randomNumber")
    public Json generateRandomNumber(Json params) {
        if(params == null){
            params = Json.map();
        }
        // this log will be sent to the app and can be seen in the general logs in the app monitor
        appLogger.info("Request to generate random number received [%s]", params);

        params.set("token", token);

        // generate random number
        int max = 10000;
        if (params.contains("max")) {
            try {
                max = params.integer("max");
            } catch (Exception e) {
                throw new IllegalArgumentException("Parameter 'max' is not a valid number");
            }
        }
        Json res = Json.map();
        res.set("number", random.nextInt(max));

        // this is an internal log of the endpoint
        logger.info(String.format("Random number generated: [%s]", res.integer("number")));

        return res;
    }

    @EndpointFunction
    public Json ping(FunctionRequest request) {
        // in this case as the argument is FunctionRequest we don't get only the body of the request,
        // but all the information associated to it, which is needed when having callbacks
        final Json data = request.getJsonParams();

        // this log will be sent to the app and can be seen in the general logs in the app monitor
        appLogger.info("Request to ping received [%s]", data);

        Json res = Json.map();
        res.set("token", token);
        res.set("ping", "pong");

        //send event
        events().send("pong", data, request.getFunctionId());

        // this is an internal log of the endpoint
        logger.info("Pong sent to app");

        // we will return the status for the function call, while the real response will go in the callback
        return Json.map().set("status", "ok");
    }

    @EndpointFunction(name = "error")
    public Json error(FunctionRequest request){
        appLogger.warn("Request to generate error received");

        throw new IllegalArgumentException("Error generated!");
    }

    @EndpointWebService(methods = {RestMethod.POST})
    public String webhook(WebServiceRequest request){
        Json body;
        try {
            body = request.getJsonBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Body must be valid JSON");
        }

        // send event to app
        events().send("inboundEvent", body);

        // this is what the webhook caller receives as response
        return "ok";
    }
}
