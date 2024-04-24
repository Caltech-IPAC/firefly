import {Box, Chip, Link, Stack, Typography} from '@mui/joy';
import React, {useContext, useEffect, useState} from 'react';
import {FieldGroupCtx} from 'firefly/ui/FieldGroup';
import {ConstraintContext} from 'firefly/ui/tap/Constraints';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from 'firefly/ui/SimpleComponent';
import {
    getPanelPrefix,
    makeCollapsibleCheckHeader,
    makeFieldErrorList,
    makePanelStatusUpdater
} from 'firefly/ui/tap/TableSearchHelpers';
import {
    ADQL_LINE_LENGTH, getAsEntryForTableName, makeUploadSchema, maybeQuote, tapHelpId
} from 'firefly/ui/tap/TapUtil';
import PropTypes from 'prop-types';
import {TextButton} from 'firefly/visualize/ui/Buttons.jsx';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {showColSelectPopup} from 'firefly/charts/ui/ColSelectView';
import {getSizeAsString} from 'firefly/util/WebUtil';
import {ColsShape, ColumnFld, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';
import {doFetchTable, onTableLoaded} from 'firefly/tables/TableUtil';
import {get} from 'lodash';
import {FilterInfo} from 'firefly/tables/FilterInfo';
import {dispatchTableFilter} from 'firefly/tables/TablesCntlr';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';

const UploadObjectIDColumn = 'uploadObjectIDColumn';
const ObjectIDColumn = 'objectIDColumn';
const panelTitle = 'Object ID Search';
const panelValue = 'ObjectIDMatch';
const TAB_COLUMNS_MSG = 'This will be matched against Object ID selected from the uploaded table above';
const defaultTblId = 'objectID-table';

const SELECT_IN_TOOLTIP=(
        <Typography width='50rem'>
            This option builds the ADQL select statement using a adql 'where IN' instead of using TAP upload.
            Some TAP servers have trouble with upload so using this option may be preferred.
            The results could be slightly different since upload creates more columns.
        </Typography>
);
const panelPrefix = getPanelPrefix(panelValue);

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= [UploadObjectIDColumn, ObjectIDColumn];
let savedFileName;

export function ObjectIDSearch({cols, capabilities, tableName, columnsModel}) {
    const [getUploadInfo, setUploadInfo]=  useFieldGroupValue('uploadInfoObjectID');
    const [getSelectInObjList, setSelectInObjList]=  useFieldGroupValue('selectInObjList');
    const [getUseSelectIn, setUseSelectIn]=  useFieldGroupValue('useSelectIn');
    const [working,setWorking]= useState(false);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const {canUpload=false}= capabilities ?? {};
    const [openMsg, setOpenMsg]= useState(TAB_COLUMNS_MSG);
    const {setVal,getVal,makeFldObj}= useContext(FieldGroupCtx);
    const [clickingSelectCols, setClickingSelectCols] = useState(false);


    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const uploadInfo= getUploadInfo() || undefined;
    const [constraintResult, setConstraintResult] = useState({});
    const posOpenKey= 'objectID-column-selected-table';

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), 'ObjectID');

    useFieldGroupWatch([UploadObjectIDColumn, ObjectIDColumn],
        ([uploadCol,selectedTableCol],isInit) => {
            if (isInit) return;
            if (uploadCol||selectedTableCol) checkHeaderCtl.setPanelActive(true);
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
        const errMsg= 'Object ID searches require identifying a table column containing an Object ID.  Please provide a column name.';
        const prevObjectIDcol = getVal(ObjectIDColumn);
        let prevObjectColExists = false;
        if (prevObjectIDcol) prevObjectColExists = cols.some((c) => c.name === prevObjectIDcol);
        const objectIDcol = getDefaultObjectIDval(cols);
        let isActive = false;
        if (checkHeaderCtl.isPanelActive()) isActive = true;
        setVal(ObjectIDColumn, prevObjectColExists ? prevObjectIDcol : objectIDcol, {validator: getColValidator(cols, true, false, errMsg), valid: true});
        if (!isActive) checkHeaderCtl.setPanelActive(false); //set PanelActive to false, if it was false before the setVal call above
    }, [columnsModel]);

    useEffect(() => {
        const constraints= makeObjectIDConstraints(makeFldObj(fldListAry), uploadInfo, tableName, canUpload,
            getUseSelectIn()==='use',getSelectInObjList());
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <Stack {...{mt: 1/2, spacing:1}}>
                <Typography level='body-xs'>Performs an exact match on the ID(s) provided, not a spatial search in the neighborhood of the designated objects.</Typography>
                {!canUpload && <Typography level='body-xs'>This search uses "Select IN" style SQL as this service does not support uploads.</Typography>}
                <UploadTableSelectorObjectID {...{uploadInfo,setUploadInfo, setSelectInObjList, getUseSelectIn, setWorking}}/>
                <ObjectIDCol {...{
                    objectCol: getVal(ObjectIDColumn), cols,
                    headerTitle: 'Object ID (from table):', openKey: posOpenKey,
                    headerPostTitle: '(from the selected table on the right)',
                    openPreMessage:openMsg,
                    headerStyle:{paddingLeft:1},
                    objectKey: ObjectIDColumn,
                    colTblId: defaultTblId,
                    clickingSelectCols,
                    setClickingSelectCols
                }} />
                {canUpload && <CheckboxGroupInputField
                    fieldKey='useSelectIn' alignment='horizontal' initialState={{ value: canUpload ? '' : 'use' }}
                    tooltip={SELECT_IN_TOOLTIP}
                    options={[{label:'Use "select IN" style SQL instead of TAP Upload', value:'use'}]}
                />}
                {working &&  <div className='loading-mask'/>}

            </Stack>
        </CollapsibleCheckHeader>
    );
}

ObjectIDSearch.propTypes = {
    cols: ColsShape,
    capabilities: PropTypes.object,
    tableName: PropTypes.string,
    columnsModel: PropTypes.object
};

function filterTable(cols, tbl) {
    const colsWithMetaID = cols.filter((col) => { if (col.ucd) {return col.ucd.includes('meta.id');}  });
    if (colsWithMetaID.length > 1) {
        if (!tbl) return;
        const filterInfo = get(tbl, 'request.filters');
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const meta = 'meta.id';
        const filter = `like '%${meta}%'`;
        filterInfoCls.setFilter('UCD', filter);
        const newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
    }
}

function getDefaultObjectIDval(cols) {
    const colsWithMetaID = cols.filter((col) => { if (col.ucd) {return col.ucd.includes('meta.id');}  });
    if (colsWithMetaID.length === 0) return '';
    const colWithMetaMain = colsWithMetaID.filter((col) => col.ucd.includes('meta.main'));
    //return first instance of meta.id if meta.id;meta.main doesn't exist
    return colWithMetaMain.length === 0 ? colsWithMetaID[0].name : colWithMetaMain[0].name;
}

async function getColumnFromUploadTable(tableOnServer, columnName) {
    const params= { startIdx : 0, pageSize : MAX_ROW, inclCols : `"${columnName}"` };
    const tableModel= await doFetchTable(makeFileRequest('',tableOnServer,undefined,params));
    return tableModel?.tableData?.data?.map( ([d]) => d);
}

const loadedColumns= new Map();


function loadTableColumn(objectCol,serverFile,setSelectInObjList,setWorking) {
    if (objectCol && serverFile) {
        const cachedCol = loadedColumns.get(serverFile + '---' + objectCol);
        if (cachedCol) {
            setSelectInObjList(cachedCol);
        } else {
            setTimeout(
                async () => {
                    try {
                        setWorking(true);
                        const list = await getColumnFromUploadTable(serverFile, objectCol);
                        loadedColumns.set(serverFile + '---' + objectCol, list);
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





function UploadTableSelectorObjectID({uploadInfo, setUploadInfo, setSelectInObjList,getUseSelectIn,setWorking}) {
    const [getObjectCol,setObjectCol]= useFieldGroupValue(UploadObjectIDColumn);
    const {fileName,columns,totalRows,fileSize}= uploadInfo ?? {};
    const columnsUsed= columns?.filter( ({use}) => use)?.length ?? 0;
    const openKey= 'upload-objectID-column';

    useEffect(() => {
        //if user changes position column(s), make the new columns entries selectable in the columns/search
        const columns = uploadInfo?.columns;
        if (columns?.length===1) {
            setObjectCol(columns[0].name);
        }
        if (getObjectCol()) {
            const cObj= columns.find((col) => col.name === getObjectCol());
            if (cObj) cObj.use = true;
        }
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
        if (getUseSelectIn()==='use') {
            loadTableColumn(getObjectCol(),uploadInfo.serverFile,setSelectInObjList,setWorking);
        }
    }, [getObjectCol, getUseSelectIn]);

    const preSetUploadInfo= (ui) => {
        setObjectCol('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setUploadInfo(ui);
    };

    const haveFile= Boolean(fileName && columns);

    const onColsSelected = (selectedColNames) => {
        //get rid of extra quotes within each selectedColNames - because non-alphanumeric entries may have
        //been quoted by calling quoteNonAlphanumeric
        // , e.g.: ['"Object Name"', 'RA', 'Notes']
        selectedColNames = selectedColNames.map((col) => col.replace(/^"(.*)"$/, '$1'));
        const columns = uploadInfo?.columns.map((col) => (
            {...col, use:selectedColNames.includes((col.name))}));
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
    };

    return (
        <div style={{margin: '10px 0 0 0'}}>
            <div style={{display:'flex', alignItems:'center'}}>
                <div style={{display:'flex', alignItems:'center'}}>
                    <TextButton text={fileName ? 'Change Upload Table...' : 'Add Upload Table...'}
                                 onClick={() => showUploadTableChooser(preSetUploadInfo,'objectIDMatch',{colTypes: ['int','long','string'],colCount: 3})} style={{marginLeft: 42}} />
                    {haveFile &&
                        <div style={{width:200, overflow:'hidden', whiteSpace:'nowrap', fontSize:'larger',
                            textOverflow:'ellipsis', lineHeight:'2em', paddingLeft:15}}>
                            {`${fileName}`}
                        </div>
                    }
                </div>
            </div>
            {haveFile &&
                <div style={{display:'flex', flexDirection:'row', marginLeft: 195, justifyContent:'flex-start'}}>
                    <div style={{whiteSpace:'nowrap'}}>
                        <span>Rows: </span>
                        <span>{totalRows},</span>
                    </div>
                    <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        {getUseSelectIn()!=='use' &&
                            <>
                                <Chip onClick={() => showColSelectPopup(columns, onColsSelected, 'Choose Columns', 'OK',
                                    null,true)}>
                                    <span>Columns: </span>
                                    <span>{columns.length} (using {columnsUsed})</span>
                                </Chip>
                                {fileSize &&<span>,</span>}
                            </>
                        }
                    </div>
                    {fileSize && <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        <span>Size: </span>
                        <span>{getSizeAsString(fileSize)}</span>
                    </div>}
                </div>
            }
            {haveFile &&
            <ObjectIDCol {...{objectCol: getObjectCol(), cols:columns,
                headerTitle:'Uploaded Object ID:', openKey,
                headerPostTitle:'(from the uploaded table)',
                headerStyle:{paddingLeft:1},
                style:{margin:'0 0 10px 195px'},
                objectKey:UploadObjectIDColumn}} />
            }
        </div>
    );
}

function ObjectIDCol({objectCol, style={},cols, objectKey, openKey,
                           headerTitle, headerPostTitle = '', openPreMessage='', headerStyle,colTblId=null}) {
    const posHeader= (
        <Box ml={-1}>
            <Typography display='inline' color={!objectCol?'warning':undefined} level='title-md' style={{...headerStyle}}>
                {(objectCol) ? `${objectCol || 'unset'}` : 'unset'}
            </Typography>
            <Typography display='inline' level='body-sm' pl={3}  whiteSpace='nowrap'>
                {headerPostTitle}
            </Typography>
        </Box>
    );

    const [clickingSelectCols, setClickingSelectCols] = useState(false);

    //clickingSelectCols is toggled each time user clicks on the 'magnifying glass' to select cols, rendering this useEffect
    useEffect(() => {
        onTableLoaded(defaultTblId).then((tbl) => {
            filterTable(cols,tbl);
        });
    }, [clickingSelectCols]);

    return (
        <div style={{margin: '5px 0 0 0',...style}}>
            <Stack {...{direction:'row', spacing:1}}>
                <Typography sx={{width:'14rem', pt:1, whiteSpace:'nowrap'}} component='div'>
                    {headerTitle}
                </Typography>
                <FieldGroupCollapsible header={posHeader}
                                       initialState={{value:'open'}} fieldKey={openKey}>
                    {openPreMessage && <div style={{padding:'0 0 10px 0'}}>
                        {openPreMessage}
                    </div>}
                    <ColumnFld fieldKey={objectKey} cols={cols}
                               name='Object ID Column'  // label that appears in column chooser
                               tooltip='Object ID Column'
                               label='Object ID'
                               placeholder='choose object id column'
                               validator={getColValidator(cols, true, false)}
                               colTblId={colTblId}
                               onSearchClicked= {() => {
                                   if (clickingSelectCols) setClickingSelectCols(false);
                                   else setClickingSelectCols?.(true);
                                   return true;
                                }}
                    />
                </FieldGroupCollapsible>

            </Stack>
        </div>
    );
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



function makeObjectIDConstraints(fldObj, uploadInfo, tableName, canUpload, useSelectIn, selectInObjList) {
    const {fileName,serverFile, columns:uploadColumns, totalRows, fileSize}= uploadInfo ?? {};
    const {[UploadObjectIDColumn]:uploadObjectIDCol, [ObjectIDColumn]:objectIDCol }= fldObj;
    const errList= makeFieldErrorList();
    let adqlConstraint;
    const uploadedObjectID = uploadObjectIDCol?.value;
    const objectID = objectIDCol?.value;
    errList.checkForError(uploadObjectIDCol);
    errList.checkForError(objectIDCol);
    if (!uploadedObjectID && !objectID) errList.addError('The Uploaded Table and Selected Table Object IDs are not set.');
    else if (!uploadedObjectID) errList.addError('Uploaded Table Object ID is not set');
    else if (!objectID) errList.addError('Selected Table (on the right) Object ID is not set');

    if (useSelectIn && selectInObjList?.length) {
        const str= makeColsLines(selectInObjList);
        adqlConstraint = `${objectID} IN (${str})`;
    }
    else {
        const preFix= (serverFile && canUpload) ? `${getAsEntryForTableName(tableName)}` : '';
        adqlConstraint = `(ut.${uploadedObjectID} = ${preFix}.${objectID})`;
    }

    const errAry= errList.getErrors();
    return {
        valid: errAry.length === 0, errAry,
        adqlConstraintsAry: adqlConstraint ? [adqlConstraint] : [],
        siaConstraints: [],
        siaConstraintErrors: [],
        TAP_UPLOAD: useSelectIn ? undefined : makeUploadSchema(fileName, serverFile, uploadColumns, totalRows, fileSize),
        uploadFile: fileName
    };
}