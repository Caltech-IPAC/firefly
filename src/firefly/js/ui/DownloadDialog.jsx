/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useState} from 'react';
import PropTypes from 'prop-types';
import {cloneDeep, get} from 'lodash';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import Validate from '../util/Validate.js';
import {ValidationField} from './ValidationField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import {getActiveTableId, getTblById, hasRowAccess, getProprietaryInfo} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {dispatchPackage} from '../core/background/BackgroundCntlr.js';
import {WS_HOME} from '../visualize/WorkspaceCntlr.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {useStoreConnector} from './SimpleComponent.jsx';
import {BgMaskPanel} from '../core/background/BgMaskPanel.jsx';
import {WsSaveOptions} from './WorkspaceSelectPane.jsx';
import {NotBlank} from '../util/Validate.js';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {Stack, Typography, Box} from '@mui/joy';
import {ToolbarButton} from 'firefly/ui/ToolbarButton';

const DOWNLOAD_DIALOG_ID = 'Download Options';
const OptionsContext = React.createContext();
const emailNotif = 'enableEmailNotification';
const emailKey = 'Email';          // should match server DownloadRequest.EMAIL

/**
 * This download button does 2 things:
 * 1. track a table for selected rows, then changes style to reflect whether or not it can be clicked
 * 2. when clicked, display a popup dialog of the given download options panel
 *    or a message prompting the user to select rows first
 *
 * table can be specified via the tbl_id props or it will take the active table id from tbl_grp props.
 * tbl_grp is default to 'main' if not specified.
 *
 * download options panel is passed into the DownloadButton via its only child component.
 * if one is not given, DownloadOptionPanel is used.
 * DownloadOptionPanel also allow children elements as well.  The children will be rendered on top of
 * the common fields.  This allow customization of the download dialog.
 * Below is an example of how to use it.
 *
 * <code>
 *    <DownloadButton>
 *        <DownloadOptionPanel
 *            cutoutSize = '200'
 *            dlParams = {{
 *                     MaxBundleSize: 1024*1024*1024,
 *                     FilePrefix: 'WISE_Files',
 *                     BaseFileName: 'WISE_Files',
 *                     DataSource: 'WISE images',
 *                     PreTitleMessage:'Message to appear above Title field'
 *                     FileGroupProcessor: 'LightCurveFileGroupsProcessor'
 *                 }}>
 *             <ValidationField
 *                 initialState= {{
 *                         value: 'A sample download',
 *                         label : 'Title for this download:'
 *                     }}
 *                 fieldKey='Title'
 *         </DownloadOptionPanel>
 *     </DownloadButton>
 * </code>
 * IRSA-3944: SOFIA required a message to guide the user before downloading AOR such as:
 * 'All levels associated with this AOR will be downloaded.'
 * PreTitleMessage can be added to the dlParams from the parent and passed here (FIREFLY-723)
 * @param props
 * @param props.tbl_id
 * @param props.tbl_grp
 * @param props.children
 * @param props.checkSelectedRow
 * @param props.makeButton
 * @returns {*}
 */
export function DownloadButton({tbl_id:inTblId , tbl_grp, children, checkSelectedRow=true, makeButton}) {

    const tblIdGetter = () => inTblId || getActiveTableId(tbl_grp);
    const selectInfoGetter = () => get(getTblById(tblIdGetter()), 'selectInfo');

    const tbl_id = useStoreConnector(tblIdGetter);
    const selectInfo = useStoreConnector(selectInfoGetter);
    const selectInfoCls = SelectInfo.newInstance(selectInfo);

    const onClick = useCallback(() => {
        if (selectInfoCls.getSelectedCount() || checkSelectedRow===false) {
            if(hasOnlyProprietaryData(getTblById(tbl_id))){
                showInfoPopup('You do not have permission to download the selected data set(s).', 'Private Data Selected');
            }else if(!hasOnlyProprietaryData()){
                showDownloadDialog((
                    <OptionsContext.Provider value={{tbl_id, checkSelectedRow}}>
                        {children ? React.Children.only(children) : <DownloadOptionPanel/>}
                    </OptionsContext.Provider>
                ));
            }
        } else {
            showInfoPopup('You have not chosen any data to download', 'No Data Selected');
        }
    }, [selectInfo]);

    const isRowSelected = selectInfoCls.getSelectedCount()>0;

    const defButton=
        <ToolbarButton variant={isRowSelected?'solid':'soft'} color='warning' onClick={() =>onClick()} text='Prepare Download'/>;

    return (
        makeButton?.(onClick,tbl_id,isRowSelected) ?? defButton
    );
}


DownloadButton.propTypes = {
    tbl_id      : PropTypes.string,
    tbl_grp     : PropTypes.string,
    checkSelectedRow:PropTypes.bool
};

let dlTitleIdx = 0;
const newBgKey = () => 'DownloadOptionPanel-' + Date.now();

// TODO: showEmailNotify changed to default of false, when we make a notification plan we can change it back.

export function DownloadOptionPanel ({groupKey='DownloadDialog', cutoutSize, help_id, children, style, title, dlParams,
                                         updateSearchRequest, updateDownloadRequest, validateOnSubmit,
                                         cancelText='Cancel', showZipStructure=true, showEmailNotify=false,
                                         showFileLocation=true, showTitle=true, ...props}) {
    const {tbl_id:p_tbl_id, checkSelectedRow} = React.useContext(OptionsContext);
    const tbl_id = props.tbl_id || p_tbl_id;

    const [bgKey, setBgKey] = useState(newBgKey());

    const onSubmit = useCallback((formInputs={}) => {

        const {request,  selectInfo} = getTblById(tbl_id);
        const {FileGroupProcessor} = dlParams;

        const selectInfoCls = SelectInfo.newInstance(selectInfo);
        if (checkSelectedRow && !selectInfoCls.getSelectedCount()) {
            return showInfoPopup('You have not chosen any data to download', 'No Data Selected');
        }

        const {valid, message} = validateOnSubmit?.(formInputs) ?? {valid : true};
        if (!valid) {
            showInfoPopup(message ?? 'Invalid form input(s)', 'Error in form inputs');
            return false; // to prevent FormPanel to submit
        }

        formInputs.wsSelect = formInputs.wsSelect && formInputs.wsSelect.replace(WS_HOME, '');
        //make a download request
        let dlRequest = makeTblRequest(FileGroupProcessor, formInputs.Title, Object.assign(dlParams, {cutoutSize}, formInputs));

        if (!dlParams[emailNotif]) Reflect.deleteProperty(dlRequest, emailKey);
        Reflect.deleteProperty(dlRequest, emailNotif);

        //make a search request
        let searchRequest = cloneDeep(request);

        /*If a calling application has its own parameters to be added, deleted or updated, those parameters
          can be provided by using getOverrideRequestParams function.
         */

        const allParams =  Object.assign(dlParams, formInputs);

        if (updateSearchRequest){ //update search request
            searchRequest = updateSearchRequest(tbl_id, allParams, searchRequest);
        }
        if (updateDownloadRequest){ //update download request
            dlRequest = updateDownloadRequest(tbl_id, allParams, dlRequest);
        }

        const akey = newBgKey();
        dispatchPackage(dlRequest, searchRequest, SelectInfo.newInstance(selectInfo).toString(), akey);
        dlTitleIdx++;
        setBgKey(akey);
    }, [cutoutSize, dlParams, tbl_id]);

    // const showWarnings = hasProprietaryData(getTblById(tbl_id)); // it feature is not working correctly

    const maskPanel = (<BgMaskPanel key={bgKey} componentKey={bgKey}
                                   onMaskComplete={() =>hideDownloadDialog()}/>);

    const saveAsProps = {
        initialState: {
            value: get(dlParams, 'BaseFileName')
        }
    };
    const dlTitle = get(dlParams, 'TitlePrefix', 'Download') + '-' + dlTitleIdx;
    const preTitleMessage = dlParams?.PreTitleMessage ?? '';
    return (
        <Stack sx ={{m:1/2, position: 'relative', minWidth:400, height:'auto', ...style}}>
            <FormPanel
                groupKey = {groupKey}
                onSuccess= {onSubmit}
                onCancel= {() => dispatchHideDialog(DOWNLOAD_DIALOG_ID)}
                completeText='Prepare Download'
                cancelText={cancelText}
                help_id  = {help_id}>

                <FieldGroup groupKey={groupKey} keepState={true}>
                    {preTitleMessage && (<Typography sx={{p:1,mb:1,whiteSpace: 'wrap'}}>
                            {preTitleMessage}
                        </Typography>
                    )}
                    <Stack spacing={1}>
                        {showTitle && <TitleField {...{value:dlTitle}}/>}

                        {children}

                        {cutoutSize         && <DownloadCutout />}
                        {showZipStructure   && <ZipStructure />}
                        {showFileLocation   && <WsSaveOptions {...{groupKey, labelWidth:110, saveAsProps}}/>}
                        {showEmailNotify    && <EmailNotification {...{groupKey}}/>}
                    </Stack>
                </FieldGroup>
            </FormPanel>
            {maskPanel}
        </Stack>
    );
}

DownloadOptionPanel.propTypes = {
    groupKey:   PropTypes.string,
    tbl_id:     PropTypes.string,
    cutoutSize: PropTypes.string,
    help_id:    PropTypes.string,
    title:      PropTypes.string,           // title of the dialog, appears at top of the dialog
    style:      PropTypes.object,

    showTitle:        PropTypes.bool,           // layout Title field.  This is the title of the package request.  It will be displayed in background monitor.
    showZipStructure: PropTypes.bool,           // layout ZipStructure field
    showEmailNotify:  PropTypes.bool,           // layout EmailNotification field
    showFileLocation: PropTypes.bool,           // layout FileLocation field
    updateSearchRequest: PropTypes.func,   // customized parameters to be added or updated in request
    updateDownloadRequest:PropTypes.func,
    validateOnSubmit: PropTypes.func,      // to validate form inputs on submit
    dlParams:   PropTypes.shape({               // these params should be used as defaults value if they appears as input fields
        TitlePrefix:    PropTypes.string,           // default title of the download..  an index number will be appended to this.
        FilePrefix:     PropTypes.string,           // packaged file prefix
        BaseFileName:   PropTypes.string,           // zip file name
        DataSource:     PropTypes.string,
        MaxBundleSize:  PropTypes.number,
        FileGroupProcessor: PropTypes.string.isRequired,
    })
};

export function TitleField({style={}, value, label='Title:', size=30}) {

    return (
        <ValidationField
            forceReinit={true}
            fieldKey='Title'
            tooltip='Enter a description to identify this download.'
            {...{validator:NotBlank, initialState:{value}, label, size, style}}
        />
    );
}

export function ZipStructure({fieldKey='zipType'}) {
    return (
        <ListBoxInputField
            fieldKey = {fieldKey}
            initialState = {{
                tooltip: 'Zip File Structure',
                label : 'Zip File Structure:'
            }}
            options = {[
                {label: 'Structured (with folders)', value: 'folder'},
                {label: 'Flattened (no folders)', value: 'flat'}
            ]}
        />

    );
}

export function DownloadCutout({fieldKey='dlCutouts'}) {
    return (
        <ListBoxInputField
            fieldKey = {fieldKey}
            initialState = {{
                tooltip: 'Download Cutouts Option',
                label : 'Download:'
            }}
            options = {[
                {label: 'Specified Cutouts', value: 'cut'},
                {label: 'Original Images', value: 'orig'}
            ]}
        />
    );
}
export function EmailNotification({style, groupKey}) {
    const enableEmail = useStoreConnector(() => getFieldVal(groupKey, emailNotif));

    return (
        <Box sx={{...style}} spacing={1}>
            <Stack width={250} mt={2}>
                <CheckboxGroupInputField fieldKey={emailNotif}
                                     initialState= {{value: ''}}
                                     options={[{label:'Enable email notification', value: 'true'}]}/>
            </Stack>
            {enableEmail &&
                <ValidationField
                    fieldKey={emailKey}
                    validator={Validate.validateEmail.bind(null, 'an email field')}
                    tooltip='Enter an email to be notified when a process completes.'
                    label='Email:'
                    labelStyle={{marginLeft: 18, fontWeight: 'bold'}}
                    placeholder='Enter an email to get notification'
                    style={{width: 170}}
                    actOn={['blur', 'enter']}
                />
            }
        </Box>
    );
}

function hasProprietaryData(tableModel={}) {

    if (getProprietaryInfo(tableModel).length === 0) return false;

    const {selectInfo} = tableModel;
    const selectInfoCls = SelectInfo.newInstance(selectInfo);

    const selectedRows = [...selectInfoCls.getSelected()];
    for (let i = 0; i < selectedRows.length; i++) {
        if (!hasRowAccess(tableModel, selectedRows[i])) {
            return true;
        }
    }
    return false;
}


function hasOnlyProprietaryData(tableModel={}){
    if (getProprietaryInfo(tableModel).length === 0) return false;

    const {selectInfo} = tableModel;
    const selectInfoCls = SelectInfo.newInstance(selectInfo);

    const selectedRows = [...selectInfoCls.getSelected()];
    for (let i = 0; i < selectedRows.length; i++) {
        if (hasRowAccess(tableModel, selectedRows[i])) {
            return false;
        }
    }
    return true;
}



/**
 * creates and show the DownloadDialog.
 * @param {Component}  panel  the panel to show in the popup.
 */
function showDownloadDialog(panel) {
    const title = get(panel, 'props.title', DOWNLOAD_DIALOG_ID);
    const content= (
        <PopupPanel title={title} >
            {panel}
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DOWNLOAD_DIALOG_ID, content);
    dispatchShowDialog(DOWNLOAD_DIALOG_ID);
}

const hideDownloadDialog= () => dispatchHideDialog(DOWNLOAD_DIALOG_ID);
