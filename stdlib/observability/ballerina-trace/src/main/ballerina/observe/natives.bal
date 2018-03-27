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

package ballerina.observe;

import ballerina/net.http;

@Description {value:"Reference types between spans"}
@Field {value:"CHILDOF: The parent span depends on the child span in some capacity"}
@Field {value:"FOLLOWSFROM: The parent span do not depend on the result of the child span"}
@Field {value:"ROOT: Specify this as a root span that has no parent span"}
public enum ReferenceType {
    CHILDOF, FOLLOWSFROM, ROOT
}

@Description {value:"Represents a opentracing span in Ballerina"}
@Field {value:"spanId: The id of the span"}
@Field {value:"serviceName: The service name this span belongs to"}
@Field {value:"spanName: The name of the span"}
public struct Span {
    string spanId;
    string serviceName;
    string spanName;
}

@Description {value:"Starts a span and sets the specified reference to parentSpanId"}
@Param {value:"serviceName: The service name of the process"}
@Param {value:"spanName: The name of the span"}
@Param {value:"tags: The map of tags to be associated to the span"}
@Param {value:"reference: childOf, followsFrom, root"}
@Param {value:"parentSpanId: The Id of the parent span. If root reference, then parentSpanId will not be used"}
@Return {value:"The span struct"}
public function startSpan (string serviceName, string spanName, map tags, ReferenceType reference,
                           string parentSpanId) returns (Span) {
    Span span = {};
    span.spanId = init(serviceName, spanName, tags, reference, parentSpanId);
    span.serviceName = serviceName;
    span.spanName = spanName;
    return span;
}

@Description {value:"Builds a span and sets the specified reference to parentSpanId"}
@Param {value:"serviceName: The service name of the process"}
@Param {value:"spanName: The name of the span"}
@Param {value:"tags: The map of tags to be associated to the span"}
@Param {value:"reference: childOf, followsFrom"}
@Param {value:"parentSpanId: The Id of the parent span"}
@Return {value:"String value of the span id that was generated"}
native function init (string serviceName, string spanName, map tags, ReferenceType reference,
                      string parentSpanId) returns (string);

@Description {value:"Finish the span specified by the spanId"}
@Param {value:"spanId: The ID of the span to be finished"}
public native function <Span span> finishSpan ();

@Description {value:"Add a tag to the current span. Tags are given as a key value pair"}
@Param {value:"tagKey: The key of the key value pair"}
@Param {value:"tagValue: The value of the key value pair"}
public native function <Span span> addTag (string tagKey, string tagValue);

@Description {value:"Add a baggage item to the current span. Baggage items are given as a key value pair"}
@Param {value:"tagKey: The key of the key value pair"}
@Param {value:"tagValue: The value of the key value pair"}
public native function <Span span> setBaggageItem (string baggageKey, string baggageValue);

@Description {value:"Add a baggage item to the current span. Baggage items are given as a key value pair"}
@Param {value:"tagKey: The key of the key value pair"}
public native function <Span span> getBaggageItem (string baggageKey) returns (string);

@Description {value:"Attach an info log to the current span"}
@Param {value:"event: The type of event this log represents"}
@Param {value:"message: The message to be logged"}
public native function <Span span> log (string event, string message);

@Description {value:"Attach an error log to the current span"}
@Param {value:"errorKind: The kind of error. e.g. DBError"}
@Param {value:"message: The error message to be logged"}
public native function <Span span> logError (string errorKind, string message);

@Description {value:"Adds span headers when request chaining"}
@Return {value:"The span context as a key value pair that should be passed out to an external function"}
public native function <Span span> injectTraceContext (string traceGroup) returns (map);

@Description {value:"Creates a span context of a parent span propogated through request chaining"}
@Param {value:"headers: The map of headers"}
@Param {value:"traceGroup: The kind of error. e.g. DBError"}
@Return {value:""}
public native function extractTraceContext (map headers, string traceGroup) returns (string);

@Description {value:"Method to save the parent span and extract the span Id"}
@Param {value:"req: The http request that contains the header maps"}
@Param {value:"traceGroup: The group to which this span belongs to"}
@Return {value:"The id of the parent span passed in from an external function"}
public function extractTraceContextFromHttpHeader (http:Request req, string traceGroup) returns (string) {
    map headers = req.getCopyOfAllHeaders();
    return extractTraceContext(headers, traceGroup);
}

@Description {value:"Injects the span context the OutRequest struct to send to another service"}
@Param {value:"req: The http request used when calling an endpoint"}
@Param {value:"traceGroup: The group that the span context is associated to"}
@Return {value:"The http request which includes the span context related headers"}
public function <Span span> injectTraceContextToHttpHeader (http:Request req, string traceGroup) returns (http:Request) {
    map headers = span.injectTraceContext(traceGroup);
    foreach key, v in headers {
        var value = <string>v;
        req.addHeader(key, value);
    }
    return req;
}