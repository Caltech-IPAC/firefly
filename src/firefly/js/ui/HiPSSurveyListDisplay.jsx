import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {isEmpty, get} from 'lodash';
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
import {onHiPSSurveys, isLoadingHiPSSurverys,
        HiPSPopular, HiPSId, HiPSData, HiPSSurveyTableColumm,
        getHiPSSurveys,  getHiPSLoadingMessage, updateHiPSId,
        indexInHiPSSurveys, isOnePopularSurvey} from '../visualize/HiPSCntlr.js';
import {FieldGroup} from './FieldGroup.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', padding:5, width: '100%', height: 'calc(100% - 20px)', overflow: 'hidden', display: 'flex', flexDirection: 'column'};
const hipsSurveysPopupId = 'hipsSurveys';
const HiPSSurvey = 'HiPS_Surveys_';


export function makeHiPSSurveysTableName(hipsId) {
    if (!hipsId) {
        const popularHiPS = getFieldVal(gKeyHiPSPanel, fKeyHiPSPopular);
        hipsId = updateHiPSId(HiPSId, (popularHiPS === HiPSPopular));
    }

    return HiPSSurvey + hipsId;
}
/**
 * table to contain HiPS survey info including url, data_product and title
 * @param id
 * @param surveys
 * @param isPopular
 * @param hipsUrl
 * @param moreStyle
 * @returns {XML}
 */
function renderHiPSSurveysTable(id, surveys, isPopular, hipsUrl, moreStyle={}) {
    const hipsId = updateHiPSId(id, isPopular);
    if (!surveys) {
        surveys = getHiPSSurveys(hipsId);
    }
    const surveyTableStyle = isEmpty(moreStyle) ? tableStyle : Object.assign(tableStyle, moreStyle);
    const tableId = makeHiPSSurveysTableName(hipsId);

    let tableModel = getTblById(tableId);

    if (!tableModel && !isEmpty(surveys)) {
        const columns = [ {name: HiPSSurveyTableColumm.type.key, width: 8, type: 'char'},
            {name: HiPSSurveyTableColumm.title.key, width: 35, type: 'char'},
            {name: HiPSSurveyTableColumm.order.key, width: 6, type: 'int'},
            {name: HiPSSurveyTableColumm.sky_fraction.key, width: 12, type: 'float'},
            {name: HiPSSurveyTableColumm.url.key, width: 22, type: 'char'}];

        const data = surveys.reduce((prev, oneSurvey) => {
            prev.push([oneSurvey[HiPSSurveyTableColumm.type.key],
                oneSurvey[HiPSSurveyTableColumm.title.key],
                oneSurvey[HiPSSurveyTableColumm.order.key],
                oneSurvey[HiPSSurveyTableColumm.sky_fraction.key],
                oneSurvey[HiPSSurveyTableColumm.url.key]]);
            return prev;
        }, []);

        const highlightedRow = indexInHiPSSurveys(hipsId, hipsUrl);

        tableModel = {
            totalRows: data.length,
            tbl_id: tableId,
            tableData: {columns, data},
            highlightedRow
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
 * @param hipsUrl
 * @returns {*}
 */
function showHiPSSurveyList(id, isUpdatingHips, popularHiPS, hipsUrl) {
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
        return renderHiPSSurveysTable(id, hipsSurveys, isPopular, hipsUrl);
    };

    return (
        (isUpdatingHips) ? loading() : showHiPSSurvey()
    );
}


/**
 * show HiPS survey info table in popup panel
 * @param hipsUrl
 * @param pv
 * @param surveysId
 * @param dataType
 * @returns {*}
 */
export function showHiPSSurverysPopup(hipsUrl,  pv, surveysId = HiPSId, dataType=HiPSData) {
    const popupPanelResizableStyle = {
        width: 550,
        height: 400,
        minWidth: 400,
        minHeight: 350,
        resize: 'both',
        overflow: 'hidden'
    };

    const hipsSurveys = getHiPSSurveys(surveysId);
    // no surveys in the store yet
    if (!hipsSurveys) {
        onHiPSSurveys(dataType, surveysId);
    }

    const startHiPSPopup = () => {
        const popup = (
            <PopupPanel title={'HiPS Surveys'}>
                <div style={popupPanelResizableStyle}>
                    <HiPSSurveyListSelection
                        surveysId={surveysId}
                        wrapperStyle={{height: 'calc(100% - 60px)'}}
                        hipsUrl={hipsUrl}
                    />

                    <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 15, marginBottom: 15}}>
                        <div style={{display: 'flex', alignItems: 'flexStart', marginLeft: 10}}>
                            <div style={{marginRight: 10}}>
                                <CompleteButton
                                    onSuccess={onSelectPlot(surveysId, pv ? primePlot(pv) : primePlot(visRoot()))}
                                    text={'Search'}
                                    dialogId={hipsSurveysPopupId}
                                    groupKey={gKeyHiPSPanel}
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

export const gKeyHiPSPanel = 'HIPSList_PANEL';
export const fKeyHiPSPopular = 'popularHiPS';

function getHiPSPopularSetting(hipsUrl) {
    return (!hipsUrl || isOnePopularSurvey(hipsUrl)) ? HiPSPopular : '';
}

/**
 * show HiPS survey info table plus check box for showing popular surveys in popup panel or form panel
 */
export class HiPSSurveyListSelection extends PureComponent {
    constructor(props) {
        super(props);

        this.state = {[fKeyHiPSPopular]: getHiPSPopularSetting(props.hipsUrl)};
    }

    componentWillMount() {
        const hipsSurveys = getHiPSSurveys(this.props.surveysId);
        // no surveys in the store yet
        if (!hipsSurveys) {
            const {dataType=HiPSData} = this.props;

            onHiPSSurveys(dataType, this.props.surveysId);
        }
        this.setState({isUpdatingHips: isLoadingHiPSSurverys(this.props.surveysId)});
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
            const {surveysId} = this.props;
            const isUpdatingHips = isLoadingHiPSSurverys(surveysId);
            const pSetting = getFieldVal(gKeyHiPSPanel, fKeyHiPSPopular);

            if (pSetting !== get(this.state, [fKeyHiPSPopular])) {
                this.setState({[fKeyHiPSPopular]: pSetting});
            }
            if (isUpdatingHips !== get(this.state, 'isUpdatingHips')) {
                this.setState({isUpdatingHips});
            }

        }
    }

    render() {
        const {surveysId, wrapperStyle, hipsUrl} = this.props;
        const {isUpdatingHips, [fKeyHiPSPopular]: popularS} = this.state;

        return (
            <div style={wrapperStyle}>
                <FieldGroup groupKey={gKeyHiPSPanel} validatorFunc={null} keepState={true}
                            reducerFunc={fieldReducer(hipsUrl)}
                            style={{height: '100%', width: '100%'}}>
                    <CheckboxGroupInputField
                        fieldKey={fKeyHiPSPopular}
                        options={[{label: 'Popular HiPS', value: HiPSPopular}]}
                        alignment='horizontal'
                        wrapperStyle={{textAlign: 'center'}}
                    />
                    {showHiPSSurveyList(surveysId, isUpdatingHips, popularS, hipsUrl)}
                </FieldGroup>
            </div>
        );
    }
}

HiPSSurveyListSelection.propTypes = {
    surveysId: PropTypes.string.isRequired,
    wrapperStyle: PropTypes.object,
    hipsUrl: PropTypes.string,
    dataType: PropTypes.array
};

function fieldReducer(hipsUrl) {
    return (inFields, action ) => {
        if (!inFields) {
            const pSetting = getHiPSPopularSetting(hipsUrl);
            return {[fKeyHiPSPopular]: {
                fieldKey: fKeyHiPSPopular,
                value: pSetting,
                tooltip: 'display popular HiPS'
            }};
        }
        return inFields;
    };
}

function onSelectPlot(surveysId, plot) {
    return (request) => {
        const tblId = makeHiPSSurveysTableName(updateHiPSId(surveysId, (get(request, fKeyHiPSPopular) === HiPSPopular)));
        const tableModel = getTblById(tblId);
        if (!tableModel) {
            return;
        }

        const rootUrl = getCellValue(tableModel, get(tableModel, 'highlightedRow', 0), 'url');
        dispatchChangeHiPS({plotId: plot.plotId, hipsUrlRoot: rootUrl});
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
