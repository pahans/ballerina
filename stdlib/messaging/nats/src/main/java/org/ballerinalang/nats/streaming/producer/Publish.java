/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.nats.streaming.producer;

import io.nats.streaming.StreamingConnection;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.ballerinalang.nats.Utils.convertDataIntoByteArray;

/**
 * Remote function implementation for publishing a message to a NATS streaming server.
 */
@BallerinaFunction(orgName = "ballerina",
                   packageName = "nats",
                   functionName = "externPublish",
                   receiver = @Receiver(type = TypeKind.OBJECT,
                                        structType = "StreamingProducer",
                                        structPackage = "ballerina/nats"),
                   isPublic = true)
public class Publish {

    public static Object externPublish(Strand strand, ObjectValue publisher, String subject, Object data) {
        StreamingConnection streamingConnection = (StreamingConnection) publisher
                .getNativeData(Constants.NATS_STREAMING_CONNECTION);
        byte[] byteData = convertDataIntoByteArray(data);
        try {
            NonBlockingCallback nonBlockingCallback = new NonBlockingCallback(strand);
            AckListener ackListener = new AckListener(nonBlockingCallback);
            streamingConnection.publish(subject, byteData, ackListener);
            return null;
        } catch (InterruptedException e) {
            return Utils.createNatsError("Failed to publish due to an internal error");
        } catch (IOException | TimeoutException e) {
            return Utils.createNatsError(e.getMessage());
        }
    }
}
