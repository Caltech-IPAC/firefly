/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {flux} from '../../Firefly.js';
import {onCreateFolder, onCreateFiles, onRenameFile, onRenameFolder,  onMoveFile, onMoveFolder,
        onDeleteFile, onDeleteFolder,  WorkspaceViewField}  from '../../ui/WorkspaceViewer.jsx';
import {initWorkspace, getWorkspaceList} from '../WorkspaceCntlr.js';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';

const workspacePopup = 'workspacePopup';

function displayWorkspacePopup() {
    const newList = getWorkspaceList();
    const aroundButton = {margin: 5};
    const popupId = 'workspaceId';

    const startWorkspacePopup = () => {
        const popup = (
            <PopupPanel title={'workspace list'}>
                <FieldGroup groupKey={workspacePopup} keepState={true}>
                    <div style={{width: 500, height: 600, margin: 10, overflow: 'auto', border: '1px solid #a3aeb9'}}>
                        <WorkspaceViewField fieldKey={'files'}
                                            files={newList}
                                            wrapperStyle={{width: 'calc(100% - 10px)', height: 'calc(100%-10px)', marginTop: 15}}/>
                    </div>
                </FieldGroup>
                <div style={{display: 'flex', margin: '30px 10px 10px 10px', justifyContent:'space-between'}} >
                    <div style={aroundButton}>
                        <button type='button' className='button std hl'
                                onClick={() => {dispatchHideDialog(popupId);}}>Cancel
                        </button>
                    </div>
                    <div style={aroundButton}>
                        <CompleteButton
                            groupKey={workspacePopup}
                            onSuccess={onSuccess(popupId)}
                            text={'ok'} />
                    </div>
                </div>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(popupId, popup);
        dispatchShowDialog(popupId);
    };

    startWorkspacePopup();
}

function onSuccess(popupId) {
    return (request) => {
        if (popupId) {
            dispatchHideDialog(popupId);
        }
    };
}

const WS_list = 1;
const WS_reset = 0;

export class WorkspaceViewPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {clickStatus: WS_reset, wsList: getWorkspaceList()};
        this.onClickStatus = this.onClickStatus.bind(this);
    }

    onClickStatus(status) {
        return () => {
            this.setState((state) => {
                state.clickStatus = (state.clickStatus === status) ? WS_reset : status;
                return state;
            });
        };
    }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const wsList = getWorkspaceList();

            this.setState((state) => {
                if (wsList !== state.wsList) {
                    state.wsList = wsList;
                }
                return state;
            });
        }
    }

    render() {
        const buttonMargin = {margin: 10, width: 160};
        const {wsList} = this.state;

        const displayList = (wsList) => {
            return (
                <div style={{border: '1px solid #a3aeb9', height: 300, overflow: 'auto', padding: 10}} >
                    <FieldGroup groupKey={workspacePopup} keepState={true}>
                        <WorkspaceViewField
                            files={wsList}
                            onCreateFolder={onCreateFolder()}
                            onCreateFiles={onCreateFiles}
                            onDeleteFile={onDeleteFile()}
                            wrapperStyle={{width: 'calc(100%-10px)'}} />
                    </FieldGroup>
                </div>
            );
        };

        return (
            <div style={{display: 'flex', flexDirection:'column',
                            width: 500}}>
                <button style={buttonMargin}
                        type='button'
                        className='button std hl'
                        onClick={initWorkspace}>
                    Init Workspace
                </button>

                <div style={{display: 'flex'}}>
                    <button style={buttonMargin}
                            type = 'button'
                            className='button std hl'
                            onClick={this.onClickStatus(WS_list)}>
                        Show Workspace below
                    </button>
                    <button style={buttonMargin}
                            type='button'
                            className='button std hl'
                            onClick={displayWorkspacePopup}>
                        Show Workspace Popup
                    </button>
                </div>
                <div>
                    {(this.state.clickStatus === WS_list) && displayList(wsList)}
                </div>
              </div>
        );
    }

}


export function resultSuccess() {
    return (request) =>
    {
        return false;
    };

}

export function resultFail() {
    return (request) => {

    };
}