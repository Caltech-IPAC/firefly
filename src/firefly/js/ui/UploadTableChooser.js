import {Stack, Typography, Box} from '@mui/joy';
import React from 'react';

import {dispatchHideDialog, dispatchShowDialog} from '../core/ComponentCntlr.js';
import {FileAnalysisType, Format} from '../data/FileAnalysis.js';
import {findTableCenterColumns} from '../voAnalyzer/TableAnalysis.js';
import {convertFitsTableDataType, isFitsTableDataTypeArray} from '../visualize/FitsHeaderUtil.js';
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
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr';
import {MetaConst} from 'firefly/data/MetaConst';
import {makeFileRequest} from 'firefly/api/ApiUtilTable';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr';

const dialogId = 'Upload-spatial-table';
const UPLOAD_TBL_SOURCE= 'UPLOAD_TBL_SOURCE';
const EXISTING_TBL_SOURCE= 'EXISTING_TBL_SOURCE';


function getFitsColumnInfo(data) {
    return data
        .filter( (row) => row[1].startsWith('TTYPE'))
        .map(([,,name],idx) => {
            const fdt= data.find( (c) => c[1]===`TFORM${idx+1}`)?.[2];
            const type= convertFitsTableDataType(fdt) ?? 'string';
            return ({
                    name, use:true, description:'',
                    type:  type + (isFitsTableDataTypeArray(fdt)?'[*]':''),
                    unit: data.find( (c) => c[1]===`TUNIT${idx+1}`)?.[2] ?? '',
                }
            );
        });
}

/**
 * handle submit for an uploaded table
 * @param request
 * @param setUploadInfo
 * @param {DefaultColsEnabled} defaultColsEnabled

 * @returns {boolean}
 */
let tblCount = 0;
function uploadSubmit(request,setUploadInfo,defaultColsEnabled)  {
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
    const columnsSelected = applyDefColumnSelection(columns,defaultColsEnabled);
    tblCount++;
    const META_INFO= {
        [MetaConst.CATALOG_OVERLAY_TYPE]:'TRUE',
        [MetaConst.UPLOAD_TABLE]:'TRUE'
    };

    const options=  {
        META_INFO,
        tbl_id: 'Upload_Tbl_'+tblCount
    };
    const tblReq = makeFileRequest('Upload_Tbl_'+tblCount, serverFile, null, options);
    //tblReq.tbl_id = 'Upload_Tbl_' + tblReq.tbl_id;
    const uploadInfo = {serverFile, fileName, columns:columnsSelected, totalRows, fileSize, tableSource: UPLOAD_TBL_SOURCE, tbl_id: tblReq.tbl_id};
    dispatchTableSearch(tblReq);
    setUploadInfo(uploadInfo);
    dispatchHideDialog(dialogId);
    return false;
}

/**
 * handle submit for an existing table
 * @param request
 * @param setUploadInfo
 * @param {DefaultColsEnabled} defaultColsEnabled
 * @returns {boolean}
 */
function existingTableSubmit(request,setUploadInfo,defaultColsEnabled) {
    if (!request) return false;
    const tbl = getTblById('existing-table-list-ui');
    const idx = tbl.highlightedRow;
    const activeTblId = tbl.tableData.data[idx][3]; //tbl_id
    const activeTbl = getTableUiByTblId(activeTblId);
    const tableRequest = activeTbl.request;
    const columnData = activeTbl.columns;
    const columns = columnData.map((col) => col.visibility === 'hide' || col.visibility === 'hidden'? ({...col, use:false}) :  ({...col, use:true})); //filter out hidden cols
    const columnsSelected = applyDefColumnSelection(columns,defaultColsEnabled);

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
            title: tableRequest?.META_INFO?.title,
            fileName: activeTbl.title,
            tbl_id: activeTblId,
            columns:columnsSelected,
            totalRows: activeTbl.totalRows,
            tableSource: EXISTING_TBL_SOURCE
        };
        tblCount++;
        setUploadInfo(uploadInfo);
        dispatchHideDialog(dialogId);
    });
    return false;
}

/**
 * Return the default cols to be selected for the uploaded table based on colTypes and colCount
 *
 * @param columns
 * @param colTypes comes from {@link DefaultColsEnabled}
 * @param colCount comes from {@link DefaultColsEnabled}
 * @returns defaultCols
 */
function defaultColumnsSelector(columns,colTypes,colCount) {
    if (!columns?.length) return;

    let cols = columns
        ?.filter((column) => colTypes.includes(column.type))
        .slice(0, colCount);

    cols = cols?.map((col) => col.name.replace(/^"(.*)"$/, '$1'));
    const defautltCols = columns?.map((col) => (
        {...col, use:cols.includes((col.name))}));

    return defautltCols;
}

/**
 * Set use to true or false for column entries to help set default selected columns for uploaded table
 *
 * @param columns
 * @param {DefaultColsEnabled} defaultColsEnabled
 * @returns columnsSelected
 */
function applyDefColumnSelection(columns,defaultColsEnabled) {
    let columnsSelected;
    if (defaultColsEnabled) { //default cols enabled
        const {colTypes,colCount} = defaultColsEnabled;
        columnsSelected = defaultColumnsSelector(columns,colTypes,colCount);
    }
    else {
        const {lonCol='', latCol=''} = findTableCenterColumns({tableData:{columns}}) ?? {}; //centerCols
        columnsSelected = columns.map((col) => col.name === lonCol || col.name === latCol? ({...col, use:true}) :  ({...col, use:false})); //select position cols only
    }
    return columnsSelected?.filter( (c) => c.visibility!=='hidden');
}

const NoTables = () => {
    return (
        <Typography level={'title-lg'} color={'neutral'} textAlign='center' width={1} height={1} pt={2} mt={2}>
            {'No tables available to load.'}
        </Typography>
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
        const title = [tables[tblId].title,
            (tbl.tableData.columns?.filter( (c) => c.visibility!=='hidden').length).toString(),
            (tbl.totalRows).toString(), tblId];
        data.push(title);
    }
    if (data.length === 0) {
        return <NoTables/>;
    }
    const columns = [{name: 'Table Title', width: 40}, {name: 'No. of Cols', width: 15}, {name: 'No. of Rows', width:15},
        {name: 'tblId', visibility: 'hidden'}];
    const highlightedRow = 0; //default selection
    const tbl_id = 'existing-table-list';

    const tableModel = {tbl_id, tableData: {columns, data}, highlightedRow, totalRows: data.length};

    return (
        <Stack width={1} height={1}>
            <Typography level={'title-lg'} color={'neutral'} p={1}>
                {'Select one of the existing tables below to load into the TAP panel: '}
            </Typography>
            <FieldGroup groupKey={groupKey} keepState={keepState} sx={{flexGrow: 1}}>
                <FormPanel onSuccess={onSubmit} onCancel={onCancel} completeText='Load Table'
                    slotProps={{
                        searchBar: {p:1/2},
                    }}>

                    <TablePanel tbl_id={tbl_id+'-ui'} tbl_ui_id={tbl_id+'-ui'} tableModel={tableModel} border={false} showTypes={false}
                                sx={{position: 'absolute', inset:0}}
                                showToolbar={false} showFilters={true} selectable={false} showOptionButton={false}/>
                </FormPanel>
            </FieldGroup>
        </Stack>);
};

/**
 * @typedef {object} DefaultColsEnabled
 *
 * @prop {Array.<String>} colTypes
 * @prop {int} colCount
 */


/**
 * Display the upload table chooser popup panel
 *
 * @param setUploadInfo
 * @param groupKey
 * @param {DefaultColsEnabled} defaultColsEnabledObj if this is non-empty, it will be used to replace the default selection of the uploaded table cols
 */
export function showUploadTableChooser(setUploadInfo,groupKey= 'table-chooser',defaultColsEnabledObj=undefined) {
    DialogRootContainer.defineDialog(dialogId,
        <PopupPanel title={'Upload'} layoutPosition={LayoutType.TOP_EDGE_CENTER}>
            <TapUploadPanel {...{setUploadInfo,groupKey,defaultColsEnabledObj}}/>
        </PopupPanel>
    );
    dispatchShowDialog(dialogId);
}

const TapUploadPanel= ({setUploadInfo,groupKey= 'table-chooser',defaultColsEnabledObj}) => {
    return (
        <Stack height='35rem' sx={{resize:'both', overflow:'hidden',minHeight:'35rem', minWidth:'40rem'}}>
            <FieldGroup groupKey={groupKey} sx={{ flexGrow: 1}}>
                <Stack height='100%' pt={1}>
                    <FieldGroupTabs initialState={{value: 'upload'}} fieldKey='upload-type-tabs' groupKey={groupKey}
                                    sx={{width: 1,  flex: '1 1 auto'}}>
                        <Tab name='Upload Tables' id='upload' sx={{fontSize:'larger'}}>
                            <FileUploadDropdown {...{
                                sx:{height:1,
                                    '.ff-FileUploadViewPanel-file':{ml:3},
                                    '.ff-FileUploadViewPanel-acceptList':{ml:3},
                                },
                                acceptOneItem:true, acceptList:[TABLES], keepState:true, groupKey:groupKey+'-fileUpload',
                                onCancel:() => dispatchHideDialog(dialogId),
                                onSubmit:(request) => uploadSubmit(request,setUploadInfo,defaultColsEnabledObj),
                            }}/>
                        </Tab>
                        <Tab name='Loaded Tables' id='tableLoad' sx={{fontSize:'larger'}}>
                                <LoadedTables {...{
                                    keepState: true, groupKey:groupKey+'-tableLoad',
                                    onCancel:() => dispatchHideDialog(dialogId),
                                    onSubmit:(request) => existingTableSubmit(request,setUploadInfo,defaultColsEnabledObj)
                                }}/>
                        </Tab>
                    </FieldGroupTabs>
                </Stack>
            </FieldGroup>
        </Stack>);};
