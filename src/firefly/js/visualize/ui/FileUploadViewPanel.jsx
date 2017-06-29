/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, range, isNil, set} from 'lodash';
import {flux} from '../../Firefly.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getTblInfoById, getTblById, calcColumnWidths, makeTblRequest, getColumnIdx} from '../../tables/TableUtil.js';
import {dispatchTableReplace, dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {ZoomType} from '../ZoomType.js';
import {dispatchPlotImage } from '../ImagePlotCntlr.js';

import './ImageSelectPanel.css';

export const panelKey = 'FileUploadAnalysis';
export const summaryTableId = 'UPLOAD_SUMMARY_TABLE';
const  summaryTableGroup = 'UPLOAD_SUMMARY_GROUP';

const  isFitsFile = (analysisModel) => {
            return analysisModel && get(analysisModel, 'fileFormat', '').toLowerCase().includes('fits');
};

const emptyModel = (model) => {
    const row = model && get(model, ['totalRows']);

    return (isNil(row) || row === 0);
};

var tblId = 0;

export class FileUploadViewPanel extends PureComponent {

    constructor(props) {
        super(props);

        this.getNextState = (analysisFields) => {
            if (!analysisFields) analysisFields = FieldGroupUtils.getGroupFields(panelKey);
            const currentAnaResult = get(analysisFields, ['fileUpload', 'analysisResult'], '');
            const analysisResultObj = currentAnaResult ? JSON.parse(currentAnaResult) : {};
            const {analysisModel=null, analysisSummary=''} = analysisResultObj;
            const highlightedRow = analysisModel ? get(analysisModel, 'highlightedRow', 0) : -1;
            return {analysisModel, analysisResult: currentAnaResult, analysisSummary, highlightedRow};
        };

        this.state = this.getNextState();
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const analysisFields = FieldGroupUtils.getGroupFields(panelKey);
            const {analysisResult='', valid=true} = get(analysisFields, ['fileUpload']);

            if (!valid) {
                this.setState({analysisModel: null, analysisResult: '', analysisSummary: '', highlightedRow: -1});
            } else if (analysisResult !== this.state.analysisResult) {

                const status = this.getNextState(analysisFields);

                this.setState(status);
                if (!emptyModel(this.state.analysisModel) &&  getTblById(this.state.analysisModel.tbl_id) &&
                    !emptyModel(status.analysisModel)) {
                    dispatchTableReplace(status.analysisModel);
                }
            } else if (this.state.analysisModel) {                 // check if highlight changed
                if (getTblById(this.state.analysisModel.tbl_id)) {
                    const {highlightedRow} = getTblInfoById(this.state.analysisModel.tbl_id);

                    if (highlightedRow !== this.state.highlightedRow) {
                        this.setState({highlightedRow});
                    }
                }
            }
        }
    }

    render() {
        const {analysisSummary, analysisModel, highlightedRow} = this.state;
        const  widthTotal = 600;
        let    w1;
        const  isFits = isFitsFile(analysisModel);

        const displayHighlight = () => {
            const tblIdx = get(analysisModel, ['tableData', 'data', highlightedRow, 0]);
            let   title = '';

            if (tblIdx) {
                title = (analysisModel.fileFormat && isFits) ?
                        `Header of extension with index ${tblIdx}:` : `Information of table with index ${tblIdx}:`;
            }

            const displayEntries = () => {

                if (tblIdx) {
                    const highlightInfo = get(analysisModel, ['tableMeta', tblIdx], '');
                    const highlightObj = highlightInfo ? JSON.parse(highlightInfo) : undefined;
                    const rowInfo = highlightObj && get(highlightObj, 'rowInfo');

                    if (rowInfo) {
                        const SP = '\u00a0';
                        return rowInfo.map((entry) => {
                            let   key = get(entry, 'key');
                            const val = get(entry, 'value');
                            const comment = get(entry, 'comment', '');

                            if (isFits) {
                                const b = 8 - key.length;
                                key = range(b).reduce((prev) => {
                                    prev += SP;
                                    return prev;
                                }, key);
                            } else {
                                key += SP;
                            }
                            const msg = `${key}= ${val} ${comment ? '/' : ''} ${comment}`;

                            return <p style={{margin: 2, whiteSpace: 'nowrap', fontFamily: 'Monospace'}} key={key}>{msg} </p>;
                        });
                    }
                }
                return false;
            };

            return (
                <div style={{ width: (widthTotal - w1), height: 'calc(100% - 4px)', border: '2px solid #b5b5b5', overflow: 'scroll'}}>
                    <p style={{fontWeight: 'bold', marginLeft:2}}>{title}</p>
                    {displayEntries()}
                </div>
            );
        };


        const displayTable = () => {
            const hlTbl = Object.assign({}, analysisModel, {highlightedRow});
            const tbl_ui_id = hlTbl.tbl_id || `${summaryTableId}_${tblId++}`;
            const {columns, data} = get(hlTbl, 'tableData') || {};


            if (columns && data) {
                const widths = calcColumnWidths(columns, data);

                w1 = 0;
                columns.forEach((col, idx) => {
                    col.prefWidth = Math.min(widths[idx]*2, 20);
                    w1 += col.prefWidth;
                });
            }

            w1 = (w1 + 1) * 8 + 2;
            return (
                <div style={{width: w1, height: '100%'}} >
                    <TablePanel
                        key={summaryTableGroup}
                        tbl_ui_id={tbl_ui_id}
                        showToolbar={false}
                        showOptionButton={false}
                        tableModel={hlTbl}
                    />
                </div>
            );
        };

        const fileResultArea = () => {
            const tableStyle = {boxSizing: 'border-box', width: '100%', height: 'calc(100% - 40px)',
                                overflow: 'hidden', resize:'none', display: 'flex'};
            const showTable = () => {
                return (
                    <div style={tableStyle}>
                            {displayTable()}
                            {displayHighlight()}
                    </div>
                );
            };

            return (
                <div style={{height: 400, width: widthTotal, marginLeft: 10}}>
                    <div style={{marginBottom: 10}}>{`${analysisSummary}`}</div>
                    {emptyModel(analysisModel) ? false : showTable()}
                </div>
            );
        };

        return (
            <FieldGroup groupKey={panelKey}
                        keepState={true}>
                <div className={'imagepanel'}>
                    <FileUpload
                        wrapperStyle={{margin: '15px 10px 21px 10px'}}
                        fieldKey={'fileUpload'}
                        fileAnalysis={true}
                        initialState={{tooltip: 'Select a file with FITS, VOTABLE, CSV, TSV, or IPAC format',
                                       label: 'File:'}}/>
                    {fileResultArea() }
                </div>
            </FieldGroup>
        );
    }
}

/*
 send table request. an extension map which maps index to table index in the server is given in case the extension index
 is given.
 */
function sendTableRequest(fileCacheKey, fName, idx, extMap) {
    const n = fName.lastIndexOf('\\');

    if (n >= 0) {
        fName = fName.slice(n+1);
    }

    const title = idx ? `${fName}-${idx}` : `${fName}`;
    const tblReq = makeTblRequest('userCatalogFromFile', title, {
        filePath: fileCacheKey
    });

    if (!(isNil(idx))) {
        set(tblReq, 'tbl_index', extMap[idx]);
    }

    dispatchTableSearch(tblReq);
}

/*
send plot image request. an extension map which map index to extension number in the server is given.
 */
function sendImageRequest(fileCacheKey, fName, idx, extMap) {
    const wpr = WebPlotRequest.makeFilePlotRequest(fileCacheKey);

    //const exts = idx.join();
    //wpr.setMultiImageExts(exts);
    //wpr.setZoomType(ZoomType.TO_WIDTH_HEIGHT);
    //wpr.setPostTitle(`- extensoin ${exts}`);

    const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};

    if (viewerId) {
        wpr.setPlotGroupId(viewerId);

        idx.forEach( (oneExt) => {
            const plotId = `${fName}-${oneExt}`;
            const extNo = extMap[oneExt];

            if (extNo !== -1) {   // not primary
                wpr.setPostTitle(`- ext. ${oneExt}`);
            }

            wpr.setMultiImageExts(`${extNo}`);
            dispatchPlotImage({plotId, wpRequest: wpr, viewerId});
        });
    }
}

/*
    create map which maps table row index to table index and image extension number in the server
 */
function getExtensionMap(model) {
    const {tableData} = model;
    const tableMap = {};
    const imageMap = {};
    const typeIdx = getColumnIdx(model, 'Type');

    tableData.data.forEach((row, idx) => {
        const type = row[typeIdx];

        if (type.toLowerCase().includes('table')) {
            tableMap[idx] = Object.keys(tableMap).length;
        } else if (type.toLowerCase().includes('image')) {
            imageMap[idx] = idx === 0 ? -1 : idx;
        }
    });

    return {imageMap, tableMap};
}


export function resultSuccess() {
    return (request) => {
        const {fileUpload} = request;
        const {displayValue, analysisResult} = get(FieldGroupUtils.getGroupFields(panelKey), 'fileUpload') || {};
        const {analysisModel=null, analysisSummary} = analysisResult&&JSON.parse(analysisResult) || {};

        if (!analysisSummary.includes('invalid') && analysisModel && analysisResult) {   // no response on invalid file
            if (!emptyModel(analysisModel)) {    // votable or fits
                const typeIdx = getColumnIdx(analysisModel, 'Type');
                const isFits = isFitsFile(analysisModel);
                const {selectInfo} = getTblInfoById(analysisModel.tbl_id);
                const selectInfoCls = selectInfo&&SelectInfo.newInstance(selectInfo, 0);
                let   selected = selectInfoCls && [...selectInfoCls.getSelected()];

                if (!selected || selected.length === 0) { // no extension is selected, get the first extension for fits
                    if (isFits) {
                        selected = get(analysisModel, 'totalRows') > 1 ? [1] : [0];   // extension number is -1 for the primary HDU
                    } else {
                        selected = [0];
                    }
                }

                const tSelected = [];
                const iSelected = [];

                selected.forEach((idx) => {
                    const type = get(analysisModel, ['tableData', 'data', idx, typeIdx]);
                    if (type.toLowerCase().includes('table')) {
                        tSelected.push(idx);
                    } else if (type.toLowerCase().includes('image')) {
                        iSelected.push(idx);
                    }
                });

                const extensionMap = (iSelected.length > 0 || tSelected.length > 0) ? getExtensionMap(analysisModel) : null;
                if (iSelected.length !== 0) {
                    sendImageRequest(fileUpload, displayValue, iSelected, extensionMap.imageMap);
                }
                if (tSelected.length !== 0) {
                    tSelected.forEach((idx) => {
                        sendTableRequest(fileUpload, displayValue, idx, extensionMap.tableMap);
                    });
                }
            } else {    // csv, tsv, ipac
                sendTableRequest(fileUpload, displayValue);
            }
        } else {
            return false;
        }
    };
}

export function resultFail() {
    return (request) =>
    {
        return false;
    };
}