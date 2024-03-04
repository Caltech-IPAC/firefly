/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Button, Card, Divider, Sheet, Stack} from '@mui/joy';
/**
 *  2/20/2018 LZ
 *  IRSA-663
 *
 *  Display meaningful downloaded file names
 *  Remove redundant codes
 *  Fixed the bug in DownloadDialog.jsx
 *
 */
import React, {PureComponent, useContext} from 'react';
import PropTypes from 'prop-types';
import {pick, get, isEmpty, cloneDeep} from 'lodash';
import SplitPane from 'react-split-pane';
import {flux} from '../../core/ReduxFlux.js';
import {LO_VIEW, getLayouInfo, dispatchUpdateLayoutInfo} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {LcImageViewerContainer} from './LcImageViewerContainer.jsx';
import {SplitContent} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC, getViewerGroupKey, updateLayoutDisplay} from './LcManager.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {DownloadOptionPanel, DownloadButton} from '../../ui/DownloadDialog.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {getTblById, doFetchTable, isTblDataAvail} from '../../tables/TableUtil.js';
import {MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchMultiValueChange, dispatchRestoreDefaults}  from '../../fieldGroup/FieldGroupCntlr.js';
import {logger} from '../../util/Logger.js';
import {getConverter, getMissionName} from './LcConverterFactory.js';
import {convertAngle} from '../../visualize/VisUtil.js';

const resultItems = () => {
    return ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'searchDesc', 'images',
            LC.MISSION_DATA, LC.GENERAL_DATA, 'periodState'];
};


export class LcResult extends PureComponent {

    constructor(props) {
        super(props);

        this.state = Object.assign({}, pick(getLayouInfo(), resultItems()));
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
            const nextState = pick(getLayouInfo(), resultItems());
            this.setState(nextState);
        }
    }

    render() {
        const {title, mode, showTables, showImages, showXyPlots, searchDesc, images,
            missionEntries, generalEntries, periodState} = this.state;

        const converterId = missionEntries?.[LC.META_MISSION];
        const convertData = getConverter(converterId);
        const downloaderOptPanel = convertData.downloadOptions || defaultDownloadPanel;
        var {expanded, standard} = mode || {};
        const content = {};
        const cutoutSize = convertData?.noImageCutout ? undefined : generalEntries.cutoutSize ?? 5;
        const cutoutSizeInDeg = convertAngle('arcmin','deg', cutoutSize).toString();
        const mission = getMissionName(converterId) || 'Mission';
        var visToolbar;
        if (showImages) {
            content.imagePlot = (<LcImageViewerContainer key='res-images'
                                                         viewerId={LC.IMG_VIEWER_ID}
                                                         closeable={true}
                                                         forceRowSize={1}
                                                         imageExpandedMode={expanded===LO_VIEW.images}
                                                         activeTableId={images.activeTableId} />);
        }
        if (showXyPlots) {
            content.xyPlot = (<ChartsContainer key='res-charts'
                                               tbl_group='main'
                                               closeable={true}
                                               expandedMode={expanded===LO_VIEW.xyPlots}
            />);
        }
        if (showTables) {
            content.tables = (<TablesContainer key='res-tables'
                                               mode='both'
                                               closeable={true}
                                               expandedMode={expanded===LO_VIEW.tables}
                                               tableOptions={{help_id:'main1TSV.table'}}/>);
        }


        content.settingBox = (<SettingBox generalEntries={generalEntries}
                                          missionEntries={missionEntries}
                                          downloadButton= {downloaderOptPanel(mission, cutoutSizeInDeg)}
                                          periodState={periodState}  />);


        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        const expandedProps = {expanded, ...content};
        const standardProps = {visToolbar, title, searchDesc, standard, ...content};

        return (
            expanded === LO_VIEW.none
                ? <StandardView key='res-std-view' {...standardProps} />
                : <ExpandedView key='res-exp-view' {...expandedProps} />
        );
    }
}

export function defaultDownloadPanel(mission='', cutoutSize, addtlParams={}) {
    mission = mission.replace(/[\/ ]/g, '_');       // clean up mission description to be used for save as value.
    return (
        <DownloadButton
            makeButton={(onClick,tbl_id,isRowSelected) => {
                return (
                    <Button {...{size: 'md', variant: isRowSelected ? 'solid' : 'soft', color: 'warning', onClick}} >
                        Prepare Download
                    </Button>
                );
            }}
            >
            <DownloadOptionPanel
                groupKey = {mission}
                cutoutSize={cutoutSize}
                title={'Image Download Options'}
                dlParams={{
                    MaxBundleSize: 200 * 1024 * 1024,    // set it to 200mb to make it easier to test multi-parts download.  each wise image is ~64mb
                    TitlePrefix: mission,
                    BaseFileName: `${mission}_Files`,
                    DataSource: `${mission} images`,
                    FileGroupProcessor: 'LightCurveFileGroupsProcessor',
                    ...addtlParams
                }}/>
        </DownloadButton>
    );
}


const ExpandedView = ({expanded, imagePlot, xyPlot, tables}) => {
    const view = expanded === LO_VIEW.tables ? tables
        : expanded === LO_VIEW.xyPlots ? xyPlot
        : imagePlot;
    return (
        <div style={{ flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'}}>{view}</div>
    );
};

const buttonW = 650;

const StandardView = ({visToolbar, title, searchDesc, imagePlot, xyPlot, tables, settingBox}) => {

    const {landingPage}= useContext(AppPropertiesCtx);

    if (!tables) {
        return landingPage ?? <Box mt={10} ml={10}>Landing page here</Box>;
    }

    const converterId = settingBox?.props?.missionEntries?.[LC.META_MISSION];
    const convertData = getConverter(converterId);
    const cutoutSize = get(convertData, 'noImageCutout') ? undefined : get(settingBox, 'props.generalEntries.cutoutSize', '5');
    const mission = getMissionName(converterId) || 'Mission';
    const showImages = isEmpty(imagePlot);

    // convert the default Cutout size in arcmin to deg for WebPlotRequest, expected to be string in download panel
    const cutoutSizeInDeg = (convertAngle('arcmin','deg', cutoutSize)).toString();
    // const downloaderOptPanel = convertData.downloadOptions || defaultDownloadPanel;

    const tsView = (err) => {

        if (!err) {
            return (
                <SplitPane split='horizontal' maxSize={-20} minSize={20} defaultSize={'60%'}>
                    <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={buttonW}>
                        <SplitContent>
                            <Stack {...{height: '100%', spacing:1}}>
                                {settingBox}
                                <Box {...{flexGrow: 1, position: 'relative'}}>
                                    <div className='abs_full'>{tables}</div>
                                </Box>
                            </Stack>
                        </SplitContent>
                        <SplitContent>
                            <Sheet variant='outlined' sx={ (theme) => ({ml:1/2, height:1, borderRadius:theme.radius.md})}>
                                <Stack sx={{height:1, width:1, p:1/4}}>
                                    {xyPlot}
                                </Stack>
                            </Sheet>
                        </SplitContent>
                    </SplitPane>
                    <SplitContent>{imagePlot}</SplitContent>
                </SplitPane>
            );
        } else {
            return (
                <SplitPane split='vertical' maxSize={-20} minSize={20} defaultSize={buttonW}>
                    <SplitContent>
                        <div style={{display: 'flex', flexDirection: 'column', height: '100%'}}>
                            <div className='settingBox'>{settingBox}</div>
                            <div style={{flexGrow: 1, position: 'relative'}}>
                                <div className='abs_full'>{tables}</div>
                            </div>
                        </div>
                    </SplitContent>
                    <SplitContent>{xyPlot}</SplitContent>
                </SplitPane>
            );
        }

    };
    return (
        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, position: 'relative'}}>
            <div style={{flexGrow: 1, position: 'relative'}}>
                <div style={{position: 'absolute', top: 0, right: 0, bottom: 0, left: 0}}>
                    {tsView(showImages)}
                </div>
            </div>
        </div>
    );
};


class SettingBox extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {generalEntries, missionEntries, periodState, downloadButton} = this.props;

        if (isEmpty(generalEntries) || isEmpty(missionEntries)) return false;

        const converterId = get(missionEntries, LC.META_MISSION);
        const converterData = converterId && getConverter(converterId);
        if (!converterId || !converterData) {
            return null;
        }
        const {MissionOptions} = converterData;

        const groupKey = getViewerGroupKey(missionEntries);
        return (
            <Card>
                <Stack {...{spacing:2}}>
                    <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'center'}}>
                        <Stack {...{direction:'row', spacing:2, alignItems:'center' }}>
                            <CompleteButton groupKey={groupKey} onSuccess={setViewerSuccess(periodState)} onFail={setViewerFail()}
                                            text='Period Finder...' />
                            {downloadButton}
                        </Stack>
                        <HelpIcon helpId={'main1TSV.settings'}/>
                    </Stack>
                    <Divider orientation='horizontal'/>
                    <MissionOptions {...{missionEntries, generalEntries}}/>
                </Stack>
            </Card>
        );
    }
}

SettingBox.propTypes = {
    generalEntries: PropTypes.object,
    missionEntries: PropTypes.object,
    downloadButton: PropTypes.object,
    periodState: PropTypes.string
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
    var layoutInfo = getLayouInfo();
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

        // var period = get( FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER), ['period', 'value'], '');

        const period = '';
        var fields = FieldGroupUtils.getGroupFields(LC.FG_PERIOD_FINDER);
        var initState;

        if (fields) {      // fields already exists and new table is loaded
            initState = [
                {fieldKey: 'time', value: get(layoutInfo, [LC.MISSION_DATA, LC.META_TIME_CNAME])},
                {fieldKey: 'flux', value: get(layoutInfo, [LC.MISSION_DATA, LC.META_FLUX_CNAME])},
                {fieldKey: 'periodMin', value: `${min}`},
                {fieldKey: 'periodMax', value: `${max}`},
                {fieldKey: 'period', value: `${period}`},
                {fieldKey: 'tzero', value: `${tzero}`},
                {fieldKey: 'tzeroMax', value: `${tzeroMax}`}];

            dispatchMultiValueChange(LC.FG_PERIOD_FINDER, initState);
        }
        fields = FieldGroupUtils.getGroupFields(LC.FG_PERIODOGRAM_FINDER);
        if (fields) {
            dispatchRestoreDefaults(LC.FG_PERIODOGRAM_FINDER);
        }

        //IRSA-464: the smartMerge does not replace array component.  It updates the array. Therefore, setting it
        //null first to force the new fullRawTable is updated to the layout
        dispatchUpdateLayoutInfo(Object.assign({}, layoutInfo, {
            fullRawTable:null,
            periodRange: {min, max, tzero, tzeroMax, period}
        }));
        //add the fullRawtable into the layout
        dispatchUpdateLayoutInfo(Object.assign({}, layoutInfo, {
            fullRawTable,
            periodRange: {min, max, tzero, tzeroMax, period}
        }));


        callback && callback();
    };

    var rawTable = getTblById(LC.RAW_TABLE);
    if (layoutInfo.fullRawTable && layoutInfo.fullRawTable.totalRows===rawTable) {
        callback && callback();
    } else {

        if (isTblDataAvail(0, rawTable.totalRows, rawTable)) {
            setTableData(rawTable);
        } else {
            var req = Object.assign(cloneDeep(rawTable.request), {pageSize: MAX_ROW});

            doFetchTable(req).then(
                (tableModel) => {
                    setTableData(tableModel);
                }
            ).catch(
                (reason) => {
                    logger.error(`Failed to get full raw table: ${reason}`, reason);
                }
            );
       }
    }
}
