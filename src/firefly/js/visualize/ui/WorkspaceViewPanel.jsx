/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Button} from '@mui/joy';
import React, {PureComponent} from 'react';
import {flux} from '../../core/ReduxFlux.js';
import {WorkspaceViewField}  from '../../ui/WorkspaceViewer.jsx';
import {initWorkspace, getWorkspaceList, isExistWorkspaceList} from '../WorkspaceCntlr.js';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {workspacePopupMsg} from '../../ui/WorkspaceViewer.jsx';

const workspacePopup = 'workspacePopup';

function displayWorkspacePopup() {
    const newList = getWorkspaceList(true);
    const aroundButton = {margin: 5};
    const popupId = 'workspaceId';

    const startWorkspacePopup = () => {
        const popup = (
            <PopupPanel title={'workspace list'}>
                <FieldGroup groupKey={workspacePopup} keepState={true}>
                    <div style={{width: 500, height: 600, margin: 10, overflow: 'auto', border: '1px solid #a3aeb9'}}>
                        <WorkspaceViewField fieldKey={'files'}
                                            files={newList}
                                            wrapperStyle={{width: 'calc(100% - 10px)', height: 'calc(100% - 10px)', marginTop: 15}}/>
                    </div>
                </FieldGroup>
                <div style={{display: 'flex', margin: '30px 10px 10px 10px', justifyContent:'space-between'}} >
                    <div style={aroundButton}>
                        <CompleteButton onSuccess={onSuccess(popupId)} />
                    </div>
                    <div style={aroundButton}>
                        <Button onClick={() => dispatchHideDialog(popupId)}>
                            Cancel
                        </Button>
                    </div>
                </div>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(popupId, popup);
        dispatchShowDialog(popupId);
    };

    if (!isExistWorkspaceList()) {
        workspacePopupMsg('Workspace access error', 'Workspace access');
    } else {
        startWorkspacePopup();
    }
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
                state = Object.assign({}, state);
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
                state = Object.assign({}, state);
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
                isExistWorkspaceList() ?
                (<div style={{border: '1px solid #a3aeb9', height: 500, overflow: 'auto', padding: 10}} >
                    <FieldGroup groupKey={workspacePopup} keepState={true}>
                        <WorkspaceViewField
                            files={wsList}
                            canCreateFolder={true}
                            canCreateFiles={true}
                            canDeleteFile={true}
                            canRenameFile={true}
                            canMoveFile={true}
                            wrapperStyle={{width: 'calc(100%-10px)'}} />
                    </FieldGroup>
                </div>) : workspacePopupMsg('Workspace access error', 'Workspace access')
            );
        };

        return (
            <div style={{display: 'flex', flexDirection:'column',
                            width: 500}}>
                <Button style={buttonMargin} onClick={initWorkspace}>
                    Init Workspace
                </Button>

                <div style={{display: 'flex'}}>
                    <Button style={buttonMargin} onClick={this.onClickStatus(WS_list)}>
                        Show Workspace below
                    </Button>
                    <Button style={buttonMargin} onClick={displayWorkspacePopup}>
                        Show Workspace Popup
                    </Button>
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
