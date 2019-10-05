/**
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import * as Swagger from "openapi3-ts";
import * as React from "react";
import { Accordion, AccordionTitleProps, Button, Label } from "semantic-ui-react";

import { ExpandMode, OpenApiContext, OpenApiContextConsumer } from "../components/context/open-api-context";

import AddOpenApiOperation from "../operations/add-operation";
import OpenApiOperation from "../operations/operations-list";

import InlineEdit from "../components/utils/inline-edit/inline-edit";

interface OpenApiPathProps {
    paths: Swagger.PathsObject;
    expandMode: ExpandMode;
    onTitleExpannd: (state: boolean) => void;
}

interface OpenApiPathState {
    activeIndex: number[];
    showAddOperation: number[];
}

class OpenApiPathList extends React.Component<OpenApiPathProps, OpenApiPathState> {
    constructor(props: OpenApiPathProps) {
        super(props);

        this.state = {
            activeIndex: [],
            showAddOperation: [],
        };

        this.onAccordionTitleClick = this.onAccordionTitleClick.bind(this);
        this.onAddOperationClick = this.onAddOperationClick.bind(this);
    }

    public componentWillReceiveProps(nextProps: OpenApiPathProps) {
        const { paths, expandMode } = nextProps;
        let activePaths: number[] = this.state.activeIndex;
        let hideForm: number[] = [];

        if (expandMode.isEdit) {
            return;
        }

        if (expandMode.type === "collapse") {
            hideForm = [];
            activePaths = [];
        }

        if (expandMode.type === "operations" || expandMode.type === "resources") {
            Object.keys(paths).sort().map((openApiResource, index) => {
                if (!activePaths.includes(index)) {
                    activePaths.push(index);
                }
            });
        }

        this.setState({
            activeIndex: activePaths,
            showAddOperation: hideForm
        });
    }

    public render() {
        const { paths } = this.props;
        const { activeIndex, showAddOperation } = this.state;

        return (
            <OpenApiContextConsumer>
                {(context: OpenApiContext | null) => {
                    return (
                        <Accordion fluid styled>
                            {Object.keys(paths).sort().map((openApiResource, index) => {
                                return(
                                    <React.Fragment key={openApiResource}>
                                        <Accordion.Title
                                            active={activeIndex.includes(index)}
                                            index={index}
                                            onClick={this.onAccordionTitleClick}>
                                            <OpenApiContextConsumer>
                                                {(appContext: OpenApiContext) => {
                                                    return (
                                                        <InlineEdit
                                                            changeModel={appContext.openApiJson}
                                                            changeAttribute={{
                                                                changeValue: openApiResource,
                                                                key: "resource.name",
                                                            }}
                                                            editableObject={openApiResource}
                                                            onValueChange={appContext.onInlineValueChange}
                                                        />
                                                    );
                                                }}
                                            </OpenApiContextConsumer>
                                            <div className="path-op-container">
                                                {Object.keys(paths[openApiResource]).map((op, opIndex) => {
                                                    return (
                                                        <Label basic className={op} >{op}</Label>
                                                    );
                                                })}
                                            </div>
                                            {activeIndex.includes(index) &&
                                                <Button
                                                    title="Add operation to resource."
                                                    size="mini"
                                                    compact
                                                    className="add-operation-action"
                                                    circular
                                                    floated="right"
                                                    onClick={(e: React.MouseEvent) => {
                                                        e.stopPropagation();
                                                        this.onAddOperationClick(index);
                                                    }}
                                                >
                                                    {!showAddOperation.includes(index) ?
                                                        <i className={"fw fw-add"}></i>
                                                    :
                                                        <i className={"fw fw-close"}></i>
                                                    }
                                                </Button>
                                            }
                                        </Accordion.Title>
                                        <Accordion.Content
                                            active={activeIndex.includes(index)}>
                                            {showAddOperation.includes(index) &&
                                                <AddOpenApiOperation
                                                    openApiJson={context!.openApiJson}
                                                    onAddOperation={context!.onAddOpenApiOperation}
                                                    resourcePath={openApiResource}
                                                    handleOnClose={this.onAddOperationClick}
                                                    operationIndex={index}
                                                />
                                            }
                                            <OpenApiOperation
                                                expandMode={context!.expandMode}
                                                path={openApiResource}
                                                pathItem={paths[openApiResource]}
                                            />
                                        </Accordion.Content>
                                    </React.Fragment>
                                );
                            })}
                        </Accordion>
                    );
                }}
            </OpenApiContextConsumer>
        );
    }

    private onAddOperationClick(id: number) {
        if (this.state.showAddOperation.includes(id)) {
            this.setState({
                showAddOperation: this.state.showAddOperation.filter((index) => {
                    return index !== id;
                })
            });
        } else {
            this.setState({
                showAddOperation: [...this.state.showAddOperation, id],
            });
        }
    }

    private onAccordionTitleClick(e: React.MouseEvent, titleProps: AccordionTitleProps) {
        const { index } = titleProps;
        const { activeIndex } = this.state;

        this.setState({
            activeIndex: !activeIndex.includes(Number(index)) ?
                [...this.state.activeIndex, Number(index)] : this.state.activeIndex.filter((i) => i !== index),
            showAddOperation: activeIndex.includes(Number(index)) ?
                this.state.showAddOperation.filter((i) => i !== index) : this.state.showAddOperation
        }, () => {
            this.props.onTitleExpannd(true);
        });
    }
}

export default OpenApiPathList;
