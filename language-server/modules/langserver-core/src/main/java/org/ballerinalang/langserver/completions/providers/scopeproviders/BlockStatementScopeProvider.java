/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.langserver.completions.providers.scopeproviders;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.providers.contextproviders.IfWhileConditionContextProvider;
import org.ballerinalang.langserver.completions.providers.contextproviders.InvocationArgsContextProvider;
import org.ballerinalang.langserver.completions.providers.contextproviders.StatementContextProvider;
import org.ballerinalang.langserver.completions.spi.LSCompletionProvider;
import org.eclipse.lsp4j.CompletionItem;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves all items that can appear within the block statement.
 */
@JavaSPIService("org.ballerinalang.langserver.completions.spi.LSCompletionProvider")
public class BlockStatementScopeProvider extends LSCompletionProvider {

    public BlockStatementScopeProvider() {
        this.attachmentPoints.add(BLangBlockStmt.class);
    }

    @Override
    public List<CompletionItem> getCompletions(LSContext context) {
        Optional<LSCompletionProvider> contextProvider = getContextProvider(context);
        return contextProvider.map(lsCompletionProvider -> lsCompletionProvider.getCompletions(context)).orElse(null);
    }

    @Override
    public Optional<LSCompletionProvider> getContextProvider(LSContext ctx) {
        List<CommonToken> lhsDefaultTokens = ctx.get(CompletionKeys.LHS_TOKENS_KEY).stream()
                .filter(commonToken -> commonToken.getChannel() == Token.DEFAULT_CHANNEL)
                .collect(Collectors.toList());
        if (ctx.get(CompletionKeys.IN_INVOCATION_PARAM_CONTEXT_KEY) != null
                && ctx.get(CompletionKeys.IN_INVOCATION_PARAM_CONTEXT_KEY)) {
            return Optional.ofNullable(this.getProvider(InvocationArgsContextProvider.class));
        }
        if (ctx.get(CompletionKeys.IN_CONDITION_CONTEXT_KEY) != null
                && ctx.get(CompletionKeys.IN_CONDITION_CONTEXT_KEY)) {
            return Optional.ofNullable(this.getProvider(IfWhileConditionContextProvider.class));
        }
        if (lhsDefaultTokens.isEmpty()) {
            // Within the block statement and no syntax error
            return Optional.ofNullable(this.getProvider(StatementContextProvider.class));
        }
        /*
        Statement context, since the assign token has been handled from the previous condition
         */
        return Optional.ofNullable(this.getProvider(StatementContextProvider.class));
    }
}
