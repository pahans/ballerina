const fs = require('fs');
const path = require('path');
const { expect } = require('chai');
const { spawn } = require('child_process');
var WebSocket = require('ws');
var http = require("http");
const kill  = require('tree-kill');
const findProcess = require('find-process');

const testEnv = require('../../environment');

global.targetPath = path.join(__dirname, '..', '..', '..', '..', '..', 'target');
const debugManagerPath = path.join(__dirname, '..', '..', '..', '..', '..', 'target');

const targetPath = path.join(`${global.targetPath}`, '/ballerina-dist/');
const balDir = fs.readdirSync(targetPath).filter(fn => fn.includes('ballerina'));

let executable = path.join(targetPath, balDir[0], 'bin', 'ballerina');
if (process.platform === 'win32') {
    executable += '.bat';
}

const testFilesDir = path.join(__dirname, 'resources/');

let backEndProcess;

describe('Ballerina Composer Debugger Test Suite', () => {

    beforeEach(function () {
        findProcess('ballerina')
        backEndProcess && terminate(backEndProcess.pid);
    });

    afterEach(function () {
        backEndProcess && terminate(backEndProcess.pid);
    });

    it('Hello world Main function debug hit', function (done) {
        backEndProcess = spawn(`${executable}`, [`run`, `${testFilesDir}/hello_world.bal`, '--debug', '5006']);
        backEndProcess.stderr.pipe(process.stderr);
        let stdIndata = '';
        backEndProcess.stdout.on('data', (data) => {
            stdIndata += data;
            if (data.includes("Ballerina remote debugger is activated on port : 5006") > 0) {
                var websocket = new WebSocket("ws://127.0.0.1:5006/debug");
                websocket.onopen = function (event) {
                    websocket.send(JSON.stringify({
                        command: "SET_POINTS", points: [
                            { fileName: "hello_world.bal", lineNumber: 4, packagePath: "." }
                        ]
                    }));
                    websocket.send(JSON.stringify({ command: "START" }));
                }
                websocket.onmessage = function (event) {
                    const data = JSON.parse(event.data);
                    if (data.code === 'DEBUG_HIT') {
                        expect(data.location).to.deep.equal({
                            "fileName": "hello_world.bal",
                            "lineNumber": 4,
                            "packagePath": ".",
                        });
                        done();
                        backEndProcess.kill();
                        backEndProcess.stdin.pause();
                    }
                }
                websocket.onerror = () => {};
                websocket.onclose = () => {};
            }
        });
    });

    it('Hello world service debug hit', function (done) {
        const filepath = path.join(testFilesDir, 'hello_world_service.bal');
        backEndProcess = spawn(`${executable}`, [`run`, filepath, '--debug', '5006']);
        backEndProcess.stderr.pipe(process.stderr);
        let stdIndata = '';
        backEndProcess.stdout.on('data', (data) => {
            stdIndata += data;
            console.log(stdIndata);
            if (stdIndata.includes('ballerina: started HTTP/WS endpoint') > 0) {
                const req = http.get('http://127.0.0.1:9090/hello/sayHello', function(res) {
                    // console.log(res.statusCode)
                });
                req.end();
            }
            if (stdIndata.includes("Ballerina remote debugger is activated on port : 5006") > 0) {
                var websocket = new WebSocket("ws://127.0.0.1:5006/debug");
                websocket.onopen = function (event) {
                    websocket.send(JSON.stringify({
                        command: "SET_POINTS", points: [
                            { fileName: "hello_world_service.bal", lineNumber: 9, packagePath: "." }
                        ]
                    }));
                    websocket.send(JSON.stringify({ command: "START" }));
                }
                websocket.onmessage = function (event) {
                    const data = JSON.parse(event.data);
                    console.log(data.code);
                    if (data.code === 'DEBUG_HIT') {
                        expect(data.location).to.deep.equal({
                            "fileName": "hello_world_service.bal",
                            "lineNumber": 9,
                            "packagePath": ".",
                        });
                        done();
                        backEndProcess.kill();
                    }
                }
                websocket.onerror = () => {};
                websocket.onclose = () => {};

            }
        });
    });
});