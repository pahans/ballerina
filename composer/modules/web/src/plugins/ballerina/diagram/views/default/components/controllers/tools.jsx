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
 */

import React from 'react';
import ToolsOverlay from './tools-overlay';
import './tools.scss';

class Tools extends React.Component {
    constructor() {
        super();
        this.state = {
            isMenuActive: false,
            isMenuButtonActive: false,
        };
    }

    render() {
        const {bBox: {x, y, w, h}, children} = this.props;
        return (
            <g
                onMouseEnter={(e) => {
                    this.setState({
                        isMenuButtonActive: true,
                    });
                }}
                onMouseLeave={(e) => {
                    this.setState({
                        isMenuButtonActive: false,
                    });
                }}
            >
                <rect
                    x={x - (w / 2)}
                    y={y - (h / 2)}
                    width={w}
                    height={h}
                    rx='0'
                    ry='0'
                    className='button-group-area'
                />
                <g
                    visibility={this.state.isMenuButtonActive ? '' : 'hidden'}
                    onClick={() => {
                        this.setState({
                            isMenuActive: true,
                            isMenuButtonActive: false,
                        });
                    }}
                    onMouseEnter={(e) => {
                        e.stopPropagation();
                        this.setState({
                            isMenuButtonActive: true,
                        });
                    }}
                    onMouseLeave={(e) => {
                        e.stopPropagation();
                    }}
                    style={{ cursor: 'pointer' }}
                >
                    <rect
                        x={x - 15}
                        y={y - 15}
                        width={30}
                        height={30}
                        rx='0'
                        ry='0'
                        className='button-group'
                    >
                        {this.state.isMenuActive ?
                            <ToolsOverlay
                                bBox={{ x, y }}
                                hide={() => {
                                    this.setState({
                                        isMenuActive: false,
                                        isMenuButtonActive: false,
                                    });
                                }}
                            > {children} </ToolsOverlay> : ''}
                    </rect>
                    <text
                        x={x}
                        y={y}
                        className='button-icon'
                    >
                        +
                    </text>
                </g>
            </g>
        );
    }
}

export default Tools;
