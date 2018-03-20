import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {isEmpty, get, isUndefined} from 'lodash';
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
import {onHiPSSurveys, isLoadingHiPSSurverys, HiPSPopular, HiPSId, HiPSData, HiPSSurveyTableColumn,
        getHiPSSurveys,  getHiPSLoadingMessage, getPopularHiPSTable,
        isOnePopularSurvey, makeHiPSSurveysTableName, indexInHiPSSurveys,
        updateHiPSTblHighlightOnUrl} from '../visualize/HiPSListUtil.js';
import {FieldGroup} from './FieldGroup.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';

//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', padding:5, width: '100%', height: 'calc(100% - 20px)', overflow: 'hidden', display: 'flex', flexDirection: 'column'};
const hipsSurveysPopupId = 'hipsSurveys';

/**
 * table to contain HiPS survey info including url, data_product and title
 * @param hipsId
 * @param isPopular
 * @param hipsUrl
 * @param moreStyle
 * @returns {XML}
 */
function renderHiPSSurveysTable(hipsId, isPopular, hipsUrl, moreStyle={}) {
    const surveyTableStyle = isEmpty(moreStyle) ? tableStyle : Object.assign(tableStyle, moreStyle);
    const tableId = makeHiPSSurveysTableName(hipsId, isPopular);
    let   tableModel = getTblById(tableId);

    if (!tableModel && isPopular) {
        tableModel = getPopularHiPSTable(hipsId, hipsUrl);
    } else if (tableModel && hipsUrl) {
        // for first time table display
        tableModel.highlightedRow = indexInHiPSSurveys(tableModel, hipsUrl);
    }

    return (tableModel && !tableModel.error) ? (
        <div style={surveyTableStyle}>
            <TablePanel
                key={tableModel.tbl_id}
                tbl_ui_id = {tableModel.tbl_id + '-ui'}
                tableModel={tableModel}
                height={'calc(100%)'}
                showToolbar={false}
                selectable={false}
                showFilters={isPopular ? false : true}
                showOptionButton={true}
            />
        </div>) :
        (<div style={{display:'flex', justifyContent: 'center', alignItems: 'center', padding: 10}}>
               {getHiPSLoadingMessage(tableId) || 'HiPS surveys are not found'}
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
    const loading = () => {
        return (
            <div style={{width: '100%', height: '100%', display:'flex', justifyContent: 'center', alignItems: 'center'}}>
                <img style={{width:14,height:14}} src={LOADING}/>
            </div>
        );
    };

    const showHiPSSurvey = () => {
        return renderHiPSSurveysTable(id, (popularHiPS === HiPSPopular), hipsUrl);
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


    const startHiPSPopup = () => {
        const plot = pv ? primePlot(pv) : primePlot(visRoot());
        const popup = (
            <PopupPanel title={'HiPS Surveys'}>
                <div style={popupPanelResizableStyle}>
                    <HiPSSurveyListSelection
                        surveysId={surveysId}
                        wrapperStyle={{height: 'calc(100% - 60px)'}}
                        hipsUrl={hipsUrl}
                        dataType={dataType}
                    />

                    <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 15, marginBottom: 15}}>
                        <div style={{display: 'flex', alignItems: 'flexStart', marginLeft: 10}}>
                            <div style={{marginRight: 10}}>
                                <CompleteButton
                                    onSuccess={onSelectPlot(surveysId, plot)}
                                    text={'Search'}
                                    dialogId={hipsSurveysPopupId}
                                    groupKey={gKeyHiPSPanel}
                                />
                            </div>
                            <div>
                                <button type='button' className='button std hl'
                                        onClick={resultCancel(surveysId, plot)}>Cancel
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

    if (hipsUrl) {
        updateHiPSTblHighlightOnUrl(hipsUrl, surveysId);
    }
    startHiPSPopup();
}

export const gKeyHiPSPanel = 'HIPSList_PANEL';
export const fKeyHiPSPopular = 'popularHiPS';

function getHiPSPopularSetting(hipsUrl) {
    let pSetting = getFieldVal(gKeyHiPSPanel, fKeyHiPSPopular);

    if (isUndefined(pSetting)) {
        pSetting = (!hipsUrl || isOnePopularSurvey(hipsUrl)) ? HiPSPopular : '';
        // pSetting = HiPSPopular;
    }

    return pSetting;
}

export function isPopularHiPSChecked() {
    return getFieldVal(gKeyHiPSPanel, fKeyHiPSPopular) === HiPSPopular;
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
        const hipsSurveys = getHiPSSurveys(makeHiPSSurveysTableName(this.props.surveysId));
        // no surveys in the store yet
        if (!hipsSurveys) {
            const {dataType} = this.props;

            onHiPSSurveys({dataTypes: dataType, id: this.props.surveysId});
        }
        // if HiPS table is created by server
        this.setState({isUpdatingHips: isLoadingHiPSSurverys(makeHiPSSurveysTableName(this.props.surveysId))});

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
            const pSetting = getFieldVal(gKeyHiPSPanel, fKeyHiPSPopular);
            const isUpdatingHips = isLoadingHiPSSurverys(makeHiPSSurveysTableName(surveysId));

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

HiPSSurveyListSelection.defaultProps={
    dataType: HiPSData
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

export function getTblModelOnPanel(surveysId) {
    const isPopular = isPopularHiPSChecked();
    const tblId = makeHiPSSurveysTableName(surveysId, isPopular);

    return tblId ? getTblById(tblId) : null;
}

function onSelectPlot(surveysId, plot) {
    return (request) => {
        const isPopular = (get(request, fKeyHiPSPopular) === HiPSPopular);
        const tblId = makeHiPSSurveysTableName(surveysId, isPopular);
        const tableModel = getTblById(tblId);
        if (!tableModel) {
            return;
        }


        const rootUrl = getCellValue(tableModel, get(tableModel, 'highlightedRow', 0), HiPSSurveyTableColumn.Url.key);

        if (rootUrl) {
            // update the table highlight of the other one which is not shown in table panel
            updateHiPSTblHighlightOnUrl(rootUrl, surveysId, !isPopular);
            dispatchChangeHiPS({plotId: plot.plotId, hipsUrlRoot: rootUrl});
        }
    };
}

export function HiPSPopupMsg(msg, title) {
    showInfoPopup(msg, title);
}

function resultCancel(surveysId, plot) {
    return () => {
        const rootUrl = get(plot, 'hipsUrlRoot');

        // reset the highlight of HiPS tables based on current plot
        if (rootUrl) {
            updateHiPSTblHighlightOnUrl(rootUrl, surveysId);
        }
        dispatchHideDialog(hipsSurveysPopupId);
        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
    };
}
