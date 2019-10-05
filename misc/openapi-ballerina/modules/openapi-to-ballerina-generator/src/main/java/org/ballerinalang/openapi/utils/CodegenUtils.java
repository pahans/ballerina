/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.openapi.utils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Utilities used by ballerina openapi code generator.
 */
public class CodegenUtils {

    /**
     * Is {@code path} is a valid ballerina project directory.
     *
     * @param path path to suspecting dir
     * @return if {@code path} is a ballerina project directory or not
     */
    public static boolean isBallerinaProject(Path path) {
        boolean isProject = false;
        Path cachePath = path.resolve("Ballerina.toml");

        // .ballerina cache path should exist in ballerina project directory
        if (Files.exists(cachePath)) {
            isProject = true;
        }

        return isProject;
    }

    /**
     * Resolves path to write generated main source files.
     *
     * @param pkg  module
     * @param path output path without module name
     * @return path to write generated source files
     */
    public static Path getSourcePath(String pkg, String path) {
        return (pkg == null || pkg.isEmpty()) ?
                Paths.get(path).resolve("src") :
                Paths.get(path).resolve("src").resolve(Paths.get(pkg));
    }

    /**
     * Resolves path to write generated implementation source files.
     *
     * @param pkg     module
     * @param srcPath resolved path for main source files
     * @return path to write generated source files
     */
    public static Path getImplPath(String pkg, Path srcPath) {
        return (pkg == null || pkg.isEmpty()) ? srcPath : srcPath.getParent();
    }

    /**
     * Writes a file with content to specified {@code filePath}.
     *
     * @param filePath valid file path to write the content
     * @param content  content of the file
     * @throws IOException when a file operation fails
     */
    public static void writeFile(Path filePath, String content) throws IOException {
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(filePath.toString(), "UTF-8");
            writer.print(content);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Removes underscores and hyphens for identifiers.
     *
     * @param identifier Path
     * @return Cleaned identifier.
     */
    public static String normalizeForBIdentifier(@Nullable String identifier) {
        if (identifier == null) {
            return null;
        }

        String resourceName = identifier;
        if (identifier.split("-").length > 0) {
            String[] spllitedIdentifier = identifier.split("-");
            resourceName = spllitedIdentifier[spllitedIdentifier.length - 1];
        }

        return resourceName.replaceAll(" ", "_").replaceAll("-", "")
                .replaceAll("/", "");
    }

    /**
     * Generate operation ID using pattern "resource[number]".
     *
     * @param openAPI open api definition for the openapi
     * @return {@link String}operation ID which match to the "resource[number]"
     */
    public static String generateOperationId(OpenAPI openAPI) {
        int prevNumber = 0;
        for (Map.Entry<String, PathItem> path : openAPI.getPaths().entrySet()) {
            for (Operation operation : path.getValue().readOperations()) {
                String operationId = operation.getOperationId();
                if (operationId != null && operationId.matches("resource\\d+")) {
                    String[] numbers = operationId.split("resource");
                    if (numbers.length > 0) {
                        int number = Integer.parseInt(numbers[numbers.length - 1]);
                        if (prevNumber < number) {
                            prevNumber = number;
                        }
                    }
                }
            }
        }
        return "resource" + (prevNumber + 1);
    }
}
