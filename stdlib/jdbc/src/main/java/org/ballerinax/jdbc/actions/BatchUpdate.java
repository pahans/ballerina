/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.jdbc.actions;

import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinax.jdbc.Constants;
import org.ballerinax.jdbc.datasource.SQLDatasource;
import org.ballerinax.jdbc.statement.BatchUpdateStatement;
import org.ballerinax.jdbc.statement.SQLStatement;

/**
 * {@code BatchUpdate} is the Batch update remote function implementation of the JDBC client.
 *
 * @since 0.8.6
 */
@BallerinaFunction(
        orgName = "ballerinax", packageName = "java.jdbc",
        functionName = "nativeBatchUpdate"
)
public class BatchUpdate {

    public static MapValue<String, Object> nativeBatchUpdate(Strand strand, ObjectValue client, String sqlQuery,
                                                             boolean rollbackAllInFailure, ArrayValue parameters) {
        SQLDatasource datasource = (SQLDatasource) client.getNativeData(Constants.JDBC_CLIENT);
        SQLStatement batchUpdateStatement = new BatchUpdateStatement(client, datasource, sqlQuery, parameters,
                rollbackAllInFailure, strand);
        return (MapValue<String, Object>) batchUpdateStatement.execute();
    }

    private BatchUpdate() {

    }
}
