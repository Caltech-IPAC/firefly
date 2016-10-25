/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {FormPanel} from '../../ui/FormPanel.jsx';
import { get, merge, isEmpty} from 'lodash';
import {updateMerge} from '../../util/WebUtil.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {doFetchTable, makeTblRequest, makeLsstCatalogRequest, getTblById} from '../../tables/TableUtil.js';
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
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {parseWorldPt} from '../../visualize/Point.js';
import {VoSearchPanel} from '../../ui/VoSearchPanel.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {convertAngle} from '../VisUtil.js';
import {validateSql, validateConstraints, initRadiusArcSec} from './CatalogSelectViewPanel.jsx';
import {masterTableFilter} from './IrsaMasterTableFilters.js';

import './CatalogTableListField.css';
import './CatalogSelectViewPanel.css';

/**
 * group key for fieldgroup comp
 */
const gkey = 'LSST_CATALOG_PANEL';
const  gkeySpatial = 'LSST_CATALOG_PANEL_spatial';
const dropdownName = 'LsstCatalogDropDown';
const constraintskey = 'inputconstraint';
const projectName = 'Lsst';
const RADIUS_COL = '7';
const COLDEF1 = '9';
const COLDEF2 = '8';

export const LSSTDDPID = 'LSSTMetaSearch';


/**
 * Globally scoped here, master table, columns object
 * @type {Array}
 */
var catmaster = [], cols = [];


/**
 * @summary component for LSST catalog search panel
 */
export class LSSTCatalogSelectViewPanel extends Component {

    constructor(props) {
        super(props);
        this.state = {cattable: get(FieldGroupUtils.getGroupFields(gkey), 'cattable', '')};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(gkey, (fields) => {
            if (fields.cattable.value !== this.state.cattable && this.iAmMounted) {
                this.setState({cattable: fields.cattable.value});
            }
        });
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        var {cattable}= this.state;
        return (
            <div>
                <FormPanel
                    width='auto' height='auto'
                    groupKey={[gkey, gkeySpatial]}
                    onSubmit={(request) => onSearchSubmit(request)}
                    onCancel={hideSearchPanel}>
                    <LSSTCatalogSelectView cattable={cattable}/>
                </FormPanel>
            </div>

        );
    }
}

/**
 * @summary call back funciton on search button
 * @param {Object} request
 */
function onSearchSubmit(request) {
    if (request[gkey].Tabs === 'catalogLsst') {
        const {spatial} = request[gkeySpatial];

        if (spatial === SpatialMethod.Cone.value ||
            spatial === SpatialMethod.Box.value ||
            spatial === SpatialMethod.Elliptical.value) {

            const wp = parseWorldPt(request[gkeySpatial][ServerParams.USER_TARGET_WORLD_PT]);
            if (!wp) {
                showInfoPopup('Target is required');
                return;
            }
        } else if (spatial === SpatialMethod.Polygon.value) {
            if (!get(request[gkeySpatial], 'polygoncoords')) {
                showInfoPopup('polygon coordinate is required');
                return;
            }
        } else if (spatial ===   SpatialMethod.get('Multi-Object').value) {
            if (!get(request[gkeySpatial], 'fileUpload')) {
                showInfoPopup('multi-object file is required');
                return;
            }
        }
        if (validateConstraints(gkey)) {
            doCatalog(request);
        }
    }
    else if (request[gkey].Tabs === 'loadcatLsst') {
        doLoadTable(request[gkey]);
    }
    else {
        console.log('request no supported');
    }
}
/**
 * @summary callback function for search cancel button
 */
function hideSearchPanel() {
    dispatchHideDropDown();
}

/**
 * @summary catalog search
 * @param {Object} request
 */
function doCatalog(request) {

    const catPart = request[gkey];
    const spatPart = request[gkeySpatial];
    const {project, cattable} = catPart;
    const {spatial} = spatPart;

    const conesize = convertAngle('deg', 'arcsec', spatPart.conesize);
    var title = `${projectName}-${catPart.cattable}`;
    var tReq = {};

    if (spatial === SpatialMethod.get('Multi-Object').value) {
        var filename = catPart.fileUpload;

        tReq = makeLsstCatalogRequest(title, project, cattable, {
            table_path: '/hydra/cm/firefly_test_data/DAXTestData/',  // TODO: to remove later
            filename,
            radius: conesize,
            SearchMethod: spatial
        });
    } else {
        title += ` (${spatial}`;
        if (spatial === SpatialMethod.Box.value || spatial === SpatialMethod.Cone.value || spatial === SpatialMethod.Elliptical.value) {
            title += ':' + conesize + '\'\'';
        }
        title += ')';

        tReq = makeLsstCatalogRequest(title, project, cattable, {
            SearchMethod: spatial,
            table_path: '/hydra/cm/firefly_test_data/DAXTestData/'  //TODO: to remove later
        });
    }

    // change and merge others parameters in request if elliptical
    // plus change spatial name to cone
    // TODO: (assume search method for elliptical is cone)
    if (spatial === SpatialMethod.Elliptical.value) {

        const pa = get(spatPart, 'posangle', 0);
        const ar = get(spatPart, 'axialratio', 0.26);

        // see PA and RATIO string values in edu.caltech.ipac.firefly.server.catquery.GatorQuery
        merge(tReq, {'posang': pa, 'ratio': ar});
    }

    if (spatial === SpatialMethod.Cone.value ||
        spatial === SpatialMethod.Box.value ||
        spatial === SpatialMethod.Elliptical.value) {
        merge(tReq, {[ServerParams.USER_TARGET_WORLD_PT]: spatPart[ServerParams.USER_TARGET_WORLD_PT]});
        if (spatial === SpatialMethod.Box.value) {
            tReq.size = conesize;
        } else {
            tReq.radius = conesize;
        }
    } else if (spatial === SpatialMethod.Polygon.value) {
        tReq.polygon = spatPart.polygoncoords;
    }

    const {tableconstraints, txtareasql} = FieldGroupUtils.getGroupFields(gkey);
    const sql = tableconstraints.value;

    tReq.constraints = '';
    let addAnd = false;

    if (sql.constraints.length > 0) {
        tReq.constraints += sql.constraints;
        addAnd = true;
    }

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

/**
 * @summary load catalog table
 * @param {Object} request
 */
function doLoadTable(request) {
    var tReq = makeTblRequest('userCatalogFromFile', 'Lsst Table Upload', {
        filePath: request.fileUpload
    });
    dispatchTableSearch(tReq);
}

/**
 * @summary class LSSTCatalogSelectView for selecting and handling LSST catalog
 * state: {master: {catmaster[] = [{project, catalogs[]},... {project, catalogs[]}], cols[]}}
 */
class LSSTCatalogSelectView extends Component {

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
     * Fetch master catalog table with search id = 'lsstCatalogMasterTable'
     * set the project name lsst.
     * result is set in the state as 'master' object {catmaster, cols}
     * @see setMaster
     * @returns {Object} fetch master table and once fetched, set state with it
     */
    loadMasterCatalogTable() {    //TODO send the request to the server and fill out catmaster


        //const request = {id: 'lsstCatalogMasterTable'}; //Fetch master table
        var project = projectName;
        var catalogs = [
                {
                    label: 'Deep Forced Source',
                    value: 'RunDeepForcedSourceDD',       //TODO: temporary hard code of catalog name
                    cat:[]
                },
                {
                    label: 'Deep Source',
                    value: 'RunDeepSourceDD',
                    cat: []
                }
        ];

        catmaster.push({  project, catalogs });
        this.setMaster({catmaster, cols});
    }

    /**
     * Sets the object with the table model fetched, each element consists of
     * an Object {project, catalogs} where'progject' is the progject name and 'catalogs' is array of catalog tables.
     * Each element in catalogs is with 'value' for catalog name value, 'label' for text shown on UI, and 'cat' for an
     * array containing column name of the catalog.
     * @see master table file from http://irsa.ipac.caltech.edu/cgi-bin/Gator/nph-scan?mode=ascii
     * @param {Object} master table model
     * @returns {XML} set the state to include master table
     */
    setMaster(master = {}) {
        this.setState({master});
    }

    render() {
        const {master={}} = this.state;
        if (isEmpty(master)) {
            return (
                <div style={{position: 'relative'}}>
                    <div className='loading-mask'></div>
                </div>
            );
        }

        return (
            <FieldGroup groupKey={gkey}
                        reducerFunc={userChangeLsstDispatch()}
                        keepState={true}>
                <FieldGroupTabs initialState={{ value:'catalog' }} fieldKey='Tabs' resizable={true}>
                    <Tab name='Search Catalogs' id='catalogLsst'>
                        <LsstCatalogDDList {...this.props} {...this.state} />
                    </Tab>
                    <Tab name='Load Catalog' id='loadcatLsst'>
                        <CatalogLoad forGroup={gkey} />
                    </Tab>
                </FieldGroupTabs>
            </FieldGroup>
        );
    }
}

/**
 * @summary Reducer from field group component, should return updated catalog selection
 * @returns {Function} reducer to change fields when user interact with the dialog
 */
var userChangeLsstDispatch = function () {

    return (inFields, action) => {

        if (!inFields) return fieldInit();

        switch (action.type) {
            // update the size field in case tab selection is changed

            case FieldGroupCntlr.VALUE_CHANGE:

                const {fieldKey} = action.payload;

                if (fieldKey === 'targettry' || fieldKey === 'UserTargetWorldPt' || fieldKey === 'conesize' ) {  // image select panel
                    break;
                }
                const projName = inFields.project.value;
                let currentCatValue = get(inFields, 'cattable.value', catmaster[0].catalogs[0].value);
                const catTable = getCatalogs(catmaster, projName);
                const currentIdx = getCatalogIndex(catTable, currentCatValue);

                if (fieldKey === 'cattable') {
                    let sizeFactor = 1;
                    const method = get(inFields, 'spatial.value', SpatialMethod.Cone.value);
                    if (method === SpatialMethod.Box.value) {
                        sizeFactor = 2;
                    }

                    // TODO: wait until catalog table is set
                    const radius = get(catTable[currentIdx], ['cat', RADIUS_COL], 100);
                    const coldef = get(catTable[currentIdx], ['cat', COLDEF1]) || get(catTable[currentIdx], ['cat', COLDEF2]) || "";

                    inFields = updateMerge(inFields, 'cattable', {
                        value: currentCatValue,
                        coldef
                    });
                    inFields = updateMerge(inFields, 'conesize', {
                        max: sizeFactor * radius / 3600
                    });

                    inFields = updateMerge(inFields, 'tableconstraints', {
                        tbl_id: `${currentCatValue}-dd-table-constraint`
                    });
                }
                break;
            case FieldGroupCntlr.CHILD_GROUP_CHANGE:
                //console.log('Child group change called...');
                break;
            default:
                break;
        }
        return inFields;
    };
};

/**
 * @summary Return the collection of catalog elements based on the project
 * @param {Object} catmaster master table data
 * @param {string} project project name
 * @returns {Object[]} array with ith element is an object which option values and an object with n attribute which ith attribute is corresponding
 * to ith columns in cols of master table and its value, i.e. ith = 0, 0:'WISE', where cols[0]=projectshort
 */
function getCatalogs(catmaster, project) {
    return catmaster.find((op) => {
        return op.project === project;
    }).catalogs;
}


/**
 * @summary return the catalog index by given the catalog table name
 * @param {Object[]} catTab
 * @param {string} catVal
 * @returns {number} index of the catalog in the catalog array
 */
function getCatalogIndex(catTab, catVal) {
    return catTab.findIndex((cat) => cat.value === catVal);
}

/**
 * @summary component of the constraint table for catalog search
 */
class LsstCatalogDDList extends Component {

    constructor(props) {
        super(props);
        this.state = Object.assign({...this.state}, {...this.props}, {optList: ''});
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }


    render() {
        const {catmaster} = this.state.master;
        if (!catmaster) false;

        const selProj = get(FieldGroupUtils.getGroupFields(gkey), 'project.value', catmaster[0].project);
        const selCat =  get(FieldGroupUtils.getGroupFields(gkey), 'cattable.value', catmaster[0].catalogs[0].value);
        const catTable = getCatalogs(catmaster, selProj);
        const currentIdx = getCatalogIndex(catTable, selCat);
        const radius = parseFloat(get(catTable[currentIdx], ['cat', RADIUS_COL], '100'));
        const catPanelStyle = {height: 350};

        //TODO: re-examine the setting
        const coneMax= radius / 3600;
        const boxMax= coneMax*2;

        const polygonDefWhenPlot= get(window.firefly, 'catalogSpacialOp')==='polygonWhenPlotExist';

        var metadataSelector = () => {
                var options = catTable.map(cat => {
                    return {value: cat.value, label: cat.label}
                });

                return (
                    <div className='ddselectors' style={catPanelStyle}>
                        <RadioGroupInputField
                            inline={false}
                            initialState={{value: 'lsst cat table name 1',
                                           fieldKey: 'cattable'}}
                            label={`${projectName} Catalogs`}
                            labelWidth={200}
                            alignment={'vertical'}
                            fieldKey='cattable'
                            options={options}
                        />
                     </div>
                )
        };

        return (
            <div>
                <div className='catalogpanel'>
                    {metadataSelector()}
                    <div className='spatialsearch' style={catPanelStyle}>
                        <CatalogSearchMethodType groupKey={gkeySpatial} polygonDefWhenPlot={polygonDefWhenPlot}
                                                 coneMax={coneMax} boxMax={boxMax}/>
                    </div>
                </div>
                <div className='ddtable'>
                    <CatalogConstraintsPanel fieldKey={'tableconstraints'}
                                             constraintskey={constraintskey}
                                             catname={selCat}
                                             showFormType={false}
                                             processId={LSSTDDPID}
                                             groupKey={gkey}
                                             createDDRequest={()=>{
                                                return {id: LSSTDDPID, table_name: selCat, table_path: '/hydra/cm/firefly_test_data/DAXTestData/'};
                                             }}
                    />
                </div>
                <div style={{display:'flex',flexDirection:'column', alignItems:'flex-end'}}>
                    <HelpIcon
                        helpId={'basics.catalogs'}/>
                </div>
            </div>
        );
    }
}

LSSTCatalogSelectViewPanel.propTypes = {
    name: PropTypes.oneOf([dropdownName])
};

LSSTCatalogSelectViewPanel.defaultProps = {
    name: dropdownName
};


/**
 * @summary component for loading catalog file
 * @returns {XML}
 * @constructor
 */
function CatalogLoad({}) {

    return (
        <div style={{padding: 5, width: '800px', height: '300px'}}>
            <FileUpload
                wrapperStyle={{margin: '5px 0'}}
                fieldKey='fileUpload'
                initialState={{
                    tooltip: 'Select an LSST catalog table file to upload',
                    label: 'File:'
                }}
            />
            <div style={{marginTop: 20}}>
                <em style={{color: 'gray'}}>Custom catalog in LSST table format</em>
                <HelpIcon helpId={'basics.loadcatalog'}/>
            </div>
        </div>
    );
}

CatalogLoad.propTypes = {};

/*
 Define fields init and per action
 */
function fieldInit() {
    return (
        {
            'project': {
                fieldKey: 'project',
                label: projectName,
                value: projectName,
                labelWidth: '100'
            },
            'cattable': {
                fieldKey: 'cattable',
                value: catmaster[0].catalogs[0].value,
                coldef: ''
            },
            'tableconstraints': {
                fieldKey: 'tableconstraints',
                value: {constraints: '', selcols: '', filters: {}},
                tbl_id: ''
            },
            'txtareasql': {
                fieldKey: 'txtareasql',
                value: ''
            }
        }
    );
}