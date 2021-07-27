/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';

import {URL_COL, makeHiPSRequest, defHiPSSources, getHiPSSources} from '../visualize/HiPSListUtil.js';
import {ValidationField} from './ValidationField.jsx';
import {getCellValue, getTblById} from '../tables/TableUtil.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {parseWorldPt} from '../visualize/Point.js';
import {dispatchTableFetch} from '../tables/TablesCntlr.js';
import {TablePanel} from '../tables/ui/TablePanel.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchChangeHiPS, visRoot} from '../visualize/ImagePlotCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from '../core/ComponentCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {BLANK_HIPS_URL} from '../visualize/WebPlot.js';

const useSource = 'useSource';
let activeHipsTblId;
let activeGroupKey;

export const HiPSImageSelect= ({style={}, groupKey}) => {
    const [imageSource] = useStoreConnector(() => getFieldVal(groupKey,'imageSource'));
    activeGroupKey = groupKey;

    if (imageSource === 'url') {
        return (
            <div className='ImageSearch__section' style={style}>
                <div className='ImageSearch__section--title'>4. Enter URL</div>
                <ValidationField labelWidth={150} style={{width: 475}} fieldKey='txURL' />
            </div>
        );
    } else {
        return (
            <div className='ImageSearch__section hips-table' style={style}>
                <div className='ImageSearch__section--title' style={{display: 'inline-flex',width: '100%'}}>
                    <div style={{width: 175}}>4. Select Data Set</div>
                    <SourceSelect/>
                </div>
                <HiPSSurveyTable groupKey={groupKey}/>
            </div>
        );
    }
};

HiPSImageSelect.propTypes = {
    groupKey: PropTypes.string.isRequired,
    style: PropTypes.object
};


/**
 * show HiPS survey info table in popup panel
 * @param pv
 * @returns {*}
 */
export function showHiPSSurverysPopup(pv=visRoot()) {

    const dialogId = 'HiPSImageSelectPopup';
    const groupKey = activeGroupKey || dialogId;

    const onSubmit = () => {
        const rootUrl = getHipsUrl();
        if (rootUrl) {
            const plot = pv ? primePlot(pv) : primePlot(visRoot());
            // update the table highlight of the other one which is not shown in table panel
            dispatchChangeHiPS({plotId: plot.plotId, hipsUrlRoot: rootUrl});
            dispatchHideDialog(dialogId);
        }
    };
    const popup = (
        <PopupPanel title={'Change HiPS Image'} modal={true}>
            <FormPanel  submitBarStyle = {{flexShrink: 0, padding: '0 6px 3px 6px'}}
                        groupKey = {groupKey}
                        submitText={'Search'}
                        onSubmit = {onSubmit}
                        onCancel = {() => dispatchHideDialog(dialogId)}
                        help_id = 'visualization.changehips'>
                <FieldGroup groupKey={groupKey} keepState={true}>
                    <div className='ImageSearch__HipsPopup'>
                        <SourceSelect/>
                        <HiPSSurveyTable groupKey={groupKey}/>
                    </div>
                </FieldGroup>
            </FormPanel>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(dialogId, popup);
    dispatchShowDialog(dialogId);
}

export function getHipsUrl() {
    const tableModel = getTblById(activeHipsTblId);
    const {highlightedRow} = tableModel;
    // blank hips is empty-space in the table.  this is done so the info icon is not shown.
    // however, it should be treated as BLANK_HIPS_URL
    return getCellValue(tableModel, highlightedRow, URL_COL)?.trim() || BLANK_HIPS_URL;
}

function SourceSelect() {
    const sourceOptions = defHiPSSources()?.map((oneSource) => {
        return {label: oneSource.label, value: oneSource.source};
    });
    return (
        <CheckboxGroupInputField wrapperStyle={{color: 'black', fontWeight: 'normal', fontSize: 12, display: 'inline-flex', alignItems: 'center', marginTop: -4}}
                                 fieldKey={useSource}
                                 initialState={{value: sourceOptions?.[0]?.value}}
                                 options={sourceOptions}/>
    );
}

function SelectUrl({style}) {
    return (
        <div className='ImageSearch__section' style={style} title={'enter url of HiPS image'}>
            <div className='ImageSearch__section--title'>Enter URL</div>
            <ValidationField labelWidth={150} style={{width: 475}} fieldKey='txURL' />
        </div>
    );
}

SelectUrl.propTypes = {
    style: PropTypes.object
};

/**
 * create webPlotRequest for HiPS image request
 * @param {WebPlotRequest} request
 * @param {String} plotId
 * @param {String} groupId
 */
export function makeHiPSWebPlotRequest(request, plotId, groupId= DEFAULT_FITS_VIEWER_ID) {

    const url = ( request?.imageSource === 'url') ? request.txURL?.trim() : getHipsUrl();
    if (!url) {
        showInfoPopup('No HiPS URL found', 'HiPS search');
        return null;
    }

    const fov = request?.sizeFov ?? 180;
    const wp = parseWorldPt(request.UserTargetWorldPt) || null;
    const wpRequest = WebPlotRequest.makeHiPSRequest(url, wp, Number(fov) || NaN);
    wpRequest.setPlotGroupId(groupId);
    wpRequest.setPlotId(plotId);
    return wpRequest;
}

function HiPSSurveyTable({groupKey}) {

    let [sources] = useStoreConnector(() => getFieldVal(groupKey, useSource));
    sources = sources ||  getHiPSSources();
    activeHipsTblId = 'HiPS_tbl_id-' + sources.replaceAll(',', '-');

    useEffect ( () => {
        if (!getTblById(activeHipsTblId)) {
            const req = makeHiPSRequest(sources, activeHipsTblId);
            req && dispatchTableFetch(req);
        }
    }, [sources]);

    return (
        <div style={{flexGrow: 1, width: '100%', position: 'relative'}}>
            <div style={{position: 'absolute', top:0, bottom:0, left:0, right:0}}>
                <TablePanel key={activeHipsTblId} tbl_id={activeHipsTblId} tbl_ui_id={activeHipsTblId+'-ui'}
                        {...{showToolbar: false, selectable:false, showFilters:true, showOptionButton: true}}/>
            </div>
        </div>
    );


}
