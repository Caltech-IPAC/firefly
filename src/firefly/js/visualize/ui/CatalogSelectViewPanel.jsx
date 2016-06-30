/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import FormPanel from '../../ui/FormPanel.jsx';
import { get, merge, isEmpty} from 'lodash';
import {updateMerge} from '../../util/WebUtil.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {doFetchTable, makeTblRequest, makeIrsaCatalogRequest, makeVOCatalogRequest, getTblById} from '../../tables/TableUtil.js';
import {CatalogTableListField} from './CatalogTableListField.jsx';
import {CatalogConstraintsPanel} from './CatalogConstraintsPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {fieldGroupConnector} from '../../ui/FieldGroupConnector.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {CatalogSearchMethodType, SpatialMethod} from '../../ui/CatalogSearchMethodType.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {parseWorldPt} from '../../visualize/Point.js';
import {VoSearchPanel} from '../../ui/VoSearchPanel.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {convertAngle} from '../VisUtil.js';

import './CatalogTableListField.css';
import './CatalogSelectViewPanel.css';

/**
 * group key for fieldgroup comp
 */
export const gkey = 'CATALOG_PANEL';

/**define the helpButton*/
const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height: 40, marginRight: 20};

const dropdownName = 'IrsaCatalogDropDown';

const initRadiusArcSec = (10 / 3600) + '';

const constraintskey = 'inputconstraint';

/**
 * Globally scoped here, master table, columns object
 * @type {Array}
 */
var catmaster = [], cols = [];

export class CatalogSelectViewPanel extends Component {

    constructor(props) {
        super(props);
        this.state = {fields: FieldGroupUtils.getGroupFields(gkey)};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(gkey, (fields) => {
            if (fields !== this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        var {fields}= this.state;
        return (
            <div style={{padding: 10}}>
                <FormPanel
                    width='auto' height='auto'
                    groupKey={gkey}
                    onSubmit={(request) => onSearchSubmit(request)}
                    onCancel={hideSearchPanel}>
                    <CatalogSelectView fields={fields}/>
                </FormPanel>
            </div>
        );
    }
}

function onSearchSubmit(request) {
    console.log('original request <br />' + JSON.stringify(request));

    if (request.Tabs === 'catalog') {
        const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
        if (!wp && (request.spatial === SpatialMethod.Cone.value
            || request.spatial === SpatialMethod.Box.value
            || request.spatial === SpatialMethod.Elliptical.value)) {
            showInfoPopup('Target is required');
            return;
        }
        doCatalog(request);
    }
    else if (request.Tabs === 'loadcat') {
        doLoadTable(request);
    }
    else if (request.Tabs === 'vosearch') {
        doVoSearch(request);
    }
    else {
        console.log('request no supported');
    }
}
function hideSearchPanel() {
    dispatchHideDropDown();
}

function doCatalog(request) {

    const conesize = convertAngle('deg', 'arcsec', request.conesize);
    var title = `${request.project}-${request.cattable}`;
    var tReq = {};
    if (request.spatial === SpatialMethod.get('Multi-Object').value) {
        var filename = request.fileUpload;
        // export function makeIrsaCatalogRequest(title, project, catalog, use='catalog_overlay', params={}, options={}, tbl_id=uniqueTblId()) {
        tReq = makeIrsaCatalogRequest(title, request.project, request.cattable, {
            filename,
            radius,
            SearchMethod: request.spatial,
            RequestedDataSet: request.catalog
        });
    } else {
        const id = 'GatorQuery';
        title += ` (${request.spatial}`;
        if (request.spatial === SpatialMethod.Box.value || request.spatial === SpatialMethod.Cone.value || request.spatial === SpatialMethod.Elliptical.value) {
            title += ':' + conesize + '\'\'';
        }
        title += ')';
        tReq = makeIrsaCatalogRequest(title, request.project, request.cattable, {
            SearchMethod: request.spatial,
            RequestedDataSet: request.catalog
        });
    }

    // change and merge others parameters in request if elliptical
    // plus change spatial name to cone
    // (Gator search method for elliptical is cone)
    if (request.spatial === SpatialMethod.Elliptical.value) {

        const pa = get(request, 'posangle', 0);
        const ar = get(request, 'axialratio', 0.26);

        // see PA and RATIO string values in edu.caltech.ipac.firefly.server.catquery.GatorQuery
        merge(tReq, {'posang': pa, 'ratio': ar});
    }

    if (request.spatial === SpatialMethod.Cone.value
        || request.spatial === SpatialMethod.Box.value
        || request.spatial === SpatialMethod.Elliptical.value) {
        merge(tReq, {[ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT]});
        if (request.spatial === SpatialMethod.Box.value) {
            tReq.size = conesize;
        } else {
            tReq.radius = conesize;
        }
    }

    if (request.spatial === SpatialMethod.Polygon.value) {
        tReq.polygon = request.polygoncoords;
    }

    const {tableconstraints} = FieldGroupUtils.getGroupFields(gkey);
    const sql = tableconstraints.value;
    tReq.constraints = '';
    let addAnd = false;
    if (sql.constraints.length > 0) {
        tReq.constraints += sql.constraints;
        addAnd = true;
    }

    const {txtareasql} = FieldGroupUtils.getGroupFields(gkey);
    const sqlTxt = txtareasql.value.trim();
    if (sqlTxt.length > 0) {
        tReq.constraints += (addAnd ? ' AND ' : '') + validateSql(sqlTxt);
    }

    const colsSearched = sql.selcols.lastIndexOf(',') > 0 ? sql.selcols.substring(0, sql.selcols.lastIndexOf(',')) : sql.selcols;
    if (colsSearched.length > 0) {
        tReq.selcols = colsSearched;
    }

    console.log('final request: ' + JSON.stringify(tReq));
    dispatchTableSearch(tReq);
}

//TODO parse whatever format and return SQL standard
function validateSql(sqlTxt) {
    //const filterInfoCls = FilterInfo.parse(sql);
    //return filterInfoCls.serialize();//sql.replace(';',' AND ');
    // Check that text area sql doesn't starts with 'AND', if needed, update the value without
    if (sqlTxt.toLowerCase().indexOf('and') == 0) { // text sql starts with and, but and will be added if constraints is added to any column already, so remove here if found
        sqlTxt = sqlTxt.substring(3, sqlTxt.length).trim();
    }
    if (sqlTxt.toLowerCase().lastIndexOf('and') == sqlTxt.length - 3) {
        sqlTxt = sqlTxt.substring(0, sqlTxt.length - 3).trim();
    }
    return sqlTxt;
}

import {FilterInfo} from '../../tables/FilterInfo.js';

/**
 * VO search using 'ConeSearchByURL' search processor
 * N.B.: radius in degree!
 * TODO: doesn't trigger a coverage default image nor overlay compared to OPS usage of 'ConeSearchByURL' search processor
 * @param request
 */
function doVoSearch(request) {
    //VO url that work http://vizier.u-strasbg.fr/viz-bin/votable/-A?-source=J/A+A/402/549
    const radius = convertAngle('deg', 'arcsec', request.conesize);//arcsec
    const accessUrl = request.vourl.trim();//.replace('&', 'URL_PARAM_SEP');
    const wp = parseWorldPt(request[ServerParams.USER_TARGET_WORLD_PT]);
    const nameUsed = wp.getObjName() || wp.toString();
    const name = `${nameUsed} (VO SCS ${radius}")`;
    var tReq = makeVOCatalogRequest(name,
        {
            [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
            SearchMethod: 'Cone',
            radius: request.conesize, //degree!
            accessUrl,
/*            radunits: 'DEGREE',
            displayUnits: 'ARCSEC',
            META_INFO: {
 POS_EQ_RA_MAIN:'_RA' //Shouldn't be needing this.
 POS_EQ_DE_MAIN:'_DE'
            }*/
        }
    );
    dispatchTableSearch(tReq);
}

function doLoadTable(request) {
    var tReq = makeTblRequest('userCatalogFromFile', 'Table Upload', {
        filePath: request.fileUpload,
        use: 'catalog_overlay'
    });
    dispatchTableSearch(tReq);
}

/**
 *
 * @type {ClassicComponentClass<P>}
 */
class CatalogSelectView extends Component {

    constructor(props) {
        super(props);
        if (!isEmpty(catmaster)) {
            this.state = {master: {catmaster, cols}};
        } else {
            this.state = {master: {}};
        }
    }

    componentWillMount() {
        if (isEmpty(catmaster)) {
            this.loadMasterCatalogTable();
        }
    }

    /**
     * Fetch master catalog table with search id = 'irsaCatalogMasterTable'
     * 'tableModel.tableData.data' returned in the promise is an array of elements which value is refered to the ith column name
     * from tableModel.tableData.columns
     * result is set in the state as 'master' object {catmaster, cols}
     * @see setMaster
     * @returns {Void} fetch master table and once fetched, set state with it
     */
    loadMasterCatalogTable() {

        const request = {id: 'irsaCatalogMasterTable'}; //Fetch master table
        doFetchTable(request).then((tableModel) => {

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
     *      {label: "AllWISE Source Catalog", value: "wise_allwise_p3as_psd", proj: "AllWISE Database", cat: Array[12]} and
     *      Each 'cat' array ith element is the value representing the ith column name below
     *      ["projectshort", "subtitle", "description", "server", "catname", "cols", "nrows", "coneradius", "infourl", "ddlink", "catSearchProcessor", "ddSearchProcessor"]
     * @see master table file from http://irsa.ipac.caltech.edu/cgi-bin/Gator/nph-scan?mode=ascii
     * @param {Object} master table model
     * @returns {Void} set the state to include master table
     */
    setMaster(master = {}) {
        this.setState({master});
    }

    render() {
        const {master={}} = this.state;
        if (isEmpty(master)) {
            return (
                <div style={{position: 'relative'}}>
                    <div className='loading-mask'/>
                </div>
            );
        }

        return (
            <FieldGroup groupKey={gkey}
                        reducerFunc={userChangeDispatch()}
                        keepState={true}>
                <FieldGroupTabs initialState={{ value:'catalog' }} fieldKey='Tabs'>
                    <Tab name='Search Catalogs' id='catalog'>
                        <CatalogDDList {...this.props} {...this.state} />
                    </Tab>
                    <Tab name='Load Catalog' id='loadcat'>
                        <div
                            style={{padding:5, width:'800px', height:'300px'}}>
                            <FileUpload
                                wrapperStyle={{margin: '5px 0'}}
                                fieldKey='fileUpload'
                                initialState={{
                                            tooltip: 'Select an IPAC catalog table file to upload',
                                            label: 'File:'}}
                            />
                            <div style={helpIdStyle}>
                                <em style={{color:'gray'}}>Custom catalog in IPAC table format</em>
                                <HelpIcon
                                    helpid={'basics.loadcatalog'}/>
                            </div>
                        </div>
                    </Tab>
                    <Tab name='VO Catalog' id='vosearch'>

                        <VoSearchPanel fieldKey='vopanel'/>

                    </Tab>
                </FieldGroupTabs>
            </FieldGroup>

        );
    }
}
const currentField = {};
/**
 * Reducer from field group component, should return updated project and sub-project updated
 * @returns {Function} reducer to change fields when user interact with the dialog
 */
var userChangeDispatch = function () {

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
                if (fieldKey === 'UserTargetWorldPt'
                    || fieldKey === 'conesize') {

                    break;

                }

                const radius = parseFloat(catTable[currentIdx].cat[7]);
                const coldef = catTable[currentIdx].cat[9] === 'null' ? catTable[currentIdx].cat[8] : catTable[currentIdx].cat[9];
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
                const shortdd = formsel == 'true' ? 'short' : 'long';
                inFields = updateMerge(inFields, 'tableconstraints', {
                    tbl_id: `${catname}-${shortdd}-dd-table-constraint`
                });


                break;
            case FieldGroupCntlr.CHILD_GROUP_CHANGE:
                console.log('Child group change called...');
                break;
            default:
                break;
        }
        return inFields;
    };
};

function cleanFilterRestrictions(inFields) {
    //Object.keys(inFields).forEach((k) => {
    //
    //        if (k.startsWith(constraintskey)) {
    //            const v = get(inFields, `${k}.value`, '');
    //            if (v.length > 0) {
    //                inFields = updateMerge(inFields, k, {value: ''});
    //            }
    //        }
    //    }
    //);
    //reset text area:
    inFields = updateMerge(inFields, 'txtareasql', {value: ''});
    return inFields;
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
 * @example cat object example: ["WISE", "AllWISE Database", "AllWISE Source Catalog", "WISE_AllWISE", "wise_allwise_p3as_psd", "334", "747634026", "3600", "<a href='http://irsa.ipac.caltech.edu/Missions/wise.html' target='info'>info</a>", "<a href='http://wise2.ipac.caltech.edu/docs/release/allwise/expsup/sec2_1a.html' target='Column Def'>Column Def</a>", "GatorQuery", "GatorDD"]
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

class CatalogDDList extends Component {

    constructor(props) {
        super(props);
        this.state = Object.assign({...this.state}, {...this.props}, {optList: ''});
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        const selProject0 = catmaster[0].project;
        const selCat0 = catmaster[0].subproject[0].value;

        let catTable, optProjects, optList;
        // User interact, coming from field group reducer function
        // @see userChangeDispatch

        //let selProj=get(this.state,'tableview.selProj', "");
        //let selCat=get(this.state,'tableview.selCat', "");

        //const {selProj, selCat} = this.props;

        const selProj = get(FieldGroupUtils.getGroupFields(gkey), 'project.value', selProject0);
        const selCat = get(FieldGroupUtils.getGroupFields(gkey), 'catalog.value', selCat0);

        //if (!isEmpty(selCat) && !isEmpty(selProj)) {
        //    selCat0 = selCat;
        //    selProject0 = selProj;
        //}
        // Build option list for project,  sub-project and catalog table based on the selected or initial value of project and sub-project
        optProjects = getProjectOptions(this.props.master.catmaster);
        optList = getSubProjectOptions(catmaster, selProj);
        catTable = getCatalogOptions(catmaster, selProj, selCat).option;

        const catname0 = get(FieldGroupUtils.getGroupFields(gkey), 'cattable.value', catTable[0].value);
        const ddform = get(FieldGroupUtils.getGroupFields(gkey), 'ddform.value', 'true');
        const shortdd = ddform == 'true' ? 'short' : 'long';
        const tbl_id = `${catname0}-${shortdd}-dd-table-constraint`;

        const {cols} = this.props.master;
        return (
            <div>
                <div className='catalogpanel'>
                    <div className='ddselectors'>
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
                    <div className='spatialsearch'>
                        <CatalogSearchMethodType groupKey={gkey}/>
                    </div>
                </div>
                {/*
                 <div style={{display:'flex', flexDirection:'row', padding:'20px', border:'1px solid #a3aeb9'}}>
                 */}
                <div className='ddtable'>
                    <CatalogConstraintsPanel fieldKey={'tableconstraints'}
                                             constraintskey={constraintskey}
                                             catname={catname0}
                                             dd_short={ddform}
                                             tbl_id={tbl_id}
                                             groupKey={gkey}
                    />
                </div>
                {/*</div>*/}
            </div>
        );
    }
}

CatalogSelectViewPanel.propTypes = {
    name: PropTypes.oneOf([dropdownName])
};

CatalogSelectViewPanel.defaultProps = {
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
            labelWidth: '100'
        },

        'catalog': {
            fieldKey: 'catalog',
            label: catmaster[0].subproject[0].value,
            value: catmaster[0].subproject[0].value,
            labelWidth: '100'
        },
        'cattable': {
            fieldKey: 'cattable',
            value: '',
            coldef: '',
            indexClicked: 0
        },
        'conesize': {
            fieldKey: 'conesize',
            value: initRadiusArcSec,
            unit: 'arcsec',
            min: 1 / 3600,
            max: parseInt(catmaster[0].catalogs[0].option[0].cat[7]) / 3600
        },
        'tableconstraints': {
            fieldKey: 'tableconstraints',
            value: {constraints: '', selcols: ''},
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
        'vourl': {
            fieldKey: 'vourl',
            value: ''
        }

    }
    );
}