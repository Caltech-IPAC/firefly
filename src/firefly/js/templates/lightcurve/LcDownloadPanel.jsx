
import React, {useCallback} from 'react';
import PropTypes from 'prop-types';
import {set, cloneDeep, get} from 'lodash';
import {LC } from './LcManager.js';
import { DownloadButton} from '../../ui/DownloadDialog.jsx';
import {DL_DATA_TAG} from './LcConverterFactory.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import { getTypeData,WORKSPACE,DownloadOptionsDialog,fileNameValidator} from '../../ui/DownloadOptionsDialog.jsx';
import {getTblInfoById} from '../../tables/TableUtil.js';
import {DataTagMeta, makeTblRequest} from '../../tables/TableRequestUtil.js';
import {dispatchBgSetEmailInfo} from '../../core/background/BackgroundCntlr.js';
import {getWorkspaceConfig,isWsFolder, isValidWSFolder} from  '../../visualize/WorkspaceCntlr.js';
import {workspacePopupMsg} from '../../ui/WorkspaceViewer.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {updateSet,doDownload} from '../../util/WebUtil.js';
import {getBgEmailInfo,bgDownload} from '../../core/background/BackgroundUtil.js';
import Validate from '../../util/Validate.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {InputField} from '../../ui/InputField.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {showDownloadDialog} from '../../ui/DownloadDialog.jsx';
import {getGroupFields} from '../../fieldGroup/FieldGroupUtils.js';
import {LcXYPlotCompKey} from './LcResult';


const currentTime =  (new Date()).toLocaleString('en-US', { hour12: false });
const DOWNLOAD_DIALOG_ID = 'Image Download Options';


export function LcDownloadOptionPanel (props) {
        const {mask,  mission, help_id, style, cutoutSize, title} = props;//this.props; //
        const [{email, enableEmail}] = useStoreConnector(getBgEmailInfo);
        const onEmailChanged = useCallback((v) => {
            if (get(v, 'valid')) {
                if (email !== v.value) dispatchBgSetEmailInfo({email: v.value});
            }
        }, [email]);
        const ttl = title || DOWNLOAD_DIALOG_ID;
        const gKey = mission || DOWNLOAD_DIALOG_ID;

        const dlParams = getParamters(mission);
        const toggleEnableEmail = (e) => {
            const enableEmail = e.target.checked;
            const email = enableEmail ? email : '';
            dispatchBgSetEmailInfo({email, enableEmail});
        };
        const isWs = getWorkspaceConfig();
        const labelWidth = 110;
        const fKeyDef = {

            fileName: {fKey: 'fileName', label: 'Save As:'},
            location: {fKey: 'fileLocation', label: 'File Location:'},
            wsSelect: {fKey: 'wsSelect', label: ''},
            overWritable: {fKey: 'fileOverwritable', label: 'File overwritable: '}

        };
        const defValues = {
            [fKeyDef.fileName.fKey]: Object.assign(getTypeData(fKeyDef.fileName.fKey, `${mission.replace('/', '_')}_Files`,
                'Please enter a filename, a default name will be used if it is blank', fKeyDef.fileName.label, labelWidth), {validator: null}),
            [fKeyDef.location.fKey]: Object.assign(getTypeData(fKeyDef.location.fKey, 'isLocal',
                'select the location where the file is downloaded to', fKeyDef.location.label, labelWidth), {validator: null}),
            [fKeyDef.wsSelect.fKey]: Object.assign(getTypeData(fKeyDef.wsSelect.fKey, '',
                'workspace file system', fKeyDef.wsSelect.label, labelWidth), {validator: null}),
            [fKeyDef.overWritable.fKey]: Object.assign(getTypeData(fKeyDef.overWritable.fKey, '0',
                'File is overwritable', fKeyDef.overWritable.label, labelWidth), {validator: null})
        };

        const rParams = {fKeyDef, defValues};

        const onSubmit = useCallback((options) => {
            let {request, selectInfo} = getTblInfoById(LC.RAW_TABLE);
            const {fileLocation, wsSelect, fileName} = options || {};

            const isWorkspace = (fileLocation && fileLocation === WORKSPACE) ? true : false;
            const {FileGroupProcessor} = dlParams;
            const Title = dlParams.Title || options.Title;
            const dreq = makeTblRequest(FileGroupProcessor, Title, Object.assign(dlParams, {cutoutSize}, options, {Email:email}, {wsSelect:wsSelect}));

            request = set(cloneDeep(request), DataTagMeta, DL_DATA_TAG);


            const onComplete = (bgStatus) => {
                doDownload(bgStatus, isWorkspace, wsSelect, fileName);
            };

            bgDownload({dlRequest: dreq, searchRequest: request, selectInfo}, {key: LcXYPlotCompKey, onComplete, isWs:isWorkspace});
            showDownloadDialog(this,ttl, false);
        });

        return (
            <div style={Object.assign({margin: '4px', position: 'relative'}, style)}>
                {mask && <div style={{width: '100%', height: '100%'}} className='loading-mask'/>}
                <DownloadButton
                    defaultPanel={this}>

                    <FormPanel
                        submitText='Save'
                        groupKey={gKey}
                        onSubmit={onSubmit}
                        onCancel={() => dispatchHideDialog(ttl)}
                        onError={resultsFail()}
                        help_id={help_id}
                        title={ttl}
                      >

                        <FieldGroup groupKey={gKey} keepState={true}
                                    reducerFunc={DLReducer(rParams, mission)}>
                            <DownloadOptionsDialog fromGroupKey={gKey}
                                                   workspace={isWs}
                                                   dialogWidth={'100%'}
                                                   dialogHeight={'calc(100% - 200pt)'}
                                                   children={children(cutoutSize, labelWidth, mission)}

                            />
                            <div style={{width: 250, marginTop: 10}}>
                                <input type='checkbox' checked={enableEmail} onChange={toggleEnableEmail}/>Also send me
                                email with URLs to download
                            </div>
                            {showEmail(enableEmail, email,onEmailChanged)}
                        </FieldGroup>
                    </FormPanel>

                </DownloadButton>
            </div>
        );

    }
//}

LcDownloadOptionPanel.propTypes = {
    cutoutSize: PropTypes.string,
    help_id:    PropTypes.string,
    title:      PropTypes.string,
    mask:       PropTypes.bool,
    style:      PropTypes.object

};



const children = (cutoutSize, labelWidth, mission) => {
    return  (<div>
            <ValidationField
                style={{width: 223}}
                initialState={{
                    value: `${mission}_Files: ${currentTime}`,
                    label: `${mission}:`
                }}
                fieldKey='Title'
                labelWidth={labelWidth}
            />
            {cutoutSize &&
            <ListBoxInputField
                wrapperStyle={{marginTop: 5}}
                fieldKey ='dlCutouts'
                initialState = {{
                    tooltip: 'Download Cutouts Option',
                    label : 'Download:'
                }}
                options = {[
                    {label: 'Specified Cutouts', value: 'cut'},
                    {label: 'Original Images', value: 'orig'}
                ]}
                labelWidth = {labelWidth}
            />
            }
            <ListBoxInputField
                wrapperStyle={{marginTop: 5}}
                fieldKey ='zipType'
                initialState = {{
                    tooltip: 'Zip File Structure',
                    label : 'Zip File Structure:'
                }}
                options = {[
                    {label: 'Structured (with folders)', value: 'folder'},
                    {label: 'Flattened (no folders)', value: 'flat'}
                ]}
                cutoutSize = {cutoutSize}
                labelWidth = {labelWidth}
            />

        </div>
    );
};


function getParamters(mission){
    switch (mission.toLowerCase()){
        case 'ptf':
            return {
                MaxBundleSize: 200 * 1024 * 1024,    // set it to 200mb to make it easier to test multi-parts download.  each ptf image is ~33mb
                FilePrefix: `${mission}_Files`,
                BaseFileName: `${mission}_Files`,
                DataSource: `${mission} images`,
                FileGroupProcessor: 'PtfLcDownload',
                ProductLevel:'l1',
                schema:'images',
                table:'level1'
            };
            break;
        case 'ztf' :
            return  {
                MaxBundleSize: 200 * 1024 * 1024,    // set it to 200mb to make it easier to test multi-parts download.  each ztf image is ~37mb
                FilePrefix: `${mission}_Files`,
                BaseFileName: `${mission}_Files`,
                DataSource: `${mission} images`,
                FileGroupProcessor: 'ZtfLcDownload',
                ProductLevel:'sci',
                schema:'products',
                table:'sci'};
            break;
        default:
            return  {
                MaxBundleSize: 200 * 1024 * 1024,    // set it to 200mb to make it easier to test multi-parts download.  each wise image is ~64mb
                FilePrefix: `${mission}_Files`,
                BaseFileName: `${mission}_Files`,
                DataSource: `${mission} images`,
                FileGroupProcessor: 'LightCurveFileGroupsProcessor'
            };
    };


}

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

