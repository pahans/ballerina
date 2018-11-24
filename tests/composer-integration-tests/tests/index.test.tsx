import * as path from 'path';
import { sync as globSync } from 'glob';
import { startBallerinaLangServer, MinimalLangClient, balToolsPath, BallerinaAST } from './server';
import URI from 'vscode-uri';
import * as React from 'react';
import { BallerinaDiagramWrapper } from '@ballerina/diagram';
import SizingUtil from '@ballerina/diagram/lib/plugins/ballerina/diagram/views/default/sizing-util';
import EditableText from '@ballerina/diagram/lib/plugins/ballerina/diagram/views/default/components/decorators/editable-text';
import DiagramMenu from '@ballerina/diagram/lib/plugins/ballerina/views/diagram-menu';
import { create } from 'react-test-renderer';

let langClient : MinimalLangClient;

const bbeDir = path.join(balToolsPath, 'examples');

beforeAll((done) => {
    startBallerinaLangServer()
    .then((languageClient) => {
        if (languageClient) {
            langClient = languageClient;
        } else {
            console.log('Could not start LS properly');
        }
        done();
    }, (reason) => {
        console.log('Could not start LS properly');
        console.log(reason);
        done();
    });
});

test('Lang-server is started properly', () => {
    expect(langClient.initializedResult.capabilities.experimental.astProvider).toBe(true);
});

function testDiagramRendering(ast: BallerinaAST,  uri: string) {
    const parseFragment = jest.fn();
    const goToSource = jest.fn();
    const onChange = jest.fn();
    const getEndpoints = jest.fn();

    const getAST = jest.fn();
    getAST.mockResolvedValue({ ast });

    const getTextWidth = jest.fn();
    getTextWidth.mockReturnValue(100);
    jest
        .spyOn(SizingUtil.prototype, 'getTextWidth')
        .mockImplementation(getTextWidth);

    const consoleLog = jest.fn();
    jest
        .spyOn(console, 'log')
        .mockImplementation(consoleLog);
    
    const renderEditableTextBox = jest.fn();
    jest
        .spyOn(EditableText.prototype, 'renderTextBox')
        .mockImplementation(renderEditableTextBox);
        
    const renderDiagramMenu = jest.fn();
    renderDiagramMenu.mockReturnValue(<div />);
    jest
        .spyOn(DiagramMenu.prototype, 'render')
        .mockImplementation(renderDiagramMenu);

    const setStateSpy = jest.spyOn(BallerinaDiagramWrapper.prototype,
                            'setState');

    const component = create(
        <BallerinaDiagramWrapper 
            docUri={uri}
            getAST={getAST}
            onChange={onChange}
            width={1000}
            height={1000}
            getEndpoints={getEndpoints}
            parseFragment={parseFragment}
            goToSource={goToSource}
        />
    );
    component.root.instance.updateAST(ast);
    let tree = component.toJSON();
    expect(tree).toMatchSnapshot();
    expect(getAST).toHaveBeenCalled();
    expect(setStateSpy).toHaveBeenCalled();
}

var bbeFiles = globSync(path.join(bbeDir, '**', '*.bal'), {});
bbeFiles.forEach((file) => {
    const uri = URI.file(file).toString();
    test('Parsing and Rendering BBE: ' + path.basename(file), (done) => {
        langClient.getAST({
            documentIdentifier: {
                uri 
            }
        }).then((resp) => {
            expect(resp.parseSuccess).toBeTruthy();
            expect(resp.ast).toBeDefined();
            testDiagramRendering(resp.ast, uri);
            done();
        }, (reason) => {
            console.log('Error: ' + reason);
            done();
        });
    });
});

afterAll(() => {
    if (langClient) {
        langClient.kill();
    }
});