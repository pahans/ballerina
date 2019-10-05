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

package org.ballerinalang.stdlib.crypto.nativeimpl;

import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.stdlib.crypto.Constants;
import org.ballerinalang.stdlib.crypto.CryptoUtils;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Extern function ballerina.crypto:encryptRsaEcb.
 *
 * @since 0.990.4
 */
@BallerinaFunction(orgName = "ballerina", packageName = "crypto", functionName = "encryptRsaEcb", isPublic = true)
public class EncryptRsaEcb {

    public static Object encryptRsaEcb(Strand strand, ArrayValue inputValue, Object keyUnion, Object padding) {
        byte[] input = inputValue.getBytes();
        // this is a union, but both a record types, so we can safely cast
        // TODO: unify union types when same type is duplicated eg:record:record.
        MapValue<?, ?> keyMap = (MapValue<?, ?>) keyUnion;
        Key key;
        if (keyMap.getNativeData(Constants.NATIVE_DATA_PRIVATE_KEY) != null) {
            key = (PrivateKey) keyMap.getNativeData(Constants.NATIVE_DATA_PRIVATE_KEY);
        } else if (keyMap.getNativeData(Constants.NATIVE_DATA_PUBLIC_KEY) != null) {
            key = (PublicKey) keyMap.getNativeData(Constants.NATIVE_DATA_PUBLIC_KEY);
        } else {
            return CryptoUtils.createError("Uninitialized private/public key");
        }
        return CryptoUtils.rsaEncryptDecrypt(CryptoUtils.CipherMode.ENCRYPT, Constants.ECB, padding.toString(), key,
                                             input, null, -1);
    }
}
