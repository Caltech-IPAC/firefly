/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pick, get, isEmpty, set, cloneDeep} from 'lodash';
import SplitPane from 'react-split-pane';
import {flux} from '../../Firefly.js';
import {LO_VIEW, getLayouInfo, dispatchUpdateLayoutInfo} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {VisToolbar} from '../../visualize/ui/VisToolbar.jsx';
import {LcImageViewerContainer} from './LcImageViewerContainer.jsx';
import {SplitContent} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC, updateLayoutDisplay} from './LcManager.js';
import {getTypeData, ReadOnlyText, highlightBorder} from './LcPeriod.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {LcImageToolbar} from './LcImageToolbar.jsx';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {getTblById, doFetchTable, isTblDataAvail, MAX_ROW} from '../../tables/TableUtil.js';
import {dispatchMultiValueChange, dispatchRestoreDefaults}  from '../../fieldGroup/FieldGroupCntlr.js';
import {logError} from '../../util/WebUtil.js';

const resultItems = ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'searchDesc', 'images',
                     LC.MISSION_DATA, LC.GENERAL_DATA, 'periodState'];
const labelWidth = 80;

const cTimeSeriesKeyDef = {
    time: {fkey: LC.META_TIME_CNAME, label: 'Time Column'},
    flux: {fkey: LC.META_FLUX_CNAME, label: 'Flux Column'},
    timecols: {fkey: LC.META_TIME_NAMES, label: ''},
    fluxcols: {fkey: LC.META_FLUX_NAMES, label: ''},
    cutoutsize: {fkey: 'cutoutSize', label: 'Cutout Size (deg)'},
    errorcolumn: {fkey: LC.META_ERR_CNAME, label: 'Error Column'}
};

// defValues used to keep the initial values for parameters in the field group of result page
// time: time column
// flux: flux column
// timecols:  time column candidates
// fluxcols:  flux column candidates
// errorcolumm: error column
// cutoutsize: image cutout size
const defValues = {
    [cTimeSeriesKeyDef.time.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.time.fkey, '',
                                                'time column name',
                                                `${cTimeSeriesKeyDef.time.label}:`, labelWidth),
                                                {validator: null}),
    [cTimeSeriesKeyDef.flux.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.flux.fkey, '',
                                                'flux column name',
                                                `${cTimeSeriesKeyDef.flux.label}:`, labelWidth),
                                                {validator: null}),
    [cTimeSeriesKeyDef.timecols.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.timecols.fkey, '',
                                                'time column suggestion'),
                                                {validator: null}),
    [cTimeSeriesKeyDef.fluxcols.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.fluxcols.fkey, '',
                                                'flux column suggestion'),
                                                {validator: null}),
    [cTimeSeriesKeyDef.cutoutsize.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.cutoutsize.fkey, '',
                                                'image cutout size',
                                                `${cTimeSeriesKeyDef.cutoutsize.label}:`, 100)),
    [cTimeSeriesKeyDef.errorcolumn.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.errorcolumn.fkey, '',
                                                'flux error column name',
                                                `${cTimeSeriesKeyDef.errorcolumn.label}:`, labelWidth))
    };



export class LcResult extends Component {

    constructor(props) {
        super(props);

        this.state = Object.assign({}, pick(getLayouInfo(), resultItems));
    }


    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const nextState = pick(getLayouInfo(), resultItems);
            this.setState(nextState);
        }
    }

    render() {
        const {title, mode, showTables, showImages, showXyPlots, searchDesc, images,
                missionEntries, generalEntries, periodState} = this.state;
        var {expanded, standard} = mode || {};
        const content = {};
        var visToolbar;
        if (showImages) {
            visToolbar = <VisToolbar key='res-vis-tb'/>;
            content.imagePlot = (<LcImageViewerContainer key='res-images'
                                        viewerId={LC.IMG_VIEWER_ID}
                                        closeable={true}
                                        forceRowSize={1}
                                        imageExpandedMode={expanded===LO_VIEW.images}
                                        Toolbar={LcImageToolbar}
                                        {...images}  />);
        }
        if (showXyPlots) {
            content.xyPlot = (<ChartsContainer key='res-charts'
                                        closeable={true}
                                        expandedMode={expanded===LO_VIEW.xyPlots}/>);
        }
        if (showTables) {
            content.tables = (<TablesContainer key='res-tables'
                                        mode='both'
                                        closeable={true}
                                        expandedMode={expanded===LO_VIEW.tables}/>);
        }

        content.settingBox = (<SettingBox generalEntries={generalEntries} missionEntries={missionEntries}
                                         periodState={periodState}/>);

        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        const expandedProps =  {expanded, ...content};
        const standardProps =  {visToolbar, title, searchDesc, standard, ...content};
        
        return (
            expanded === LO_VIEW.none
                ? <StandardView key='res-std-view' {...standardProps} />
                : <ExpandedView key='res-exp-view' {...expandedProps} />
        );
    }
}


// eslint-disable-next-line
const ExpandedView = ({expanded, imagePlot, xyPlot, tables}) => {
    const view = expanded === LO_VIEW.tables ? tables
        : expanded === LO_VIEW.xyPlots ? xyPlot
        : imagePlot;
    return (
        <div style={{ flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'}}>{view}</div>
    );
};

const buttonW = 650;

// eslint-disable-next-line
const StandardView = ({visToolbar, title, searchDesc, imagePlot, xyPlot, tables, settingBox}) => {

    const {cutoutSize} = settingBox.props.generalEntries || '0.3';
    //let csize = get(generalEntries, 'cutoutsize, '0.3');

    return (
        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, position: 'relative'}}>
            { visToolbar &&
                <div style={{display: 'inline-flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div>{visToolbar}</div>
                    <div>
                        <DownloadButton>
                            <DownloadOptionPanel
                                cutoutSize = {cutoutSize}
                                dlParams = {{
                                    MaxBundleSize: 200*1024*1024,    // set it to 200mb to make it easier to test multi-parts download.  each wise image is ~64mb
                                    FilePrefix: 'WISE_Files',
                                    BaseFileName: 'WISE_Files',
                                    DataSource: 'WISE images',
                                    FileGroupProcessor: 'LightCurveFileGroupsProcessor'
                                }}>
                                <ValidationField
                                        initialState= {{
                                               value: 'A sample download',
                                               label : 'Title for this download:'
                                                   }}
                                        fieldKey='Title'
                                        labelWidth={110}/>
                            </DownloadOptionPanel>
                        </DownloadButton>
                    </div>
                </div>
            }
            {searchDesc}
            {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
            <div style={{flexGrow: 1, position: 'relative'}}>
                <div style={{position: 'absolute', top: 0, right: 0, bottom: 0, left: 0}}>
                    <SplitPane split='horizontal' maxSize={-20} minSize={20}  defaultSize={'60%'}>
                        <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={buttonW}>
                            <SplitContent>
                                <div style={{display: 'flex', flexDirection: 'column', height: '100%'}}>
                                    <div className='settingBox'>{settingBox}</div>
                                    <div style={{flexGrow: 1, position: 'relative'}}><div className='abs_full'>{tables}</div></div>
                                </div>
                            </SplitContent>
                            <SplitContent>{xyPlot}</SplitContent>
                        </SplitPane>
                        <SplitContent>{imagePlot}</SplitContent>
                    </SplitPane>
                </div>
            </div>
        </div>
    );
};

const missionKeys = [cTimeSeriesKeyDef.time.fkey, cTimeSeriesKeyDef.flux.fkey];
const missionOtherKeys = [cTimeSeriesKeyDef.errorcolumn.fkey];
const missionListKeys = [cTimeSeriesKeyDef.timecols.fkey, cTimeSeriesKeyDef.fluxcols.fkey];


class SettingBox extends Component {
    constructor(props) {
        super(props);

        var fields = FieldGroupUtils.getGroupFields(LC.FG_VIEWER_FINDER);
        this.state = {fields};
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;

        this.unbinder = FieldGroupUtils.bindToStore(LC.FG_VIEWER_FINDER, (fields) => {
            if (this.iAmMounted && fields !== this.state.fields) {
                this.setState(fields);
            }
        });
    }

    render() {
        var {generalEntries, missionEntries, periodState} = this.props;

        if (isEmpty(generalEntries) || isEmpty(missionEntries)) return false;
        const wrapperStyle = {margin: '3px 0'};

        var allCommonEntries = Object.keys(generalEntries).map((key) =>
                                    <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
                                                     style={{width: 80}}/>
                                );

        var missionInputs = missionKeys.map((key, index) =>
                                    <SuggestBoxInputField key={key} fieldKey={key} wrapperStyle={wrapperStyle}
                                                          getSuggestions={(val) => {
                                                                    const list = get(missionEntries, missionListKeys[index], []);
                                                                    const suggestions =  list && list.filter((el) => {return el.startsWith(val);});
                                                                    return suggestions.length > 0 ? suggestions : missionListKeys[index];
                                                              }}/>
                                );

        var missionOthers = missionOtherKeys.map((key) =>
                                    <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}/>
                                );

        //var moveToPeriod = (periodState) => {
        //    return () => {
        //        updateLayoutDisplay(periodState);
        //    };
        //};

        return (
            <FieldGroup groupKey={LC.FG_VIEWER_FINDER} style={{position: 'relative', display: 'inline-flex'}}
                        reducerFunc={timeSeriesReducer(missionEntries, generalEntries)} keepState={true}>
                <div style={{alignSelf: 'flex-end'}}>
                    <ReadOnlyText label='Mission:' content={get(missionEntries, LC.META_MISSION, '')}
                                  labelWidth={labelWidth} wrapperStyle={{margin: '3px 0 6px 0'}}/>
                    {missionInputs}
                    {missionOthers}
                </div>
                <div style={{alignSelf: 'flex-end'}}>
                    {allCommonEntries}
                </div>

                <div style={{alignSelf: 'flex-end', marginLeft: 10}}>
                    <CompleteButton
                        groupKey={LC.FG_VIEWER_FINDER}
                        onSuccess={setViewerSuccess(periodState)}
                        onFail={setViewerFail()}
                        text={'Period Finding'}
                    />
                </div>
            </FieldGroup>
        );
    }
}

SettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object,
    periodState:    PropTypes.string
};

var timeSeriesReducer = (missionEntries, generalEntries) => {
    return (inFields, action) => {
        if (inFields) {
            return inFields;
        }

        var   defV = Object.assign({}, defValues);

        missionListKeys.forEach((key) => {
            set(defV, [key, 'value'], get(missionEntries, key, []));
        });

        // set value and validator
        missionKeys.forEach((key, idx) => {
            set(defV, [key, 'value'], get(missionEntries, key, ''));
            set(defV, [key, 'validator'], (val) => {
                let retVal = {valid: true, message: ''};
                        const cols = get(missionEntries, missionListKeys[idx], []);

                if (cols.length !== 0 && !cols.includes(val)) {
                    retVal = {valid: false, message: `${val} is not a valid column name`};
                }

                return retVal;
            });
        });
        Object.keys(generalEntries).forEach((key) => {
            set(defV, [key, 'value'], get(generalEntries, key, ''));
        });
        return defV;
    };
};

/**
 * @summary callback to go to period finding page
 * @param {string} periodState
 * @returns {Function}
 */
function setViewerSuccess(periodState) {
    return (request) => {
        updateFullRawTable(()=>updateLayoutDisplay(periodState));
    };
}

function setViewerFail() {
    return (request) => {
        return showInfoPopup('Parameter setting error');
    };
}

function updateFullRawTable(callback) {
    const layoutInfo = getLayouInfo();
    const tableItems = ['tableData', 'tableMeta'];

    // fullRawTable for the derivation of other table, like phase folded table
    var setTableData = (tbl) => {
        const fullRawTable = pick(tbl, tableItems);

        // find tzero, tzeroMax, period min, period max from table data
        var {columns, data} = fullRawTable.tableData;
        var tIdx = columns.findIndex((col) => (col.name === get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME])));
        var arr = data.reduce((prev, e)=> {
            prev.push(parseFloat(e[tIdx]));
            return prev;
        }, []);

        var [tzero, tzeroMax] = arr.length > 0 ? [Math.min(...arr), Math.max(...arr)] : [0.0, 0.0];
        var max = 365;
        var min = Math.pow(10, -3);   // 0.001

        var fields = FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER);
        var initState;

        if (fields) {      // fields already exists and new table is loaded
            initState = [
                {fieldKey: 'time', value: get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME])},
                {fieldKey: 'flux', value: get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME])},
                {fieldKey: 'periodMin', value: `${min}`},
                {fieldKey: 'periodMax', value: `${max}`},
                {fieldKey: 'period', value: `${min}`},
                {fieldKey: 'tzero', value: `${tzero}`},
                {fieldKey: 'tzeroMax', value: `${tzeroMax}`}];

            dispatchMultiValueChange(LC.FG_PERIOD_FINDER, initState);
        }
        fields = FieldGroupUtils.getGroupFields(LC.FG_PERIODOGRAM_FINDER);
        if (fields) {
            dispatchRestoreDefaults(LC.FG_PERIODOGRAM_FINDER);
        }

        dispatchUpdateLayoutInfo(Object.assign({}, layoutInfo, {fullRawTable, periodRange: {min, max, tzero, tzeroMax}}));
        callback && callback();
    };

    if (layoutInfo.fullRawTable) {
        callback && callback();
    } else {
        var rawTable = getTblById(LC.RAW_TABLE);

        if (isTblDataAvail(0, rawTable.totalRows, rawTable)) {
            setTableData(rawTable);
        } else {
            var req = Object.assign(cloneDeep(rawTable.request), {pageSize:  MAX_ROW});

            doFetchTable(req).then(
                (tableModel) => {
                    setTableData(tableModel);
                }
            ).catch(
                (reason) => {
                    logError(`Failed to get full raw table: ${reason}`, reason);
                }
            );
        }
    }
}