/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useState} from 'react';
import PropTypes from 'prop-types';
import {set, cloneDeep, get} from 'lodash';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import Validate from '../util/Validate.js';
import {InputField} from './InputField.jsx';
import {ValidationField} from './ValidationField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import {getActiveTableId, getTblById, hasRowAccess, getProprietaryInfo} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {dispatchPackage, dispatchBgSetEmailInfo} from '../core/background/BackgroundCntlr.js';
import {WS_HOME} from '../visualize/WorkspaceCntlr.js';
import {getBgEmailInfo} from '../core/background/BackgroundUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {DataTagMeta} from '../tables/TableRequestUtil.js';
import {useStoreConnector} from './SimpleComponent.jsx';
import {BgMaskPanel} from '../core/background/BgMaskPanel.jsx';
import {WsSaveOptions} from './WorkspaceSelectPane.jsx';
import {NotBlank} from '../util/Validate.js';

const DOWNLOAD_DIALOG_ID = 'Download Options';
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
 *                     FileGroupProcessor: 'LightCurveFileGroupsProcessor'
 *                 }}>
 *             <ValidationField
 *                 initialState= {{
 *                         value: 'A sample download',
 *                         label : 'Title for this download:'
 *                     }}
 *                 fieldKey='Title'
 *                 labelWidth={110}/>
 *         </DownloadOptionPanel>
 *     </DownloadButton>
 * </code>
 * @param props
 * @returns {*}
 */
export function DownloadButton(props) {

    const tblIdGetter = () => props.tbl_id || getActiveTableId(props.tbl_grp);
    const selectInfoGetter = () => get(getTblById(tblIdGetter()), 'selectInfo');

    const [tbl_id, selectInfo] = useStoreConnector(tblIdGetter, selectInfoGetter);
    const selectInfoCls = SelectInfo.newInstance(selectInfo);

    const onClick = useCallback(() => {
        if (selectInfoCls.getSelectedCount()) {
            if(hasOnlyProprietaryData(getTblById(tbl_id))){
                showInfoPopup('You do not have permission to download the selected data set(s).', 'Private Data Selected');
            }else if(!hasOnlyProprietaryData()){
                var panel = props.children ? React.Children.only(props.children) : <DownloadOptionPanel/>;
                panel = React.cloneElement(panel, {tbl_id});
                showDownloadDialog(panel);
            }
        } else {
            showInfoPopup('You have not chosen any data to download', 'No Data Selected');
        }
    }, [selectInfo]);

    const style = selectInfoCls.getSelectedCount() ? 'button std attn' : 'button std hl';
    return (
        <button style={{display: 'inline-block'}}
                type = 'button'
                className = {style}
                onClick = {onClick}
        >Prepare Download</button>
    );
}


DownloadButton.propTypes = {
    tbl_id      : PropTypes.string,
    tbl_grp     : PropTypes.string,
};


const noticeCss = {
    backgroundColor: 'beige',
    color: 'brown',
    border: '1px solid #cacaae',
    padding: 3,
    borderRadius: 2,
    marginBottom: 10,
    whiteSpace: 'nowrap',
    textAlign: 'center'
};

let dlTitleIdx = 0;
const newBgKey = () => 'DownloadOptionPanel-' + Date.now();

export function DownloadOptionPanel (props) {
    const {groupKey, cutoutSize, help_id, children, style, title, tbl_id, dlParams, dataTag} = props;
    const { showZipStructure=true, showEmailNotify=true, showFileLocation=true, showTitle=true } = props;

    const labelWidth = 110;
    const ttl = title || DOWNLOAD_DIALOG_ID;
    const [bgKey, setBgKey] = useState(newBgKey());

    const onSubmit = useCallback((formInputs={}) => {
        var {request, selectInfo} = getTblById(tbl_id);
        const {FileGroupProcessor} = dlParams;
        formInputs.wsSelect = formInputs.wsSelect && formInputs.wsSelect.replace(WS_HOME, '');
        const dreq = makeTblRequest(FileGroupProcessor, formInputs.Title, Object.assign(dlParams, {cutoutSize}, formInputs));
        request = set(cloneDeep(request), DataTagMeta, dataTag);
        const akey = newBgKey();
        dispatchPackage(dreq, request, SelectInfo.newInstance(selectInfo).toString(), akey);
        showDownloadDialog(this, false);
        dlTitleIdx++;
        setBgKey(akey);
    }, [cutoutSize, dataTag, dlParams, tbl_id]);

    const showWarnings = hasProprietaryData(getTblById(tbl_id));

    const maskStyle = {
        position: 'absolute',
        top:-26,
        bottom:-4,
        right:-4,
        left:-4,
        width:undefined,
        height:undefined,
        backgroundColor: 'rgba(0,0,0,0.2)'
    };
    const maskPanel = <BgMaskPanel key={bgKey} componentKey={bgKey} style={maskStyle}/>;

    const saveAsProps = {
        initialState: {
            value: get(dlParams, 'BaseFileName')
        }
    };

    const dlTitle = get(dlParams, 'TitlePrefix', 'Download') + '-' + dlTitleIdx;

    return (
        <div style = {Object.assign({margin: '4px', position: 'relative', minWidth:400, height:'auto'}, style)}>
            <FormPanel
                submitText = 'Prepare Download'
                groupKey = {groupKey}
                onSubmit = {onSubmit}
                onCancel = {() => dispatchHideDialog(ttl)}
                help_id  = {help_id}>
                <FieldGroup groupKey={groupKey} keepState={true}>
                    {showWarnings && <div style={noticeCss}>This table contains proprietary data. Only data to which you have access will be downloaded.</div>}
                    <div className='FieldGroup__vertical--more'>
                        {showTitle && <TitleField {...{labelWidth, value:dlTitle }}/>}

                        {children}

                        {cutoutSize         && <DownloadCutout {...{labelWidth}} />}
                        {showZipStructure   && <ZipStructure {...{labelWidth}} />}
                        {showFileLocation   && <WsSaveOptions {...{groupKey, labelWidth, saveAsProps}}/>}
                        {showEmailNotify    && <EmailNotification/>}
                    </div>
                </FieldGroup>
            </FormPanel>
            {maskPanel}
        </div>
    );
}


DownloadOptionPanel.propTypes = {
    groupKey:   PropTypes.string,
    tbl_id:     PropTypes.string,
    cutoutSize: PropTypes.string,
    help_id:    PropTypes.string,
    title:      PropTypes.string,           // title of the dialog, appears at top of the dialog
    style:      PropTypes.object,
    dataTag:    PropTypes.string,

    showTitle:        PropTypes.bool,           // layout Title field.  This is the title of the package request.  It will be displayed in background monitor.
    showZipStructure: PropTypes.bool,           // layout ZipStructure field
    showEmailNotify:  PropTypes.bool,           // layout EmailNotification field
    showFileLocation: PropTypes.bool,           // layout FileLocation field

    dlParams:   PropTypes.shape({               // these params should be used as defaults value if they appears as input fields
        TitlePrefix:    PropTypes.string,           // default title of the download..  an index number will be appended to this.
        FilePrefix:     PropTypes.string,           // packaged file prefix
        BaseFileName:   PropTypes.string,           // zip file name
        DataSource:     PropTypes.string,
        MaxBundleSize:  PropTypes.number,
        FileGroupProcessor: PropTypes.string.isRequired,
    })
};

DownloadOptionPanel.defaultProps= {
    groupKey: 'DownloadDialog',
};


export function TitleField({style={}, labelWidth, value, label='Title:', size=30}) {

    return (
        <ValidationField
            forceReinit={true}
            fieldKey='Title'
            tooltip='Enter a description to identify this download.'
            {...{validator:NotBlank, initialState:{value}, label, labelWidth, size, style}}
        />
    );
}

export function ZipStructure({style={}, fieldKey='zipType', labelWidth}) {
    return (
        <ListBoxInputField
            wrapperStyle={style}
            fieldKey = {fieldKey}
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

    );
}

export function DownloadCutout({style={}, fieldKey='dlCutouts', labelWidth}) {
    return (
        <ListBoxInputField
            wrapperStyle = {style}
            fieldKey = {fieldKey}
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
    );
}
export function EmailNotification({style}) {

    const [{email, enableEmail}] = useStoreConnector(getBgEmailInfo);

    const toggleEnableEmail = (e) => {
        const enableEmail = e.target.checked;
        const email = enableEmail ? email : '';
        dispatchBgSetEmailInfo({email, enableEmail});
    };

    const onEmailChanged = useCallback((v) => {
        if (get(v, 'valid')) {
            if (email !== v.value) dispatchBgSetEmailInfo({email: v.value});
        }
    }, [email]);

    return (
        <div style={style}>
            <div style={{width: 250, marginTop: 15}}><input type='checkbox' checked={enableEmail} onChange={toggleEnableEmail}/>Enable email notification</div>
            {enableEmail &&
                <InputField
                    validator={Validate.validateEmail.bind(null, 'an email field')}
                    tooltip='Enter an email to be notified when a process completes.'
                    label='Email:'
                    labelStyle={{display: 'inline-block', marginLeft: 18, width: 32, fontWeight: 'bold'}}
                    value={email}
                    placeholder='Enter an email to get notification'
                    style={{width: 170}}
                    onChange={onEmailChanged}
                    actOn={['blur', 'enter']}
                />
            }
        </div>
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
 * @param {boolean} [show=true] show or hide this dialog
 */
function showDownloadDialog(panel, show=true) {
    const ttl = get(panel, 'props.title', DOWNLOAD_DIALOG_ID);
    if (show) {
        const content= (
            <PopupPanel title={ttl} >
                {panel}
            </PopupPanel>
        );
        DialogRootContainer.defineDialog(ttl, content);
        dispatchShowDialog(ttl);
    } else {
        dispatchHideDialog(ttl);
    }
}

