import React from 'react';

import {dispatchHideDialog, dispatchShowDialog} from '../core/ComponentCntlr.js';
import {FileAnalysisType, Format} from '../data/FileAnalysis.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {FileUploadDropdown} from './FileUploadDropdown.jsx';
import {TABLES} from './FileUploadUtil.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {LayoutType, PopupPanel} from './PopupPanel.jsx';
import {showInfoPopup} from './PopupUtil.jsx';

const dialogId = 'Upload-spatial-table';
const UPLOAD_TBL_SOURCE= 'UPLOAD_TBL_SOURCE';
const EXISTING_TBL_SOURCE= 'EXISTING_TBL_SOURCE';

function isArray(fdt) {
    const aStr= fdt.match(/^[1-9]*/)?.[0];
    return (aStr && aStr!=='1');
}

function convertsFitsDataType(fdt) {
    const aStr= fdt.match(/^[1-9]*/)?.[0];
    const typeChar= fdt[aStr?.length??0];
    switch (typeChar) {
        case 'L': return 'boolean';
        case 'X': return 'int';
        case 'B': return 'int';
        case 'I': return 'int';
        case 'J': return 'int';
        case 'K': return 'long';
        case 'A': return 'string';
        case 'E': return 'float';
        case 'D': return 'double';
        case 'C': return 'string';
        case 'M':
        case 'P': return 'string';
        case 'Q': return 'string';
        default: return;
    }
}

function getFitsColumnInfo(data) {
    return data
        .filter( (row) => row[1].startsWith('TTYPE'))
        .map(([,,name],idx) => {
            const fdt= data.find( (c) => c[1]===`TFORM${idx+1}`)?.[2];
            const type= convertsFitsDataType(fdt) ?? 'string';
            return ({
                    name, use:true, description:'',
                    type:  type + (isArray(fdt)?'[*]':''),
                    unit: data.find( (c) => c[1]===`TUNIT${idx+1}`)?.[2] ?? '',
                }
            );
        });
}


function uploadSubmit(request, setUploadInfo)  {
    if (!request) return false;
    const {additionalParams = {}, fileUpload: serverFile} = request;
    const {detailsModel, report, summaryModel} = additionalParams;
    if (!detailsModel || !report || !summaryModel || !serverFile) return false;
    const {fileName,fileSize} = report;
    const {tableData={}}= detailsModel;
    const {data=[]}= tableData;
    const {TOTAL_TABLE_ROWS:totalRows}= detailsModel.tableMeta;

    if (report.parts[summaryModel.highlightedRow]?.type===FileAnalysisType.Image || totalRows<1) {
        showInfoPopup('Could not find any tabular data in your selection','Must choose valid table');
        return false;
    }

    const columns= report.fileFormat===Format.FITS ?
        getFitsColumnInfo(data) :
        data.map(([name,type,u,d]) => ({name, type, units: u?u:'', description: d?d:'', use:true}));


    const uploadInfo = {serverFile, fileName, columns, totalRows, fileSize, tableSource: UPLOAD_TBL_SOURCE};
    setUploadInfo(uploadInfo);
    dispatchHideDialog(dialogId);
    return false;
}


function existingTableSubmit(request,setUploadInfo) {
    // todo: call this function for preloaded tables
    const uploadInfo = {
        serverFile: '${upload}//stuff/somename.tbl',
        title: 'tbl title',
        tbl_id: 'someTblId',
        columns: [],
        totalRows: 123,
        tableSource: EXISTING_TBL_SOURCE,
    };
    setUploadInfo(uploadInfo);
    dispatchHideDialog(dialogId);
    return false;
}




export function showUploadTableChooser(setUploadInfo) {
    DialogRootContainer.defineDialog(dialogId,
        <PopupPanel title={'Upload'} layoutPosition={LayoutType.TOP_EDGE_CENTER}>
            <TapUploadPanel {...{setUploadInfo}}/>
        </PopupPanel>
    );
    dispatchShowDialog(dialogId);
}

const TapUploadPanel= ({setUploadInfo,groupKey= 'table-chooser'}) => (
    <FieldGroup groupKey={groupKey}>
        <div style={{ resize: 'both', overflow: 'hidden', zIndex: 1, paddingTop:8, minWidth: 600, minHeight: 500, display: 'flex' }}>
            <FieldGroupTabs initialState={{value: 'upload'}} fieldKey='upload-type-tabs' groupKey={groupKey}
                            style={{width: '100%', flex: '1 1 auto'}}>
                <Tab name='Upload Tables' id='upload' style={{fontSize:'larger'}}>
                    <FileUploadDropdown {...{
                        style:{height: '100%'},
                        acceptOneItem:true, acceptList:[TABLES], keepState:true, groupKey:groupKey+'-fileUpload',
                        onCancel:() => dispatchHideDialog(dialogId),
                        onSubmit:(request) => uploadSubmit(request,setUploadInfo),
                    }}/>
                </Tab>
                <Tab name='Loaded Tables' id='current-table' style={{fontSize:'larger'}}>
                    <div style={{margin: 20, fontSize: 'large'}}>
                        TODO: Add Support for using an already loaded table
                    </div>
                </Tab>
            </FieldGroupTabs>
        </div>
    </FieldGroup> );
