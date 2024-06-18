/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Divider, Sheet, Skeleton, Stack, Typography} from '@mui/joy';
import React, {useEffect, useState} from 'react';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {set, get, merge, isEmpty, isFunction, memoize} from 'lodash';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {doFetchTable} from '../../tables/TableUtil.js';
import {makeIrsaCatalogRequest} from '../../tables/TableRequestUtil.js';
import {CatalogConstraintsPanel} from './CatalogConstraintsPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils, {getFieldVal, setFieldValue} from '../../fieldGroup/FieldGroupUtils';
import {ServerParams} from '../../data/ServerParams.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {CatalogSearchMethodType, SpatialMethod} from '../../ui/CatalogSearchMethodType.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {parseWorldPt} from '../../visualize/Point.js';
import {convertAngle} from '../VisUtil.js';
import {masterTableFilter} from './IrsaMasterTableFilters.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';

import {PREF_KEY} from 'firefly/tables/TablePref.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {OptionListField} from 'firefly/ui/OptionListField.jsx';
import {GridMask} from 'firefly/ui/panel/MaskPanel.jsx';

/**
 * group key for fieldgroup comp
 */
export const irsaCatalogGroupKey = 'CATALOG_PANEL';
export const gkeySpacial = 'CATALOG_PANEL_spacial';

const RADIUS_COL = '7';
const constraintskey = 'inputconstraint';


export function IrsaCatalogSearch() {

    return (
        <FieldGroup groupKey={irsaCatalogGroupKey} keepState={true} sx={{position:'relative', flexGrow:1}}>
            <FormPanel sx={{position:'absolute', inset:0, overflow:'hidden', maxWidth:'100em', margin:'0 auto'}}
                groupKey={() => ([irsaCatalogGroupKey, gkeySpacial])}
                onSuccess={onSearchSubmit}
                onError={onSearchFail}
                cancelText=''
                help_id={'catalogs.irsacatalogs'}
                slotProps={{
                    input: {overflow: 'auto'},
                }}>

                <Stack spacing={1} height={1}>
                    <Stack direction='row' spacing={1} height='24em'>
                        <ProjectPart/>
                        <PositionPart/>
                    </Stack>
                    <Stack flexGrow={1}>
                        <TableConstraint/>
                    </Stack>
                </Stack>
            </FormPanel>
        </FieldGroup>
    );

}

function ProjectPart() {
    const {catmaster} = useMasterTableInfo() || {};
    const valP = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'project'));
    const valC = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'catalog'));

    useEffect(() => {
        if (catmaster) {
            setFieldValue(irsaCatalogGroupKey, 'project', catmaster[0].project);
        }
    }, [catmaster]);    // when catmaster is available, set select first project

    useEffect(() => {
        if (catmaster && valP) {
            const cat = catmaster?.find((p) => p.project === valP)?.subproject[0]?.value;
            cat && setFieldValue(irsaCatalogGroupKey, 'catalog', cat);
        }
    }, [valP]);     // if project changes, select first catalog of that project

    useEffect(() => {
        if (catmaster && valP) {
            const optCatTable = getCatalogOptions(catmaster, valP, valC);
            const table = optCatTable?.[0].value;
            table && setFieldValue(irsaCatalogGroupKey, 'cattable', table);
        }
    }, [valC]);     // if catalog changes, select first table of that catalog

    if (!(catmaster && valP && valC)) return <GridMask cols={1} rows={2} sx={{flexGrow:1}}/>;

    const optProjects = getProjectOptions(catmaster);
    const optCatalogs = getSubProjectOptions(catmaster, valP);
    const optCatTable = getCatalogOptions(catmaster, valP, valC);

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
                             value={valC}
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

function PositionPart() {

    const {catmaster, cols} = useMasterTableInfo() || {};

    const valP = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'project'));
    const valC = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'catalog'));
    const valT = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'cattable'));

    const optCatTable = getCatalogOptions(catmaster, valP, valC);
    const selCatTable = optCatTable?.find((c) => c.value === valT);

    if (!(catmaster && valP && valC && selCatTable)) return  <Skeleton variant='rectangle' sx={{flexGrow:1}}/>;

    const POS_COL = cols.findIndex((c) => c?.name?.toLowerCase() === 'pos');

    const polygonDefWhenPlot= get(getAppOptions(), 'catalogSpatialOp')==='polygonWhenPlotExist';
    const radius = parseFloat(selCatTable.cat[RADIUS_COL]);
    const coneMax= radius / 3600;
    const boxMax= coneMax*2;
    const withPos = (selCatTable?.cat[POS_COL] || 'y').includes('y');

    return (
        <CatalogSearchMethodType groupKey={gkeySpacial} sx={{height:'24em', minWidth:'35em'}}
                                 polygonDefWhenPlot={polygonDefWhenPlot}
                                 coneMax={coneMax} boxMax={boxMax} withPos={withPos}
        />

    );
}

function TableConstraint() {

    const masterTableInfo = useMasterTableInfo();
    const valP = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'project'));
    const valC = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'catalog'));
    const cattableValue = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'cattable'));
    const ddform = useStoreConnector(() => getFieldVal(irsaCatalogGroupKey, 'ddform', 'true'));

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
                                 groupKey={irsaCatalogGroupKey}
                                 createDDRequest={()=>{
                                     return {id: 'GatorDD', catalog: catname0, short: shortdd};
                                 }}
        />
    );
}


function useMasterTableInfo () {
    const [masterTableInfo, setMasterTableInfo] = useState();

    useEffect(() => {
        async function fetchData() {
            const resp = await getMasterTable();
            setMasterTableInfo(resp);
            hasMasterTable = true;
        };
        fetchData();
    }, []);
    return masterTableInfo;
}

let hasMasterTable;       // this is needed because onSearchSubmit cannot be async
const getMasterTable = memoize(async function() {
    return await loadMasterTable();
});

async function loadMasterTable () {

    const request = {id: 'irsaCatalogMasterTable'}; //Fetch master table
    return doFetchTable(request).then((originalTableModel) => {

        const filter= get(getAppOptions(), 'irsaCatalogFilter', 'defaultFilter');
        const tableModel= isFunction(filter) ? filter(tableModel) : masterTableFilter[filter](originalTableModel);

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



export function validateConstraints(groupKey) {
    const {tableconstraints} = FieldGroupUtils.getGroupFields(groupKey);

    var errMsg = get(tableconstraints, ['value', 'errorConstraints'], '');

    if (!isEmpty(errMsg)) {
        showInfoPopup(errMsg);
        return false;
    }
    return true;
}

function onSearchFail() {
    showInfoPopup('One or more fields are not valid');
}

function onSearchSubmit(request) {

    if (!hasMasterTable) {
        showInfoPopup('Error: Master table was not loaded.');
        return false;
    }
    const spacPart = request[gkeySpacial] || {};
    const {spatial} = spacPart;
    const wp = parseWorldPt(spacPart[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp && (spatial === SpatialMethod.Cone.value
        || spatial === SpatialMethod.Box.value
        || spatial === SpatialMethod.Elliptical.value)) {
        showInfoPopup('Target is required','Error');
        return false;
    }
    if (validateConstraints(irsaCatalogGroupKey)) {
        doCatalog(request);
    }
    return true;
}


function doCatalog(request) {

    const catPart= request[irsaCatalogGroupKey];
    const spacPart= request[gkeySpacial] || {};
    const {catalog, project, cattable}= catPart;
    const {spatial='AllSky'}= spacPart;  // if there is no 'spatial' field (catalog with no position information case)

    const conesize = convertAngle('deg', 'arcsec', spacPart.conesize);
    var title = `${catPart.project}-${catPart.cattable}`;
    var tReq = {};
    if (spatial === SpatialMethod.get('Multi-Object').value) {
        var filename = get(spacPart, 'fileUpload');

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
            title += Number(get(spacPart, 'posangle', 0)) + '_' + Number(get(spacPart, 'axialratio', 0.26));
        }
        if (spatial === SpatialMethod.Box.value || spatial === SpatialMethod.Cone.value || spatial === SpatialMethod.Elliptical.value) {
            title += ':' + conesize + '\'\'';
        }
        title += ')';
        tReq = makeIrsaCatalogRequest(title, project, cattable, {
            SearchMethod: spatial,
            RequestedDataSet: catalog
        });
    }

    // change and merge others parameters in request if elliptical
    // plus change spatial name to cone
    // (Gator search method for elliptical is cone)
    if (spatial === SpatialMethod.Elliptical.value) {

        const pa = get(spacPart, 'posangle', 0);
        const ar = get(spacPart, 'axialratio', 0.26);

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

    const {tableconstraints} = FieldGroupUtils.getGroupFields(irsaCatalogGroupKey);
    const sql = tableconstraints.value;
    tReq.constraints = '';
    let addAnd = false;
    if (sql.constraints.length > 0) {
        tReq.constraints += sql.constraints;
        addAnd = true;
    }

    const {txtareasql} = FieldGroupUtils.getGroupFields(irsaCatalogGroupKey);
    const sqlTxt = txtareasql.value?.trim() ?? '';
    if (sqlTxt.length > 0) {
        tReq.constraints += (addAnd ? ' AND ' : '') + validateSql(sqlTxt);
    }

    const colsSearched = sql.selcols.lastIndexOf(',') > 0 ? sql.selcols.substring(0, sql.selcols.lastIndexOf(',')) : sql.selcols;
    if (colsSearched.length > 0) {
        tReq.selcols = colsSearched;
    }
    //console.log('final request: ' + JSON.stringify(tReq));

    set(tReq, `META_INFO.${PREF_KEY}`, `${tReq.catalogProject}-${tReq.catalog}`);
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
