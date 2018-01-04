import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {isEmpty, get} from 'lodash';
import {getHiPSSurveys,  getHiPSLoadingMessage, updateHiPSId, HiPSSurveyTableColumm} from '../visualize/HiPSCntlr.js';
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
import {onHiPSSurveys, isLoadingHiPSSurverys, HiPSPopular, _HiPSPopular} from '../visualize/HiPSCntlr.js';
import {FieldGroup} from './FieldGroup.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {getFieldVal, getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', padding:5, width: '100%', height: 'calc(100% - 20px)', overflow: 'hidden', display: 'flex', flexDirection: 'column'};
const hipsSurveysPopupId = 'hipsSurveys';
export const HiPSSurvey = 'HiPS_Surveys_';

/**
 * table to contain HiPS survey info including url, data_product and title
 * @param id
 * @param surveys
 * @param isPopular
 * @param moreStyle
 * @returns {XML}
 */
function renderHiPSSurveysTable(id, surveys, isPopular, moreStyle={}) {
    if (!surveys) {
        surveys = getHiPSSurveys(updateHiPSId(id, isPopular));
    }
    const surveyTableStyle = isEmpty(moreStyle) ? tableStyle : Object.assign(tableStyle, moreStyle);
    const tableId = HiPSSurvey + updateHiPSId(id, isPopular);

    let tableModel = getTblById(tableId);

    if (!tableModel && !isEmpty(surveys)) {
        const columns = [ {name: HiPSSurveyTableColumm.type.key, width: 8, type: 'char'},
            {name: HiPSSurveyTableColumm.title.key, width: 35, type: 'char'},
            {name: HiPSSurveyTableColumm.url.key, width: 22, type: 'char'}];

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
                height={'calc(100%)'}
                showToolbar={false}
                selectable={false}
                showOptionButton={true}
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
 * @param popularHiPS
 * @returns {*}
 */
function showHiPSSurveyList(id, isUpdatingHips, popularHiPS = '') {
    const isPopular =  (popularHiPS === HiPSPopular);
    const hipsSurveys = getHiPSSurveys(updateHiPSId(id, isPopular));

    const loading = () => {
        return (
            <div style={{width: '100%', height: '100%', display:'flex', justifyContent: 'center', alignItems: 'center'}}>
                <img style={{width:14,height:14}} src={LOADING}/>
            </div>
        );
    };

    const showHiPSSurvey = () => {
        return renderHiPSSurveysTable(id, hipsSurveys, isPopular);
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
    const surveysId =  get(pv, ['request', 'params', 'hipsSurveysId']);
    const popupPanelResizableStyle = {
        width: 480,
        height: 400,
        minWidth: 400,
        minHeight: 350,
        resize: 'both',
        overflow: 'hidden'
    };

    if (!surveysId ) {
        HiPSPopupMsg('No HiPS Surveys Id found', 'HiPS Surverys search');
        return;
    }

    const hipsSurveys = getHiPSSurveys(surveysId);
    if (!hipsSurveys) {
        onHiPSSurveys(dataType, surveysId);
    }

    const startHiPSPopup = () => {
        const popup = (
            <PopupPanel title={'HiPS Surveys'}>
                <div style={popupPanelResizableStyle}>
                    <HiPSSurveyListSelection
                        surveysId={surveysId}
                        pv={pv}
                        wrapperStyle={{height: 'calc(100% - 60px)'}}
                    />

                    <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 15, marginBottom: 15}}>
                        <div style={{display: 'flex', alignItems: 'flexStart', marginLeft: 10}}>
                            <div style={{marginRight: 10}}>
                                <CompleteButton
                                    onSuccess={onSelectPlot(surveysId, pv.plotId)}
                                    text={'Search'}
                                    dialogId={hipsSurveysPopupId}
                                    groupKey={gKeyHiPSPopup}
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

export const gKeyHiPSPopup = 'HIPSList_PANEL';
export const fKeyHiPSPopular = 'popularHiPS';

const getHiPSPopularSetting = (pv) => {
    const surveysId = get(pv, ['request', 'params', 'hipsSurveysId']);

    return (surveysId&&surveysId.endsWith(_HiPSPopular)) ? HiPSPopular
                                                         : getFieldVal( gKeyHiPSPopup, fKeyHiPSPopular, HiPSPopular);
};

/**
 * show HiPS survey info table plus check box for showing popular surveys in popup panel or form panel
 */
export class HiPSSurveyListSelection extends PureComponent {
    constructor(props) {
        super(props);

        const popularStr = getHiPSPopularSetting(props.pv);
        const isUpdatingHips = isLoadingHiPSSurverys(props.surveysId);
        this.state = {fields: {popularHiPS: {value: popularStr}}, isUpdatingHips};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(()=>this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const {fields} = getFieldGroupState(gKeyHiPSPopup) || {};

            if (fields && (fields !== get(this.state, 'fields'))) {
                this.setState({fields});
            }

            const isUpdatingHips = isLoadingHiPSSurverys(this.props.surveysId);

            if (isUpdatingHips !== get(this.state, 'isUpdatingHips')) {
                this.setState({isUpdatingHips});
            }

        }
    }

    render() {
        const {surveysId, wrapperStyle} = this.props;
        const {fields, isUpdatingHips} = this.state;
        const popularHiPS = get(fields, [fKeyHiPSPopular, 'value']);


        return (
            <div style={wrapperStyle}>
                <FieldGroup groupKey={gKeyHiPSPopup} validatorFunc={null} keepState={true}
                            style={{height: '100%', width: '100%'}}>
                    <CheckboxGroupInputField
                        fieldKey={fKeyHiPSPopular}
                        initialState={{
                                        value: popularHiPS,   // workaround for _all_ for now
                                        tooltip: 'display popular HiPS'
                                      }}
                        options={[{label: 'Popular HiPS', value: HiPSPopular}]}
                        alignment='horizontal'
                        wrapperStyle={{textAlign: 'center'}}
                    />
                    {showHiPSSurveyList(surveysId, isUpdatingHips, popularHiPS)}
                </FieldGroup>
            </div>
        );
    }
}

HiPSSurveyListSelection.propTypes = {
    pv: PropTypes.object,
    surveysId: PropTypes.string.isRequired,
    wrapperStyle: PropTypes.object
};

function onSelectPlot(surveysId, plotId) {
    return (request) => {
        const tblId = HiPSSurvey + updateHiPSId(surveysId, (get(request, fKeyHiPSPopular) === HiPSPopular));
        const tableModel = getTblById(tblId);
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
