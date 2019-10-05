// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;
import ballerina/reflect;

# Auth annotation module.
const string ANN_MODULE = "ballerina/http";
# Resource level annotation name.
const string RESOURCE_ANN_NAME = "ResourceConfig";
# Service level annotation name.
const string SERVICE_ANN_NAME = "ServiceConfig";

# Represents the Authorization header name.
public const string AUTH_HEADER = "Authorization";

# Specifies how to send the authentication credentials when exchanging tokens.
public type CredentialBearer AUTH_HEADER_BEARER|POST_BODY_BEARER|NO_BEARER;

# Indicates that the authentication credentials should be sent via the Authentication header.
public const AUTH_HEADER_BEARER = "AUTH_HEADER_BEARER";

# Indicates that the Authentication credentials should be sent via the body of the POST request.
public const POST_BODY_BEARER = "POST_BODY_BEARER";

# Indicates that the authentication credentials should not be sent.
public const NO_BEARER = "NO_BEARER";

# Indicates the status code.
public const STATUS_CODE = "STATUS_CODE";

# Extracts the Authorization header value from the request.
#
# + req - Request instance
# + return - Value of the Authorization header
public function extractAuthorizationHeaderValue(Request req) returns @tainted string {
    // extract authorization header
    return req.getHeader(AUTH_HEADER);
}

# Tries to retrieve the inbound authentication handlers based on their hierarchy
# (i.e., first from the resource level and then from the service level, if it is not there at the resource level).
#
# + context - The `FilterContext` instance
# + return - Returns the authentication handlers or whether it is needed to engage listener-level handlers or not
function getAuthHandlers(FilterContext context) returns InboundAuthHandler[]|InboundAuthHandler[][]|boolean {
    ServiceResourceAuth? resourceLevelAuthAnn;
    ServiceResourceAuth? serviceLevelAuthAnn;
    [resourceLevelAuthAnn, serviceLevelAuthAnn] = getServiceResourceAuthConfig(context);

     // check if authentication is enabled for resource and service
    boolean resourceSecured = isServiceResourceSecured(resourceLevelAuthAnn);
    boolean serviceSecured = isServiceResourceSecured(serviceLevelAuthAnn);

    // if resource is not secured, no need to check further
    if (!resourceSecured) {
        log:printWarn("Resource is not secured. `enabled: false`.");
        return false;
    }
    // Checks if Auth providers are given at the resource level.
    if (resourceLevelAuthAnn is ServiceResourceAuth) {
        var resourceAuthHandlers = resourceLevelAuthAnn?.authHandlers;
        if (!(resourceAuthHandlers is ())) {
            return resourceAuthHandlers;
        }
    }

    // if service is not secured, no need to check further
    if (!serviceSecured) {
        log:printWarn("Service is not secured. `enabled: false`.");
        return true;
    }
    // No Auth providers found at the resource level. Thus, try at the service level.
    if (serviceLevelAuthAnn is ServiceResourceAuth) {
        var serviceAuthHandlers = serviceLevelAuthAnn?.authHandlers;
        if (!(serviceAuthHandlers is ())) {
            return serviceAuthHandlers;
        }
    }
    return true;
}

# Tries to retrieve the authorization scopes hierarchically - first from the resource level and then
# from the service level, if it is not there in the resource level.
#
# + context - `FilterContext` instance
# + return - Authorization scopes or whether it is needed to engage listener level scopes or not
function getScopes(FilterContext context) returns string[]|string[][]|boolean {
    ServiceResourceAuth? resourceLevelAuthAnn;
    ServiceResourceAuth? serviceLevelAuthAnn;
    [resourceLevelAuthAnn, serviceLevelAuthAnn] = getServiceResourceAuthConfig(context);

    // check if authentication is enabled for resource and service
    boolean resourceSecured = isServiceResourceSecured(resourceLevelAuthAnn);
    boolean serviceSecured = isServiceResourceSecured(serviceLevelAuthAnn);

    // if resource is not secured, no need to check further
    if (!resourceSecured) {
        return false;
    }
    // check if auth providers are given at resource level
    if (resourceLevelAuthAnn is ServiceResourceAuth) {
        var resourceScopes = resourceLevelAuthAnn?.scopes;
        if (!(resourceScopes is ())) {
            return resourceScopes;
        }
    }

    // if service is not secured, no need to check further
    if (!serviceSecured) {
        return true;
    }
    // no auth providers found in resource level, try in service level
    if (serviceLevelAuthAnn is ServiceResourceAuth) {
        var serviceScopes = serviceLevelAuthAnn?.scopes;
        if (!(serviceScopes is ())) {
            return serviceScopes;
        }
    }
    return true;
}

# Retrieve the authentication annotation value for resource level and service level.
#
# + context - The `FilterContext` instance
# + return - Returns the resource-level and service-level authentication annotations
function getServiceResourceAuthConfig(FilterContext context) returns [ServiceResourceAuth?, ServiceResourceAuth?] {
    // get authn details from the resource level
    any annData = reflect:getResourceAnnotations(context.getService(), context.getResourceName(), RESOURCE_ANN_NAME,
                                                 ANN_MODULE);
    ServiceResourceAuth? resourceLevelAuthAnn = ();
    if !(annData is ()) {
        HttpResourceConfig resourceConfig = <HttpResourceConfig> annData;
        resourceLevelAuthAnn = resourceConfig?.auth;
    }

    annData = reflect:getServiceAnnotations(context.getService(), SERVICE_ANN_NAME, ANN_MODULE);
    ServiceResourceAuth? serviceLevelAuthAnn = ();
    if !(annData is ()) {
        HttpServiceConfig serviceConfig = <HttpServiceConfig> annData;
        serviceLevelAuthAnn = serviceConfig?.auth;
    }

    return [resourceLevelAuthAnn, serviceLevelAuthAnn];
}

# Check for the service or the resource is secured by evaluating the enabled flag configured by the user.
#
# + serviceResourceAuth - Service or resource auth annotation
# + return - Whether the service or resource secured or not
function isServiceResourceSecured(ServiceResourceAuth? serviceResourceAuth) returns boolean {
    boolean secured = true;
    if (serviceResourceAuth is ServiceResourceAuth) {
        secured = serviceResourceAuth.enabled;
    }
    return secured;
}

# Creates a map out of the headers of the HTTP response.
#
# + resp - The `Response` instance
# + return - Returns the map of the response headers
function createResponseHeaderMap(Response resp) returns @tainted map<anydata> {
    map<anydata> headerMap = { STATUS_CODE: resp.statusCode };
    string[] headerNames = resp.getHeaderNames();
    foreach string header in headerNames {
        string[] headerValues = resp.getHeaders(<@untainted> header);
        headerMap[header] = headerValues;
    }
    return headerMap;
}

# Logs, prepares, and returns the `AuthenticationError`.
#
# + message -The error message
# + err - The `error` instance
# + return - Returns the prepared `AuthenticationError` instance
function prepareAuthenticationError(string message, error? err = ()) returns AuthenticationError {
    log:printDebug(function () returns string { return message; });
    if (err is error) {
        AuthenticationError preparedError = error(AUTHN_FAILED, message = message, cause = err);
        return preparedError;
    }
    AuthenticationError preparedError = error(AUTHN_FAILED, message = message);
    return preparedError;
}

# Logs, prepares, and returns the `AuthorizationError`.
#
# + message -The error message
# + err - The `error` instance
# + return - Returns the prepared `AuthorizationError` instance
function prepareAuthorizationError(string message, error? err = ()) returns AuthorizationError {
    log:printDebug(function () returns string { return message; });
    if (err is error) {
        AuthorizationError preparedError = error(AUTHZ_FAILED, message = message, cause = err);
        return preparedError;
    }
    AuthorizationError preparedError = error(AUTHZ_FAILED, message = message);
    return preparedError;
}
