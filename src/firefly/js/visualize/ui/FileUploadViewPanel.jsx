/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get, set, isNil, isEqual} from 'lodash';
import {flux} from '../../Firefly.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getTblInfoById, getTblById, calcColumnWidths, makeTblRequest, getColumnIdx} from '../../tables/TableUtil.js';
import {dispatchTableSearch, dispatchTableRemove} from '../../tables/TablesCntlr.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchPlotImage } from '../ImagePlotCntlr.js';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import HelpIcon from '../../ui/HelpIcon.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {updateMerge, getSizeAsString} from '../../util/WebUtil.js';

import './ImageSelectPanel.css';

export const panelKey = 'FileUploadAnalysis';
const  fileId = 'fileUpload';
const  urlId = 'urlUpload';

const SUMMARY_INDEX_COL = 0;
const SUMMARY_TYPE_COL = 2;
const HEADER_KEY_COL = 1;
const HEADER_VAL_COL = 2;
const analysisTblIds = [];
const headerTblIds = [];

const  isFitsFile = (analysisModel) => {
            return analysisModel && get(analysisModel, 'fileFormat', '').toLowerCase().includes('fits');
};

/**
 * check if table model contain data
 * @param model
 * @returns {*|boolean}
 */
const isNoDataInModel = (model) => {
    const row = model && get(model, ['totalRows']);

    return (isNil(row) || row === 0);
};

const nowTime = () => (new Date().valueOf());


function makeHeaderTable(model, highlightedRow) {
    const tblIdx = get(model, ['tableData', 'data', highlightedRow, 0]);
    const highlightInfo = tblIdx && get(model, ['tableMeta', tblIdx], '');
    const hlTable = highlightInfo ? JSON.parse(highlightInfo) : null;

    if (hlTable) {
       hlTable.tbl_id = `${hlTable.tbl_id}_${nowTime()}`;
        headerTblIds.push(hlTable.tbl_id);
    }

    return hlTable;
}

/**
 * compute highlightedRow to be the row in original table
 * @param tableModel
 * @param highlightedRow
 * @returns {*}
 */
function adjustHighlightedRow(tableModel, highlightedRow) {
    const {data} = get(tableModel, 'tableData');

    if (!isNil(highlightedRow)) {
        return parseInt(data[highlightedRow][SUMMARY_INDEX_COL]);
    }

    return -1;
}

/**
 * compute the selected row to be the row in the original table
 * @param tableModel
 * @param selectInfo
 * @returns {*}
 */
function adjustSelectInfo(tableModel, selectInfo) {
    const selectInfoCls = SelectInfo.newInstance(selectInfo);
    const newSelectCls = new SelectInfo.newInstance({rowCount: tableModel.totalRows}, selectInfoCls.offset);

    let   bNew = false;

    Array.from(selectInfoCls.getSelected()).forEach((idx) => {
         const newIdx = adjustHighlightedRow(tableModel, idx);

         newSelectCls.setRowSelect(newIdx, true);
         if (idx !== newIdx) {
             bNew = true;
         }
    });

    return bNew ? newSelectCls.data : selectInfo;
}

/**
 * analyze the selected units - image, table, no data contained, exceeding the limit
 * @param model
 * @param limit
 * @returns {{image: Array, table: Array, noDataUnit: Array, extra: Array}}
 */
const getSelectionResult = (model, limit) => {
    const {selectInfo} = model || {};
    let   results = {image: [], table: [], noDataUnit: [], extra:[]};


    if (model && !isNoDataInModel(model) && selectInfo) {
        const selectInfoCls = SelectInfo.newInstance(selectInfo);
        const {data} = get(model, 'tableData');

        if (data) {
            const {tableMeta} = model;
            let   totalGood = 0;

            results = Array.from(selectInfoCls.getSelected()).reduce((prev, idx) => {
                if (isNil(limit) || totalGood <= limit) {
                    if (hasGoodData(get(tableMeta, idx))) {
                        totalGood++;
                        if (!isNil(limit) && totalGood > limit) {
                            prev.extra.push(idx);
                        } else {
                            data[idx][SUMMARY_TYPE_COL].toLowerCase().includes('image') ? prev.image.push(idx) : prev.table.push(idx);
                        }
                    } else {
                        prev.noDataUnit.push(idx);
                    }
                }
                return prev;
            }, results);
        }
    }
    return results;
};


export class FileUploadViewPanel extends PureComponent {

    constructor(props) {
        super(props);

        this.getNextState = (analysisFields) => {
            const bInit = !analysisFields;
            if (!analysisFields) analysisFields = FieldGroupUtils.getGroupFields(panelKey);

            const uploadSrc = get(analysisFields, ['uploadTabs', 'value'], fileId);
            const displayValue = get(analysisFields, [uploadSrc, 'displayValue']);
            const currentAnaResult = get(analysisFields, [uploadSrc, 'analysisResult'], '');
            const analysisResultObj = currentAnaResult ? JSON.parse(currentAnaResult) : {};
            const {analysisSummary=''} = analysisResultObj;
            let   {analysisModel=null} = analysisResultObj;
            let   {crtAnalysisId} = this.state || {};
            let   highlightedRow = -1;
            let   hlHeaderTable = null;

            if (analysisModel) {

                if (bInit && analysisTblIds.length === 1) {
                    crtAnalysisId = analysisTblIds[0];       // last uploaded file
                } else {
                    crtAnalysisId = `${analysisModel.tbl_id}_${nowTime()}`;
                    analysisTblIds.push(crtAnalysisId);
                }
                analysisModel = Object.assign(analysisModel, {tbl_id: crtAnalysisId});

                updatePreferColumnWidth(analysisModel);

                const tblInfo =  getTblInfoById(crtAnalysisId);

                let selectInfo = get(tblInfo, 'selectInfo', null);

                if (!selectInfo) {
                    selectInfo = selectRowFromSummaryTable(analysisModel);
                }
                analysisModel = Object.assign(analysisModel, {selectInfo});

                highlightedRow = get(tblInfo, 'highlightedRow', 0);
                hlHeaderTable = (highlightedRow >= 0) ? makeHeaderTable(analysisModel, highlightedRow) : null;
            }

            return {analysisModel, analysisResult: currentAnaResult, analysisSummary, highlightedRow, hlHeaderTable,
                    crtAnalysisId, isUploading: false, displayValue, uploadSrc};
        };

        this.state = this.getNextState();
        this.onLoading = this.onLoading.bind(this);
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
            const uploadSrc = get(analysisFields, ['uploadTabs', 'value']);
            const {analysisResult='', valid=true, displayValue} = get(analysisFields, uploadSrc) || {};

            if (!valid) {  // upload fails
                this.setState({
                    isUploading: false,
                    analysisModel: null,
                    analysisResult: '',
                    analysisSummary: '',
                    highlightedRow: -1,
                    hlHeaderTable: null,
                    uploadSrc
                });
            } else if (uploadSrc !== this.state.uploadSrc) {
                this.setState(this.getNextState(analysisFields));
            } else if ((displayValue && displayValue !== this.state.displayValue) && uploadSrc === fileId) {  // in uploading the new file
                this.setState({
                    isUploading: true,
                    displayValue
                });
            } else if (analysisResult !== this.state.analysisResult) {  // a new file is successfully loaded
                this.setState(this.getNextState(analysisFields));
            } else if (this.state.analysisModel) {                 // check if highlight or selection is changed
                const {crtAnalysisId} = this.state;

                if (getTblById(crtAnalysisId)) {
                    let {highlightedRow, selectInfo} = getTblInfoById(crtAnalysisId);
                    const {tableModel} = getTblInfoById(crtAnalysisId);

                    if (tableModel && tableModel.tableData) {
                        highlightedRow = adjustHighlightedRow(tableModel, highlightedRow);
                        if (highlightedRow !== this.state.highlightedRow) {
                            const hlHeaderTable = makeHeaderTable(this.state.analysisModel, highlightedRow);

                            this.setState({highlightedRow, hlHeaderTable});
                        }

                        selectInfo = adjustSelectInfo(tableModel, selectInfo);
                        if (selectInfo && !isEqual(selectInfo, get(this.state.analysisModel, 'selectInfo'))) {
                            const analysisModel = Object.assign({}, this.state.analysisModel, {selectInfo});
                            this.setState({analysisModel});

                        }

                    }
                }
            }
        }
    }

    onLoading() {
        this.setState({isUploading: true});  // show spinning while is uploading
    }

    render() {
        const {analysisSummary, analysisModel, highlightedRow, hlHeaderTable, isUploading=false} = this.state;
        const tableStyle = {width: '100%', height:'calc(100% - 40px)', overflow: 'hidden'};
        const pStyle = {fontWeight: 'bold', fontSize: 12, marginLeft:2, textAlign: 'center', height: 16};
        const summaryStyle = {height: 'calc(100% - 2px)', overflow: 'hidden'};
        let   w1;

        var displayHeaderTable = () => {

            if (!hlHeaderTable) {
                return false;
            }
            const {columns, data} = get(hlHeaderTable, 'tableData') || {};

            if (columns && data) {
                const widths = calcColumnWidths(columns, data);

                columns.forEach((col, idx) => {
                    col.prefWidth = widths[idx] + 4;
                });
            }

            return  (
                <div style={{ width: `calc(100% - ${w1}px)`, ...summaryStyle, paddingLeft: 3}}>
                    <p style={pStyle}>{hlHeaderTable.title}</p>
                    <div style={tableStyle}>
                        <TablePanel
                            key={hlHeaderTable.tbl_id}
                            tbl_ui_id={hlHeaderTable.tbl_id}
                            showToolbar={false}
                            showOptionButton={false}
                            selectable={false}
                            tableModel={hlHeaderTable}
                        />
                    </div>
                </div>
            );
        };

        var displayTable = () => {
            if (!analysisModel) {
                return false;
            }

            let {selectInfo} = analysisModel || getTblInfoById(analysisModel.tbl_id) || {};
            if (isNil(selectInfo)) {
                selectInfo = selectRowFromSummaryTable(analysisModel);
            }
            const hlTbl = Object.assign({}, analysisModel, {highlightedRow}, {selectInfo});
            const {columns, data} = get(hlTbl, 'tableData') || {};

            w1 = 0;
            if (columns && data) {
                w1 = columns.reduce((prev, col) => {
                    prev += col.prefWidth;
                    return prev;
                }, 0);
            }

            w1 = (w1 + 1) * 8;

            const getExtensionInfo = () => {
                const rows = get(analysisModel, 'totalRows');

                if (isFitsFile(analysisModel)) {
                    return rows > 1 ? `${rows-1} extension${rows > 2 ? 's' : ''}` : '';
                } else {
                    return `${rows} table${rows > 1 ? 's': ''}`;
                }
            };

            return (
                <div style={{width: w1, ...summaryStyle}} >
                    <p style={pStyle}>{`File Summary ${getExtensionInfo()}`}</p>
                    <div style={tableStyle}>
                        <TablePanel
                            key={hlTbl.tbl_id}
                            tbl_ui_id={hlTbl.tbl_id}
                            showToolbar={false}
                            showOptionButton={false}
                            tableModel={hlTbl}
                        />
                    </div>
                </div>
            );
        };

        const fileResultArea = () => {
            const tableStyle = {boxSizing: 'border-box', width: '100%', height: 'calc(100% - 40px)',
                                overflow: 'hidden', display: 'flex', resize:'none', marginTop: 10, marginBottom: 10};

            var showTable = () => {
                return (
                    <div style={tableStyle}>
                            {displayTable()}
                            {displayHeaderTable()}
                    </div>
                );
            };



            const isMultipleImageSelected = () => {
                const selResults = getSelectionResult(analysisModel);

                return get(selResults, 'image').length > 1;
            };

            const showImageButtons = () => {
                const imgOptions = [{value: 'oneWindow', label: 'All images in one window'},
                                     {value: 'mulWindow', label: 'One extension image per window'}];
                return (
                    <RadioGroupInputField
                        inline={false}
                        initialState={{fieldKey: 'imageDisplay',
                                       tooltip: 'display image extensions in one window or multiple windows'}}
                        fieldKey={'imageDisplay'}
                        wrapperStyle={{height: imageOptH, fontSize: 12}}
                        options={imgOptions}
                    />
                );
            };

            const bShowImageOptions = isMultipleImageSelected();
            const imageOptH = 20;
            const isTable = !isNoDataInModel(analysisModel);
            const allH = isTable ? 420+imageOptH : 100;

            if (isUploading) {
                return (
                    <div style={{height: allH, width: '100%', margin: 10}}>
                        {loadingBox()}
                    </div>
                );
            } else {
                return (
                    <div style={{height: allH, width: 'calc(100% - 10px)', marginLeft: 5, marginRight: 5}}>
                        {isNoDataInModel(analysisModel) ? false : showTable()}
                        {bShowImageOptions ? showImageButtons() : false}
                    </div>
                );
            }
        };

        const loadingBox = () => {
            const maskWrapper= {
                position:'absolute',
                left:0,
                top:0,
                width:'100%',
                height:'100%'
            };
            return (
                <div style={maskWrapper}><div className='loading-mask'/></div>
            );
        };

        const helpIcon = () => {
            const helpId = 'basics.searching';

            return (
                <div style={{display:'flex', flexDirection:'column', alignItems:'flex-end'}}>
                    <HelpIcon helpId={helpId}/>
                </div>

            );
        };

        const uploadStyle = {marginTop: 12, marginBottom: 20};
        const uploadMethod = [{value: fileId, label: 'Upload file'},
                              {value: urlId, label: 'Upload from URL'}];
        const {uploadSrc} = this.state;
        const lineH = 16;

        const uploadSection = () => {
            if (uploadSrc === fileId) {
                return (
                        <FileUpload
                            wrapperStyle={{...uploadStyle, marginRight: 16}}
                            fileNameStyle={{marginLeft: 0, fontSize: 12}}
                            fieldKey={fileId}
                            fileAnalysis={this.onLoading}
                            innerStyle={{width: 80}}
                            initialState={{tooltip: 'Select a file with FITS, VOTABLE, CSV, TSV, or IPAC format',
                                           label: ''
                                           }}/>
                );
            } else if (uploadSrc === urlId) {
                return (
                        <FileUpload
                            wrapperStyle={{...uploadStyle, marginRight: 32, width: '50%'}}
                            fieldKey={urlId}
                            fileAnalysis={this.onLoading}
                            isFromURL={true}
                            innerStyle={{width: '70%'}}
                            initialState={{tooltip: 'Select a URL with file in FITS, VOTABLE, CSV, TSV, or IPAC format',
                                           label: 'Enter URL of a file:'
                                         }}/>
                );
            }
        };

        const showSummary = () => {
            let summaryLine = analysisSummary ? analysisSummary.split('--', 1): ''; // only get one line summary
            if (summaryLine) {
                summaryLine += (analysisModel&&analysisModel.size ? `: ${getSizeAsString(analysisModel.size)} Bytes` : '');
            }

            return (
                    <div style={{height: lineH*2, paddingLeft: 2, fontSize: 12}}>
                        {summaryLine}
                        <br/>
                    </div>
                );
        };

        return (
            <FieldGroup groupKey={panelKey}
                        reducerFunc={fieldReducer()}
                        keepState={true}>
                <div style={{width:'100%', height:'calc(100% - 20px)'}}>
                    <div style={{display:'flex',  flexDirection: 'column', marginTop: 10, paddingLeft: '30%'}}>
                        <RadioGroupInputField
                            initialState={{value: uploadMethod[0].value}}
                            fieldKey='uploadTabs'
                            alignment={'horizontal'}
                            options={uploadMethod}
                            wrapperStyle={{fontWeight: 'bold', fontSize: 12}}/>
                         {uploadSection()}
                         {showSummary()}
                    </div>
                    {fileResultArea(fileId) }
                </div>
                {helpIcon() }
            </FieldGroup>
        );
    }
}

/**
 * find the prefer column width based on the text length of each cell of the releveant column
 * @param tblModel
 */
function  updatePreferColumnWidth(tblModel) {
    const {columns, data} = get(tblModel, 'tableData');

    if (!columns || !data) return;

    const widths = calcColumnWidths(columns, data);

    columns.forEach((col, idx) => {
        col.prefWidth = widths[idx] + 4;
    });
}

const errorMsg =  {invalidFile: 'no valid file is uploaded',
    invalidVTableelection: 'no table with data is selected',
    invalidFITSSelection: 'no extension with valid data is selected',
    noTableSelected: 'no table is selected',
    noExtensionSelected: 'no extenstion is selected'};

const returnValidate = (retVal) => {
    if (retVal.message) {
        console.log(retVal.message);
        showInfoPopup(retVal.message, 'search uploaded file error');
    }
    return retVal;
};


/**
 * validate the table or extension selection
 * @param uploadTabs
 */
export function validateModelSelection(uploadTabs) {
        const analysisFields = FieldGroupUtils.getGroupFields(panelKey);
        const {valid=false, analysisResult='', displayValue}  = get(analysisFields, uploadTabs) || {};

        if (!valid || !analysisResult || analysisTblIds.length === 0) {    // no file uploaded yet
            return returnValidate({
                valid: false,
                message: errorMsg.invalidFile
            });
        }

        const analysisResultObj = JSON.parse(analysisResult);
        const {analysisSummary='invalid', analysisModel=null} = analysisResultObj;
        if (analysisSummary.startsWith('invalid') || !analysisModel) {    // no valid file uploaded
            return returnValidate({
                valid: false,
                message: errorMsg.invalidFile
            });
        }

        const fileFormat = get(analysisModel, 'fileFormat', '').toLowerCase();

        if (!fileFormat.includes('vo') && !fileFormat.includes('fits')) { // csv, tsv, ipac
            return returnValidate({
                valid: true,
                analysisModel,
                displayValue
            });
        }

        const crtAnalysisId = analysisTblIds[analysisTblIds.length-1];
        const {selectInfo, tableModel} = getTblInfoById(crtAnalysisId); // check if no valid extension or table selected
        const selectInfoCls = SelectInfo.newInstance(selectInfo);
        const bFits = isFitsFile(analysisModel);
        const selList = selectInfoCls.getSelected();

        if (Array.from(selList).length === 0) {
            return returnValidate({
                valid: false,
                message: (bFits ? errorMsg.noExtensionSelected : errorMsg.noTableSelected)
            });
        }

        const resultModel = Object.assign({}, analysisModel, {selectInfo: adjustSelectInfo(tableModel, selectInfo)});
        const limit = 20;
        const selectResults = getSelectionResult(resultModel, limit); // a search limit is set

        if (selectResults.image.length > 0 || selectResults.table.length > 0) {
            let message = '';

            if (selectResults.extra.length > 0) {
                message = `the first ${limit} selected image/table units with good data are searched`;
            } else if (selectResults.noDataUnit.length > 0) {
                const list = selectResults.noDataUnit.join();
                message = `${bFits? 'extension' : 'table' } ${list} contain${list.includes(',') ? '' : 's'} no data`;
            }

            return returnValidate({
                valid: true,
                analysisModel: resultModel,
                selectResults,
                message,
                displayValue
            });
        } else {
            return returnValidate({
                valid: false,
                message: (bFits ? errorMsg.invalidFITSSelection : errorMsg.invalidVTableelection)
            });
        }
}

/**
 * check if the HDU contains good data
 * @param metaInfo
 * @returns {boolean}
 */
const hasGoodData = (metaInfo) => {
    const metaTable = metaInfo ? JSON.parse(metaInfo) : null;
    const {data} = (metaTable && get(metaTable, 'tableData')) || {};
    const naxisSet = ['naxis', 'naxis1', 'naxis2', 'naxis3'];

    if (isNil(data)) return false;

    const badIndex = data.findIndex((oneKey) => {
        return (naxisSet.includes(oneKey[HEADER_KEY_COL].toLowerCase()) && (oneKey[HEADER_VAL_COL] === '0'));
    });

    return badIndex < 0;
};

/**
 * find the first valid row for selection from the summary table
 * @param tblModel
 * @returns {*}
 */
function selectRowFromSummaryTable(tblModel) {
    const {fileFormat, totalRows, tableMeta} = tblModel;
    let   selIndex = -1;


    if (fileFormat && fileFormat.toLowerCase().includes('fits')) {
        if (totalRows) {
            for (let i = (totalRows === 1 ? 0 : 1); i < totalRows; i++) {
                if (hasGoodData(tableMeta[i])) {
                    selIndex = i;
                    break;
                }
            }
        }
    } else {
        selIndex = 0;
    }

    const selectInfoCls = SelectInfo.newInstance({rowCount: tblModel.totalRows});
    if (selIndex >= 0) {
        selectInfoCls.setRowSelect(selIndex, true);
    }
    return selectInfoCls.data;
}

/**
 * send request to get the data of table unit, for votable and fits, the table index is mapped to be
 * that at the server side
 * @param fileCacheKey
 * @param fName
 * @param idx
 * @param extMap
 * @param totalRows
 */
function sendTableRequest(fileCacheKey, fName, idx, extMap, totalRows) {
    const title = !isNil(idx)&&!isNil(totalRows)&&(totalRows !== 1) ? `${fName}-${idx}` : `${fName}`;
    const tblReq = makeTblRequest('userCatalogFromFile', title, {
        filePath: fileCacheKey
    });

    if (!(isNil(idx))) {
        set(tblReq, 'tbl_index', extMap[idx]);
    }

    dispatchTableSearch(tblReq);
}

/**
 * send request to get the data of image unit, the extension index is mapped to be that at
 * the server side
 * @param fileCacheKey
 * @param fName
 * @param idx
 * @param extMap
 * @param imageDisplay
 */
function sendImageRequest(fileCacheKey, fName, idx, extMap, imageDisplay) {
    const wpr = WebPlotRequest.makeFilePlotRequest(fileCacheKey);

    const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};
    const mapToExtensionNo = () => {
        return idx.reduce( (prev, oneExt) => {
            prev.push(extMap[oneExt]);           // convert to extension number at the server
            return prev;
        }, []);
    };


    if (viewerId) {
        wpr.setPlotGroupId(viewerId);

        let plotId;
        const allExt = mapToExtensionNo();

        // all in one window
        if (isNil(imageDisplay) || imageDisplay.startsWith('one')) {
            const extList = allExt.join();

            wpr.setMultiImageExts(extList);
            if (!allExt.includes(-1)) {
                wpr.setPostTitle(`- ext. ${extList}`);
            }

            plotId = `${fName}-${idx.join('_')}`;
            dispatchPlotImage({plotId, wpRequest: wpr, viewerId});
        } else {

            idx.forEach( (oneExt, id) => {
                plotId = `${fName}-${oneExt}`;

                if (allExt[id] !== -1) {   // not primary
                    wpr.setPostTitle(`- ext. ${oneExt}`);
                }

                wpr.setMultiImageExts(`${allExt[id]}`);

                dispatchPlotImage({plotId, wpRequest: wpr, viewerId});
            });
        }
    }
}

/*
    the map which maps summary table row index to table index and image extension number at the server
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
            imageMap[idx] = idx === 0 ? -1 : idx;     // the extension number is -1 for primary HDU
        }
    });

    return {imageMap, tableMap};
}

/**
 * render the selected tables or extensions (votable/fits) or the file(csv, tsv, ipac)
 * @returns {Function}
 */
export function resultSuccess() {
    const tableTitle = (displayValue, uploadTabs) => {
        const n = displayValue.lastIndexOf((uploadTabs === fileId ? '\\' : '\/'));
        return  displayValue.slice(n+1);   // n = -1 or n >= 0
    };

    return (request) => {
        const {uploadTabs, imageDisplay} = request;
        const retVal = validateModelSelection(uploadTabs);
        if (!retVal.valid) return false;

        const {analysisModel, displayValue, selectResults} = retVal;
        const {fileUpload, urlUpload} = request;
        const uploadName = uploadTabs === fileId ? fileUpload : urlUpload;

        if (selectResults) {    // votable or fits
            const extensionMap = getExtensionMap(analysisModel);

            if (selectResults.image.length !== 0) {
                sendImageRequest(uploadName, displayValue, selectResults.image, extensionMap.imageMap, imageDisplay);
            }
            if (selectResults.table.length !== 0) {
                selectResults.table.forEach((idx) => {
                    sendTableRequest(uploadName, tableTitle(displayValue, uploadTabs), idx, extensionMap.tableMap,
                                     analysisModel.totalRows);
                });
            }
        } else {    // csv, tsv, ipac
            sendTableRequest(uploadName, tableTitle(displayValue, uploadTabs));
        }

        removeAnalysisTable(analysisTblIds.length-1);   // keep the current analysisModel
    };
}

export function resultFail() {
    return (request) =>
    {
        returnValidate({
            valid: false,
            message: errorMsg.invalidFile
        });
        return false;
    };
}

function removeAnalysisTable(noRemoved) {

    let len = noRemoved;
    for (let i = 0; i < len; i++) {
        const id = analysisTblIds.shift();
        if (getTblById(id)) {
            dispatchTableRemove(id);
        }
    }

    len = headerTblIds.length;         // remove all
    for (let i = 0; i < len; i++) {
        const id = headerTblIds.shift();
        if (getTblById(id)) {
            dispatchTableRemove(id);
        }
    }
}

const fieldReducer = function () {
    return (inFields, action) => {
        if (!inFields) return fieldInit();

        // remove previous  invalid upload which switch the upload method or be back to upload dropdown
        switch (action.type) {
            case FieldGroupCntlr.VALUE_CHANGE:

                const {fieldKey, value} = action.payload;

                if (fieldKey === 'uploadTabs' && (!get(inFields, [value, 'valid'], true))) {
                    inFields = updateMerge(inFields, value,
                                           {value: '', displayValue: '', analysisResult: '', valid: true});
                }
                break;
            case FieldGroupCntlr.MOUNT_FIELD_GROUP:

                const {value: uploadMethod} = get(inFields, 'uploadTabs');

                if (uploadMethod && (!get(inFields, [uploadMethod, 'valid'], true))) {
                    inFields = updateMerge(inFields, uploadMethod,
                        {value: '', displayValue: '', analysisResult: '', valid: true});
                }
                break;
            default:
                break;
        }
        return inFields;
    };
};

function fieldInit() {
    return (
        {
            'imageDisplay': {
                fieldKey: 'imageDisplay',
                value: 'oneWindow'
            },
            'uploadTabs': {
                fieldKey: 'uploadTabs',
                value: fileId
            },
            [fileId]: {
                fieldKey: fileId,
                value: ''
            },
            [urlId]: {
                fieldKey: urlId,
                value: ''
            }
        }
    );
}
