import PropTypes from 'prop-types';
import {FieldGroupCtx} from 'firefly/ui/FieldGroup';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {findTableCenterColumns} from 'firefly/voAnalyzer/TableAnalysis';
import {ColumnFld, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {ExtraButton} from 'firefly/ui/FormPanel';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {showColSelectPopup} from 'firefly/charts/ui/ColSelectView';
import {getSizeAsString} from 'firefly/util/WebUtil';
import React, {useContext, useEffect} from 'react';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';

export function UploadTableSelector({uploadInfo, setUploadInfo, uploadTable=false}) {

    const UploadCenterLonColumns = 'uploadCenterLonColumns';
    const UploadCenterLatColumns = 'uploadCenterLatColumns';

    const {groupKey,setVal, register, unregister}= useContext(FieldGroupCtx);
    const [getLon,setLon]= useFieldGroupValue(UploadCenterLonColumns);
    const [getLat,setLat]= useFieldGroupValue(UploadCenterLatColumns);

    const {fileName,columns,totalRows,fileSize}= uploadInfo ?? {};
    const columnsUsed= columns?.filter( ({use}) => use)?.length ?? 0;
    //TODO: possibly remove this check if we want pos columns *ALWAYS* open for uploaded tables
    const openKey= uploadTable ? 'open' : 'upload-pos-columns';

    useEffect(() => {
        //if user changes position column(s), make the new columns entries selectable in the columns/search
        const columns = uploadInfo?.columns;
        if (getLon()) {
            const cObj= columns.find((col) => col.name === getLon());
            if (cObj) cObj.use = true;
        }
        if (getLat()) {
            const cObj= columns.find((col) => col.name === getLat());
            if (cObj) cObj.use = true;
        }
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
        //when setting uploadInfo, also register, so it shows up in the request object
        const additionalReqObjs = {uploadInfo};
        register('additionalParams', () => additionalReqObjs);
        return () => unregister('additionalParams');
    }, [getLon, getLat]);

    useEffect(() => {
        if (!columns) return;
        const centerCols = findTableCenterColumns({tableData:{columns}}) ?? {};
        const {lonCol='', latCol=''}= centerCols;
        if (!getLon()) setLon(lonCol);
        if (!getLat()) setLat(latCol);
        //TODO: this line can also be removed if we decide to always keep pos columns open for uplaoded tables
        if (!uploadTable) setVal(openKey, (!lonCol || !latCol) ? 'open' : 'closed');
    },[columns]);

    const preSetUploadInfo= (ui) => {
        setLon('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setLat('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setUploadInfo(ui);
    };

    const haveTable= Boolean(fileName && columns);

    const onColsSelected = (selectedColNames) => {
        //get rid of extra quotes within each selectedColNames - because non-alphanumeric entries may have
        //been quoted by calling quoteNonAlphanumeric
        // , e.g.: ['"Object Name"', 'RA', 'Notes']
        selectedColNames = selectedColNames.map((col) => col.replace(/^"(.*)"$/, '$1'));
        const columns = uploadInfo.columns.map((col) => (
            {...col, use:selectedColNames.includes((col.name))}));
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
    };

    return (
        <div style={{margin: '10px 0 0 0'}}>
            <div style={{display:'flex', alignItems:'center'}}>
                <div style={{whiteSpace:'nowrap'}}>Upload Table:</div>
                <div style={{display:'flex', alignItems:'center'}}>
                    <ExtraButton text={fileName ? 'Change...' : 'Add...'}
                                 onClick={() => showUploadTableChooser(preSetUploadInfo)} style={{marginLeft: 42}} />
                    {haveTable &&
                        <div style={{width:200, overflow:'hidden', whiteSpace:'nowrap', fontSize:'larger',
                            textOverflow:'ellipsis', lineHeight:'2em', paddingLeft:15}}>
                            {`${fileName}`}
                        </div>
                    }
                </div>
            </div>
            {haveTable &&
                <div style={{display:'flex', flexDirection:'row', marginLeft: 195, justifyContent:'flex-start'}}>
                    <div style={{whiteSpace:'nowrap'}}>
                        <span>Rows: </span>
                        <span>{totalRows},</span>
                    </div>
                    {!uploadTable && <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        <a className='ff-href'onClick={() => showColSelectPopup(columns, onColsSelected, 'Choose Columns', 'OK',
                            null,true)}>
                            <span>Columns: </span>
                            <span>{columns.length} (using {columnsUsed})</span>
                        </a>
                        {fileSize &&<span>,</span>}
                    </div>}
                    {fileSize && <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        <span>Size: </span>
                        <span>{getSizeAsString(fileSize)}</span>
                    </div>}
                </div>
            }

            {haveTable &&
                <CenterColumns {...{lonCol: getLon(), latCol: getLat(), cols:columns,
                    headerTitle:'Position Columns:', openKey,
                    headerPostTitle:'(from the uploaded table)',
                    headerStyle:{paddingLeft:1},
                    style:{margin:'0 0 10px 195px'},
                    labelStyle:{paddingRight:10},
                    lonKey:UploadCenterLonColumns, latKey:UploadCenterLatColumns, initialState: uploadTable ? 'open' : 'closed'}} />
            }
        </div>
    );
}

UploadTableSelector.propTypes = {
    uploadInfo: PropTypes.object,
    setUploadInfo: PropTypes.func,
    uploadTable: PropTypes.bool
};

export function CenterColumns({lonCol,latCol, style={},cols, lonKey, latKey, openKey, labelStyle,
                           headerTitle, headerPostTitle = '', openPreMessage='', headerStyle, initialState='closed'}) {


    const posHeader= (
        <div style={{marginLeft:-8}}>
            <span style={{fontWeight:'bold', ...headerStyle}}>
                {(lonCol || latCol) ? `${lonCol || 'unset'}, ${latCol || 'unset'}` : 'unset'}
            </span>
            <span style={{paddingLeft:12, whiteSpace:'nowrap'}}>
                {headerPostTitle}
            </span>
        </div>
    );

    return (
        <div style={{margin: '5px 0 0 0',...style}}>
            <div style={{display:'flex'}}>
                <div style={{width:140, marginTop:10, whiteSpace:'nowrap', ...labelStyle}}>
                    {headerTitle}
                </div>
                <FieldGroupCollapsible header={posHeader} headerStyle={{paddingLeft:0}} contentStyle={{marginLeft:4}}
                                       initialState={{value:initialState}} fieldKey={openKey}>
                    {openPreMessage && <div style={{padding:'0 0 10px 0'}}>
                        {openPreMessage}
                    </div>}
                    <ColumnFld fieldKey={lonKey} cols={cols}
                               name='longitude column'  // label that appears in column chooser
                               inputStyle={{overflow:'auto', height:12, width: 100}}
                               tooltip={'Center longitude column for spatial search'}
                               label='Lon Column:'
                               labelWidth={62}
                               validator={getColValidator(cols, true, false)} />
                    <div style={{marginTop: 5}}>
                        <ColumnFld fieldKey={latKey} cols={cols}
                                   name='latitude column' // label that appears in column chooser
                                   inputStyle={{overflow:'auto', height:12, width: 100}}
                                   tooltip={'Center latitude column for spatial search'}
                                   label='Lat Column:'
                                   labelWidth={62}
                                   validator={getColValidator(cols, true, false)} />
                    </div>
                </FieldGroupCollapsible>

            </div>
        </div>
    );
}