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
import {getTableGroup, getTableUiByTblId, getTblById} from 'firefly/tables/TableUtil';
import {TablePanel} from 'firefly/tables/ui/TablePanel';
import {FormPanel} from 'firefly/ui/FormPanel';
import {ServerParams} from 'firefly/data/ServerParams';
import {doJsonRequest} from 'firefly/core/JsonUtils';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr';
import {findTableCenterColumns} from 'firefly/util/VOAnalyzer';

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
    const columnsSelected = getSelectedColumns(columns);
    const uploadInfo = {serverFile, fileName, columns:columnsSelected, totalRows, fileSize, tableSource: UPLOAD_TBL_SOURCE};
    setUploadInfo(uploadInfo);
    dispatchHideDialog(dialogId);
    return false;
}

function existingTableSubmit(request,setUploadInfo) {
    if (!request) return false;
    const tbl = getTblById('existing-table-list-ui');
    const idx = tbl.highlightedRow;
    const activeTblId = tbl.tableData.data[idx][3]; //tbl_id
    const activeTbl = getTableUiByTblId(activeTblId);
    const tableRequest = activeTbl.request;
    const columnData = activeTbl.columns;
    const columns = columnData.map((col) => col.visibility === 'hide' || col.visibility === 'hidden'? ({...col, use:false}) :  ({...col, use:true})); //filter out hidden cols
    const columnsSelected = getSelectedColumns(columns);

    const params ={
        [ServerParams.COMMAND]: ServerParams.TABLE_SAVE,
        [ServerParams.REQUEST]: JSON.stringify(tableRequest),
        file_name: tableRequest?.META_INFO?.title ?? 'existing_tbl_upload',
        file_format: Format.IPACTABLE,
        save_to_temp: 'true'
    };

    doJsonRequest(ServerParams.TABLE_SAVE, params).then((result) => {
        if (!result.success) {
            showInfoPopup('Error loading this table', result.error);
            return false;
        }
        const uploadInfo = {
            serverFile: result?.serverFile ?? null,
            title: activeTbl.title,
            fileName: activeTbl.title,
            tbl_id: activeTblId,
            columns:columnsSelected,
            totalRows: activeTbl.totalRows,
            tableSource: EXISTING_TBL_SOURCE,
        };
        setUploadInfo(uploadInfo);
        dispatchHideDialog(dialogId);
    });
    return false;
}

function getSelectedColumns(columns) {
    const {lonCol='', latCol=''} = findTableCenterColumns({tableData:{columns}}) ?? {}; //centerCols
    const columnsSelected = columns.map((col) => col.name === lonCol || col.name === latCol? ({...col, use:true}) :  ({...col, use:false})); //select position cols only
    return columnsSelected;
}

const NoTables = () => {
    return (
        <div style={{margin: '40px 10px 10px 10px', fontSize: 'large',
            padding: '5px 0 5px 0', textAlign: 'center', width: '100%'}}>
            {'No tables available to load.'}
        </div>
    );
};

const LoadedTables= (props) => {
    const {onSubmit, onCancel=dispatchHideDropDown, keepState=true, groupKey} = props;
    const tables = getTableGroup()?.tables ?? null;
    if (!tables) {
        return <NoTables/>;
    }
    const data = []; //row entry [title, num of cols, num of rows, tbl_id (hidden)]
    for (const tblId in tables) {
        const tbl = getTblById(tblId);
        if (!tbl.tableData) continue;
        //ToFix: converted row and col to string because searching their columns as number gives an error
        const title = [tables[tblId].title, (tbl.tableData.columns.length).toString(), (tbl.totalRows).toString(), tblId];
        data.push(title);
    }
    if (data.length === 0) {
        return <NoTables/>;
    }
    const columns = [{name: 'Table Title', width: 45}, {name: 'No. of Cols', width: 15}, {name: 'No. of Rows', width:15},
        {name: 'tblId', visibility: 'hidden'}];
    const highlightedRow = 0; //default selection
    const tbl_id = 'existing-table-list';

    const tableModel = {tbl_id, tableData: {columns, data}, highlightedRow, totalRows: data.length};

    return (
        <div style={{margin: '10px', position: 'relative', width: '100%', display: 'flex', alignItems: 'stretch', flexDirection: 'column'}}>
            <div style={{color: 'gray', fontSize: 'larger', lineHeight: '1.3em'}}>
                {'Select one of the existing tables below to load into the TAP panel: '}
            </div>
            <FieldGroup groupKey={groupKey} keepState={keepState} style={{height:'100%', width: '100%',
                display: 'flex', alignItems: 'stretch', flexDirection: 'column'}}>
                <FormPanel
                    onSubmit={onSubmit}
                    onCancel={onCancel}
                    params={{hideOnInvalid: false}}
                    inputStyle={{height:'100%'}}
                    submitText={'Load Table'}
                    submitBarStyle={{padding: '2px 3px 3px'}} >
                    <TablePanel tbl_id={tbl_id+'-ui'} tbl_ui_id={tbl_id+'-ui'} tableModel={tableModel} border={false} showTypes={false}
                                showToolbar={false} showFilters={true} selectable={false} showOptionButton={false}/>
                </FormPanel>
            </FieldGroup>
        </div>);
};


export function showUploadTableChooser(setUploadInfo) {
    DialogRootContainer.defineDialog(dialogId,
        <PopupPanel title={'Upload'} layoutPosition={LayoutType.TOP_EDGE_CENTER}>
            <TapUploadPanel {...{setUploadInfo}}/>
        </PopupPanel>
    );
    dispatchShowDialog(dialogId);
}

const TapUploadPanel= ({setUploadInfo,groupKey= 'table-chooser'}) => {
   return (
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
                <Tab name='Loaded Tables' id='tableLoad' style={{fontSize:'larger'}}>
                        <LoadedTables {...{
                            style:{height: '100%', width:'100%'}, keepState: true, groupKey:groupKey+'-tableLoad',
                            onCancel:() => dispatchHideDialog(dialogId),
                            onSubmit:(request) => existingTableSubmit(request, setUploadInfo)
                        }}/>
                </Tab>
            </FieldGroupTabs>
        </div>
    </FieldGroup> );};
