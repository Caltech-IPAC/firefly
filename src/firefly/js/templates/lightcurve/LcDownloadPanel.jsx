
import React, {PureComponent} from 'react';
import {set,  cloneDeep} from 'lodash';

import {LC, } from './LcManager.js';

import { DownloadButton} from '../../ui/DownloadDialog.jsx';
import {DL_DATA_TAG} from './LcConverterFactory.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import { getTypeData,WORKSPACE,validateFileName} from '../../ui/DownloadOptionsDialog.jsx';
import {getTblInfoById} from '../../tables/TableUtil.js';
import {DataTagMeta} from '../../tables/TableRequestUtil.js';
import {makeTblRequest} from '../../tables/TableRequestUtil.js';
import {dispatchPackage} from '../../core/background/BackgroundCntlr.js';
import { IRSADownloadOptionPanel} from '../../ui/IRSADownloadOptionalPanel.jsx';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {WS_SERVER_PARAM,getWorkspacePath} from  '../../visualize/WorkspaceCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';

const currentTime =  (new Date()).toLocaleString('en-US', { hour12: false });

export const LcDownloadPanel = (mission, cutoutSize) => {
    const fKeyDef = {
        fileName: {fKey: 'fileName', label: mission+':'},
        location: {fKey: 'fileLocation', label: 'File Location:'},
        wsSelect: {fKey: 'wsSelect', label: ''},
        overWritable: {fKey: 'fileOverwritable', label: 'File overwritable: '}
    };

    const labelWidth = 110;
    const defValues = {
        [fKeyDef.fileName.fKey]: Object.assign(getTypeData(fKeyDef.fileName.fKey, `${mission}_Files: ${currentTime}`,
            'Please enter a filename, a default name will be used if it is blank', fKeyDef.fileName.label, labelWidth), {validator: null}),
        [fKeyDef.location.fKey]: Object.assign(getTypeData(fKeyDef.location.fKey, 'isLocal',
            'select the location where the file is downloaded to', fKeyDef.location.label, labelWidth), {validator: null}),
        [fKeyDef.wsSelect.fKey]: Object.assign(getTypeData(fKeyDef.wsSelect.fKey, '',
            'workspace file system', fKeyDef.wsSelect.label, labelWidth), {validator: null}),
        [fKeyDef.overWritable.fKey]: Object.assign(getTypeData(fKeyDef.overWritable.fKey, '0',
            'File is overwritable', fKeyDef.overWritable.label, labelWidth), {validator: null})
    };

    const rParams = {fKeyDef, defValues};
    const dlParams = getParamters(mission);

    const onSearchSubmit = (options) => {
        var {request, selectInfo} = getTblInfoById(LC.RAW_TABLE);
        const {fileLocation, wsSelect, fileName} = options || {};
        const isWorkspace = () => (fileLocation && fileLocation === WORKSPACE);
        const {FileGroupProcessor} = dlParams;
        /*If the default file name is used, the default file name is mission, some mission such as WISE/NEWWISE,
          there is '/' in the name, it needs to be removed
         */
        const fName = fileName?fileName.replace('/', '_').split(':')[0]:'';
        const dreq = makeTblRequest(FileGroupProcessor, fName, Object.assign(dlParams, {cutoutSize}, options));
        dreq.Title = fName;
        request = set(cloneDeep(request), DataTagMeta, DL_DATA_TAG);
        if (isWorkspace()){
            const fName = isWorkspace()?fileName.replace('/', '_').split(':')[0]:fileName;
            const zipFileName = fName.replace('/', '_').split(':')[0] + '.zip';
            if (!validateFileName(wsSelect, zipFileName) ) return false;
            const params = {
                wsCmd: ServerParams.WS_PUT_IMAGE_FILE,
                [WS_SERVER_PARAM.currentrelpath.key]:getWorkspacePath(wsSelect, zipFileName),
                [WS_SERVER_PARAM.newpath.key]: zipFileName,
                [ServerParams.COMMAND]: ServerParams.WS_PUT_IMAGE_FILE,
                [WS_SERVER_PARAM.should_overwrite.key]: true};
            dispatchPackage(dreq, request, SelectInfo.newInstance(selectInfo).toString(), true, params);
        }
        else{
            dispatchPackage(dreq, request, SelectInfo.newInstance(selectInfo).toString());
        }

    };


    const children = (<div>
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
                labelWidth = {110}
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
                labelWidth = {labelWidth}
            />

        </div>
    );


    return (

        <DownloadButton>
            <IRSADownloadOptionPanel
                groupKey = {mission}
                children = {children}
                submitRequest = {(options)=>onSearchSubmit(options)}
                title = {mission + ' Image Download Options'}
                rParams={rParams}
            />
        </DownloadButton>

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
