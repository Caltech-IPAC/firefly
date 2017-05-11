/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';

import {flux} from '../Firefly.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import Validate from '../util/Validate.js';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {ValidationField} from './ValidationField.jsx';
import {showInfoPopup} from './PopupUtil.jsx';

import {makeTblRequest, getTblInfoById, getActiveTableId} from '../tables/TableUtil.js';
import {dispatchPackage, doOnPackage} from '../core/background/BackgroundCntlr.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';

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
 */
export class DownloadButton extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {tbl_id: props.tbl_id};
        this.onClick = this.onClick.bind(this);
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }


    storeUpdate() {
        if (!this.isUnmounted) {
            const tbl_id = this.props.tbl_id || getActiveTableId(this.props.tbl_grp);
            const {selectInfo} = getTblInfoById(tbl_id);
            this.setState({selectInfo, tbl_id});
        }
    }

    onClick() {
        const {tbl_id, selectInfo={}} = this.state;
        const selectInfoCls = SelectInfo.newInstance(selectInfo);
        if (selectInfoCls.getSelectedCount()) {
            var panel = this.props.children ? React.Children.only(this.props.children) : <DownloadOptionPanel/>;
            panel = React.cloneElement(panel, {tbl_id});
            showDownloadDialog(panel);
        } else {
            showInfoPopup('You have not chosen any data to download', 'No Data Selected');
        }
    }

    render() {
        const {selectInfo={}} = this.state;
        const selectInfoCls = SelectInfo.newInstance(selectInfo);
        const style = selectInfoCls.getSelectedCount() ? 'button std attn' : 'button std hl';
        return (
            <button style={{display: 'inline-block'}}
                    type = 'button'
                    className = {style}
                    onClick = {this.onClick}
            >Prepare Download</button>
        );
    }
}


DownloadButton.propTypes = {
    tbl_id      : PropTypes.string,
    tbl_grp     : PropTypes.string,
};


export class DownloadOptionPanel extends PureComponent {

    constructor(props) {
        super(props);
        const sendEmail = FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(props.groupKey), 'sendEmail');
        this.state = {sendEmail, mask: false};
        this.onSubmit = this.onSubmit.bind(this);
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    storeUpdate() {
        const {groupKey} = this.props;
        if (!this.isUnmounted) {
            const sendEmail = FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey), 'sendEmail');
            this.setState({sendEmail});
        }
    }

    onSubmit(options) {
        const {tbl_id, dlParams, cutoutSize} = this.props;
        const {request, selectInfo} = getTblInfoById(tbl_id);
        const {FileGroupProcessor} = dlParams;
        const Title = dlParams.Title || options.Title;
        const dreq = makeTblRequest(FileGroupProcessor, Title, Object.assign(dlParams, {cutoutSize}, options));
        this.setState({mask: true});
        dispatchAddSaga(doOnPackage, {title: Title, callback:() => {
            this.setState({mask: false});
            showDownloadDialog(this, false);
        }});
        dispatchPackage(dreq, request, SelectInfo.newInstance(selectInfo).toString());
    }

    render() {
        const {groupKey, cutoutSize, help_id, children, style, title} = this.props;
        const {mask, sendEmail} = this.state;
        const labelWidth = 110;
        let ttl = title || DOWNLOAD_DIALOG_ID;
        return (
            <div style = {Object.assign({margin: '4px', position: 'relative', minWidth: 350}, style)}>
                {mask && <div style={{width: '100%', height: '100%'}} className='loading-mask'/>}
                <FormPanel
                    submitText = 'Prepare Download'
                    groupKey = {groupKey}
                    onSubmit = {this.onSubmit}
                    onCancel = {() => dispatchHideDialog(ttl)}
                    help_id  = {help_id}>
                    <FieldGroup groupKey={'DownloadDialog'} keepState={true}>

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
                        <CheckboxGroupInputField
                            wrapperStyle={{marginTop: 20}}
                            fieldKey='sendEmail'
                            initialState= {{label : ' '}}
                            options={[
                                        {label: 'Also send me email with URLs to download', value: 'true'}
                                    ]}
                            labelWidth = {0}
                        />
                        {sendEmail &&
                            <div>
                                <ValidationField
                                    wrapperStyle={{marginTop: 5, marginLeft: 80}}
                                    initialState = {{
                                           value: '',
                                           validator: Validate.validateEmail.bind(null, 'Email'),
                                           label : 'Email:',
                                           tooltip: 'Enter or modify your email address',
                                       }}
                                    fieldKey = 'Email'
                                    size = {30}
                                    labelWidth = {30}/>
                            </div>
                        }
                    </FieldGroup>
                </FormPanel>
            </div>
        );
    }
}


DownloadOptionPanel.propTypes = {
    groupKey:   PropTypes.string,
    tbl_id:     PropTypes.string,
    cutoutSize: PropTypes.string,
    help_id:    PropTypes.string,
    title:      PropTypes.string,
    mask:       PropTypes.bool,
    style:      PropTypes.object,
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
    let ttl = panel.props.title || DOWNLOAD_DIALOG_ID;
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

