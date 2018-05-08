/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import { get,set, merge, isArray, isEmpty, capitalize} from 'lodash';
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
import {validateFieldGroup} from '../../fieldGroup/FieldGroupUtils';
import {doJsonRequest} from '../../core/JsonUtils.js';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {makeTblRequest, makeLsstCatalogRequest} from '../../tables/TableRequestUtil.js';
import {CatalogConstraintsPanel, getTblId} from './CatalogConstraintsPanel.jsx';
import {validateSql, validateConstraints} from './CatalogSelectViewPanel.jsx';
import {LSSTImageSpatialType} from './LSSTImageSpatialType.jsx';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {sortInfoString}  from '../../tables/SortInfo.js';

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

const CATTYPE = '0';
const IMAGETYPE = '1';

const DEC = 3;

const LSSTTableTypes = {
    [CATTYPE]: 'Catalogs',
    [IMAGETYPE]: 'Images'
};

const LSSTDDPID = 'LSSTMetaSearch';


var lsstTables;
//var lsstTablesLoading;

function getLSSTMasterTable() {
    if (isArray(lsstTables)) {
        return [{projectMaster: projectName, missions: lsstTables}];
    }
}

function currentSelection() {
    const catmaster = getLSSTMasterTable();
    if (catmaster) {
        const fields = FieldGroupUtils.getGroupFields(gkey);
        const project = get(fields, ['project', 'value'], getDefaultProjectName(catmaster));
        const cattype = get(fields, ['cattype', 'value'], CATTYPE);
        const cattable = get(fields, ['cattable', 'value'], getDefaultTableName(catmaster, project, cattype));

        // cattable: table selected from select view, cattype: catalog/image from select view
        return {catmaster, project, cattype, cattable, error: undefined};
    } else {
        return {};
    }
}

function getSortCols(tableName) {
    const tblFound = lsstTables && lsstTables.map((project) => {
        return project.subproject
                .map((oneSub) => {
                    const allTables = [...oneSub.catalogs, ...oneSub.images];

                    return allTables.find((oneTable) => {
                        if (oneTable.value.includes(tableName)) {
                            return oneTable;
                        } else {
                            return false;
                        }
                    });
                })
                .find((tbl) => tbl);
    }).find((tbl) => tbl);

    return tblFound ? tblFound.sortCols : null;
}

/**
 * @summary component for LSST catalog search panel
 */
export class LSSTCatalogSelectViewPanel extends PureComponent {

    constructor(props) {
        super(props);

        this.state = currentSelection();
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.iAmMounted = true;
        if (!this.state.cattable) {
            doJsonRequest(ServerParams.JSON_SEARCH, {[ServerParams.REQUEST]: JSON.stringify({id: 'LSSTTables'})}).then((jsonTables) => {
                lsstTables = jsonTables;
                //lsstTablesLoading = false;
                this.setState(currentSelection());

            }).catch((err) => {
                console.log(err);
                //lsstTablesLoading = false;
                this.setState({error: `${err}`});
            });
        }
        this.unbinder = FieldGroupUtils.bindToStore(gkey, (fields) => {
            if (this.iAmMounted && lsstTables && fields.cattable.value !== this.state.cattable) {
                this.setState({cattable: fields.cattable.value,
                               cattype: fields.cattype.value,
                               project: fields.project.value});
            }
        });
    }


    render() {
        const {error, catmaster, cattable, cattype, project}= this.state;
        if (error) {
            return (<div>{error}</div>);
        }
        return (
            <div>
                <FormPanel
                    width='auto' height='auto'
                    groupKey={[gkey,gkeySpatial]}
                    onSubmit={onSearchSubmit()}
                    onCancel={hideSearchPanel}>
                    <LSSTCatalogSelectView cattype={cattype} cattable={cattable} project={project} master={catmaster}/>
                </FormPanel>
            </div>
        );
    }
}

/**
 * @summary call back funciton on search button
 */
function onSearchSubmit() {
    return (request) => {
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
                    if (!get(catalogState, ['conesize', 'valid'])) {
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
                    validateFieldGroup(gkeySpatial).then((valid) => {
                        if (!valid) {
                            showInfoPopup('invalid input');
                            return;
                        }
                        doCatalog(request, catalogState);
                    });
                }

            } else if (cattype === IMAGETYPE) {

                const imageState = FieldGroupUtils.getGroupFields(gkeyImageSpatial);
                const wp = get(imageState, [ServerParams.USER_TARGET_WORLD_PT, 'value']);
                const intersect = get(imageState, ['intersect', 'value']);

                if (!wp && intersect !== 'ALLSKY') {
                    showInfoPopup('Target is required');
                    return;
                }

                if (intersect !== 'CENTER' && intersect !== 'ALLSKY') {
                    if (!get(imageState, ['size', 'valid'])) {
                        showInfoPopup('box size is required');
                        return;
                    }
                }
                if (validateConstraints(gkey)) {
                    validateFieldGroup(gkeyImageSpatial).then((valid) => {
                        if (!valid) {
                            showInfoPopup('invalid input');
                            return;
                        }
                        doImage(request, imageState);
                    });
                }
            }
        }
        else if (request[gkey].Tabs === 'loadcatLsst') {
            doLoadTable(request[gkey]);
        }
        else {
            console.log('request not supported');
        }
    };
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
    const d = digits&&digits >= 0 ?  digits : DEC;

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
    const noSizeMethod = ['ALLSKY', 'CENTER'];
    const noSubsizeMethod = ['ALLSKY', 'ENCLOSED'];
    const noTargetMethod = ['ALLSKY'];
    const {project, cattable} = request[gkey] || {};
    const [,tableName] = cattable ? cattable.split('\.') : [];
    const spatial =  get(imgPart, ['spatial', 'value']);
    const intersect = get(imgPart, ['intersect', 'value']);
    const size = (!noSizeMethod.includes(intersect)) ? get(imgPart, ['size', 'value']) : '';
    const sizeUnit = 'deg';
    const subsize = (!noSubsizeMethod.includes(intersect)) ? get(imgPart, ['subsize', 'value']) : ''; // in degrees
    const wp = (!noTargetMethod.includes(intersect)) && get(imgPart, [ServerParams.USER_TARGET_WORLD_PT,'value']);

    let title = `${projectName}-${tableName}-${capitalize(intersect)}`;
    const loc = wp && wp.split(';').slice(0, 2).join();

    if (!noSizeMethod.includes(intersect)) {
        title += `([${loc}]:${formatNumberString(size)}deg)`;
    } else if (!noTargetMethod.includes(intersect)) {
        title += `([${loc}])`;
    }

    let tReq = makeLsstCatalogRequest(title, projectName, cattable,
                                      {[ServerParams.USER_TARGET_WORLD_PT]: wp,
                                       intersect,
                                       size,
                                       sizeUnit,
                                       subsize,
                                       SearchMethod: spatial},
                                       {use: 'lsst_image'});

    const sortCols = getSortCols(tableName);
    if (sortCols) tReq.sortInfo = sortInfoString(sortCols.split(','));

    tReq = addConstraintToQuery(tReq);
    const downloadButton = ({tbl_id}) => {
                return (
                    <DownloadButton tbl_id = {tbl_id}>
                        <DownloadOptionPanel
                            cutoutSize = {size}
                            dlParams = {{
                                    Title: title,
                                    FilePrefix: tableName,
                                    BaseFileName: tableName,
                                    DataSource: tableName,
                                    FileGroupProcessor: 'LSSTFileGroupProcessor'     // insert FileGroupProcessor's ID here.
                                }}/>
                    </DownloadButton>

                );
            };
    const tblOptions = {backgroundable: true};
    // currently, download is only implemented for SDSS
    if (project.includes('sdss')) {
        tblOptions.leftButtons = [downloadButton];
    }
    dispatchTableSearch(tReq, tblOptions);
}

/**
 * @summary catalog search
 * @param {Object} request
 * @param {Object} spatPart
 */
function doCatalog(request, spatPart) {

    const catPart = request[gkey];
    const {cattable} = catPart;
    const [,tableName] = cattable ? cattable.split('\.') : [];
    const spatial =  get(spatPart, ['spatial', 'value']);
    const conesize = get(spatPart, ['conesize', 'value']);
    const wp = get(spatPart, [ServerParams.USER_TARGET_WORLD_PT,'value']);
    const sizeUnit = 'deg';
    const conestr = formatNumberString(conesize);

    let title = `${projectName}-${tableName}`;
    let tReq;

    if (spatial === SpatialMethod.get('Multi-Object').value) {
        const filename = get(spatPart, ['fileUpload', 'value']);
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

        const pa = get(spatPart, ['posangle', 'value'], '0');
        const ar = get(spatPart, ['axialratio', 'value'], '0.26');

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

    dispatchTableSearch(tReq, {backgroundable:true});
}

/**
 * @summary load catalog table
 * @param {Object} request
 */
function doLoadTable(request) {
    var tReq = makeTblRequest('userCatalogFromFile', 'Lsst Table Upload', {
        filePath: get(request, 'fileUpload')
    });
    dispatchTableSearch(tReq);
}

/**
 * @summary class LSSTCatalogSelectView for selecting and handling LSST catalog
 */
class LSSTCatalogSelectView extends PureComponent {

    constructor(props) {
        super(props);
    }

    render() {
        var  {cattable, project, cattype, master} = this.props;

        if (isEmpty(master)) {
            return (
                <div style={{position: 'relative'}}>
                    <div className='loading-mask'/>
                </div>
            );
        }


        var tblId = getTblId((cattable ? cattable : getDefaultTableName(master, project, cattype)));

        // pass cattable and master to  LsstCatalogDDList
        return (
            <FieldGroup groupKey={gkey}
                        reducerFunc={userChangeLsstDispatch(tblId, master)}
                        keepState={true}>
                <FieldGroupTabs initialState={{ value:'catalog' }} fieldKey='Tabs' resizable={true}>
                    <Tab name='Search' id='catalogLsst'>
                        <LsstCatalogDDList {...this.props}/>
                    </Tab>
                    <Tab name='Load Catalog' id='loadcatLsst'>
                        <CatalogLoad forGroup={gkey} />
                    </Tab>
                </FieldGroupTabs>
            </FieldGroup>
        );
    }
}

// master: [{masterProject: 'xxx', missions: [{project: 'SDSS', ...}, {project:'WISE', ....}]}, {masterProject: 'xxx', ....}]
LSSTCatalogSelectView.propTypes = {
    cattable: PropTypes.string,
    cattype: PropTypes.string,
    project: PropTypes.string,
    master: PropTypes.arrayOf(PropTypes.shape({masterProject: PropTypes.string, missions: PropTypes.arrayOf(PropTypes.object)}))
};

/**
 * @summary Reducer from field group component, should return updated catalog selection
 * @param {string} tblId
 * @param {array} catmaster
 * @returns {Function} reducer to change fields when user interact with the dialog
 */
const userChangeLsstDispatch = function (tblId, catmaster) {

    return (inFields, action) => {

        if (!inFields) return fieldInit(tblId);

        switch (action.type) {
            // update the size field in case tab selection is changed

            case FieldGroupCntlr.VALUE_CHANGE:

                const {fieldKey, value} = action.payload;

                if (fieldKey === 'targettry' || fieldKey === 'UserTargetWorldPt' || fieldKey === 'conesize' ) {  // image select panel
                    break;
                }
                if (fieldKey === 'Tabs' && value !== 'catalogLsst') {
                    break;
                }

                const projName = get(inFields, 'project.value');
                if (!projName) break;

                const cattype = (fieldKey === 'project') ? CATTYPE : get(inFields, 'cattype.value');
                const currentCat = (fieldKey === 'cattable') ? get(inFields, 'cattable.value') :
                                                               getDefaultTableName(catmaster, projName, cattype);
                if (fieldKey === 'project') {
                    inFields = updateMerge(inFields, 'cattype', {value: cattype});
                }
                if (fieldKey === 'project' || fieldKey === 'cattype') {
                    inFields = updateMerge(inFields, 'cattable', {value: currentCat});
                }

                if (!currentCat) break;
                const tbl_id = getTblId(currentCat, '');


                if (fieldKey !== 'tableconstraints' && fieldKey !== 'txtareasql') {
                    const method = get(inFields, 'spatial.value',
                                       (cattype === CATTYPE ? SpatialMethod.Cone.value : SpatialMethod.Box.value));
                    const sizeFactor = (method === SpatialMethod.Box.value) ? 2: 1;
                    const radius = getLSSTCatalogRadius(catmaster, currentCat);

                    inFields = updateMerge(inFields, 'conesize', {
                        max: sizeFactor * radius / 3600
                    });

                    // tbl_id changes, resume the stored tableconstraints and txtareasql
                    if (tbl_id && (tbl_id !== get(inFields, ['tableconstraints', 'tbl_id']))) {   // catalog changes
                        inFields = updateMerge(inFields, 'tableconstraints', { tbl_id });
                        inFields = updateMerge(inFields, 'tableconstraints',
                                        {value: get(inFields, ['tableconstraints', 'options', tbl_id],
                                                              {constraints: '', selcols: '', filters: undefined})});
                        inFields = updateMerge(inFields, 'txtareasql',
                                        {value: get(inFields, ['txtareasql', 'options', tbl_id], '')});

                    }
                } else  {
                    if (tbl_id) {
                        set(inFields, [fieldKey, 'options', tbl_id], get(inFields, [fieldKey, 'value']));
                    }
                }
                 break;
            case FieldGroupCntlr.CHILD_GROUP_CHANGE:
                break;
            default:
                break;
        }
        return inFields;
    };
};

/**
 * @summary Return the collection of catalogs based on the project
 * @param {Object} catmaster master table data
 * @param {string} project project name
 * @param {string} cattype
 * @returns {Object[]} array with ith element is an object which option values and an object with n attribute which ith attribute is corresponding
 * to ith columns in cols of master table and its value, i.e. ith = 0, 0:'WISE', where cols[0]=projectshort
 */
function getCatalogs(catmaster, project, cattype) {
    let cats = [];

    if (!isEmpty(catmaster)) {
        catmaster.filter((masterProj) => masterProj.missions)
                 .find((oneMaster)=> {
                     let subProj = null;

                     // check if any masterProject which has subproject with value as the given project
                     oneMaster.missions.find((proj) => {
                         if (proj.subproject) {
                             subProj = proj.subproject.find((sub) => sub.value === project);
                         }
                         return subProj;
                     });

                     if (subProj) {
                         cats = cattype === CATTYPE ? subProj.catalogs.slice() : subProj.images.slice();
                     }
                     return subProj;
                });
    }

    return cats;
}

/**
 * @summary get the default table value fromt the project's catalog (and image meta) list
 * @param catmaster
 * @param project
 * @param cattype
 * @returns {string}
 */
function getDefaultTableName(catmaster, project, cattype) {
    const cats = getCatalogs(catmaster, project, cattype);

    return get(cats, ['0', 'value'], '');
}

/**
 * @summary return the catalog (or image meta) by given the catalog (or image meta) table name
 * @param {Object[]} catmaster
 * @param {string} project
 * @param {string} cattype
 * @param {string} catVal
 * @returns {Object} the catalog in the catalog array
 */
function getCatalog(catmaster, project, cattype, catVal) {
    const cats = getCatalogs(catmaster, project, cattype);

    return cats ? cats.find((cat) => cat.value === catVal) : null;
}

/**
 * @summary get all subprojects
 * @param catmaster
 * @returns {Object}  subproject set
 */
function getAllProjects(catmaster) {
    return isEmpty(catmaster) ? [] :
                catmaster.filter((oneMaster) => oneMaster.missions)
                    .reduce((subInMaster, oneMaster) => {
                        return oneMaster.missions.reduce((prev, oneProj) => {
                            if (oneProj.subproject) {
                                prev.push(...oneProj.subproject);
                            }
                            return prev;
                        }, subInMaster);
                    }, []);

}

/**
 * @summary default project (the first subproject defined)
 * @param catmaster
 * @returns {*}
 */
function getDefaultProjectName(catmaster) {
    let projIndex = -1;

    const oneMaster = catmaster.find((oneMaster) => {
        if (oneMaster.missions) {
            projIndex = oneMaster.missions.findIndex((proj) => get(proj, ['subproject', '0']));
        }
        return projIndex !== -1;
    });

    projIndex = projIndex === -1 ? 0 : projIndex;

    const subProj = get(get(oneMaster, 'missions'), [projIndex, 'subproject', '0']);
    return get(subProj, 'value', '');
}

/**
 * @summary get radius hint for the given table
 * @param catmaster
 * @param catName
 * @returns {Number}
 */
function getLSSTCatalogRadius(catmaster, catName) {
    const proj = get(FieldGroupUtils.getGroupFields(gkey), 'project.value', getDefaultProjectName(catmaster));
    const cattype = get(FieldGroupUtils.getGroupFields(gkey), 'cattype.value', CATTYPE);
    const catObj = getCatalog(catmaster, proj, cattype, catName);
    let radius;

    if (catObj) {
        radius = get(catObj, ['cat', RADIUS_COL]);
    }

    return radius || 100;
}

/**
 * @summary component of the constraint table for catalog search, get 'cattable' and 'master' from the parent
 */
class LsstCatalogDDList extends PureComponent {

    constructor(props) {
        super(props);
        this.state = Object.assign({}, {...this.props}, {optList: ''});
    }

    render() {
        const {master:catmaster} = this.state;
        if (isEmpty(catmaster)) return false;

        const {cattable, cattype, project} = this.props;

        const spatialH = 300;
        const spatialPanelStyle = {height: spatialH, width: 550, paddingLeft: 2, paddingRight: 2};
        const catPanelStyle = {paddingLeft: 20, paddingRight: 20, height: spatialH, width: 450};

        const metadataSelector = () => {

                const typeOptions = Object.keys(LSSTTableTypes).map((t) => {
                        return {value: t, label: LSSTTableTypes[t]};
                });
                const allProjects = getAllProjects(catmaster);
                const projOptions = allProjects.map((proj) => {
                    return {label: proj.label, value: proj.value, tooltip: `project: ${proj.value}`};
                }) || [];
                const options = getCatalogs(catmaster, project, cattype).map((cat) => {
                    return {label: cat.label, value: cat.value, tooltip: `table name: ${cat.value}`};
                }) || [];
                const {infourl, infodesc, imagenote} = allProjects.find((proj) => proj.value === project);

                return (
                    <div className='ddselectors' style={catPanelStyle}>
                        <ListBoxInputField fieldKey='project'
                                           wrapperStyle={{padding: 5}}
                                           initialState={{
                                                tooltip: 'select project',
                                                value: project
                                           }}
                                           options={projOptions}
                                           multiple={false}
                                           label='Select Project'
                        />
                        <RadioGroupInputField
                            inline={false}
                            initialState={{fieldKey: 'cattype',
                                           tooltip: 'select type to search'}}
                            fieldKey='cattype'
                            options={typeOptions}
                        />

                        {isEmpty(options) ? false :
                            <RadioGroupInputField
                                inline={false}
                                initialState={{value: options&&options[0],
                                               tooltip: `${LSSTTableTypes[cattype]} tables of ${project}`,
                                               fieldKey: 'cattable'}}
                                alignment={'vertical'}
                                fieldKey='cattable'
                                options={options}
                                wrapperStyle={{marginLeft: 15, marginTop: 10}}
                            />
                        }

                        {isEmpty(imagenote) || cattype === CATTYPE ? false :
                            <div style={{paddingLeft: 15, paddingTop: 5, color: 'gray'}}>{imagenote}</div>
                        }

                        <div style={{paddingTop: 10}}>
                            <a href={infourl} target='info'>{infodesc}</a>
                        </div>
                     </div>
                );
        };

        const searchMethod = () => {
            let method;

            if (cattype === CATTYPE) {
                const radius = getLSSTCatalogRadius(catmaster, cattable);
                const coneMax = radius / 3600;
                const boxMax = coneMax * 2;
                const polygonDefWhenPlot = get(getAppOptions(), 'catalogSpacialOp') === 'polygonWhenPlotExist';

                method = (<CatalogSearchMethodType groupKey={gkeySpatial} polygonDefWhenPlot={polygonDefWhenPlot}
                                                  coneMax={coneMax} boxMax={boxMax}/>);
            } else {
                method = <LSSTImageSpatialType groupKey={gkeyImageSpatial} />;
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
                    {!cattable ? false :
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
                    }
                </div>
                <div style={{display:'flex',flexDirection:'column', alignItems:'flex-end'}}>
                    <HelpIcon
                        helpId={'basics.catalogs'}/>
                </div>
            </div>
        );
    }
}

LsstCatalogDDList.propTypes = {
    cattable: PropTypes.string,
    cattype: PropTypes.string,
    project: PropTypes.string,
    master: PropTypes.arrayOf(PropTypes.object)
};

/**
 * @summary component for loading catalog file
 * @returns {XML}
 * @constructor
 */
function CatalogLoad() {
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
    if (!lsstTables) return {};
    const project = get(lsstTables, '0.subproject.0');
    const catalogs = get(project, 'catalogs');
    const constraintV = {constraints: '', selcols: '', filters: undefined};

    return (
        {
            'project': {
                fieldKey: 'project',
                value: get(project, 'value'),
                labelWidth: 100
            },
            'cattype': {
                fieldKey: 'cattype',
                value: get(catalogs, ['0', 'type'])
            },
            'cattable': {
                fieldKey: 'cattable',
                value: get(catalogs, ['0', 'value'], '')
            },
            'tableconstraints': {
                fieldKey: 'tableconstraints',
                value: constraintV,
                options: {[tblId]: constraintV}
            },
            'txtareasql': {
                fieldKey: 'txtareasql',
                value: '',
                options: {[tblId]: ''}
            }
        }
    );
}



