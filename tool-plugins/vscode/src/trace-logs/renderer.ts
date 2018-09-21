import { ExtendedLangClient } from '../lang-client';
import { Uri, ExtensionContext } from 'vscode';
import { getLibraryWebViewContent } from '../utils';

export function render (context: ExtensionContext, langClient: ExtendedLangClient, resourceRoot: Uri)
        : Thenable<string | undefined> {    
            return Promise.resolve(renderTraces(context, langClient, resourceRoot));
}

function renderTraces(context: ExtensionContext, langClient: ExtendedLangClient, resourceRoot: Uri) {
    const script = `
    function drawTraces() {
        try {
            ballerinaDiagram.renderTracingUI(document.getElementById("traces"), []);
            console.log('Successfully rendered tracing');
        } catch(e) {
            console.log(e.stack);
            drawError('Oops. Something went wrong.');
        }
        window.addEventListener('message', event => {
            const message = event.data; // The JSON data our extension sent
            switch (message.command) {
                case 'update':
                    try {
                        ballerinaDiagram.renderTracingUI(document.getElementById("traces"), message.traces);
                        console.log('Successfully rendered tracing');
                    } catch(e) {
                        console.log(e.stack);
                        drawError('Oops. Something went wrong.');
                    }
                    break;
            }
        });
    }
    
    drawTraces();
    `;
    const body = `<body>
        <div id="traces"></div>
    </body>`;

    const styles = ``;
    return getLibraryWebViewContent(context, body, script, styles);
}