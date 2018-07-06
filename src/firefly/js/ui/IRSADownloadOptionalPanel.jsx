import React from 'react';

import PropTypes from 'prop-types';

import {DownloadOptionsDialog, fileNameValidator, WORKSPACE, LOCALFILE} from './DownloadOptionsDialog.jsx';

import {getWorkspaceConfig,isWsFolder, isValidWSFolder,
   dispatchWorkspaceUpdate} from  '../visualize/WorkspaceCntlr.js';
import {workspacePopupMsg} from './WorkspaceViewer.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {get,set} from 'lodash';
import {updateSet} from '../util/WebUtil.js';
import {SimpleComponent} from './SimpleComponent.jsx';
import FieldGroupUtils,{getFieldVal,getGroupFields} from '../fieldGroup/FieldGroupUtils.js';
import {getBgEmailInfo} from '../core/background/BackgroundUtil.js';
import {dispatchBgSetEmailInfo} from '../core/background/BackgroundCntlr.js';
import Validate from '../util/Validate.js';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import {showDownloadDialog} from './DownloadDialog';
import {InputField} from './InputField.jsx';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';


import {FormPanel} from './FormPanel.jsx';

const DOWNLOAD_DIALOG_ID = 'Download Panel';

export class IRSADownloadOptionPanel extends SimpleComponent {

  getNextState() {
    const fields=FieldGroupUtils.getGroupFields(this.props.groupKey);
    const {email, enableEmail} = getBgEmailInfo();
    const currentFileLocation = getFieldVal(fields, 'fileLocation', LOCALFILE);
    return {fields, mask:false, email, enableEmail,currentFileLocation};
  }

  componentDidUpdate(oldState) {
    if (oldState.currentFileLocation!==WORKSPACE && this.state.currentFileLocation=== WORKSPACE) {
      dispatchWorkspaceUpdate();
    }
  }
  onEmailChanged(v) {
    if (get(v, 'valid')) {
      const {email} = this.state;
      if (email !== v.value) dispatchBgSetEmailInfo({email: v.value});
    }
  }

  render() {



     const { groupKey, children, rParams, submitRequest, title,  help_id, style}= this.props;
     const { mask, email, enableEmail} = this.state;
      const ttl = title || DOWNLOAD_DIALOG_ID;
      const toggleEnableEmail = (e) => {
          const enableEmail = e.target.checked;
          const email = enableEmail ? email : '';
          dispatchBgSetEmailInfo({email, enableEmail});
      };
      const isWs = getWorkspaceConfig();
      const onSubmit = (options) => {

          submitRequest(options);
          showDownloadDialog(this, false);
      };

    return (
        <div style = {Object.assign({margin: '4px', position: 'relative', minWidth: 350}, style)}>
            {mask && <div style={{width: '100%', height: '100%'}} className='loading-mask'/>}
        <FormPanel
            submitText = 'Prepare Download'
            groupKey = {groupKey}
            onSubmit = {onSubmit}
            onCancel = {() => dispatchHideDialog(ttl)}
            onError = { resultsFail()}
            help_id  = {help_id}
         >

          <FieldGroup groupKey={groupKey} keepState={true}
                      reducerFunc= { DLReducer(rParams, groupKey)} >



            <DownloadOptionsDialog fromGroupKey={groupKey}
                                   workspace={isWs}
                                   dialogWidth={'100%'}
                                   dialogHeight={'calc(100% - 200pt)'}
                                   children={children}
            />
              <div style={{width: 250, marginTop: 10}}>
                  <input type='checkbox' checked={enableEmail} onChange={toggleEnableEmail}/>Also send me email with URLs to download
              </div>
              {showEmail(enableEmail, email,this.onEmailChanged.bind(this))}
          </FieldGroup>
        </FormPanel>
        </div>
    );
  }
}


IRSADownloadOptionPanel.propTypes = {
        groupKey:   PropTypes.string,
        onSubmit:  PropTypes.func,
        tbl_id:     PropTypes.string,
        cutoutSize: PropTypes.string,
        help_id:    PropTypes.string,
        title:      PropTypes.string
};

IRSADownloadOptionPanel.defaultProps= {
    groupKey: 'IRSADownloadOptionPanel'
};

const showEmail = (enabled, email, onChange)=>{
    if(enabled) {
        return (
            <InputField
                validator={Validate.validateEmail.bind(null, 'an email field')}
                tooltip='Enter an email to be notified when a process completes.'
                label='Email:'
                labelStyle={{display: 'inline-block', marginLeft: 18, width: 32, fontWeight: 'bold'}}
                value={email}
                placeholder='Enter an email to get notification'
                style={{width: 170}}
                onChange={onChange}
                actOn={['blur','enter']}
            />
        );
    }
    else{
        return (<div></div>);
    }

};

function resultsFail() {
    return (request) => {
        const {wsSelect, fileLocation} = request;

        if (fileLocation === WORKSPACE) {
            if (!wsSelect) {
                workspacePopupMsg('please select a workspace folder', 'Save to workspace');
            } else {
                const isAFolder = isValidWSFolder(wsSelect);
                if (!isAFolder.valid) {
                    workspacePopupMsg(isAFolder.message, 'Save to workspace');
                }
            }
        }
    };
}


function DLReducer(rParams, groupKey) {
    const {fKeyDef, defValues} = rParams;
    return (inFields, action) => {

        if (!inFields) {
            const defV = Object.assign({}, defValues);
            set(defV, [fKeyDef.wsSelect.fKey, 'value'], '');
            set(defV, [fKeyDef.wsSelect.fKey, 'validator'], isWsFolder());
            set(defV, [fKeyDef.fileName.fKey, 'validator'], fileNameValidator(groupKey));
            return defV;
        } else {
            if (action.type===FieldGroupCntlr.VALUE_CHANGE) {

                if (action.payload.fieldKey === fKeyDef.wsSelect.fKey) {
                    // change the filename if a file is selected from the file picker
                    const val = action.payload.value;

                    if (val && isValidWSFolder(val, false).valid) {
                        const fName = val.substring(val.lastIndexOf('/') + 1);

                        inFields = updateSet(inFields, [fKeyDef.fileName.fKey, 'value'], fName);
                    }
                }

            }
            return Object.assign({}, inFields);
        }
    };
}

