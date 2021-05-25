import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {isEmpty, get, isUndefined, isNull} from 'lodash';
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
import {isLoadingHiPSSurverys, HiPSId, HiPSData, URL_COL, updateHiPSTblHighlightOnUrl,
        getHiPSSurveys,  getHiPSLoadingMessage, makeHiPSSurveysTableName, getDefHiPSSources,
        loadHiPSSurverysWithHighlight, getHiPSSources, defHiPSSources} from '../visualize/HiPSListUtil.js';
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
 * @param sources
 * @param moreStyle
 * @returns {XML}
 */
function renderHiPSSurveysTable(hipsId, sources, moreStyle={}) {
    const surveyTableStyle = isEmpty(moreStyle) ? tableStyle : Object.assign(tableStyle, moreStyle);
    const tableId = makeHiPSSurveysTableName(hipsId, sources);
    const tableModel = getTblById(tableId);

    return tableModel && (
        <div style={surveyTableStyle}>
            <TablePanel
                key={tableModel.tbl_id}
                tbl_ui_id = {tableModel.tbl_id + '-ui'}
                tableModel={tableModel}
                height={'calc(100%)'}
                showToolbar={false}
                selectable={false}
                showFilters={true}
                showOptionButton={true}
            />
        </div>
    );
}

/**
 * show HiPS survey table or loading sign
 * @param id
 * @param isUpdatingHips
 * @param hipsSources
 * @returns {*}
 */
function showHiPSSurveyList(id, isUpdatingHips, hipsSources) {
    const loading = () => {
        return (
            <div style={{width: '100%', height: '100%', display:'flex', justifyContent: 'center', alignItems: 'center'}}>
                <img style={{width:14,height:14}} src={LOADING}/>
            </div>
        );
    };

    const showHiPSSurvey = () => {
        return renderHiPSSurveysTable(id, hipsSources);
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

    // get the opposite status of HiPS list checkbox if there is single checkbox only
    const getOtherStatus = (sources) => {
        if (defHiPSSources().length === 1) {
            return (sources === getDefHiPSSources()) ? sourcesPerChecked('') : getDefHiPSSources();
        } else {
            return '';
        }
    };

    const startHiPSPopup = () => {
        const plot = pv ? primePlot(pv) : primePlot(visRoot());
        const popup = (
            <PopupPanel title={'Change HiPS Image'} modal={true}>
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
                            <HelpIcon helpId={'visualization.changehips'}/>
                        </div>
                    </div>
                </div>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(hipsSurveysPopupId, popup);
        dispatchShowDialog(hipsSurveysPopupId);
    };

    if (hipsUrl) {
        const sources = sourcesPerChecked();
        updateHiPSTblHighlightOnUrl(hipsUrl, surveysId, sourcesPerChecked());

        const otherStatus = getOtherStatus(sources);
        if (otherStatus) {
            updateHiPSTblHighlightOnUrl(hipsUrl, surveysId, otherStatus);
        }
    }
    startHiPSPopup();
}

export const gKeyHiPSPanel = 'HIPSList_PANEL';
export const fKeyHiPSSources = 'hipsSources';

export function getHiPSSourcesChecked() {
     return getFieldVal(gKeyHiPSPanel, fKeyHiPSSources);
}

/**
 * get the HiPS sources based on current checkbox status or the passed status if there is
 * @param {string} statusCB checkbox status
 * @returns {*}
 */
export function sourcesPerChecked(statusCB=null) {
    const sources = isNull(statusCB) ? getHiPSSourcesChecked() : statusCB.toLowerCase();
    const oneCB = defHiPSSources().length <= 1;

    if (isUndefined(sources)) {
        return getDefHiPSSources();   // get default before rendering
    } else  {
        return (!sources && oneCB) ? getHiPSSources() : sources;  // get all sources for one checkbox
    }
}

/**
 * get HiPS source for the HiPS list selection checkbox - a single one
 * @returns {*}
 */
function initHiPSCheckboxStatus() {
    const defFromPanel =  getHiPSSourcesChecked();

    if (!isUndefined(defFromPanel)) {
        return defFromPanel;
    } else {   // at init stage
        return getDefHiPSSources();
    }
}

/**
 * get table model based on the checkbox status
 * @param surveysId
 * @returns {*}
 */
export function getTblModelOnPanel(surveysId) {
    const tblId = makeHiPSSurveysTableName(surveysId, sourcesPerChecked());

    return tblId ? getTblById(tblId) : null;
}

/**
 * pop-up showing error message for HiPS map search
 * @param msg
 * @param title
 */
export function HiPSPopupMsg(msg, title) {
    showInfoPopup(msg, title);
}

/**
 * show HiPS survey info table plus check box for showing popular surveys in popup panel or form panel
 */
export class HiPSSurveyListSelection extends PureComponent {
    constructor(props) {
        super(props);
        const sources = sourcesPerChecked();
        const {dataType, surveysId, hipsUrl} = props;

        loadHiPSSurverysWithHighlight({dataTypes: dataType, id: surveysId, sources, ivoOrUrl: hipsUrl, columnName: URL_COL});

        this.state = {[fKeyHiPSSources]: initHiPSCheckboxStatus(),
                       isUpdatingHips: isLoadingHiPSSurverys(makeHiPSSurveysTableName(surveysId, sources))
        };
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
            const pSetting = getFieldVal(gKeyHiPSPanel, fKeyHiPSSources);
            const isUpdatingHips = isLoadingHiPSSurverys(makeHiPSSurveysTableName(surveysId,
                                                                                  sourcesPerChecked()));

            if (pSetting !== get(this.state, [fKeyHiPSSources])) {
                this.setState({[fKeyHiPSSources]: pSetting});
            }
            if (isUpdatingHips !== get(this.state, 'isUpdatingHips')) {
                this.setState({isUpdatingHips});
            }
        }
    }

    componentDidUpdate(prevProps, prevState) {
        const {[fKeyHiPSSources]:sources} = this.state;
        const {[fKeyHiPSSources]:preSources} = prevState;
        const sSources = sourcesPerChecked();
        const {dataType, surveysId} = this.props;

        if (sources !== preSources) {
            const tblModel =  getHiPSSurveys(makeHiPSSurveysTableName(surveysId, sourcesPerChecked(sources)));
            const url = tblModel && tblModel.tableData ?
                                    getCellValue(tblModel, tblModel.highlightedRow, URL_COL) : this.props.hipsUrl;

            loadHiPSSurverysWithHighlight({dataTypes: dataType, id: surveysId, sources: sSources, ivoOrUrl: url,
                                           columnName: URL_COL});
        }
    }

    render() {
        const {surveysId, wrapperStyle} = this.props;
        const {isUpdatingHips, [fKeyHiPSSources]: hipsSources} = this.state;
        const optionsMenu = getOptionsMenu();

        return (
            <div style={wrapperStyle}>
                <FieldGroup groupKey={gKeyHiPSPanel} validatorFunc={null} keepState={true}
                            reducerFunc={fieldReducer(hipsSources)}
                            style={{height: '100%', width: '100%'}}>
                   <div style={{height: 20, width: '100%'}}>
                       <CheckboxGroupInputField
                            fieldKey={fKeyHiPSSources}
                            options={optionsMenu}
                            alignment='horizontal'
                            wrapperStyle={{textAlign: 'center'}}
                        />
                    </div>
                    {showHiPSSurveyList(surveysId, isUpdatingHips, sourcesPerChecked())}
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

/**
 * menu for HiPS list selection checkbox
 * @returns {*}
 */
function getOptionsMenu(){
    const defSourceInfo = defHiPSSources();

    return defSourceInfo.map((oneSource) => {
        return {label: oneSource.label, value: oneSource.source};
    });
}


function fieldReducer(defSources) {
    return (inFields) => {
        if (!inFields) {
            return {[fKeyHiPSSources]: {
                fieldKey: fKeyHiPSSources,
                value: defSources,
                tooltip: 'HiPS sources'
            }};
        }
        return inFields;
    };
}


function onSelectPlot(surveysId, plot) {
    return (request) => {
        const sources = sourcesPerChecked();
        if (!sources) {
            HiPSPopupMsg('No HiPS source selected', 'HiPS search');
            return;
        }

        const tblId = makeHiPSSurveysTableName(surveysId, sources);
        const tableModel = getTblById(tblId);
        if (!tableModel) {
            HiPSPopupMsg('no table with id ' + tblId + ' is found', 'HiPS search');
            return;
        }

        const rootUrl = getCellValue(tableModel, get(tableModel, 'highlightedRow', 0), URL_COL);

        if (rootUrl) {
            // update the table highlight of the other one which is not shown in table panel
            dispatchChangeHiPS({plotId: plot.plotId, hipsUrlRoot: rootUrl});
        }
    };
}



function resultCancel(surveysId, plot) {
    return () => {
        const rootUrl = get(plot, 'hipsUrlRoot');

        // reset the highlight of HiPS tables based on current plot
        if (rootUrl) {
            updateHiPSTblHighlightOnUrl(rootUrl, surveysId, sourcesPerChecked());
        }
        dispatchHideDialog(hipsSurveysPopupId);
        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
    };
}
