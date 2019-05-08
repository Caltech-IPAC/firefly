/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback} from 'react';
import PropTypes from 'prop-types';
import {set, cloneDeep, get, isEmpty} from 'lodash';

import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import Validate from '../util/Validate.js';
import {InputField} from './InputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {showInfoPopup} from './PopupUtil.jsx';

import {getTblInfoById, getActiveTableId, getProprietaryInfo, getTblById} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {dispatchPackage, dispatchBgSetEmailInfo} from '../core/background/BackgroundCntlr.js';
import {getBgEmailInfo} from '../core/background/BackgroundUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {DataTagMeta} from '../tables/TableRequestUtil.js';
import {useStoreConnector} from './SimpleComponent.jsx';

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
    const selectInfoGetter = () => get(getTblInfoById(tblIdGetter()), 'selectInfo');

    const [tbl_id, selectInfo] = useStoreConnector(tblIdGetter, selectInfoGetter);
    const selectInfoCls = SelectInfo.newInstance(selectInfo);

    const onClick = useCallback(() => {
        if (selectInfoCls.getSelectedCount()) {
            var panel = props.children ? React.Children.only(props.children) : <DownloadOptionPanel/>;
            panel = React.cloneElement(panel, {tbl_id});
            showDownloadDialog(panel);
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
    marginBottom: 3,
};

export function DownloadOptionPanel (props) {
    const {mask, groupKey, cutoutSize, help_id, children, style, title, tbl_id, dlParams, dataTag} = props;

    const [{email, enableEmail}] = useStoreConnector(getBgEmailInfo);
    const onEmailChanged = useCallback((v) => {
        if (get(v, 'valid')) {
            if (email !== v.value) dispatchBgSetEmailInfo({email: v.value});
        }
    }, [email]);

    const labelWidth = 110;
    const ttl = title || DOWNLOAD_DIALOG_ID;

    const onSubmit = useCallback((options) => {
        var {request, selectInfo} = getTblInfoById(tbl_id);
        const {FileGroupProcessor} = dlParams;
        const Title = dlParams.Title || options.Title;
        const dreq = makeTblRequest(FileGroupProcessor, Title, Object.assign(dlParams, {cutoutSize}, options));
        request = set(cloneDeep(request), DataTagMeta, dataTag);
        dispatchPackage(dreq, request, SelectInfo.newInstance(selectInfo).toString());
        showDownloadDialog(this, false);
    }, tbl_id);

    const toggleEnableEmail = (e) => {
        const enableEmail = e.target.checked;
        const email = enableEmail ? email : '';
        dispatchBgSetEmailInfo({email, enableEmail});
    };

    const hasProprietaryData = !isEmpty(getProprietaryInfo(getTblById(tbl_id)));

    return (
        <div style = {Object.assign({margin: '4px', position: 'relative', minWidth: 350}, style)}>
            {mask && <div style={{width: '100%', height: '100%'}} className='loading-mask'/>}
            {hasProprietaryData && <div style={noticeCss}>This table contains proprietary data. Only data to which you have access will be downloaded.</div>}
            <FormPanel
                submitText = 'Prepare Download'
                groupKey = {groupKey}
                onSubmit = {onSubmit}
                onCancel = {() => dispatchHideDialog(ttl)}
                help_id  = {help_id}>
                <FieldGroup groupKey={groupKey} keepState={true}>

                    {children}

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
                        labelWidth = {labelWidth}
                    />

                    <div style={{width: 250, marginTop: 10}}><input type='checkbox' checked={enableEmail} onChange={toggleEnableEmail}/>Enable email notification</div>
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
                        actOn={['blur','enter']}
                    />
                    }
                </FieldGroup>
            </FormPanel>
        </div>
    );
}


DownloadOptionPanel.propTypes = {
    groupKey:   PropTypes.string,
    tbl_id:     PropTypes.string,
    cutoutSize: PropTypes.string,
    help_id:    PropTypes.string,
    title:      PropTypes.string,
    mask:       PropTypes.bool,
    style:      PropTypes.object,
    dataTag:    PropTypes.string,
    dlParams:   PropTypes.shape({
        TitlePrefix:    PropTypes.string,
        FilePrefix:     PropTypes.string,
        BaseFileName:   PropTypes.string,
        Title:          PropTypes.string,
        DataSource:     PropTypes.string,
        MaxBundleSize:  PropTypes.number,
        FileGroupProcessor: PropTypes.string.isRequired,
    })
};

DownloadOptionPanel.defaultProps= {
    groupKey: 'DownloadDialog',
};



/**
 * creates and show the DownloadDialog.
 * @param {Component}  panel  the panel to show in the popup.
 * @param {boolean} [show=true] show or hide this dialog
 */
function showDownloadDialog(panel, show=true) {
    const ttl = panel.props.title || DOWNLOAD_DIALOG_ID;
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

