/*
 * License information at https://github.com/CaltechIPAC/firefly/blob/master/License.txt
 */
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import Enum from 'enum';
import {get} from 'lodash';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {FileUpload} from './FileUpload.jsx';
import {WorkspaceUpload} from './WorkspaceViewer.jsx';
import {isAccessWorkspace} from '../visualize/WorkspaceCntlr.js';

export const LOCALFILE = 'isLocal';

const ULOptionsKey = new Enum(['local', 'workspace', 'url', 'location']);


export class UploadOptionsDialog extends PureComponent {
    constructor(props) {
        super(props);

        this.workspace = get(props, 'workspace', false);
        //fieldKey for fileLocation, fileUpload, and workspaceUpload fields
        this.fileLocation = get(props, ['fieldKeys', ULOptionsKey.location.key], ULOptionsKey.location.key );
        this.fileUpload   = get(props, ['fieldKeys', ULOptionsKey.local.key], ULOptionsKey.local.key );
        this.workspaceUpload = get(props, ['fieldKeys', ULOptionsKey.workspace.key], ULOptionsKey.workspace.key );

        const where = getFieldVal(props.fromGroupKey, this.fileLocation, LOCALFILE);
        this.state = {where, isLoading: isAccessWorkspace()};
    }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {

            const isLoading = isAccessWorkspace();
            const loc = this.workspace && getFieldVal(this.props.fromGroupKey, this.fileLocation, LOCALFILE);

            this.setState((state) => {
                if (loc && loc !== state.where) {
                    state = Object.assign({}, state, {where: loc});
                }

                if (isLoading !== state.isLoading) {
                    state = Object.assign({}, state, {isLoading});
                }
                return state;
            });
        }
    }

    render() {
        const {where, isLoading} = this.state;
        const {dialogWidth, preloadWsFile=true, style={}} = this.props;


        const showUploadLocation = () => {
            const options = [ {id: 0, label: 'Local File', value: 'isLocal'},
                              {id: 1, label: 'Workspace',  value: 'isWs'}];

            return (
                <div style={{margin: '5px 10px 2px 10px'}}>
                    <RadioGroupInputField
                        orientation='horizontal'
                        fieldKey={this.fileLocation}
                        options={options}
                        initialState={
                           {tooltip: get(this.props, ['tooltips', ULOptionsKey.location.key], 'Select where the file is from'),
                            }
                        }
                    />
                </div>
            );
        };
        
        const showFileUploadButton = () => {
            return (where === 'isLocal') ?
                (
                    <FileUpload
                        sx={{margin: (this.workspace ? '2px 10px 8px 10px' : '15px 10px 21px 10px')}}
                        fieldKey={this.fileUpload}
                        fileNameStyle={{marginLeft: 14}}
                        initialState={
                             {tooltip: get(this.props, ['tooltips', ULOptionsKey.local.key], 'Select a file to upload')}}
                    />
                ) :
                (
                    <WorkspaceUpload
                        wrapperStyle={{margin: '2px 10px 8px 10px'}}
                        preloadWsFile={preloadWsFile}
                        fieldKey={this.workspaceUpload}
                        isLoading={isLoading}
                        initialState={
                            {tooltip: get(this.props, ['tooltips', ULOptionsKey.workspace.key],
                                                       'Select a file from workspace to upload')}}
                    />
                );
        };

        
        return (
            <div style={{width: dialogWidth, ...style}} >
                {this.workspace && showUploadLocation()}
                {showFileUploadButton()}
            </div>
        );
    }
}

UploadOptionsDialog.propTypes = {
    fromGroupKey: PropTypes.string.isRequired,
    dialogWidth: PropTypes.number,
    fieldKeys: PropTypes.object,
    tooltips: PropTypes.object,
    preloadWsFile: PropTypes.bool,
    workspace: PropTypes.oneOfType([PropTypes.bool, PropTypes.string]),
    style: PropTypes.object
};
