/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import { get,set, merge, isEmpty, isArray, isNil, capitalize} from 'lodash';
import {updateMerge} from '../../util/WebUtil.js';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {CatalogSearchMethodType, SpatialMethod} from '../../ui/CatalogSearchMethodType.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {makeTblRequest, makeLsstCatalogRequest} from '../../tables/TableUtil.js';
import {CatalogConstraintsPanel, getTblId} from './CatalogConstraintsPanel.jsx';
import {validateSql, validateConstraints} from './CatalogSelectViewPanel.jsx';
import {LSSTImageSpatialType} from './LSSTImageSpatialType.jsx';

//import './CatalogTableListField.css';
import './CatalogSelectViewPanel.css';

/**
 * group key for fieldgroup comp
 */
const gkey = 'LSST_CATALOG_PANEL';
const gkeySpatial = 'LSST_CATALOG_PANEL_spatial';
const gkeyImageSpatial = 'LSST_IMAGE_PANEL_spatial';
const constraintskey = 'inputconstraint';
const projectName = 'Lsst';
const RADIUS_COL = '7';
const COLDEF1 = '9';
const COLDEF2 = '8';

const DEEPSOURCE = 0;
const DEEPFORCEDSOURCE = 1;
const DEEPCOADD = 2;
const CCDEXPOSURE = 3;
const CATTYPE = '0';
const IMAGETYPE = '1';
const defCatalog = 0;
const defImage = 2;

const DEC = 3;

const LSSTTables = [
    {
        id: DEEPSOURCE,
        label: 'Deep Source',
        value: 'RunDeepSource',
        type: CATTYPE,
        cat: {}
    },
    {
        id: DEEPFORCEDSOURCE,
        label: 'Deep Forced Source',
        value: 'RunDeepForcedSource',
        type: CATTYPE,
        cat: {}
    },
    {
        id: DEEPCOADD,
        label: 'Deep Coadd',
        value: 'DeepCoadd',
        type: IMAGETYPE,
        cat: {}
    },
    {
        id: CCDEXPOSURE,
        label: 'Science CCD Exposure',
        value: 'Science_Ccd_Exposure',
        type: IMAGETYPE,
        cat: {}
    }
];

const LSSTTableTypes = {
   [CATTYPE]: 'Catalogs',
   [IMAGETYPE]: 'Images'
};

const LSSTMaster = {
    project: projectName,
    catalogs: LSSTTables
};

export const LSSTDDPID = 'LSSTMetaSearch';

var catmaster = [];

/**
 * @summary component for LSST catalog search panel
 */
export class LSSTCatalogSelectViewPanel extends Component {

    constructor(props) {
        super(props);

        var fields = FieldGroupUtils.getGroupFields(gkey);
        this.state = {
            cattable: fields ? get(fields, ['cattable', 'value'], '') : '',
            cattype: fields ? get(fields, ['cattype', 'value'], CATTYPE) : CATTYPE};
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.unbinder = FieldGroupUtils.bindToStore(gkey, (fields) => {
            if (fields.cattable.value !== this.state.cattable && this.iAmMounted) {
                this.setState({cattable: fields.cattable.value, cattype: fields.cattype.value});
            }
        });
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        var {cattable, cattype}= this.state;
        return (
            <div>
                <FormPanel
                    width='auto' height='auto'
                    groupKey={[gkey]}
                    onSubmit={(request) => onSearchSubmit(request)}
                    onCancel={hideSearchPanel}>
                    <LSSTCatalogSelectView cattype={cattype} cattable={cattable} />
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
        const {cattype} = request[gkey];

        if (cattype === CATTYPE) {
            const catalogState = FieldGroupUtils.getGroupFields(gkeySpatial);
            const spatial = get(catalogState, ['spatial', 'value']);

            if (spatial === SpatialMethod.Cone.value ||
                spatial === SpatialMethod.Box.value ||
                spatial === SpatialMethod.Elliptical.value) {

                const wp = get(catalogState, [ServerParams.USER_TARGET_WORLD_PT, 'value']);
                if (!wp) {
                    showInfoPopup('Target is required');
                    return;
                }
                if (!get(catalogState, ['conesize', 'valid'])){
                    showInfoPopup('invalid size');
                    return;
                }

            } else if (spatial === SpatialMethod.Polygon.value) {
                if (!get(catalogState, ['polygoncoords', 'value'])) {
                    showInfoPopup('polygon coordinate is required');
                    return;
                }
            } else if (spatial === SpatialMethod.get('Multi-Object').value) {
                if (!get(catalogState, ['fileUpload', 'value'])) {
                    showInfoPopup('multi-object file is required');
                    return;
                }
            }
            if (validateConstraints(gkey)) {
                doCatalog(request, catalogState);
            }
        } else if (cattype === IMAGETYPE) {
            const imageState = FieldGroupUtils.getGroupFields(gkeyImageSpatial);
            const wp = get(imageState, [ServerParams.USER_TARGET_WORLD_PT, 'value']);
            if (!wp) {
                showInfoPopup('Target is required');
                return;
            }

            const intersect = get(imageState, ['intersect', 'value']);

            if (intersect !== 'CENTER') {

                if (!get(imageState, ['size', 'valid'])) {
                    showInfoPopup('box size is required');
                    return;
                }
            }
            doImage(request, imageState);
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
 * @summary format number string with specified decimal digit
 * @param numstr
 * @param digits
 * @returns {string}
 */
function formatNumberString(numstr, digits = DEC) {
    var d = digits&digits >= 0 ?  digits : DEC;

    return parseFloat(numstr).toFixed(d).replace(/(?:\.0+|(\.\d+?)0+)$/, '$1');
}

function addConstraintToQuery(tReq) {
    const sql = get(FieldGroupUtils.getGroupFields(gkey), ['tableconstraints', 'value']);

    tReq.constraints = '';
    let addAnd = false;

    if (sql.constraints.length > 0) {
        tReq.constraints += sql.constraints;
        addAnd = true;
    }

    const sqlTxt = get(FieldGroupUtils.getGroupFields(gkey), ['txtareasql', 'value'], '').trim();
    if (sqlTxt.length > 0) {
        tReq.constraints += (addAnd ? ' AND ' : '') + validateSql(sqlTxt);
    }

    const colsSearched = sql.selcols.lastIndexOf(',') > 0 ? sql.selcols.substring(0, sql.selcols.lastIndexOf(',')) : sql.selcols;
    if (colsSearched.length > 0) {
        tReq.selcols = colsSearched;
    }

    return tReq;
}

function doImage(request, imgPart) {
    const {cattable} = request[gkey] || {};
    const spatial =  get(imgPart, ['spatial', 'value']);
    const intersect = get(imgPart, ['intersect', 'value']);
    const size = (intersect !== 'CENTER') ? get(imgPart, ['size', 'value']) : '0';
    const sizeUnit = 'deg';
    const wp = get(imgPart, [ServerParams.USER_TARGET_WORLD_PT,'value']);

    var title = `${projectName}-${cattable}-${capitalize(intersect)}`;
    var loc = wp.split(';').slice(0, 2).join();

    if (intersect !== 'CENTER') {
        title += `([${loc}]:${formatNumberString(size)}deg)`;
    } else {
        title += `([${loc}])`;
    }

    var tReq = makeLsstCatalogRequest(title, projectName, cattable,
                                      {[ServerParams.USER_TARGET_WORLD_PT]: wp,
                                       intersect,
                                       size,
                                       sizeUnit,
                                       SearchMethod: spatial},
                                       {use: 'lsst_image'});


    tReq = addConstraintToQuery(tReq);
    console.log('final request: ' + JSON.stringify(tReq));
    dispatchTableSearch(tReq);
}

/**
 * @summary catalog search
 * @param {Object} request
 * @param {Object} spatPart
 */
function doCatalog(request, spatPart) {

    const catPart = request[gkey];
    const {cattable} = catPart;
    const spatial =  get(spatPart, ['spatial', 'value']);
    const conesize = get(spatPart, ['conesize', 'value']);
    const wp = get(spatPart, [ServerParams.USER_TARGET_WORLD_PT,'value']);
    const sizeUnit = 'deg';
    const conestr = formatNumberString(conesize);

    var title = `${projectName}-${catPart.cattable}`;
    var tReq;

    if (spatial === SpatialMethod.get('Multi-Object').value) {
        var filename = catPart.fileUpload;

        tReq = makeLsstCatalogRequest(title, projectName, cattable, {
            filename,
            radius: conestr,
            sizeUnit,
            SearchMethod: spatial
        });
    } else {
        title += ` (${spatial}`;
        if (spatial === SpatialMethod.Box.value || spatial === SpatialMethod.Cone.value || spatial === SpatialMethod.Elliptical.value) {
            title += ':' + conestr + ((sizeUnit === 'deg') ? 'deg' : ((sizeUnit === 'arcsec') ? '\'\'' : '\''));
        }
        title += ')';

        tReq = makeLsstCatalogRequest(title, projectName, cattable, {
            SearchMethod: spatial
        });
    }

    // change and merge others parameters in request if elliptical
    // plus change spatial name to cone
    if (spatial === SpatialMethod.Elliptical.value) {

        const pa = get(spatPart, ['posangle', 'value'], 0);
        const ar = get(spatPart, ['axialratio', 'value'], 0.26);

        // see PA and RATIO string values in edu.caltech.ipac.firefly.server.catquery.GatorQuery
        merge(tReq, {'posang': pa, 'ratio': ar});
    }

    if (spatial === SpatialMethod.Cone.value ||
        spatial === SpatialMethod.Box.value ||
        spatial === SpatialMethod.Elliptical.value) {
        merge(tReq, {[ServerParams.USER_TARGET_WORLD_PT]: wp});
        if (spatial === SpatialMethod.Box.value) {
            tReq.size = conesize;
        } else {
            tReq.radius = conesize;
        }
        tReq.sizeUnit = sizeUnit;
    } else if (spatial === SpatialMethod.Polygon.value) {
        tReq.polygon = get(spatPart, ['polygoncoords', 'value']);
    }

    tReq = addConstraintToQuery(tReq);

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
            this.state = {master: {catmaster}};
        } else {
            this.state = {master: {}};
        }

    }

    shouldComponentUpdate(np,ns) {
        return sCompare(this, np, ns);
    }

    componentWillMount() {
        var idx = catmaster.findIndex((m) => m.project === LSSTMaster.project);

        if (idx === -1) {
            this.loadMasterCatalogTable();
        }
    }

    /**
     * Fetch master catalog table with search id = 'lsstCatalogMasterTable'
     * set the project name lsst.
     * result is set in the state as 'master' object {catmaster}
     * @see setMaster
     * @returns {Object} fetch master table and once fetched, set state with it
     */
    loadMasterCatalogTable() {
        catmaster.push(LSSTMaster);
        this.setMaster({catmaster});
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
        var  {cattable} = this.props;

        if (isEmpty(master)) {
            return (
                <div style={{position: 'relative'}}>
                    <div className='loading-mask'></div>
                </div>
            );
        }


        var tblId = getTblId((cattable ? cattable : getDefaultTable(master.catmaster, LSSTMaster.project)));

        // pass cattable and master to  LsstCatalogDDList
        return (
            <FieldGroup groupKey={gkey}
                        reducerFunc={userChangeLsstDispatch(tblId)}
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
var userChangeLsstDispatch = function (tblId) {

    return (inFields, action) => {

        if (!inFields) return fieldInit(tblId);

        switch (action.type) {
            // update the size field in case tab selection is changed

            case FieldGroupCntlr.VALUE_CHANGE:

                const {fieldKey} = action.payload;

                if (fieldKey === 'targettry' || fieldKey === 'UserTargetWorldPt' || fieldKey === 'conesize' ) {  // image select panel
                    break;
                }
                const projName = get(inFields, 'project.value');
                if (!projName) break;

                var cattype = get(inFields, 'cattype.value');
                var currentCat;

                if (fieldKey === 'cattype') {
                    currentCat = get(inFields, ['cattype', 'cattable', cattype]);
                    inFields = updateMerge(inFields, 'cattable', {value: currentCat});
                }  else {
                    currentCat = get(inFields, 'cattable.value');
                }
                if (!currentCat) break;

                var currentIdx = getCatalogIndex(catmaster, projName, currentCat);
                if (currentIdx < 0) break;
                var idstr = `${currentIdx}`;

                if (fieldKey === 'cattable' || fieldKey === 'cattype') {
                    const method = get(inFields, 'spatial.value',
                                      (cattype === CATTYPE ? SpatialMethod.Cone.value : SpatialMethod.Box.value));
                    var sizeFactor = (method === SpatialMethod.Box.value) ? 2: 1;
                    const radius = getLSSTCatalogRadius(catmaster, currentCat);

                    inFields = updateMerge(inFields, 'conesize', {
                        max: sizeFactor * radius / 3600
                    });


                    var tbl_id = getTblId(currentCat, '');
                    if (tbl_id !== get(inFields, ['tableconstraints', 'tbl_id'])) {   // catalog changes

                        if (fieldKey === 'cattable') {
                            inFields = updateMerge(inFields, 'cattype.cattable',
                                                   {[cattype]: currentCat});
                        }
                        inFields = updateMerge(inFields, 'cattable',
                                        {coldef: get(inFields, ['cattable', 'coldefOptions', idstr])});
                        inFields = updateMerge(inFields, 'tableconstraints', { tbl_id });
                        inFields = updateMerge(inFields, 'tableconstraints',
                                        {value: get(inFields, ['tableconstraints', 'options', idstr])});
                        inFields = updateMerge(inFields, 'txtareasql',
                                        {value: get(inFields, ['txtareasql', 'options', idstr])});

                    }
                } else if (fieldKey === 'tableconstraints' || fieldKey === 'txtareasql') {
                    set(inFields, [fieldKey, 'options', idstr], get(inFields, [fieldKey, 'value']));
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
    return catmaster&&catmaster.find((op) => {
        return op.project === project;
    }).catalogs;
}

/**
 * @summry get the default table value fromt the project's catalog list
 * @param catmaster
 * @param project
 * @returns {string}
 */
function getDefaultTable(catmaster, project) {
    var cats = getCatalogs(catmaster, project);

    return cats ? cats[0].value : '';
}

/**
 * @summary return the catalog index by given the catalog table name
 * @param {Object[]} catmaster
 * @param {string} project
 * @param {string} catVal
 * @returns {number} index of the catalog in the catalog array
 */
function getCatalogIndex(catmaster, project, catVal) {
    var cats = getCatalogs(catmaster, project);

    return cats ?  cats.findIndex((cat) => cat.value === catVal) : -1;
}


/**
 * @summary  get catalog table type
 * @param project
 * @param catname
 * @returns {number}
 */
function getTableType(project, catname) {
    var cats = getCatalogs(catmaster, project);
    var cat = cats&&cats.find((c) => (c.value === catname));

    return cat ? cat.type : CATTYPE;
}

/**
 * @summary get radius hint for the given table
 * @param catmaster
 * @param catName
 * @returns {Number}
 */
function getLSSTCatalogRadius(catmaster, catName) {
    const proj = get(FieldGroupUtils.getGroupFields(gkey), 'project.value', LSSTMaster.project);
    var radius = 100;

    catmaster.find((m) => {
        if (m.project === proj) {
            var catObj = m.catalogs.find((cat) => {
                return cat.value === catName;
            });

            if (catObj) {
                radius = get(catObj, ['cat', RADIUS_COL], radius);
            }

            return true;
        } else {
            return false;
        }
    });
    return radius;
}
/**
 * @summary component of the constraint table for catalog search, get 'cattable' and 'master' from the parent
 */
class LsstCatalogDDList extends Component {

    constructor(props) {
        super(props);
        this.state = Object.assign({...this.props}, {optList: ''});
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }


    render() {
        const {catmaster} = this.state.master;
        if (!catmaster) return false;

        var {cattable} = this.props;
        if (!cattable) return false;

        var {cattype} = this.props;
        if (!cattype) return false;

        const spatialH = 300;
        const spatialPanelStyle = {height: spatialH, width: 550, paddingLeft: 2, paddingRight: 2};
        const catPanelStyle = {paddingLeft: 20, paddingRight: 20, height: spatialH, width: 200};

        var metadataSelector = () => {

                var typeOptions = Object.keys(LSSTTableTypes).map((t) => {
                        return {value: t, label: LSSTTableTypes[t]};
                });

                var options = LSSTMaster.catalogs.reduce((prev, cat) => {
                    if (cat.type === cattype) {
                        prev.push({value: cat.value, label: cat.label});
                    }
                    return prev;
                }, []);

                return (
                    <div className='ddselectors' style={catPanelStyle}>
                        <RadioGroupInputField
                            inlin={false}
                            initialState={{fieldKey: 'cattype'}}
                            fieldKey='cattype'
                            options={typeOptions}
                        />


                        <RadioGroupInputField
                            inline={false}
                            initialState={{value: 'lsst cat table name 1',
                                           fieldKey: 'cattable'}}
                            alignment={'vertical'}
                            fieldKey='cattable'
                            options={options}
                            tooltip={`${projectName} table types`}
                            wrapperStyle={{marginLeft: 15, marginTop: 10}}
                        />
                     </div>
                )
        };

        var searchMethod = () => {
            var method;

            if (cattype === CATTYPE) {
                const radius = getLSSTCatalogRadius(catmaster, cattable);
                const coneMax = radius / 3600;
                const boxMax = coneMax * 2;
                const polygonDefWhenPlot = get(getAppOptions(), 'catalogSpacialOp') === 'polygonWhenPlotExist';

                method = <CatalogSearchMethodType groupKey={gkeySpatial} polygonDefWhenPlot={polygonDefWhenPlot}
                                                  coneMax={coneMax} boxMax={boxMax}/>;
            } else {
                method = <LSSTImageSpatialType groupKey={gkeyImageSpatial} />
            }

            return (<div className='spatialsearch' style={spatialPanelStyle}>
                     {method}
                    </div>);
        };

        return (
            <div>
                <div className='catalogpanel'>
                    {metadataSelector()}
                    {searchMethod()}
                </div>
                <div className='ddtable'>
                    <CatalogConstraintsPanel fieldKey={'tableconstraints'}
                                             constraintskey={constraintskey}
                                             catname={cattable}
                                             showFormType={false}
                                             processId={LSSTDDPID}
                                             groupKey={gkey}
                                             createDDRequest={()=>{
                                                return {id: LSSTDDPID, table_name: cattable};
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
function fieldInit(tblId) {
    var constraintV = LSSTMaster.catalogs.map((t) =>  ({constraints: '', selcols: '', filters: undefined}));
    var catOptions = LSSTMaster.catalogs.map((t) => (t.value));
    var sqlValues = LSSTMaster.catalogs.map((t) => (''));
    var coldefList = LSSTMaster.catalogs.map((t) => (get(t, ['cat', COLDEF1]) || get(t, ['cat', COLDEF2]) || ""));

    return (
        {
            'project': {
                fieldKey: 'project',
                label: LSSTMaster.project,
                value: projectName,
                labelWidth: '100'
            },
            'cattype': {
                fieldKey: 'cattype',
                value: LSSTMaster.catalogs[0].type,
                cattable: [LSSTMaster.catalogs[defCatalog].value, LSSTMaster.catalogs[defImage].value]
            },
            'cattable': {
                fieldKey: 'cattable',
                value: LSSTMaster.catalogs[0].value,
                coldef: coldefList[0],
                options: catOptions,
                coldefOptions: coldefList
            },
            'tableconstraints': {
                fieldKey: 'tableconstraints',
                value: constraintV[0],
                tbl_id:  tblId,
                options: constraintV
            },
            'txtareasql': {
                fieldKey: 'txtareasql',
                value: sqlValues[0],
                options: sqlValues
            }
        }
    );
}


