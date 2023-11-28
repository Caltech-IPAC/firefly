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
import {getAsEntryForTableName, makeUploadSchema, tapHelpId} from 'firefly/ui/tap/TapUtil';
import PropTypes from 'prop-types';
import {ExtraButton} from 'firefly/ui/FormPanel';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {showColSelectPopup} from 'firefly/charts/ui/ColSelectView';
import {getSizeAsString} from 'firefly/util/WebUtil';
import {ColsShape, ColumnFld, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';
import {onTableLoaded} from 'firefly/tables/TableUtil';
import {get} from 'lodash';
import {FilterInfo} from 'firefly/tables/FilterInfo';
import {dispatchTableFilter} from 'firefly/tables/TablesCntlr';

const UploadObjectIDColumn = 'uploadObjectIDColumn';
const ObjectIDColumn = 'objectIDColumn';
const panelTitle = 'Object ID Search';
const panelValue = 'ObjectIDMatch';
const TAB_COLUMNS_MSG = 'This will be matched against Object ID selected from the uploaded table above';
const defaultTblId = 'objectID-table';
const panelPrefix = getPanelPrefix(panelValue);

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= [UploadObjectIDColumn, ObjectIDColumn];
let savedFileName;

export function ObjectIDSearch({cols, capabilities, tableName, columnsModel}) {
    const [getUploadInfo, setUploadInfo]=  useFieldGroupValue('uploadInfoObjectID');
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
        const constraints= makeObjectIDConstraints(makeFldObj(fldListAry), uploadInfo, tableName, canUpload);
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    if (!canUpload) return <div />; //this service does not support uploads

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <div style={{marginTop: 5}}>
                <div style={{fontStyle: 'italic'}}>Performs an exact match on the ID(s) provided, not a spatial search in the neighborhood of the designated objects.</div>
                <UploadTableSelectorObjectID {...{uploadInfo,setUploadInfo}}/>
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
            </div>
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

function UploadTableSelectorObjectID({uploadInfo, setUploadInfo}) {
    const [getObjectCol,setObjectCol]= useFieldGroupValue(UploadObjectIDColumn);
    const {fileName,columns,totalRows,fileSize}= uploadInfo ?? {};
    const columnsUsed= columns?.filter( ({use}) => use)?.length ?? 0;
    const openKey= 'upload-objectID-column';

    useEffect(() => {
        //if user changes position column(s), make the new columns entries selectable in the columns/search
        const columns = uploadInfo?.columns;
        if (getObjectCol()) {
            const cObj= columns.find((col) => col.name === getObjectCol());
            if (cObj) cObj.use = true;
        }
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
    }, [getObjectCol]);

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
                    <ExtraButton text={fileName ? 'Change Upload Table...' : 'Add Upload Table...'}
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
                        <a className='ff-href' onClick={() => showColSelectPopup(columns, onColsSelected, 'Choose Columns', 'OK',
                            null,true)}>
                            <span>Columns: </span>
                            <span>{columns.length} (using {columnsUsed})</span>
                        </a>
                        {fileSize &&<span>,</span>}
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
                labelStyle:{paddingRight:10},
                objectKey:UploadObjectIDColumn}} />
            }
        </div>
    );
}

function ObjectIDCol({objectCol, style={},cols, objectKey, openKey, labelStyle,
                           headerTitle, headerPostTitle = '', openPreMessage='', headerStyle,colTblId=null}) {
    const posHeader= (
        <div style={{marginLeft:-8}}>
            <span style={{fontWeight:'bold', ...headerStyle}}>
                {(objectCol) ? `${objectCol || 'unset'}` : 'unset'}
            </span>
            <span style={{paddingLeft:12, whiteSpace:'nowrap'}}>
                {headerPostTitle}
            </span>
        </div>
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
            <div style={{display:'flex'}}>
                <div style={{width:140, marginTop:10, whiteSpace:'nowrap', ...labelStyle}}>
                    {headerTitle}
                </div>
                <FieldGroupCollapsible header={posHeader} headerStyle={{paddingLeft:0}} contentStyle={{marginLeft:4}}
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

            </div>
        </div>
    );
}

function makeObjectIDConstraints(fldObj, uploadInfo, tableName, canUpload) {
    const {fileName,serverFile, columns:uploadColumns, totalRows, fileSize}= uploadInfo ?? {};
    const {[UploadObjectIDColumn]:uploadObjectIDCol, [ObjectIDColumn]:objectIDCol }= fldObj;

    const errList= makeFieldErrorList();
    const uploadedObjectID = uploadObjectIDCol?.value;
    const objectID = objectIDCol?.value;
    const tabAs= 'ut';
    errList.checkForError(uploadObjectIDCol);
    errList.checkForError(objectIDCol);
    if (!uploadedObjectID && !objectID) errList.addError('The Uploaded Table and Selected Table Object IDs are not set.');
    else if (!uploadedObjectID) errList.addError('Uploaded Table Object ID is not set');
    else if (!objectID) errList.addError('Selected Table (on the right) Object ID is not set');
    const validUpload= Boolean(serverFile && canUpload);
    const preFix= validUpload ? `${getAsEntryForTableName(tableName)}` : '';
    const adqlConstraint = `(${tabAs}.${uploadedObjectID} = ${preFix}.${objectID})`;

    const errAry= errList.getErrors();
    return {
        valid: errAry.length === 0, errAry,
        adqlConstraintsAry: adqlConstraint ? [adqlConstraint] : [],
        siaConstraints: [],
        siaConstraintErrors: [],
        TAP_UPLOAD: makeUploadSchema(fileName, serverFile, uploadColumns, totalRows, fileSize),
        uploadFile: fileName
    };
}