/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pick, get, isEmpty, set} from 'lodash';
import SplitPane from 'react-split-pane';
import {flux} from '../../Firefly.js';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {VisToolbar} from '../../visualize/ui/VisToolbar.jsx';
import {LcImageViewerContainer} from './LcImageViewerContainer.jsx';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC, updateLayoutDisplay} from './LcManager.js';
import {getTypeData, ReadOnlyText} from './LcPeriod.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {LcImageToolbar} from './LcImageToolbar.jsx';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
const resultItems = ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'searchDesc', 'images',
                     LC.MISSION_DATA, LC.GENERAL_DATA, 'periodState'];
const labelWidth = 100;

const cTimeSeriesKeyDef = {
    time: {fkey: LC.META_TIME_CNAME, label: 'Time Column'},
    flux: {fkey: LC.META_FLUX_CNAME, label: 'Flux Column'},
    timecols: {fkey: LC.META_TIME_NAMES, label: ''},
    fluxcols: {fkey: LC.META_FLUX_NAMES, label: ''},
    cutoutsize: {fkey: 'cutoutSize', label: 'Cutout Size (deg)'},
    errorcolumn: {fkey: 'errorColumn', label: 'Error Column'}
};

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
                                                `${cTimeSeriesKeyDef.cutoutsize.label}:`, labelWidth)),
    [cTimeSeriesKeyDef.errorcolumn.fkey]: Object.assign(getTypeData(cTimeSeriesKeyDef.errorcolumn.fkey, '',
                                                'flux column name',
                                                `${cTimeSeriesKeyDef.errorcolumn.label}:`, labelWidth),
                                                {validator: null})
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


// eslint-disable-next-line
const StandardView = ({visToolbar, title, searchDesc, imagePlot, xyPlot, tables, settingBox}) => {

    return (
        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, position: 'relative'}}>
            { visToolbar &&
                <div style={{display: 'inline-flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div>{visToolbar}</div>
                    <div>
                        <DownloadButton>
                            <DownloadOptionPanel
                                cutoutSize = '200'
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
                    <SplitPane split='horizontal' minSize={20}  defaultSize={'60%'}>
                        <SplitPane split='vertical' minSize={20} defaultSize={'50%'}>
                            <SplitPane split='horizontal' minSize={20} defaultSize={100}>
                                {createContentWrapper(settingBox)}
                                {createContentWrapper(tables)}
                            </SplitPane>
                            {createContentWrapper(xyPlot)}
                        </SplitPane>
                        {createContentWrapper(imagePlot)}
                    </SplitPane>
                </div>
            </div>
        </div>
    );
};

const missionKeys = [cTimeSeriesKeyDef.time.fkey, cTimeSeriesKeyDef.flux.fkey];
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
        const wrapperStyle = {marginLeft: 10, marginTop: 5};

        var allCommonEntries = () => {
            return Object.keys(generalEntries).map((key) => {
                return (
                    <ValidationField key={key} fieldKey={key} wrapperStyle={wrapperStyle}/>
                );
            });
        };


        var allMissionEntries = () => {
            var missionInputs = missionKeys.map((key, index) => {
                return (<div style={wrapperStyle} key={key}>
                    <SuggestBoxInputField fieldKey={key} wrapperStyle={wrapperStyle}
                                          getSuggestions={(val) => {
                                                        const list = get(missionEntries, missionListKeys[index], []);
                                                        const suggestions =  list && list.filter((el) => {return el.startsWith(val);});

                                                        return suggestions.length > 0 ? suggestions : missionListKeys[index];
                                                  }}/>
                </div>);
            });

            return (<div>
                {ReadOnlyText({
                    label: 'Mission:', labelWidth,
                    content: get(missionEntries, LC.META_MISSION, ''), wrapperStyle
                })}
                {missionInputs}
            </div>);
        };

        var moveToPeriod = (periodState) => {
            return () => {
                updateLayoutDisplay(periodState);
            };
        };

        return (
            <div>
                <FieldGroup groupKey={LC.FG_VIEWER_FINDER}
                            reducerFunc={timeSeriesReducer(missionEntries, generalEntries)} keepState={true}>
                    <div style={{display: 'flex', alignItems: 'flex-end'}}>
                        {allMissionEntries()}
                        <div>
                            {allCommonEntries()}
                        </div>
                        <div>
                            <button type='button' className={'button std'} style={{width: 90, marginLeft: 10}}
                                    onClick={moveToPeriod(periodState)}> Period Finding
                            </button>
                        </div>
                    </div>
                </FieldGroup>
            </div>
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
                    return Object.assign({}, inFields);
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

