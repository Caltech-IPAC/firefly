import {Skeleton, Stack, Typography} from '@mui/joy';
import React, {useContext, useEffect, useState} from 'react';
import {FieldGroupCtx} from 'firefly/ui/FieldGroup';
import {ConstraintContext} from 'firefly/ui/tap/Constraints';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from 'firefly/ui/SimpleComponent';
import {
    DebugObsCore, getPanelPrefix, makeCollapsibleCheckHeader, makeFieldErrorList, makePanelStatusUpdater
} from 'firefly/ui/tap/TableSearchHelpers';
import { ADQL_LINE_LENGTH, getAsEntryForTableName, makeUploadSchema, tapHelpId } from 'firefly/ui/tap/TapUtil';
import {bool,string,object} from 'prop-types';
import {ColsShape, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {InputAreaFieldConnected} from '../InputAreaField.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {SingleCol, UploadTableSelectorSingleCol
} from 'firefly/ui/UploadTableSelectorSingleCol';
import {makeFileRequest, MAX_ROW} from 'firefly/tables/TableRequestUtil';
import {doFetchTable} from 'firefly/tables/TableUtil';

const UploadSingleColumn = 'uploadSingleColumn'; //UploadObjectIDColumn
const ObjectIDColumn = 'objectIDColumn';
const tapPanelTitle = 'Object ID Search';
const siaPanelTitle = 'Observation ID Search';
const panelValue = 'ObjectIDMatch';
const TAB_COLUMNS_MSG = 'This will be matched against Object ID selected from the uploaded table above';
const defaultTblId = 'singleColTable';
const tapEnterList= 'Enter list of object IDs';
const siaEnterList= 'Enter list of observation IDs';
const tapFromTable= 'Load object IDs from a table';
const siaFromTable= 'Load observation IDs from a table';

const SELECT_IN_TOOLTIP=(
        <Typography width='50rem'>
            This option builds the ADQL select statement using a adql 'where IN' instead of using TAP upload.
            Some TAP servers have trouble with upload so using this option may be preferred.
            The results could be slightly different since upload creates more columns.
        </Typography>
);
const panelPrefix = getPanelPrefix(panelValue);
const ENTRY_TYPE='entryType';
const OBJ_ID_ENTRY='objIdEntry';
const ENTER= 'enter';
const UPLOAD= 'upload';

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= [UploadSingleColumn, ObjectIDColumn, OBJ_ID_ENTRY,ENTRY_TYPE];
let savedFileName;

export function ObjectIDSearch({cols, capabilities, tableName, columnsModel, useSIAv2}) {
    const [getUploadInfo, setUploadInfo]=  useFieldGroupValue('uploadInfoObjectID');
    const [getSelectInObjList, setSelectInObjList]=  useFieldGroupValue('selectInObjList');
    const [getUseSelectIn, setUseSelectIn]=  useFieldGroupValue('useSelectIn');
    const entryType=  useFieldGroupValue(ENTRY_TYPE)[0]();
    const objIdEntry=  useFieldGroupValue(OBJ_ID_ENTRY)[0]();
    const [working,setWorking]= useState(false);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const {canUpload=false}= capabilities ?? {};
    const [openMsg, setOpenMsg]= useState(TAB_COLUMNS_MSG);
    const {setVal,getVal,makeFldObj}= useContext(FieldGroupCtx);
    const [clickingSelectCols, setClickingSelectCols] = useState(false);

    const objIdEntryType = [
        {label: useSIAv2 ? siaEnterList : tapEnterList, value: ENTER},
        {label: useSIAv2 ? siaFromTable : tapFromTable, value: UPLOAD}
    ];

    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const uploadInfo= getUploadInfo() || undefined;
    const [constraintResult, setConstraintResult] = useState({});
    const posOpenKey= 'objectID-column-selected-table';

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), useSIAv2? 'Observation ID' : 'Object ID');

    useFieldGroupWatch([UploadSingleColumn, OBJ_ID_ENTRY],
        ([uploadCol,objIdEntry],isInit) => {
            if (isInit) return;
            if (uploadCol||objIdEntry) checkHeaderCtl.setPanelActive(true);
        }
    );

    useEffect(() => {
        if (canUpload && getUseSelectIn()==='use') return;
        setUseSelectIn(!canUpload ? 'use' : '');
    },[canUpload]);

    useEffect(() => {
        if (uploadInfo?.columns && savedFileName!==uploadInfo.fileName) {
            checkHeaderCtl.setPanelActive(true);
            savedFileName= uploadInfo.fileName;
        }
    }, [uploadInfo]);

    useEffect(() => {
        if (entryType===UPLOAD) return;
        const ids= objIdEntry?.split(/[,;\s]/).map( (id) => id.trim()).filter( (id) => id);
        setSelectInObjList(ids?.length ? ids : undefined);
    }, [entryType,objIdEntry]);

    useEffect(() => {
        let isActive = false;
        if (useSIAv2) {
            if (checkHeaderCtl.isPanelActive()) isActive = true;
            return;
        }
        const errMsg= 'Object ID searches require identifying a table column containing an Object ID.  Please provide a column name.';
        const prevObjectIDcol = getVal(ObjectIDColumn);
        let prevObjectColExists = false;
        if (prevObjectIDcol) prevObjectColExists = cols.some((c) => c.name === prevObjectIDcol);
        const objectIDcol = getDefaultObjectIDval(cols);
        if (checkHeaderCtl.isPanelActive()) isActive = true;
        setVal(ObjectIDColumn, prevObjectColExists ? prevObjectIDcol : objectIDcol, {validator: getColValidator(cols, true, false, errMsg), valid: true});
        if (!isActive) checkHeaderCtl.setPanelActive(false); //set PanelActive to false, if it was false before the setVal call above
    }, [columnsModel]);

    useEffect(() => {
        const constraints= makeObjectIDConstraints(makeFldObj(fldListAry), uploadInfo, tableName, canUpload,
            getSelectInObjList(), getUseSelectIn()==='use', useSIAv2);
        updatePanelStatus(constraints, constraintResult, setConstraintResult,useSIAv2);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    return (
        <CollapsibleCheckHeader title={useSIAv2 ? siaPanelTitle : tapPanelTitle}
                                helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <Stack {...{mt: 1/2, spacing:1, position:'relative'}}>
                {working &&  <Skeleton/>}

                <Typography level='body-xs'>Performs an exact match on the ID(s) provided, not a spatial search in the neighborhood of the designated objects.</Typography>
                {!canUpload && !useSIAv2 && <Typography level='body-xs'>This search uses "Select IN" style SQL as this service does not support uploads.</Typography>}
                <RadioGroupInputField {...{
                    fieldKey:ENTRY_TYPE, options:objIdEntryType, initialState:{value: 'enter'},
                    orientation:'horizontal', tooltip:`Enter object ID's as list or load from a table`,
                }} />

                {entryType===ENTER ?
                    <InputAreaFieldConnected {...{ fieldKey:OBJ_ID_ENTRY, placeholderHighlight: true,
                        placeholder:`Enter one or more object id's separated by commas, semi-colon or space`,
                    }}/> :
                    <UploadTableSelectorObjectID {...{uploadInfo,setUploadInfo, setSelectInObjList, getUseSelectIn, setWorking}}/>
                }
                {!useSIAv2 && <SingleCol {...{
                    singleCol: getVal(ObjectIDColumn), cols,
                    headerTitle: 'Object ID (from table):', openKey: posOpenKey,
                    headerPostTitle: '(from the selected table on the right)',
                    openPreMessage:openMsg,
                    headerStyle:{paddingLeft:1},
                    colKey: ObjectIDColumn,
                    colTblId: defaultTblId,
                    colName: 'Object ID',
                    clickingSelectCols,
                    setClickingSelectCols
                }} />}
                {canUpload && entryType===UPLOAD && <CheckboxGroupInputField
                    fieldKey='useSelectIn' alignment='horizontal' initialState={{ value: canUpload ? '' : 'use' }}
                    tooltip={SELECT_IN_TOOLTIP}
                    options={[{label:'Use "select IN" style SQL instead of TAP Upload', value:'use'}]}
                />}
                <DebugObsCore {...{constraintResult}}/>
            </Stack>
        </CollapsibleCheckHeader>
    );
}

ObjectIDSearch.propTypes = {
    cols: ColsShape,
    useSIAv2: bool,
    capabilities: object,
    tableName: string,
    columnsModel: object
};

function getDefaultObjectIDval(cols) {
    const colsWithMetaID = cols.filter((col) => { if (col.ucd) {return col.ucd.includes('meta.id');}  });
    if (colsWithMetaID.length === 0) return '';
    const colWithMetaMain = colsWithMetaID.filter((col) => col.ucd.includes('meta.main'));
    //return first instance of meta.id if meta.id;meta.main doesn't exist
    return colWithMetaMain.length === 0 ? colsWithMetaID[0].name : colWithMetaMain[0].name;
}

export function makeColsLines(objAry) {
    const colSingleLine= objAry.map( (id) => `'${id}'`).join(',') ?? '';
    if (colSingleLine.length < ADQL_LINE_LENGTH) return colSingleLine;

    let multiLineCols = '';
    let line = `\n           '${objAry[0]}'`;
    const objCopy = objAry.slice(1);
    objCopy.forEach((id) => {
        if (id) line+=',';
        if ((line + id).length > ADQL_LINE_LENGTH){
            multiLineCols+= line + '\n';
            line = '           ';
        }
        line += `'${id}'`;
    });
    multiLineCols += line;
    return multiLineCols;
}

export function UploadTableSelectorObjectID({uploadInfo, setUploadInfo, setSelectInObjList, getUseSelectIn, setWorking}) {
    const [getSingleCol,setSingleCol]= useFieldGroupValue(UploadSingleColumn);

    useEffect(() => {
        const columns = uploadInfo?.columns;
        if (columns?.length === 1) {
            setSingleCol(columns[0].name);
        }
        if (getSingleCol()) {
            const cObj = columns.find((col) => col.name === getSingleCol());
            if (cObj) cObj.use = true;
        }
        uploadInfo = { ...uploadInfo, columns };
        setUploadInfo(uploadInfo);
        if (getUseSelectIn() === 'use') {
            loadTableColumn(getSingleCol(), uploadInfo.serverFile, setSelectInObjList, setWorking);
        }
    }, [getSingleCol, getUseSelectIn]);

    return ( <UploadTableSelectorSingleCol uploadInfo={uploadInfo} setUploadInfo={setUploadInfo}
        headerTitle={'Uploaded Object ID:'} colName={'Object ID'} getUseSelectIn={getUseSelectIn}
    />);
}

async function getColumnFromUploadTable(tableOnServer, columnName) {
    const params= { startIdx : 0, pageSize : MAX_ROW, inclCols : `"${columnName}"` };
    const tableModel= await doFetchTable(makeFileRequest('',tableOnServer,undefined,params));
    return tableModel?.tableData?.data?.map( ([d]) => d);
}

const loadedColumns= new Map();

function loadTableColumn(singleCol,serverFile,setSelectInObjList,setWorking) {
    if (singleCol && serverFile) {
        const cachedCol = loadedColumns.get(serverFile + '---' + singleCol);
        if (cachedCol) {
            setSelectInObjList(cachedCol);
        } else {
            setTimeout(
                async () => {
                    try {
                        setWorking(true);
                        const list = await getColumnFromUploadTable(serverFile, singleCol);
                        loadedColumns.set(serverFile + '---' + singleCol, list);
                        setSelectInObjList(list);
                        setWorking(false);
                    } catch (e) {
                        setWorking(false);
                        setSelectInObjList(undefined);
                    }
                }, 5);
        }

    } else {
        setSelectInObjList(undefined);
    }
}


function makeObjectIDConstraints(fldObj, uploadInfo, tableName, canUpload, selectInObjList, useSelectIn, useSIAv2) {
    const {fileName,serverFile, columns:uploadColumns, totalRows, fileSize}= uploadInfo ?? {};
    const { [UploadSingleColumn]:uploadObjectIDCol, [ObjectIDColumn]:objectIDCol, [ENTRY_TYPE]:entryType }= fldObj;
    const errList= makeFieldErrorList();
    let adqlConstraint;
    let siaConstraints= [];
    const uploadedObjectID = uploadObjectIDCol?.value;
    const objectID = objectIDCol?.value;
    const type= entryType?.value;

    if (type===UPLOAD) {
        errList.checkForError(uploadObjectIDCol);
        errList.checkForError(objectIDCol);
        if (!uploadedObjectID && !objectID) errList.addError('The Uploaded Table and Selected Table Object IDs are not set.');
        else if (!uploadedObjectID) errList.addError('Uploaded Table Object ID is not set');
        else if (!objectID) errList.addError('Selected Table (on the right) Object ID is not set');
    }
    else if (!useSIAv2) {
        if (!objectID) errList.addError('Selected Table (on the right) Object ID is not set');
        else if (!selectInObjList?.length) errList.addError(`Object id's are not set`); 
    }

    if (useSelectIn || type===ENTER) {
        if (selectInObjList?.length) {
            const str= makeColsLines(selectInObjList);
            adqlConstraint = `${objectID} IN (${str})`;
            siaConstraints= selectInObjList.map( (id) => `ID=${id}`);
        }
        else {
            errList.addError(`Enter at least one ${useSIAv2?'observation ID':'object ID'}`);
        }
    }
    else if (!useSIAv2) {
        const preFix= (serverFile && canUpload) ? `${getAsEntryForTableName(tableName)}` : '';
        adqlConstraint = `(ut.${uploadedObjectID} = ${preFix}.${objectID})`;
    }

    const errAry= errList.getErrors();
    return {
        valid: errAry.length === 0, errAry,
        adqlConstraintsAry: adqlConstraint ? [adqlConstraint] : [],
        siaConstraints,
        TAP_UPLOAD: (useSelectIn || type===ENTER) ? undefined : makeUploadSchema(fileName, serverFile, uploadColumns, totalRows, fileSize),
        uploadFile: fileName
    };
}