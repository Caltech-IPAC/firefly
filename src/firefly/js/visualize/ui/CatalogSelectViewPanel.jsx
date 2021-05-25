/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {FormPanel} from '../../ui/FormPanel.jsx';
import { get, merge, isEmpty, isFunction, set} from 'lodash';
import {updateMerge} from '../../util/WebUtil.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {doFetchTable} from '../../tables/TableUtil.js';
import {makeIrsaCatalogRequest, DataTagMeta} from '../../tables/TableRequestUtil.js';
import {CatalogTableListField} from './CatalogTableListField.jsx';
import {CatalogConstraintsPanel} from './CatalogConstraintsPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {CatalogSearchMethodType, SpatialMethod} from '../../ui/CatalogSearchMethodType.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {parseWorldPt} from '../../visualize/Point.js';
import {convertAngle} from '../VisUtil.js';
import {masterTableFilter} from './IrsaMasterTableFilters.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';

import './CatalogTableListField.css';
import './CatalogSelectViewPanel.css';

/**
 * group key for fieldgroup comp
 */
export const irsaCatalogGroupKey = 'CATALOG_PANEL';
export const gkeySpacial = 'CATALOG_PANEL_spacial';
export const initRadiusArcSec = (10 / 3600) + '';

const RADIUS_COL = '7';
const COLDEF1 = 9;
const COLDEF2 = 8;
const dropdownName = 'MultiTableSearchCmd';
const constraintskey = 'inputconstraint';

/**
 * Globally scoped here, master table, columns object
 * @type {Array}
 */
var catmaster = [], cols = [];

export class CatalogSelectViewPanel extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {fields: FieldGroupUtils.getGroupFields(irsaCatalogGroupKey)};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbiner) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(irsaCatalogGroupKey, (fields) => {
            if (fields !== this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {fields}= this.state;
        return (
            <div style={{width:'100%'}}>
                <FormPanel
                    width='auto' height='auto'
                    style={{width:'100%', height:'100%'}}
                    groupKey={[irsaCatalogGroupKey, gkeySpacial]}
                    onSuccess={(request) => onSearchSubmit(request)}
                    onError={(request) => onSearchFail(request)}
                    params={{hideOnInvalid: false}}
                    buttonStyle={{justifyContent: 'left'}} submitBarStyle={{padding: '2px 3px 3px'}}
                    help_id={'catalogs'}
                    onCancel={hideSearchPanel}>
                    <CatalogSelectView fields={fields}/>
                </FormPanel>
            </div>

        );
    }
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

    if (!catmaster) {
        showInfoPopup('Error: Master table was not loaded.');
        return false;
    }
    const spacPart= request[gkeySpacial] || {};
    const {spatial} = spacPart;
    const wp = parseWorldPt(spacPart[ServerParams.USER_TARGET_WORLD_PT]);
    if (!wp && (spatial === SpatialMethod.Cone.value
        || spatial === SpatialMethod.Box.value
        || spatial === SpatialMethod.Elliptical.value)) {
        showInfoPopup('Target is required');
        return false;
    }
    if (validateConstraints(irsaCatalogGroupKey)) {
        doCatalog(request);
    }
    return true;
}

const hideSearchPanel= () => dispatchHideDropDown();

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
    const sqlTxt = txtareasql.value.trim();
    if (sqlTxt.length > 0) {
        tReq.constraints += (addAnd ? ' AND ' : '') + validateSql(sqlTxt);
    }

    const colsSearched = sql.selcols.lastIndexOf(',') > 0 ? sql.selcols.substring(0, sql.selcols.lastIndexOf(',')) : sql.selcols;
    if (colsSearched.length > 0) {
        tReq.selcols = colsSearched;
    }
    //console.log('final request: ' + JSON.stringify(tReq));
    set(tReq, DataTagMeta, 'catalog');
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
 *
 * @type {ClassicComponentClass<P>}
 */
class CatalogSelectView extends PureComponent {

    constructor(props) {
        super(props);
        if (!isEmpty(catmaster)) {
            this.state = {master: {catmaster, cols}};
        } else {
            this.state = {master: {}};
            this.loadMasterCatalogTable();
        }
    }

    /**
     * Fetch master catalog table with search id = 'irsaCatalogMasterTable'
     * 'tableModel.tableData.data' returned in the promise is an array of elements which value is refered to the ith column name
     * from tableModel.tableData.columns
     * result is set in the state as 'master' object {catmaster, cols}
     * @see setMaster
     */
    loadMasterCatalogTable() {

        const request = {id: 'irsaCatalogMasterTable'}; //Fetch master table
        doFetchTable(request).then((originalTableModel) => {

            const filter= get(getAppOptions(), 'irsaCatalogFilter', 'defaultFilter');
            const tableModel= isFunction(filter) ? filter(tableModel) : masterTableFilter[filter](originalTableModel);

            var data = tableModel.tableData.data;

            cols = tableModel.tableData.columns;

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

            this.setMaster({catmaster, cols});

        }).catch(
            (reason) => {
                console.error(`Failed to get catalog: ${reason}`, reason);
            }
        );
    }

    /**
     * Sets the object with the table model fetched, each element consists of
     * an Object {catalogs: Array[7], project: "WISE", subproject: Array[7]}, where
     * Each 'subproject' element is an option for the drop-down list such as {label: "AllWISE Database", value: "AllWISE Database", proj: "WISE"} for ecample
     * Each 'catalogs' element is {project: "WISE", subproject: Object, option: Array[7]}, where
     *  Each 'option' is element with catalog name value and other attributes such as
     *      {label: "AllWISE Source Catalog", value: "allwise_p3as_psd", proj: "AllWISE Database", cat: Array[12]} and
     *      Each 'cat' array ith element is the value representing the ith column name below
     *      ["projectshort", "subtitle", "description", "server", "catname", "cols", "nrows", "coneradius", "infourl", "ddlink", "catSearchProcessor", "ddSearchProcessor"]
     * @see master table file from http://irsa.ipac.caltech.edu/cgi-bin/Gator/nph-scan?mode=ascii
     * @param {Object} master table model
     */
    setMaster(master = {}) {
        this.setState({master});
    }

    render() {
        const {master={}} = this.state;
        if (isEmpty(master)) {
            return (
                <div style={{position: 'relative', width:'100%', height:'100%'}}>
                    <div className='loading-mask'/>
                </div>
            );
        }
        const tabWrapper = {padding:5, height:'100%'};

        return (
            <FieldGroup groupKey={irsaCatalogGroupKey}
                        reducerFunc={userChangeDispatch()}
                        style={{display:'flex', alignItems:'stretch', flexDirection:'column', height:'100%'}}
                        keepState={true}>
                <div style={tabWrapper}>
                    <CatalogDDList {...this.props} {...this.state} />
                </div>
            </FieldGroup>

        );
    }
}

/**
 * Reducer from field group component, should return updated project and sub-project updated
 * @returns {Function} reducer to change fields when user interact with the dialog
 */
function userChangeDispatch() {

    return (inFields, action) => {

        if (!inFields) return fieldInit();

        switch (action.type) {
            // update the size field in case tab selection is changed
            case FieldGroupCntlr.VALUE_CHANGE:

                const {fieldKey} = action.payload;

                if (fieldKey === 'targettry') {

                    break;

                }
                const valP = inFields.project.value;
                let valC = inFields.catalog.value;
                const optList = getSubProjectOptions(catmaster, valP);
                let currentIdx = get(inFields, 'cattable.indexClicked', 0);

                if (fieldKey === 'project') {
                    currentIdx = 0; // reset table to table item clicked index = 0
                    valC = optList[currentIdx].value;//If project has changed, initialise to the first subproject found
                    inFields = updateMerge(inFields, 'catalog', {
                        value: valC
                    });
                }

                if (fieldKey === 'catalog') {
                    currentIdx = 0; // reset table to table item clicked index = 0
                }

                /*
                 // reset? or set dd form to standard when switching to ctalago or project or dataset:
                 if (fieldKey === 'catalog' || fieldKey === 'cattable' || fieldKey ==='project') {
                 inFields = updateMerge(inFields, 'ddform', {
                 value: 'true'
                 });
                 }*/

                // Reinit the table and catalog value:
                const catTable = getCatalogOptions(catmaster, valP, valC).option;

                let sizeFactor = 1;
                const method = get(inFields, 'spatial.value', SpatialMethod.Cone.value);
                if (method === SpatialMethod.Box.value) {
                    sizeFactor = 2;
                }

                //No need to act on other fields if user change user wpt or spatial method
                // if (fieldKey === 'UserTargetWorldPt'
                //     || fieldKey === 'conesize') {
                //
                //     break;
                //
                // }

                const radius = parseFloat(catTable[currentIdx].cat[RADIUS_COL]);
                const coldef = catTable[currentIdx].cat[COLDEF1] === 'null' ? catTable[currentIdx].cat[COLDEF2] : catTable[currentIdx].cat[COLDEF1];
                inFields = updateMerge(inFields, 'cattable', {
                    indexClicked: currentIdx,
                    value: catTable[currentIdx].value,
                    coldef
                });
                inFields = updateMerge(inFields, 'conesize', {
                    max: sizeFactor * radius / 3600
                });

                const catname = get(inFields, 'cattable.value', '');
                const formsel = get(inFields, 'ddform.value', 'true');
                const shortdd = formsel === 'true' ? 'short' : 'long';
                inFields = updateMerge(inFields, 'tableconstraints', {
                    tbl_id: `${catname}-${shortdd}-dd-table-constraint`
                });


                break;
            case FieldGroupCntlr.CHILD_GROUP_CHANGE:
                // console.log('Child group change called...');
                break;
            default:
                break;
        }
        return inFields;
    };
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
    return catmaster.find((op) => {
        return op.project === project;
    }).catalogs.find((c) => {
        return c.subproject.value === subproject;
    });
}

class CatalogDDList extends PureComponent {

    constructor(props) {
        super(props);
        this.state = Object.assign({...this.state}, {...this.props}, {optList: ''});
    }

    render() {
        const selProject0 = catmaster[0].project;
        const selCat0 = catmaster[0].subproject[0].value;

        // User interact, coming from field group reducer function
        // @see userChangeDispatch

        //let selProj=get(this.state,'tableview.selProj', "");
        //let selCat=get(this.state,'tableview.selCat', "");

        //const {selProj, selCat} = this.props;

        const selProj = get(FieldGroupUtils.getGroupFields(irsaCatalogGroupKey), 'project.value', selProject0);
        const selCat = get(FieldGroupUtils.getGroupFields(irsaCatalogGroupKey), 'catalog.value', selCat0);


        const {master, fields} = this.props;
        const optProjects = getProjectOptions(master.catmaster);
        const optList = getSubProjectOptions(catmaster, selProj);
        const catTable = getCatalogOptions(catmaster, selProj, selCat).option;

        const currentIdx = get(fields, 'cattable.indexClicked', 0);
        const radius = parseFloat(catTable[currentIdx].cat[RADIUS_COL]);
        const coneMax= radius / 3600;
        const boxMax= coneMax*2;

        let catname0 = get(FieldGroupUtils.getGroupFields(irsaCatalogGroupKey), 'cattable.value', catTable[0].value);
        if(isEmpty(catname0)){
            catname0 = catTable[0].value;
        }
        const ddform = get(FieldGroupUtils.getGroupFields(irsaCatalogGroupKey), 'ddform.value', 'true');
        const shortdd = ddform === 'true' ? 'short' : 'long';
        const tbl_id = `${catname0}-${shortdd}-dd-table-constraint`;

        const {cols} = master;
        const catPanelStyle = {height: 300};

        const polygonDefWhenPlot= get(getAppOptions(), 'catalogSpacialOp')==='polygonWhenPlotExist';

        // for 'pos' column
        const POS_COL = master.cols.findIndex((oneCol) => {
            const colName = get(oneCol, 'name', '');
            return (colName && (colName.toLowerCase() === 'pos'));
        });

        const withPos = (get(catTable, [currentIdx, 'cat', POS_COL]) || 'y').includes('y');

        return (
            <div style={{display:'flex', flexDirection: 'column', height:'100%'}}>
                <div className='catalogpanel'>
                    <div className='ddselectors' style={catPanelStyle}>
                        <ListBoxInputField fieldKey='project'
                                           wrapperStyle={{padding:5}}
                                           initialState={{
                                          tooltip: 'Select Project',
                                          value: selProject0
                                      }}
                                           options={optProjects}
                                           multiple={false}
                                           labelWidth={75}
                                           label='Select Project:'
                        />
                        <ListBoxInputField fieldKey='catalog'
                                           wrapperStyle={{padding:5}}
                                           initialState={{
                                          tooltip: 'Select Catalog',
                                          value: selCat0
                                      }}
                                           options={optList}
                                           multiple={false}
                                           labelWidth={75}
                                           label='Select Catalog:'
                                           selectStyle={{width:'300px'}}
                        />
                        <CatalogTableListField fieldKey='cattable'
                                               data={catTable}
                                               cols={cols}
                        />
                    </div>
                    <div className='spatialsearch' style={catPanelStyle}>
                        <CatalogSearchMethodType groupKey={gkeySpacial} polygonDefWhenPlot={polygonDefWhenPlot}
                                                 coneMax={coneMax} boxMax={boxMax} withPos={withPos}
                        />
                    </div>
                </div>
                {/*
                 <div style={{display:'flex', flexDirection:'row', padding:'20px', border:'1px solid #a3aeb9'}}>
                 */}
                <div className='ddtable' >
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
                </div>
                {/*</div>*/}
            </div>
        );
    }
}

CatalogDDList.propTypes = {
    name: PropTypes.oneOf([dropdownName]),
    fields: PropTypes.object,
    master: PropTypes.object
};

CatalogDDList.defaultProps = {
    name: dropdownName
};

/*
 Define fields init and per action
 */
function fieldInit() {
    return (
    {
        'project': {
            fieldKey: 'project',
            label: catmaster[0].project,
            value: catmaster[0].project,
            labelWidth: 100
        },

        'catalog': {
            fieldKey: 'catalog',
            label: catmaster[0].subproject[0].value,
            value: catmaster[0].subproject[0].value,
            labelWidth: 100
        },
        'cattable': {
            fieldKey: 'cattable',
            value: '',
            coldef: catmaster[0].catalogs[0].option[0].cat[COLDEF1],
            indexClicked: 0
        },
        'conesize': {
            fieldKey: 'conesize',
            value: initRadiusArcSec,
            unit: 'arcsec',
            min: 1 / 3600,
            max: parseInt(catmaster[0].catalogs[0].option[0].cat[RADIUS_COL]) / 3600
        },
        'nedconesize': {
            fieldKey: 'nedconesize',
            value: initRadiusArcSec,
            unit: 'arcsec',
            min: 1 / 3600,
            max: 5
        },
        'tableconstraints': {
            fieldKey: 'tableconstraints',
            value: {constraints: '', selcols: '', errorConstraints: ''},
            tbl_id: ''
        },
        'txtareasql': {
            fieldKey: 'txtareasql',
            value: ''
        },
        'ddform': {
            fieldKey: 'ddform',
            value: 'true'
        },
    }
    );
}
