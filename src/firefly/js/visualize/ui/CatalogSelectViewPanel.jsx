/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import FormPanel from '../../ui/FormPanel.jsx';
import { get, merge, isEmpty} from 'lodash';
import {updateMerge} from '../../util/WebUtil.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {TableRequest} from '../../tables/TableRequest';
import {doFetchTable, uniqueTblUiGid} from '../../tables/TableUtil.js';
import {CatalogTableListField} from './CatalogTableListField.jsx';
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
const gkey = 'CATALOG_PANEL';

/**define the helpButton*/
const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height: 40, marginRight: 20};

const dropdownName = 'IrsaCatalogDropDown';

const initRadiusArcSec = (500 / 3600) + '';

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
    console.log('request ' + JSON.stringify(request));

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
    var id = '';
    if (request.spatial === SpatialMethod.get('Multi-Object').value) {
        id = 'GatorQuery';
        var filename = request.fileUpload;
        var radius = conesize;
        tReq = TableRequest.newInstance({
            id,
            filename,
            radius,
            SearchMethod: request.spatial,
            title,
            catalog: request.cattable,
            RequestedDataSet: request.catalog,
            use: 'catalog_overlay',
            catalogProject: request.project
        });
    } else {
        id = 'GatorQuery';
        title += ` (${request.spatial}`;
        if (request.spatial === SpatialMethod.Box.value || request.spatial === SpatialMethod.Cone.value || request.spatial === SpatialMethod.Elliptical.value) {
            title += ':' + conesize + '\'\'';
        }
        title += ')';
        tReq = TableRequest.newInstance({
            id,
            SearchMethod: request.spatial,
            title,
            catalog: request.cattable,
            RequestedDataSet: request.catalog,
            use: 'catalog_overlay',
            catalogProject: request.project
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

    dispatchTableSearch(tReq);
}

function doVoSearch(request) {
    var tReq = TableRequest.newInstance({
        [ServerParams.USER_TARGET_WORLD_PT]: request[ServerParams.USER_TARGET_WORLD_PT],
        id: 'GatorQuery',
        title: request.catalog,
        SearchMethod: request.spatial,
        catalog: request.cattable,
        RequestedDataSet: request.catalog,
        radius: request.conesize,
        use: 'catalog_overlay',
        catalogProject: request.project
    });
    console.log('Does not dispatch yet ' + tReq);
    //dispatchTableSearch(tReq);
}

function doLoadTable(request) {
    var tReq = TableRequest.newInstance({
        id: 'userCatalogFromFile',
        filePath: request.fileUpload,
        title: 'Table Upload',
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
                        <CatalogDDListConnected fieldKey='tableview' {...this.props} {...this.state} />
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
                if (fieldKey === 'project') {
                    valC = optList[0].value;//If project has changed, initiialise to the first subproject found
                    inFields = updateMerge(inFields, 'catalog', {
                        value: valC
                    });
                }
                inFields = updateMerge(inFields, 'tableview', {
                    selProj: valP,
                    selCat: valC
                });

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
                let idx = get(inFields, 'cattable.indexClicked', 0);//Get current value
                if (fieldKey === 'project'
                    || fieldKey === 'catalog') {
                    idx = 0; // reset to first item of the catalog list
                }

                // User clicked on the table to select a catalog and needs to propagte the index in order
                // to get the item highlighted
                if (fieldKey === 'cattable') {
                    idx = catTable.findIndex((e) => {
                        return e.value === action.payload.value;
                    });

                }
                const radius = parseFloat(catTable[idx].cat[7]);

                inFields = updateMerge(inFields, 'cattable', {
                    indexClicked: idx,
                    value: catTable[idx].value
                });
                inFields = updateMerge(inFields, 'conesize', {
                    max: sizeFactor * radius / 3600
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
        let selProject0 = catmaster[0].project;
        let selCat0 = catmaster[0].subproject[0].value;

        let catTable, optProjects, optList;
        // User interact, coming from field group reducer function
        // @see userChangeDispatch

        //let selProj=get(this.state,'tableview.selProj', "");
        //let selCat=get(this.state,'tableview.selCat', "");

        const {selProj, selCat} = this.props;

        if (!isEmpty(selCat) && !isEmpty(selProj)) {
            selCat0 = selCat;
            selProject0 = selProj;
        }
        // Build option list for project,  sub-project and catalog table based on the selected or initial value of project and sub-project
        optProjects = getProjectOptions(this.props.master.catmaster);
        optList = getSubProjectOptions(catmaster, selProject0);
        catTable = getCatalogOptions(catmaster, selProject0, selCat0).option;
        const {cols} = this.props.master;
        return (
            <div className='catalogpanel'>
                <div className='ddselectors'>
                    <CatalogSearchMethodType groupKey={gkey}/>
                    <ListBoxInputField fieldKey='project'
                                       wrapperStyle={{margin:'5px 0 5px 0', padding:5}}
                                       initialState={{
                                          tooltip: 'Select Project',
                                          value: selProject0
                                      }}
                                       options={optProjects}
                                       multiple={false}
                                       labelWidth={100}
                                       label="Select Project:"
                    />
                    <ListBoxInputField fieldKey='catalog'
                                       wrapperStyle={{margin:'5px 0 5px 0', padding:5}}
                                       initialState={{
                                          tooltip: 'Select Catalog',
                                          value: selCat0
                                      }}
                                       options={optList}
                                       multiple={false}
                                       labelWidth={100}
                                       label="Select Catalog:"
                    />
                </div>
                <div>
                    <CatalogTableListField fieldKey='cattable'
                                           data={catTable}
                                           cols={cols}
                    />
                </div>
            </div>
        );
    }
}

const CatalogDDListConnected = fieldGroupConnector(CatalogDDList, getProps, CatalogDDList.propTypes, null);

CatalogDDList.propTypes = {
    selProj: PropTypes.string,
    selCat: PropTypes.string,
    master: PropTypes.object
};

CatalogDDList.defaultProps = {
    selProj: '',
    selCat: ''
};

function getProps(params, fireValueChange) {

    return params;
}

CatalogSelectViewPanel.propTypes = {
    name: PropTypes.oneOf([dropdownName]),
    resultId: PropTypes.string
};

CatalogSelectViewPanel.defaultProps = {
    name: dropdownName,
    resultId: uniqueTblUiGid()
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
        'tableview': {
            fieldKey: 'tableview',
            selProj: catmaster[0].project,
            selCat: catmaster[0].subproject[0].value,
            catName: catmaster[0].catalogs[0].option[0].cat[4]
        },
        'cattable': {
            fieldKey: 'cattable',
            value: '',
            indexClicked: 0
        },
        'conesize': {
            fieldKey: 'conesize',
            value: initRadiusArcSec,
            unit: 'arcsec',
            min: 1 / 3600,
            max: parseInt(catmaster[0].catalogs[0].option[0].cat[7]) / 3600
        }
    }
    );
}