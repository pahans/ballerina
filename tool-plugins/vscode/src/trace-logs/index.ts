/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
import { window, Uri, ViewColumn, ExtensionContext, WebviewPanel } from 'vscode';
import * as path from 'path';
import { ExtendedLangClient } from '../lang-client';
import { render } from './renderer';

let traceLogsPanel: WebviewPanel | undefined;
let traces: Object[] = [];

export function activate(context: ExtensionContext, langClient: ExtendedLangClient) {
    
	const resourcePath = Uri.file(path.join(context.extensionPath, 'resources', 'diagram'));
    const resourceRoot = resourcePath.with({ scheme: 'vscode-resource' });

    if (traceLogsPanel) {
        traceLogsPanel.reveal(ViewColumn.Two, true);
        return;
    }

    render(context, langClient, resourceRoot)
    .then((html) => {
        if (traceLogsPanel && html) {
            traceLogsPanel.webview.html = html;
        }
    });
    // Create and show a new webview
    traceLogsPanel = window.createWebviewPanel(
        'ballerinaTraceLogs',
        "Ballerina Trace logs",
        { viewColumn: ViewColumn.Two, preserveFocus: true } ,
        {
            enableScripts: true,
            retainContextWhenHidden: true,
        }
    );
    langClient.onNotification('window/traceLogs', (trace: Object) => {
        if (traceLogsPanel && traceLogsPanel.webview) {
            traces.push(trace);
            traceLogsPanel.webview.postMessage({
                traces: traces,
                command: 'update',
            });
        }
    });
}


