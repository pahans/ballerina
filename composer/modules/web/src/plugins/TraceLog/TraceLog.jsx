/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the ''License''); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * ''AS IS'' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import Proptypes from 'prop-types';
import _ from 'lodash';
import moment from 'moment';
import { Grid, Icon } from 'semantic-ui-react';
import SplitPane from 'react-split-pane';
import ErrorBoundary from 'core/editor/views/ErrorBoundary';
import ToolBar from './Toolbar';
import DetailView from './DetailView';
import './index.scss';

const directionToIcon = {
    INBOUND: {
        'http.tracelog.downstream': 'fw fw-downstream-inbound direction-icon inbound downstream',
        'http.tracelog.upstream': 'fw fw-upstream-inbound direction-icon inbound upstream',
    },
    OUTBOUND: {
        'http.tracelog.downstream': 'fw fw-downstream-outbound direction-icon outbound downstream',
        'http.tracelog.upstream': 'fw fw-upstream-outbound direction-icon outbound upstream',
    },
};

/**
 *
 * @extends React.Component
 */
class TraceLog extends React.Component {

    /**
     * Creates an instance of TraceLog.
     * @memberof TraceLog
     */
    constructor(props) {
        super(props);
        this.state = {
            messages: props.traces,
            filteredMessages: props.traces,
            details: false,
        };
        this.onFilteredMessages = this.onFilteredMessages.bind(this);
        this.toggleDetails = this.toggleDetails.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            messages: nextProps.traces,
            filteredMessages: nextProps.traces,
            details: false,
        });
    }

    onFilteredMessages(filteredMessages) {
        this.setState({
            filteredMessages: this.mergeRelatedMessages(filteredMessages),
        });
    }

    getDirectionIcon(logger, direction) {
        directionToIcon[direction] = directionToIcon[direction] || {};
        return directionToIcon[direction][logger];
    }

    mergeRelatedMessages(messages) {
        const newMessages = [];
        for (let index = 0; index < messages.length; index++) {
            let record1 = messages[index];
            let record2 = messages[index + 1];
            if (record1.message.headerType.startsWith('DefaultHttpRequest')
                && record2
                && record1.rawMessage.thread === record2.rawMessage.thread
                && (record2.message.headerType.startsWith('DefaultLastHttpContent')
                    || record2.message.headerType.startsWith('EmptyLastHttpContent'))) {

                record1.message.payload = record2.message.payload;
                newMessages.push(record1);
            } else if (record1.message.headerType.startsWith('DefaultLastHttpContent') ||
            record1.message.headerType.startsWith('EmptyLastHttpContent')) {
                // do nothing
            } else {
                newMessages.push(record1);
            }
        }
        return newMessages;
    }

    toggleDetails(message) {
        this.setState({
            details: message,
        });
    }

    /**
     * @inheritdoc
     */
    render() {
        const { height } = this.props;
        const { details } = this.state;
        return (
            <div id='logs-console' ref={(stickyContext) => { this.stickyContext = stickyContext; }}>
                <ErrorBoundary>
                    <div>
                        <ToolBar
                            messages={this.state.messages}
                            filters={{
                                'message.id': 'Activity Id',
                                'rawMessage.logger': 'Logger',
                                'message.path': 'Path',
                                'message.direction': 'Inbound/Outbound',
                                'message.httpMethod': 'Method',
                            }}
                            onFilteredMessages={this.onFilteredMessages}
                            clearLogs={() => {
                                this.setState({
                                    messages: [],
                                    filteredMessages: [],
                                });
                            }}
                        />
                        {
                            this.state.messages.length > 0 &&
                            <div >
                                <SplitPane
                                    split='vertical'
                                    size={details ? 450 : '100%'}
                                    allowResize={details ? true : false}
                                >
                                    <div>
                                        <Grid style={{ margin: 0 }}>
                                            <Grid.Row className='table-heading'>
                                                <Grid.Column className='summary'>
                                                    &nbsp;
                                                </Grid.Column>
                                                <Grid.Column className='activity'>
                                                    Activity Id
                                                </Grid.Column>
                                                <Grid.Column className='time'>
                                                    Time
                                                </Grid.Column>
                                                <Grid.Column className='path'>
                                                    Path
                                                </Grid.Column>
                                            </Grid.Row>
                                        </Grid>
                                        <Grid
                                            className='table-content'
                                        >
                                            {this.state.filteredMessages.map((record) => {
                                                const timeString = moment(parseInt(record.millis)).format('HH:mm:ss.SSS');
                                                return (
                                                    <Grid.Row
                                                        className={(details && details.id === record.id) ? 'active clickable' : 'clickable'}
                                                        onClick={() => this.toggleDetails(record)}
                                                    >
                                                        <Grid.Column
                                                            className='wrap-text summary'
                                                        >
                                                            <Icon
                                                                name={this.getDirectionIcon(record.logger,
                                                                    record.direction)}
                                                                title={record.direction}
                                                            />
                                                        </Grid.Column>
                                                        <Grid.Column className='wrap-text activity'>
                                                            {record.message.id}
                                                        </Grid.Column>
                                                        <Grid.Column className='wrap-text time'>
                                                            {timeString}
                                                        </Grid.Column>
                                                        <Grid.Column className='wrap-text path'>
                                                            {record.message.httpMethod}
                                                            &nbsp;
                                                            {record.message.path}
                                                        </Grid.Column>
                                                    </Grid.Row>
                                                );
                                            })}
                                        </Grid>
                                    </div>
                                    {details &&
                                        <div width={12} style={{ height, overflow: 'auto' }}>
                                            <DetailView
                                                rawLog={details.rawMessage}
                                                meta={details.message}
                                                hideDetailView={() => { this.setState({ details: null }); }}
                                            />
                                        </div>
                                    }
                                </SplitPane>
                            </div>
                        }
                    </div>
                </ErrorBoundary>
            </div>
        );
    }
}

TraceLog.propTypes = {

};

TraceLog.defaultProps = {

};

export default TraceLog;
