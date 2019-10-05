/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.table;

import org.ballerinalang.jvm.TypeChecker;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.TableValue;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

/**
 * This class represents the implementation of creating a new table from an existing in memory table. The records
 * which are fetched according to the given sqlQuery is inserted to the new in memory table.
 */
@BallerinaFunction(orgName = "ballerina", packageName = "lang.table",
        functionName = "queryTableWithJoinClause",
        args = {
                @Argument(name = "sqlQuery",
                        type = TypeKind.STRING),
                @Argument(name = "fromTable",
                        type = TypeKind.TABLE),
                @Argument(name = "joinTable",
                        type = TypeKind.TABLE),
                @Argument(name = "parameters",
                        type = TypeKind.ARRAY),
                @Argument(name = "retType",
                        type = TypeKind.ANY)
        },
        returnType = {@ReturnType(type = TypeKind.TABLE)})
public class QueryTableWithJoinClause {

    public static TableValue queryTableWithJoinClause(Strand strand, String query, TableValue fromTable,
                                                      TableValue joinTable, Object array,
                                                      Object tableLiteral) {
        return new TableValue(query, fromTable, joinTable,
                              (org.ballerinalang.jvm.types.BStructureType) TypeChecker.getType(tableLiteral),
                              (ArrayValue) array);
    }
}
