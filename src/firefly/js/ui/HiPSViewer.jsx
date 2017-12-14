import React from 'react';
import {isEmpty, get} from 'lodash';
import {getHiPSSurveys,  getHiPSLoadingMessage, HiPSSurveyTableColumm} from '../visualize/HiPSCntlr.js';
import {getTblById, getCellValue} from '../tables/TableUtil.js';
import LOADING from 'html/images/gxt/loading.gif';
import {TablePanel} from '../tables/ui/TablePanel.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import {dispatchChangeHiPS} from '../visualize/ImagePlotCntlr.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import {HelpIcon} from './HelpIcon.jsx';
import {CompleteButton} from './CompleteButton.jsx';
import {INFO_POPUP} from './PopupUtil.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {HiPSData}  from './TestQueriesPanel.jsx';
import {onHiPSSurveys} from '../visualize/HiPSCntlr.js';

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', padding:5, width: '100%', height: '100%', overflow: 'hidden', display: 'flex', flexDirection: 'column'};
export const HiPSSurvey = 'HiPS_Surveys_';
const hipsSurveysPopupId = 'hipsSurveys';

/**
 * table to contain HiPS survey info including url, dataproduct and title
 * @param id
 * @param surveys
 * @param moreStyle
 * @returns {XML}
 */
function renderHiPSSurveysTable(id, surveys, moreStyle={}) {
    if (!surveys) {
        surveys = getHiPSSurveys(id);
    }
    const surveyTableStyle = isEmpty(moreStyle) ? tableStyle : Object.assign(tableStyle, moreStyle);
    const tableId = HiPSSurvey + id;

    let tableModel = getTblById(tableId);

    if (!tableModel && !isEmpty(surveys)) {
        const columns = [ {name: HiPSSurveyTableColumm.type.key, width: 8, type: 'char'},
            {name: HiPSSurveyTableColumm.title.key, width: 25, type: 'char'},
            {name: HiPSSurveyTableColumm.url.key, width: 28, type: 'char'}];

        const data = surveys.reduce((prev, oneSurvey) => {
            prev.push([oneSurvey[HiPSSurveyTableColumm.type.key],
                oneSurvey[HiPSSurveyTableColumm.title.key],
                oneSurvey[HiPSSurveyTableColumm.url.key]]);
            return prev;
        }, []);

        tableModel = {
            totalRows: data.length,
            tbl_id: tableId,
            tableData: {columns, data},
            highlightedRow: 0
        };
    }

    return tableModel ? (
        <div style={surveyTableStyle}>
            <TablePanel
                key={tableModel.tbl_id}
                tbl_ui_id = {tableModel.tbl_id + '-ui'}
                tableModel={tableModel}
                title={'HiPS Plot'}
                height={'calc(100% - 20px)'}
                showToolbar={false}
                selectable={false}
                showOptionButton={false}
            />
        </div>) :
        (<div style={{display:'flex', justifyContent: 'center', alignItems: 'center', padding: 10}}>
               {getHiPSLoadingMessage(id) || 'HiPS surveys are not found'}
        </div>);
}

/**
 * show HiPS survey table or loading sign
 * @param id
 * @param isUpdatingHips
 * @returns {*}
 */
export function showHiPSSurveyList(id, isUpdatingHips) {
    const hipsSurveys = getHiPSSurveys(id);

    const loading = () => {
        return (
            <div style={{width: '100%', height: '100%', display:'flex', justifyContent: 'center', alignItems: 'center'}}>
                <img style={{width:14,height:14}} src={LOADING}/>
            </div>
        );
    };

    const showHiPSSurvey = () => {
        return renderHiPSSurveysTable(id, hipsSurveys);
    };

    return (
        (isUpdatingHips) ? loading() : showHiPSSurvey()
    );
}

/**
 * show HiPS survey info table in popup panel
 * @param pv
 * @param dataType
 * @returns {*}
 */
export function showHiPSSurverysPopup(pv, dataType=HiPSData) {
    const surveysId = get(pv, ['request', 'params', 'hipsSurveysId']);

    if (!surveysId ) {
        HiPSPopupMsg('No HiPS Surveys Id found', 'HiPS Surverys search');
        return;
    }

    const surveys = getHiPSSurveys(surveysId);
    if (!surveys) {
        return onHiPSSurveys(dataType, surveysId);
    }

    const popupPanelResizableStyle = {
        width: 450,
        height: 400,
        minWidth: 400,
        minHeight: 350,
        resize: 'both',
        overflow: 'hidden'
    };

    const startHiPSPopup = () => {
        const popup = (
            <PopupPanel title={'HiPS Surveys'}>
                <div style={popupPanelResizableStyle}>
                    {renderHiPSSurveysTable(surveysId, surveys, {height: 'calc(100% - 50px)'})}
                    <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 15, marginBottom: 15}}>
                        <div style={{display: 'flex', alignItems: 'flexStart', marginLeft: 10}}>
                            <div style={{marginRight: 10}}>
                                <CompleteButton
                                    onSuccess={onSelectPlot(surveysId, pv.plotId)}
                                    text={'Search'}
                                    dialogId={hipsSurveysPopupId}
                                />
                            </div>
                            <div>
                                <button type='button' className='button std hl'
                                        onClick={() => resultCancel()}>Cancel
                                </button>
                            </div>
                        </div>
                        <div style={{ textAlign:'right', marginRight: 10}}>
                            <HelpIcon helpId={'visualization.imageoptions'}/>
                        </div>
                    </div>
                </div>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(hipsSurveysPopupId, popup);
        dispatchShowDialog(hipsSurveysPopupId);
    };

    startHiPSPopup();
}

function onSelectPlot(surveysId, plotId) {
    return () => {
        const tableModel = getTblById(HiPSSurvey + surveysId);
        if (!tableModel) {
            return;
        }

        const rootUrl = getCellValue(tableModel, get(tableModel, 'highlightedRow', 0), 'url');
        dispatchChangeHiPS({plotId, hipsUrlRoot: rootUrl});
    };
}

export function HiPSPopupMsg(msg, title) {
    showInfoPopup(msg, title);
}

function resultCancel() {
    dispatchHideDialog(hipsSurveysPopupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
}
