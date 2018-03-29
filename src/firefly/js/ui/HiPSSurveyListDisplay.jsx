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
import {onHiPSSurveys, isLoadingHiPSSurverys, HiPSId, HiPSData, HiPSSurveyTableColumn, HiPSSources,
        getHiPSSurveys,  getHiPSLoadingMessage, makeHiPSSurveysTableName,
        updateHiPSTblHighlightOnUrl, getHiPSSources, defHiPSSources} from '../visualize/HiPSListUtil.js';
import {FieldGroup} from './FieldGroup.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {getFieldVal} from '../fieldGroup/FieldGroupUtils.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {ServerParams} from '../data/ServerParams.js';

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

    return (tableModel && !tableModel.error) ? (
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
        </div>) :
        (<div style={{display:'flex', justifyContent: 'center', alignItems: 'center', padding: 10}}>
               {getHiPSLoadingMessage(tableId)}
        </div>);
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


    const startHiPSPopup = () => {
        const plot = pv ? primePlot(pv) : primePlot(visRoot());
        const popup = (
            <PopupPanel title={'Change HiPS Image'}>
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
                            <HelpIcon helpId={'visualization.hipsViewer'}/>
                        </div>
                    </div>
                </div>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(hipsSurveysPopupId, popup);
        dispatchShowDialog(hipsSurveysPopupId);
    };

    if (hipsUrl) {
        updateHiPSTblHighlightOnUrl(hipsUrl, surveysId, getDefaultHiPSSources());
    }
    startHiPSPopup();
}

export const gKeyHiPSPanel = 'HIPSList_PANEL';
export const fKeyHiPSSources = 'hipsSources';

export function getHiPSSourcesChecked() {
     return getFieldVal(gKeyHiPSPanel, fKeyHiPSSources);
}
/**
 * show HiPS survey info table plus check box for showing popular surveys in popup panel or form panel
 */
export class HiPSSurveyListSelection extends PureComponent {
    constructor(props) {
        super(props);

        this.state = {[fKeyHiPSSources]: getDefaultHiPSSources()};
    }

    componentWillMount() {
        const {[fKeyHiPSSources]:sources} = this.state;
        const hipsSurveys = getHiPSSurveys(makeHiPSSurveysTableName(this.props.surveysId, sources));
        // no surveys in the store yet
        if (!hipsSurveys) {
            const {dataType} = this.props;

            onHiPSSurveys({dataTypes: dataType, id: this.props.surveysId, sources});
        }
        // if HiPS table is created by server
        this.setState({isUpdatingHips: isLoadingHiPSSurverys(makeHiPSSurveysTableName(this.props.surveysId, sources))});

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
            const isUpdatingHips = isLoadingHiPSSurverys(makeHiPSSurveysTableName(surveysId, pSetting));

            if (pSetting !== get(this.state, [fKeyHiPSSources])) {
                this.setState({[fKeyHiPSSources]: pSetting});
            }
            if (isUpdatingHips !== get(this.state, 'isUpdatingHips')) {
                this.setState({isUpdatingHips});
            }
        }
    }

    componentDidUpdate(prevProps, prevState) {
        const {[fKeyHiPSSources]:sources, isUpdatingHips} = this.state;


        const hipsSurveys = getHiPSSurveys(makeHiPSSurveysTableName(this.props.surveysId, sources));
        // no surveys in the store yet
        if (!hipsSurveys && !isUpdatingHips) {
            const {dataType} = this.props;

            onHiPSSurveys({dataTypes: dataType, id: this.props.surveysId, sources});
        }
    }

    render() {
        const {surveysId, wrapperStyle} = this.props;
        const {isUpdatingHips, [fKeyHiPSSources]: hipsSources} = this.state;
        const optionsMenu = getOptionsMenu(getHiPSSources());

        return (
            <div style={wrapperStyle}>
                <FieldGroup groupKey={gKeyHiPSPanel} validatorFunc={null} keepState={true}
                            reducerFunc={fieldReducer(hipsSources)}
                            style={{height: '100%', width: '100%'}}>
                   <CheckboxGroupInputField
                        fieldKey={fKeyHiPSSources}
                        options={optionsMenu}
                        alignment='horizontal'
                        wrapperStyle={{textAlign: 'center'}}
                    />
                    {showHiPSSurveyList(surveysId, isUpdatingHips, hipsSources)}
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

function getAllSources(sources) {
    let  hipsSources = sources ? sources : getHiPSSources();

    if ((!hipsSources) || hipsSources.toLowerCase() === ServerParams.ALL.toLowerCase()) {
        hipsSources= HiPSSources;
    }
    return hipsSources;
}

function getOptionsMenu(sources){
    const hipsSources = getAllSources(sources);

    return hipsSources.split(',').filter((oneSource) => oneSource.trim())
                                 .map((s) => ({label: s.trim().toUpperCase(), value: s.trim().toLowerCase()}));

}

function getDefaultHiPSSources() {
    const defFromPanel =  getHiPSSourcesChecked();

    if (!isUndefined(defFromPanel)) {
        return defFromPanel;
    } else {
        const allSources = getAllSources().split(',')
            .filter((s) => s.trim())
            .map((s) => s.trim().toLowerCase());  // value in lower case

        const defSources = defHiPSSources();

        if (!defSources) {
            return allSources[0];
        } else if (defSources.toLowerCase() === ServerParams.ALL.toLowerCase()) {
            return allSources.join(',');
        } else {
            return defSources.split(',')
                .filter((oneSource) => oneSource.trim() && allSources.includes(oneSource.trim().toLowerCase()))
                .map((s) => s.trim().toLowerCase())
                .join(',');
        }
    }
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

export function getTblModelOnPanel(surveysId) {
    const sources = getHiPSSourcesChecked();
    const tblId = sources ? makeHiPSSurveysTableName(surveysId, sources) : null;

    return tblId ? getTblById(tblId) : null;
}

function onSelectPlot(surveysId, plot) {
    return (request) => {
        const sources = getHiPSSourcesChecked();
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

        const rootUrl = getCellValue(tableModel, get(tableModel, 'highlightedRow', 0), HiPSSurveyTableColumn.Url.key);

        if (rootUrl) {
            // update the table highlight of the other one which is not shown in table panel
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
            updateHiPSTblHighlightOnUrl(rootUrl, surveysId, getHiPSSourcesChecked());
        }
        dispatchHideDialog(hipsSurveysPopupId);
        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
    };
}
