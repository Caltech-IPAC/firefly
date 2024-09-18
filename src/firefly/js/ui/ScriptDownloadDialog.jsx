/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import {createDownloadScript} from '../rpc/SearchServicesJson.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {SCRIPT_ATTRIB} from '../core/background/BackgroundUtil.js';
import {download} from '../util/fetch';


const SCRIPT_DOWNLOAD_ID = 'Download Retrieval Script';

/**
 * creates and show the ScriptDownloadDialog
 * @param {Object} props  see ScripDownloadPanel.propTypes
 */
export function showScriptDownloadDialog(props) {
    const content= (
        <PopupPanel title={SCRIPT_DOWNLOAD_ID} >
            <ScripDownloadPanel {...props}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(SCRIPT_DOWNLOAD_ID, content);
    dispatchShowDialog(SCRIPT_DOWNLOAD_ID);
}


class ScripDownloadPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {urlsOnly: false};
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
        if (!this.isUnmounted) {
            const fields = FieldGroupUtils.getGroupFields('ScriptDownloadDialog');
            const urlsOnly = FieldGroupUtils.getFldValue(fields, 'URLsOnly');
            this.setState({urlsOnly});
        }
    }

    onSubmit(request) {
        const {jobId, label, DATA_SOURCE} = this.props;
        const attributes = Object.values(request).filter((v) => SCRIPT_ATTRIB.get(String(v)));
        createDownloadScript(jobId, label.replace(/\s/g, '_'), DATA_SOURCE, attributes)
            .then((url) => {
                download(url);
                dispatchHideDialog(SCRIPT_DOWNLOAD_ID);
            });
    }

    render() {
        const {help_id, jobId} = this.props;
        const {urlsOnly} = this.state;
        return (
            <div style = {{margin: '4px'}}>
                <FormPanel
                    groupKey='ScriptDownloadDialog'
                    onSuccess={this.onSubmit}
                    onCancel={() => dispatchHideDialog(SCRIPT_DOWNLOAD_ID)}
                    completeText='OK'
                    help_id={help_id}>

                    <FieldGroup groupKey={'ScriptDownloadDialog'}>
                        <div style={{visibility: (urlsOnly ? 'hidden' : 'visible')}}>
                            <ListBoxInputField
                                fieldKey ='downloader'
                                initialState = {{
                                                  tooltip: 'How do you want this script to download the URLs (wget best for linux, curl best for mac)',
                                                  label : 'Download Command:'
                                                }}
                                options = {[
                                            {label: 'wget - best for Linux / other unix', value: 'Wget'},
                                            {label: 'curl - best for Mac', value: 'Curl'}
                                           ]}
                                labelWidth = {110}
                            />
                            <ListBoxInputField
                                fieldKey ='uncompressor'
                                initialState = {{
                                                  tooltip: 'How do you want this script to uncompress',
                                                  label : 'Uncompression:'
                                                }}
                                options = {[
                                            {label: 'unzip - on most platforms, works only on files smaller than 2GB', value: 'Unzip'},
                                            {label: 'ditto - on Mac, best for files larger than 2GB', value: 'Ditto'},
                                            {label: "Don't unzip the files", value: '-'}
                                           ]}
                            />
                        </div>

                        <CheckboxGroupInputField
                            fieldKey='URLsOnly'
                            initialState= {{label : ' '}}
                            options={[
                                        {label: 'Download just a list of URLs', value: 'URLsOnly'}
                                    ]}
                        />
                    </FieldGroup>
                </FormPanel>
            </div>
        );
    }
}


ScripDownloadPanel.propTypes = {
    jobId : PropTypes.string,      //  background ID
    label: PropTypes.string,        // background Title
    help_id : PropTypes.string
};


