/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Divider, Sheet, Skeleton, Stack, Typography} from '@mui/joy';
import React, {useContext, useEffect, useState} from 'react';
import {MetaConst} from '../../data/MetaConst';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {set, merge, isFunction} from 'lodash';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {doFetchTable} from '../../tables/TableUtil.js';
import {makeIrsaCatalogRequest} from '../../tables/TableRequestUtil.js';
import {CatalogConstraintsPanel} from './CatalogConstraintsPanel.jsx';
import {FieldGroup, FieldGroupCtx} from '../../ui/FieldGroup.jsx';
import {ServerParams} from '../../data/ServerParams.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {CatalogSearchMethodType, SpatialMethod} from '../../ui/CatalogSearchMethodType.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {parseWorldPt} from '../Point';
import {convertAngle} from '../VisUtil.js';
import {getGatorProtoServiceId} from './GatorProtocolUtil';
import {masterTableFilter} from './IrsaMasterTableFilters.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';

import {PREF_KEY} from 'firefly/tables/TablePref.js';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent.jsx';
import {OptionListField} from 'firefly/ui/OptionListField.jsx';
import {GridMask} from 'firefly/ui/panel/MaskPanel.jsx';

/** * @deprecated */
export const irsaCatalogGroupKey = 'CATALOG_PANEL';

const RADIUS_COL = '7';
const constraintskey = 'inputconstraint';


const makeSpacialGroupKey= (groupKey) => groupKey+'__spacial';

export function IrsaCatalogSearchDefault() {
    return (
        <FieldGroup groupKey='CATALOG_PANEL_DEFAULT' keepState={true} sx={{position:'relative', flexGrow:1}}>
            <IrsaCatalogSearch sx={{margin:'0 auto'}} showSqlSection={true} title='IRSA Catalogs'/>
        </FieldGroup>
    );
}




export function IrsaCatalogSearch({sx, serviceUrl, showSqlSection, searchOptionsMask, title}) {
    const [working, setWorking]= useState(true);
    const [masterTableData, setMasterTableData]= useState(undefined);
    const {groupKey} = useContext(FieldGroupCtx);
    const [getValP,setValP] = useFieldGroupValue('project');
    const [getValC,setValC]= useFieldGroupValue('catalog');

    useEffect( () => {
        setValP(undefined);
        setValC(undefined);
        const getMaster= async () => {
            setMasterTableData(await getMasterTable(serviceUrl));
            setWorking(false);
        };
        if (getMasterFromCache(serviceUrl)) {
            setWorking(false);
            setMasterTableData(getMasterFromCache(serviceUrl));
        }
        else {
            setWorking(true);
            void getMaster();
        }
    }, [serviceUrl]);

    const showWarning= !working && !masterTableData;

    return (
        <FormPanel sx={{position:'relative', inset:0, overflow:'hidden', maxWidth:'100em', ...sx}}
                   groupKey={() => ([groupKey, makeSpacialGroupKey(groupKey)])}
                   onSuccess={(request) => onSearchSubmit(request,groupKey,serviceUrl) }
                   onError={onSearchFail}
                   cancelText=''
                   help_id={'catalogs.irsacatalogs'}
                   slotProps={{
                       input: {overflow: 'auto'},
                   }}>

            {showWarning ?
                <ServiceWarning title={title} serviceUrl={serviceUrl}/> :
                <Stack spacing={1} height={1}>
                    <Stack direction='row' spacing={1} height='24em'>
                        <ProjectPart {...{serviceUrl, masterTableData, working}}/>
                        <PositionPart {...{serviceUrl, masterTableData, working, searchOptionsMask}}/>
                    </Stack>
                    <Stack flexGrow={1}>
                        <TableConstraint {...{serviceUrl, showSqlSection, masterTableData, working}}/>
                    </Stack>
                </Stack>
            }
        </FormPanel>
    );
}


function ServiceWarning({error,title,serviceUrl}) {
    if (error)  {
        return (
            <Stack>
                <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>Error</Typography>
                <Typography color='warning' sx={{textAlign:'center', mt:5}}>{error}</Typography>
            </Stack>
        );
    }
    else {
        const msg=  serviceUrl ?
            `${title} Service is not responding or not available. Select another Service` :
            `${title} Service is not responding or not available`;
        return (
            <Typography level='h3' color='warning' sx={{textAlign: 'center', mt: 5}}>
                {msg}
            </Typography>
        );
    }
}



function ProjectPart({serviceUrl, masterTableData={}}) {
    const {catmaster} = masterTableData;
    const [getValP,setValP] = useFieldGroupValue('project');
    const [getValC,setValC] = useFieldGroupValue('catalog');
    const [getValT,setValT] = useFieldGroupValue('cattable');

    useEffect(() => {
        if (catmaster) {
            setValP(catmaster[0].project);
        }
    }, [catmaster, serviceUrl]);    // when catmaster is available, set select first project

    useEffect(() => {
        if (catmaster && getValP()) {
            const cat = catmaster?.find((p) => p.project === getValP())?.subproject[0]?.value;
            cat && setValC(cat);
        }
    }, [catmaster,getValP, serviceUrl]);     // if project changes, select first catalog of that project

    useEffect(() => {
        if (catmaster && getValP()) {
            const optCatTable = getCatalogOptions(catmaster, getValP(), getValC());
            const table = optCatTable?.[0].value;
            table && setValT(table);
        }
    }, [getValC, serviceUrl]);     // if catalog changes, select first table of that catalog

    if (!(catmaster && getValP() && getValC())) return <GridMask cols={1} rows={2} sx={{flexGrow:1}}/>;

    const optProjects = getProjectOptions(catmaster);
    const optCatalogs = getSubProjectOptions(catmaster, getValP());
    const optCatTable = getCatalogOptions(catmaster, getValP(), getValC());

    return (
        <Sheet component={Stack} variant='outlined' sx={{flexGrow:1, borderRadius:'var(--joy-radius-md)'}}>
            <Stack spacing={1} p={1}>
                <ListBoxInputField fieldKey='project'
                                   tooltip= 'Select Project'
                                   initialState={{ value: '' }}
                                   options={optProjects}
                                   slotProps={{label:{sx:{width:'7rem'}}}}
                                   label='Select Project:'
                                   size='sm'
                />
                <ListBoxInputField fieldKey='catalog'
                                   tooltip='Select Catalog'
                                   initialState={{ value: ''}}
                                   options={optCatalogs}
                                   slotProps={{label:{sx:{width:'7rem'}}}}
                                   label='Select Catalog:'
                                   size='sm'
                />
            </Stack>
            <Divider/>
            <OptionListField fieldKey='cattable'
                             size='sm'
                             value={getValC()}
                             options={optCatTable}
                             sx={{overflow: 'auto', height:'20.3em'}}
                             decorator={catTableRenderer}
            />
        </Sheet>
    );
}

function catTableRenderer(opt) {

    const keyVal = (k, v) => (
        <Stack direction='row' spacing={1}>
            <Typography level='body-sm'>{k}</Typography>
            <Typography level='title-sm' color='warning'>{v}</Typography>
        </Stack>
    );
    const html = (s) => <span dangerouslySetInnerHTML={{__html: s}}/>;

    const label = <Typography level='title-sm' whiteSpace='nowrap'>{html(opt.label)}</Typography>;
    const rows = keyVal('Rows:', opt.cat[6]);
    const cols = keyVal('Cols:', opt.cat[5]);
    const info = html(opt.cat[8]);
    const cdef = html(opt.cat[9]);

    return (
        <Stack sx={{
            '& a': {
                fontSize: 'var(--joy-fontSize-sm)',
                fontStyle: 'italic',
                whiteSpace: 'nowrap'
            }}}>
            {label}
            <Stack direction='row' alignItems='baseline' spacing={2} ml={2}>
                {rows} {cols} {info} {cdef}
            </Stack>
        </Stack>
    );
}

function PositionPart({masterTableData={}, searchOptionsMask}) {

    const {groupKey} = useContext(FieldGroupCtx);
    const {catmaster, cols} = masterTableData;

    const valP = useFieldGroupValue('project')[0]();
    const valC = useFieldGroupValue('catalog')[0]();
    const valT = useFieldGroupValue('cattable')[0]();

    const optCatTable = getCatalogOptions(catmaster, valP, valC);
    const selCatTable = optCatTable?.find((c) => c.value === valT);

    if (!(catmaster && valP && valC && selCatTable)) return  <Skeleton variant='rectangle' sx={{flexGrow:1}}/>;

    const POS_COL = cols.findIndex((c) => c?.name?.toLowerCase() === 'pos');

    const polygonDefWhenPlot= getAppOptions().catalogSpatialOp==='polygonWhenPlotExist';
    const radius = parseFloat(selCatTable.cat[RADIUS_COL]);
    const coneMax= radius / 3600;
    const boxMax= coneMax*2;
    const withPos = (selCatTable?.cat[POS_COL] || 'y').includes('y');

    return (
        <CatalogSearchMethodType {...{groupKey:makeSpacialGroupKey(groupKey), sx:{height:'24em', minWidth:'35em'},
                                 polygonDefWhenPlot, searchOptionsMask, coneMax, boxMax, withPos}} />
    );
}

function TableConstraint({serviceUrl,  masterTableData={}, showSqlSection}) {

    const {groupKey} = useContext(FieldGroupCtx);
    const masterTableInfo= masterTableData ?? {};
    const valP= useFieldGroupValue('project')[0]();
    const valC= useFieldGroupValue('catalog')[0]();
    const [getCatTableValue,setCatTableValue] = useFieldGroupValue('cattable');
    const cattableValue = getCatTableValue();
    const ddform = useFieldGroupValue('ddform')[0]() || 'true';

    useEffect(() => {
        setCatTableValue(undefined);
    }, [serviceUrl]);


    if (!(masterTableInfo && valP && valC)) return  <Skeleton  variant='rectangle' sx={{flexGrow:1}}/>;

    const {catmaster} = masterTableInfo;
    const catTable = getCatalogOptions(catmaster, valP, valC);
    const catname0 = cattableValue || catTable?.[0].value;
    const shortdd = ddform === 'true' ? 'short' : 'long';
    const tbl_id = `${catname0}-${shortdd}-dd-table-constraint`;

    return (
        <CatalogConstraintsPanel fieldKey={'tableconstraints'}
                                 constraintskey={constraintskey}
                                 catname={catname0}
                                 dd_short={ddform}
                                 tbl_id={tbl_id}
                                 showSqlSection={showSqlSection}
                                 groupKey={groupKey}
                                 createDDRequest={()=>{
                                     return {id: 'GatorDD', GatorHost:serviceUrl, catalog: catname0, short: shortdd};
                                 }}
        />
    );
}

const masterDataCache= {};


async function getMasterTable(serviceUrl) {
    if (masterDataCache[serviceUrl]) return masterDataCache[serviceUrl];
    masterDataCache[serviceUrl] = await loadMasterTable(serviceUrl);
    return masterDataCache[serviceUrl];
}

const getMasterFromCache= (serviceUrl) => masterDataCache[serviceUrl];

async function loadMasterTable (serviceUrl) {

    const request = {id: 'irsaCatalogMasterTable', GatorHost:serviceUrl}; //Fetch master table
    return doFetchTable(request).then((originalTableModel) => {

        const filter= getAppOptions().irsaCatalogFilter ?? 'defaultFilter';
        const tableModel= isFunction(filter) ? filter(tableModel) : masterTableFilter[filter](originalTableModel); // todo- I am sure there is an existing error here. I don't have time to study it now

        var data = tableModel.tableData.data;

        const cols = tableModel.tableData.columns;

        var projects = [], subprojects = {}, option = [];
        var i;
        for (i = 0; i < data.length; i++) {
            if (projects.indexOf(data[i][0]) < 0) {
                projects.push(data[i][0]);
            }
        }
        for (i = 0; i < projects.length; i++) {
            option = [];
            const tmp = [];
            data.forEach((e) => {
                if (e[0] === projects[i]) {
                    if (option.indexOf(e[1]) < 0) {
                        option.push(e[1]);
                        var obj = {
                            label: e[1],
                            value: e[1],
                            proj: projects[i]
                        };
                        tmp.push(obj);
                    }
                }
            });
            subprojects[projects[i]] = tmp;
        }
        var o = 0;
        const catmaster=[];
        for (i = 0; i < projects.length; i++) {
            var opt = subprojects[projects[i]];
            const tmp = [];
            for (o = 0; o < opt.length; o++) {
                var myobj = {};
                myobj.project = projects[i];
                myobj.subproject = opt[o];
                var tab = data.filter((e) => {
                    if (e[0] === projects[i] && e[1] === opt[o].value) {
                        return e;
                    }
                });
                myobj.option = tab.map((e) => {
                    if (e[0] === projects[i] && e[1] === opt[o].value) {
                        var rObj = {};
                        rObj.label = e[2];
                        rObj.value = e[4];
                        rObj.proj = e[1];
                        rObj.cat = e;
                        return rObj;
                    }
                });
                tmp.push(myobj);
            }
            catmaster[i] = {
                catalogs: tmp,
                project: projects[i],
                subproject: opt
            };
        }
        return {catmaster, cols};
    }).catch(
        (reason) => {
            console.error(`Failed to get catalog: ${reason}`, reason);
        }
    );
}



export function validateConstraints(request, groupKey) {
    const errMsg= request[groupKey].tableconstraints?.errorConstraints;
    if (errMsg) {
        showInfoPopup(errMsg);
        return false;
    }
    return true;
}

function onSearchFail() {
    showInfoPopup('One or more fields are not valid');
}

function onSearchSubmit(request, catalogGroupKey, serviceUrl) {

    if (!masterDataCache[serviceUrl]) {
        showInfoPopup('Error: Master table was not loaded.');
        return false;
    }
    const gkeySpacial= makeSpacialGroupKey(catalogGroupKey);
    const spacPart = request[gkeySpacial] || {};
    const {spatial} = spacPart;
    const wp = parseWorldPt(spacPart[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp && (spatial === SpatialMethod.Cone.value
        || spatial === SpatialMethod.Box.value
        || spatial === SpatialMethod.Elliptical.value)) {
        showInfoPopup('Target is required','Error');
        return false;
    }
    if (validateConstraints(request, catalogGroupKey)) {
        doCatalog(request, catalogGroupKey, serviceUrl);
    }
    return true;
}


function doCatalog(request, catalogGroupKey, serviceUrl) {

    const gkeySpacial= makeSpacialGroupKey(catalogGroupKey);
    const catPart= request[catalogGroupKey];
    const spacPart= request[gkeySpacial] || {};
    const {catalog, project, cattable}= catPart;
    const {spatial='AllSky'}= spacPart;  // if there is no 'spatial' field (catalog with no position information case)

    const conesize = convertAngle('deg', 'arcsec', spacPart.conesize);
    let title = `${catPart.project}-${catPart.cattable}`;
    let tReq;
    if (spatial === SpatialMethod.get('Multi-Object').value) {
        const filename = spacPart?.fileUpload;

        title += '-MultiObject';
        tReq = makeIrsaCatalogRequest(title, catPart.project, catPart.cattable, {
            filename,
            radius: conesize,
            SearchMethod: spacPart.spatial,
            RequestedDataSet: catalog
        });
    } else {
        title += ` (${spatial}`;
        if (spatial === SpatialMethod.Elliptical.value) {
            title += Number(spacPart?.posangle ?? 0) + '_' + Number(spacPart?.axialratio ?? 0.26);
        }
        if (spatial === SpatialMethod.Box.value || spatial === SpatialMethod.Cone.value || spatial === SpatialMethod.Elliptical.value) {
            title += ':' + conesize + '\'\'';
        }
        title += ')';
        tReq = makeIrsaCatalogRequest(title, project, cattable, {
            SearchMethod: spatial,
            RequestedDataSet: catalog,
            GatorHost:serviceUrl
        });
    }

    // change and merge others parameters in request if elliptical
    // plus change spatial name to cone
    // (Gator search method for elliptical is cone)
    if (spatial === SpatialMethod.Elliptical.value) {

        const pa = spacPart?.posangle ?? 0;
        const ar = spacPart?.axialratio ?? 0.26;

        // see PA and RATIO string values in edu.caltech.ipac.firefly.server.catquery.GatorQuery
        merge(tReq, {'posang': pa, 'ratio': ar});
    }

    if (spatial === SpatialMethod.Cone.value
        || spatial === SpatialMethod.Box.value
        || spatial === SpatialMethod.Elliptical.value) {
        merge(tReq, {[ServerParams.USER_TARGET_WORLD_PT]: spacPart[ServerParams.USER_TARGET_WORLD_PT]});
        if (spatial === SpatialMethod.Box.value) {
            tReq.size = conesize;
        } else {
            tReq.radius = conesize;
        }
    }

    if (spatial === SpatialMethod.Polygon.value) {
        tReq.polygon = spacPart.polygoncoords;
    }

    const sql= request[catalogGroupKey]?.tableconstraints;
    tReq.constraints = '';
    let addAnd = false;
    if (sql?.constraints?.length > 0) {
        tReq.constraints += sql.constraints;
        addAnd = true;
    }

    const sqlTxt = request[catalogGroupKey]?.txtareasql?.value?.trim() ?? '';
    if (sqlTxt.length > 0) {
        tReq.constraints += (addAnd ? ' AND ' : '') + validateSql(sqlTxt);
    }

    const colsSearched = sql.selcols.lastIndexOf(',') > 0 ? sql.selcols.substring(0, sql.selcols.lastIndexOf(',')) : sql.selcols;
    if (colsSearched.length > 0) {
        tReq.selcols = colsSearched;
    }
    //console.log('final request: ' + JSON.stringify(tReq));

    set(tReq, `META_INFO.${PREF_KEY}`, `${tReq.catalogProject}-${tReq.catalog}`);
    const serviceId= getGatorProtoServiceId(serviceUrl);
    tReq.META_INFO[MetaConst.DATA_SERVICE_ID]= serviceId;
    dispatchTableSearch(tReq, {backgroundable:true});
}

//TODO parse whatever format and return SQL standard
export function validateSql(sqlTxt) {
    //const filterInfoCls = FilterInfo.parse(sql);
    //return filterInfoCls.serialize();//sql.replace(';',' AND ');
    // Check that text area sql doesn't starts with 'AND', if needed, update the value without
    if (sqlTxt.toLowerCase().indexOf('and') === 0) {
        // text sql starts with AND, but AND will be added if constraints is added to any column already, so remove here if found
        sqlTxt = sqlTxt.substring(3, sqlTxt.length).trim();
    }
    if (sqlTxt.toLowerCase().indexOf('where') === 0) {
        // text sql starts with WHERE, but WHERE will be added automatically
        sqlTxt = sqlTxt.substring(5, sqlTxt.length).trim();
    }
    if (sqlTxt.toLowerCase().lastIndexOf('and') === sqlTxt.length - 3) {
        sqlTxt = sqlTxt.substring(0, sqlTxt.length - 3).trim();
    }
    return sqlTxt;
}

/**
 * Return the project elements such as label, value and project is present in each
 * @param {Object} catmaster master table data
 * @returns {Array} project option array ready to be used in drop-down component
 */
function getProjectOptions(catmaster) {
    return catmaster.map((o) => {
        return {label: o.project, value: o.project, proj: o.project};
    });
}

/**
 * Returns the sub project elements from that project
 * @param {Object} catmaster master table data
 * @param {String} project the project name
 * @returns {String} returns subproject string from master table and project name
 */
function getSubProjectOptions(catmaster, project) {
    return catmaster.find((op) => {
        return op.project === project;
    }).subproject;
}

/**
 * Return the collection of catalog elements based on the project and category selected
 * @param catmaster master table data
 * @param project project name
 * @param subproject name of the category under project
 * @example cat object example: ["WISE", "AllWISE Database", "AllWISE Source Catalog", "WISE_AllWISE", "allwise_p3as_psd", "334", "747634026", "3600", "<a href='https://irsa.ipac.caltech.edu/Missions/wise.html' target='info'>info</a>", "<a href='https://wise2.ipac.caltech.edu/docs/release/allwise/expsup/sec2_1a.html' target='Column Def'>Column Def</a>", "GatorQuery", "GatorDD"]
 * @returns {Object} array with ith element is an object which option values and an object with n attribute which ith attribute is corresponding
 * to ith columns in cols of master table and its value, i.e. ith = 0, 0:'WISE', where cols[0]=projectshort
 */
function getCatalogOptions(catmaster, project, subproject) {
    return project && subproject && catmaster?.
            find((op) => op.project === project)?.catalogs.
            find((c) => c.subproject.value === subproject)?.option;
}
