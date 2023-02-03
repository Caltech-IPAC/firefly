/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';

import {
    URL_COL, makeHiPSRequest, defHiPSSources, getHiPSSources, IVOAID_COL, TITLE_COL
} from '../visualize/HiPSListUtil.js';
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
import {createHiPSMocLayer} from 'firefly/visualize/task/PlotHipsTask.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {getDefaultMOCList} from 'firefly/visualize/HiPSMocUtil.js';
import {FieldGroupTabs, Tab} from 'firefly/ui/panel/TabPanel';
import {FileUploadViewPanel} from 'firefly/visualize/ui/FileUploadViewPanel';
import {resultSuccess} from 'firefly/ui/FileUploadProcessor';
import {MOC_TABLES} from 'firefly/ui/FileUploadUtil';

const useSourceHiPS = 'useSourceHiPS';
const useSourceMOC = 'useSourceMOC';
let activeHipsTblId;
let activeGroupKey;

export const HiPSImageSelect= ({style={}, groupKey}) => {
    const imageSource = useStoreConnector(() => getFieldVal(groupKey,'imageSource'));
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
                <div className='ImageSearch__section--title' style={{display: 'inline-flex', width: '100%', boxSizing: 'border-box'}}>
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

const DIALOG_ID = 'HiPSImageSelectPopup';

/**
 * show HiPS survey info table in popup panel
 * @param {PlotView} pv
 * @param {boolean} moc
 * @returns {*}
 */
export function showHiPSSurveysPopup(pv, moc= false) {

    const groupKey = activeGroupKey || DIALOG_ID;

    const onSubmit = (request) => {
        if (request.mocTabs === 'uploadMoc') {
            if (resultSuccess(request)) dispatchHideDialog(DIALOG_ID);
        }
        else {
            const rootUrl = getHipsUrl();
            if (rootUrl) {
                const plot = pv ? primePlot(pv) : primePlot(visRoot());
                // update the table highlight of the other one which is not shown in table panel
                moc ? createHiPSMocLayer(getIvoaId(), getTitle(), rootUrl, primePlot(pv), true) :
                    dispatchChangeHiPS({plotId: plot.plotId, hipsUrlRoot: rootUrl});
                dispatchHideDialog(DIALOG_ID);
            }
        }
    };

    const popup = (
        <PopupPanel title={moc ? 'Add MOC Layer' : 'Change HiPS Image'} modal={true}>
            <FieldGroup groupKey={groupKey} keepState={true} style={{width:'100%', height: '100%'}}>
                <FormPanel submitBarStyle={{flexShrink: 0, padding: '0px 6px 3px 6px'}}
                           style={ { resize:'both', overflow: 'hidden', zIndex:1}}
                            groupKey={groupKey}
                            submitText={moc ? 'Add MOC' : 'Change HiPS'}
                            onSubmit={onSubmit}
                            onCancel={() => dispatchHideDialog(DIALOG_ID)}
                            params={{disabledDropdownHide: true}}
                            help_id='visualization.changehips'>
                    {moc &&
                        <FieldGroupTabs initialState= {{ value:'search' }} fieldKey='mocTabs'
                                            style={{minWidth:715, minHeight:500, width:'100%', height: '100%'}}>
                            <Tab name='Search' id='search'>
                                <div className='ImageSearch__HipsPopup' style={{padding: '5px 0px 0px 0px'}}>
                                    <SourceSelect moc={moc}/>
                                    <HiPSSurveyTable groupKey={groupKey} moc={moc}/>
                                </div>
                            </Tab>

                            <Tab name='Use my MOC' id='uploadMoc'>
                                <div style={{width:'100%', minWidth:715, minHeight:500}} >
                                    <FileUploadViewPanel acceptList={[MOC_TABLES]}/>
                                </div>
                            </Tab>
                        </FieldGroupTabs>}

                    {!moc &&
                        <div className='ImageSearch__HipsPopup' style={{minWidth:715, minHeight:500}}>
                            <SourceSelect moc={moc}/>
                            <HiPSSurveyTable groupKey={groupKey} moc={moc}/>
                        </div>}
                </FormPanel>
            </FieldGroup>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}

function getHighlightCell(cellStr) {
    const tableModel = getTblById(activeHipsTblId);
    return getCellValue(tableModel, tableModel?.highlightedRow, cellStr)?.trim();

}

const getIvoaId= () => getHighlightCell(IVOAID_COL) || BLANK_HIPS_URL;// blank hips is empty-space in the table
const getTitle= () => getHighlightCell(TITLE_COL) || 'Blank HiPS';// blank hips is empty-space in the table
export const getHipsUrl= () => getHighlightCell(URL_COL) || BLANK_HIPS_URL;// blank hips is empty-space in the table

function SourceSelect({moc=false}) {
    const mocSource= getAppOptions().hips?.adhocMocSource || 'Featured Sources';
    const sourceOptions = moc && mocSource ?
            [{label : mocSource.label, value: 'adhoc' }] :
            defHiPSSources()?.map((oneSource) => ({label: oneSource.label, value: oneSource.source}));
    return (
        <CheckboxGroupInputField wrapperStyle={{color: 'black', fontWeight: 'normal', fontSize: 12, display: 'inline-flex', alignItems: 'center', marginTop: -4}}
                                 fieldKey={moc ? useSourceMOC : useSourceHiPS}
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

function HiPSSurveyTable({groupKey, moc=false}) {

    let sources = useStoreConnector(() => getFieldVal(groupKey, moc ? useSourceMOC : useSourceHiPS));
    sources = sources ||  getHiPSSources();
    activeHipsTblId = 'HiPS_tbl_id-' + (moc ? '--moc--' : '--hips--') + sources.replaceAll(',', '-');

    useEffect ( () => {
        if (!getTblById(activeHipsTblId)) {
            const mocSources= getAppOptions().hips?.adhocMocSource?.sources ?? getDefaultMOCList();
            const req = makeHiPSRequest(
                moc===true? 'moc' : 'hips', sources,
                moc ? mocSources : undefined,
                activeHipsTblId);
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
