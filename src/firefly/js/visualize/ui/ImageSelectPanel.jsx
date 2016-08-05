/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {flux} from '../../Firefly.js';
import {visRoot } from '../ImagePlotCntlr.js';
import {NewPlotMode, getAViewFromMultiView, findViewerWithPlotId, getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {panelCatalogs} from './ImageSelectPanelProp.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {resultSuccess, resultFail} from './ImageSelectPanelResult.js';
import {getActivePlotView, primePlot} from '../PlotViewUtil.js';
import {FieldGroupCollapsible, CollapseBorder, CollapseHeaderCorner} from '../../ui/panel/CollapsiblePanel.jsx';
import {ImageSelPanelChangeOneColor, ImageSelPanelChange} from './ImageSelectPanelReducer.js';
import {get} from 'lodash';

import './ImageSelectPanel.css';

const popupId = 'ImageSelectPopup';
export const panelKey = 'SELECTIMAGEPANEL';
export const rgbFieldGroup = ['REDFieldGroup', 'GREENFieldGroup', 'BLUEFieldGroup'];
export const [RED, GREEN, BLUE] = [0, 1, 2];

export const keyMap = {
    'targettry': 'SELECTIMAGEPANEL_targettry',
    'catalogtab':'SELECTIMAGEPANEL_catalogtab',
    'irastypes': 'SELECTIMAGEPANEL_IRAS_types',
    'twomasstypes':'SELECTIMAGEPANEL_2MASS_types',
    'wisetypes': 'SELECTIMAGEPANEL_WISE_types',
    'wisebands': 'SELECTIMAGEPANEL_WISE_bands',
    'msxtypes':    'SELECTIMAGEPANEL_MSX_types',
    'dsstypes':    'SELECTIMAGEPANEL_DSS_types',
    'sdsstypes':   'SELECTIMAGEPANEL_SDSS_types',
    'blankinput':  'SELECTIMAGEPANEL_BLANK_input',
    'urlinput':    'SELECTIMAGEPANEL_URL_input',
    'urllist':     'SELECTIMAGEPANEL_URL_list',
    'urlextinput': 'SELECTIMAGEPANEL_URL_extinput',
    'fitslist':    'SELECTIMAGEPANEL_FITS_list',
    'fitsextinput':'SELECTIMAGEPANEL_FITS_extinput',
    'fitsupload':  'SELECTIMAGEPANEL_FITS_upload',
    'sizefield': 'SELECTIMAGEPANEL_ImgFeature_radius',
    'plotmode':    'SELECTIMAGEPANEL_targetplot'
};

export const [IRAS, TWOMASS, WISE, MSX, DSS, SDSS, FITS, URL, NONE] = [0, 1, 2, 3, 4, 5, 6, 7, 8];
export const rgb = ['red', 'green', 'blue'];

export function completeButtonKey( isThreeColor = false ) {
    return isThreeColor ? [panelKey,...rgbFieldGroup] :[panelKey];
}

/*
 * get the catalog id (tab id) based on stored fields of the tab
 *
 */

export function computeCurrentCatalogId( fields, colorFields, catalogId = [IRAS, IRAS, IRAS] ) {

    const keytab = keyMap['catalogtab'];
    var   newId = catalogId.slice();
    var   getTabNo = (val) => parseInt(typeof val === 'object' ? val.value : val);
    var   isFieldTabExist = (f) => (f && f[keytab] && (typeof f[keytab] !== 'object' || f[keytab].mounted));

    if (isFieldTabExist(fields)) {
        newId[0] = getTabNo(fields[keytab]);
    } else {
        rgb.forEach((color, index) => {
            if (isFieldTabExist(colorFields[index])) {
                newId[index] = getTabNo(colorFields[index][keytab]);
            }
        });
    }

    return newId;
}

/**
 * show image select popup window
 * @param {string} popTitle
 */

// this version is made to add a new plot to an existing viewer if no plot is found
// or replace the plot from the active viewer

export const [PLOT_NO, PLOT_REPLACE, PLOT_CREATE, PLOT_CREATE3COLOR] = [0x000, 0x100, 0x0010, 0x001];
export const PlotSelectMode = {
                            'NoPlot':     PLOT_NO,
                            'CreatePlot': PLOT_CREATE|PLOT_CREATE3COLOR,
                            'ReplacePlot':PLOT_REPLACE,
                            'ReplaceOrCreate': PLOT_REPLACE|PLOT_CREATE|PLOT_CREATE3COLOR};

// get a view which can add new plot
var getAViewId = (mvroot) => {
    var aView = getAViewFromMultiView(mvroot);
    return aView ? aView.viewerId : '';
};

var canAddNewPlot = (viewer) => (!viewer.viewerId.includes('RESERVED') && (viewer.canReceiveNewPlots === NewPlotMode.create_replace.key));
var canOnlyReplacePlot = (viewer) => (!viewer.viewerId.includes('RESERVED') && (viewer.canReceiveNewPlots === NewPlotMode.replace_only.key));
var canNotUpdatePlot = (viewer) => (viewer.viewerId.includes('RESERVED') || (viewer.canReceiveNewPlots === NewPlotMode.none.key));

// if there is plotID, find the viewer (plotId & viewerId => replace or create)
//                                     (plotId & no viewerid => replace)
// if there is no plotID, find an avaiable multiImageViewer (no plotId & viewerId => create)
export function getPlotInfo( vr ) {
    var visroot = !vr ? visRoot() : vr;
    var mvroot = getMultiViewRoot();
    var plotId = get(visroot, 'activePlotId');
    var viewerId = plotId ?  findViewerWithPlotId(getMultiViewRoot(), plotId) : null;
    var plotMode = PlotSelectMode.NoPlot;
    var viewer;

    // check if viewer can receive new plots or replace plot and not reserved
    if (viewerId ) {
        viewer = getViewer(mvroot, viewerId);

        // find another viwer which can not either receive new plots or replace the plot
        if (!viewer || canNotUpdatePlot(viewer)) {
            plotId = null;
        }
    }

    if (!plotId || (viewer && canOnlyReplacePlot(viewer))) {
        viewerId = getAViewId(mvroot);   // get a new viewer if no plot, plotId: no, viewerId: yes (create)
                                         // get a new viewer (create_replace) if plot's view container is replace_only
                                         //  -> plot for replace, viewer for create new plot
    }                                    // there is plot and no viewer, plotId: yes, vieweId: no (replace)

    if (viewerId) {
        plotMode = (plotId) ? PlotSelectMode.ReplaceOrCreate : PlotSelectMode.CreatePlot;
    } else if (plotId) {
        plotMode = PlotSelectMode.ReplacePlot;
    }

    return {plotId, plotMode, viewerId};
}

export function showImageSelPanel(popTitle) {
     var popup = (<PopupPanel title={popTitle}>
                    <ImageSelection />
                 </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

/*
 * compute the space for string to be displayed
 */

export function computeLabelWidth(labelStr) {
    return labelStr.length * 6;
}


/*
 * check if the tab selection need target (and size) setting or not
 */

export function isTargetNeeded(tabId, isThreeColor = false) {
    var notargets = (isThreeColor) ? [URL, FITS, NONE] : [URL, FITS];

    if (typeof tabId !== 'number') {
        return (isThreeColor) ? tabId.some((item) => !notargets.includes(item)) : !notargets.includes(tabId[0]);
    } else {
        return !notargets.includes(tabId);
    }
}

function getAllGroupFields(...keys) {
    return keys.reduce((prev, fg) => (Object.assign(prev, {[fg]: FieldGroupUtils.getGroupFields(fg)})), {});
}
/**
 *  container component for image select panel
 *
 */
export class ImageSelection extends Component {
     constructor(props) {
         super(props);

         this.groupKey = panelKey;
         this.allfields = getAllGroupFields(panelKey,...rgbFieldGroup);

         this.state = {
             initCatalogId: props.catalogId ? props.catalogId : [IRAS, IRAS, IRAS],
             fields: this.allfields[panelKey],
             [rgbFieldGroup[RED]]: this.allfields[rgbFieldGroup[RED]],
             [rgbFieldGroup[GREEN]]: this.allfields[rgbFieldGroup[GREEN]],
             [rgbFieldGroup[BLUE]]: this.allfields[rgbFieldGroup[BLUE]],
             visroot: visRoot()
         };
     }

    componentWillReceiveProps (nextProps) {
        this.allfields = getAllGroupFields(panelKey,...rgbFieldGroup);

        this.setState({
            fields: this.allfields[panelKey],
            [rgbFieldGroup[RED]]: this.allfields[rgbFieldGroup[RED]],
            [rgbFieldGroup[GREEN]]: this.allfields[rgbFieldGroup[GREEN]],
            [rgbFieldGroup[BLUE]]: this.allfields[rgbFieldGroup[BLUE]],
            visroot: visRoot()
        });
    }



    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.stateUpdate());
    }

    stateUpdate() {
        var allfields = getAllGroupFields(this.groupKey,...rgbFieldGroup);
        var vr = visRoot();

        var isfieldChanged = (allfields) =>
                {
                    var fields = rgbFieldGroup.find((fg) =>
                                  (this.state[fg] && allfields[fg] && this.state[fg] !== allfields[fg]));

                    return (fields) ? true : (((allfields[this.groupKey]) || (this.state.fields))&&
                                              ( allfields[this.groupKey] !== this.state.fields ));
                };

        if (isfieldChanged(allfields) || vr.activePlotId !== this.state.visroot.activePlotId) {

            if (this.iAmMounted) {
                this.setState({
                    visroot: vr,
                    fields: allfields[panelKey],
                    [rgbFieldGroup[RED]]: allfields[rgbFieldGroup[RED]],
                    [rgbFieldGroup[GREEN]]: allfields[rgbFieldGroup[GREEN]],
                    [rgbFieldGroup[BLUE]]: allfields[rgbFieldGroup[BLUE]]
                });
            }
        }
    }

    render() {
        var {loadButton} = this.props;
        var {plotId, viewerId, plotMode} = getPlotInfo(this.state.visroot);
        var params = Object.assign({}, this.state, {plotId, viewerId, plotMode, loadButton});

        return <ImageSelectionView {...params}/>;

    }
}

ImageSelection.propTypes = {
    catalogId: PropTypes.arrayOf(PropTypes.number),
    loadButton: PropTypes.bool
};

// detect if create new plot or new 3 color plots according to the option selected
// output: PLOT_REPLACE, PLOT_CREATE, PLOT_CREATE3COLOR, PLOT_NO

export function isCreatePlot(pMode, fields) {
    if (!pMode || pMode === PLOT_NO) {    // no plot to draw
        return PLOT_NO;
    } else {
        if (pMode & (PLOT_CREATE | PLOT_CREATE3COLOR)) {        // check plotMode and fields

            var pm = fields ? get(fields, keyMap['plotmode']) : null;

            if (pm && pm.value.includes('create')) {
                return (pm.value.includes('3color')) ? PLOT_CREATE3COLOR : PLOT_CREATE;
            } else {
                return (pMode & PLOT_REPLACE)? PLOT_REPLACE :PLOT_CREATE|PLOT_CREATE3COLOR;
            }
        }
        return PLOT_REPLACE;
    }
}

export function isOnThreeColorSetting(pMode, fields) {
    var pmField = fields ? get(fields, keyMap['plotmode']) : null;

    if (pmField && pmField.value === 'create3color') {
        return true;
    }

    // no radio button is mounted, then check the mode
    if ((pmField && pmField.value === 'replace') ||              // select replace already
        (!pmField && pMode && (pMode & PLOT_REPLACE)))  {        // createorreplace or replace (no field yet)
        var plotView = getActivePlotView(visRoot());

        if (plotView) {
            var pPlot = primePlot(plotView);

            return pPlot ? pPlot.plotState.isThreeColor() : false;
        }
    }
    return false;
}

class ImageSelectionView extends Component {

    constructor(props) {
        super(props);

        this.state = {
            crtCatalogId: computeCurrentCatalogId(props.fields,
                            [props[rgbFieldGroup[RED]], props[rgbFieldGroup[GREEN]], props[rgbFieldGroup[BLUE]]],
                            props.initCatalogId),
            addPlot:  isCreatePlot(props.plotMode, props.fields),
            isThreeColor: isOnThreeColorSetting(props.plotMode, props.fields)
        };
    }

    componentWillReceiveProps(nextProps) {

        this.setState ({
            crtCatalogId: computeCurrentCatalogId(nextProps.fields,
                [nextProps[rgbFieldGroup[RED]], nextProps[rgbFieldGroup[GREEN]], nextProps[rgbFieldGroup[BLUE]]],
                 nextProps.initCatalogId),
            addPlot: isCreatePlot(nextProps.plotMode, nextProps.fields),
            isThreeColor: isOnThreeColorSetting(nextProps.plotMode, nextProps.fields)}
        );
    }

    render() {

        // tabs for each catalog
        var categoryTabs = (fieldName, msg='') => {
            var tabsRes = panelCatalogs.map((item, index) =>
                (<Tab key={index} name={item.Title} id={item.CatalogId.toString()}>
                    <CatalogTabView catalog={item}  fields={get(this.props, fieldName)}/>
                </Tab>));
            if (this.state.isThreeColor) {
                var noneTab = 'Disable';
                tabsRes.push(
                    (<Tab key={noneTab} name={noneTab} id={`${NONE}`}>
                        <div className='tabview padding_disable'>
                            <span> {msg} </span>
                        </div>
                    </Tab>));
            }
            return tabsRes;
        };


        var oneImageTabs = () => (
            <FieldGroupTabs fieldKey={keyMap['catalogtab']} >
                {categoryTabs('fields')}
            </FieldGroupTabs>
        );


        var threeColorTabs = rgb.map((color, index) => {
            var msg = `${color.toUpperCase()} is not selected`;
            var corner = CollapseHeaderCorner.BottomLeft;
            const RGB = ['rgb(255, 51, 51)', 'rgb(51, 153, 51)', 'rgb(51, 51, 255)'];

            return (
                <FieldGroupCollapsible initialState= {{ value: index===RED?'open':'closed' }}
                                       fieldKey={`collapsible${index}`}
                                       key={index}
                                       header={color.toUpperCase()}
                                       isOpen={ index === RED }
                                       borderStyle={CollapseBorder.Oneborder}
                                       headerRoundCorner={ index === RED ? corner|CollapseHeaderCorner.TopRight : corner}
                                       wrapperStyle={ {marginBottom: 3} }
                                       headerStyle={{background: RGB[index],
                                                    color: 'white',
                                                    fontWeight: 'bold',
                                                    paddingTop: 5,
                                                    paddingBottom: 5 }}
                                       contentStyle={{padding: 10,
                                                     paddingTop: 10,
                                                     paddingBottom: 10,
                                                     margin: '0px 0px 0px 10px'}}>
                    <FieldGroup groupKey={rgbFieldGroup[index]}
                                reducerFunc={ImageSelPanelChangeOneColor}
                                keepState={true}>
                        <FieldGroupTabs fieldKey={keyMap['catalogtab']}>
                            {categoryTabs(rgbFieldGroup[index], msg)}
                        </FieldGroupTabs>
                    </FieldGroup>
                </FieldGroupCollapsible>
            );
        });

        var tabsArea = () => {
            if (this.state.isThreeColor) {
                return (
                    <div className={'section_lesstop'}>
                        { threeColorTabs }
                    </div>);
            } else {
                return (
                    <div className={'section'}>
                        { oneImageTabs() }
                    </div>);
            }
        };

        var sizeArea = () => {
            return isTargetNeeded(this.state.crtCatalogId, this.state.isThreeColor) ?
                    <SizeInputFields fieldKey={keyMap['sizefield']} showFeedback={true} /> :
                    <div></div>;
        };

        var targetPanelArea = () => {
            var anytab = isTargetNeeded(this.state.crtCatalogId, this.state.isThreeColor);
            return (<TargetPanelSetView plotMode={this.props.plotMode}
                                displayEntry={anytab ? true : false} />);

        };

        var loadButtonArea = () => {
             if (!this.props.loadButton) {
                 return <div></div>;
             } else {
                var plotInfo = {addPlot: this.state.addPlot,
                                isThreeColor: this.state.isThreeColor,
                                plotId: this.props.plotId,
                                viewerId: this.props.viewerId};
                return (
                    <div className={'close'}>
                        <div className={'padding'}>
                            <CompleteButton
                                dialogId={popupId}
                                groupKey={completeButtonKey(this.state.isThreeColor)}
                                onSuccess={resultSuccess(plotInfo)}
                                onFail={resultFail()}
                                text={'Load'} />
                        </div>
                        <div className={'padding'}>
                            <HelpIcon helpId={helpId}/>
                        </div>
                    </div>);
            }
        };

        var helpId = 'basics.catalog';

        /*
         * top: target panel
         * middle: tab panel for catalogs
         * bottom: size panel
         *
         * Load button and help icon
         */
        return (
            <FieldGroup  groupKey={panelKey}
                         reducerFunc={ImageSelPanelChange(this.state.crtCatalogId, this.state.isThreeColor)}
                         keepState={true}>
                <div className={'imagepanel'}>
                    <div className={'section'}>
                        {targetPanelArea()}
                    </div>
                    { tabsArea() }
                    <div className={'size'}>
                        { sizeArea()}
                    </div>
                    { loadButtonArea() }
                </div>
            </FieldGroup>
        );
    }
}

ImageSelectionView.propTypes={
    initCatalogId: PropTypes.arrayOf(PropTypes.number).isRequired,
    fields: PropTypes.object,
    [rgbFieldGroup[RED]]: PropTypes.object,
    [rgbFieldGroup[GREEN]]: PropTypes.object,
    [rgbFieldGroup[BLUE]]: PropTypes.object,
    plotId: PropTypes.string,
    viewerId: PropTypes.string,
    plotMode: PropTypes.number,
    loadButton: PropTypes.bool
};

ImageSelectionView.defaultProps={
   loadButton: true
};

/**
 * top row for target panel
 *
 * @returns {XML}
 * @constructor
 */
function TargetPanelSetView({plotMode, displayEntry}) {

    var showTarget = () => {
         if ( !displayEntry ) {
             return <div className={'intarget'}></div>;
         } else {
             return (
                 <div className={'intarget'}>
                     <TargetPanel groupKey={panelKey}/>
                     <ListBoxInputField
                         fieldKey={keyMap['targettry']}
                         options={[{label: 'Try NED then Simbad', value: 'NED'},
                                   {label: 'Try Simbad then NED', value: 'simbad'}
                              ]}
                         multiple={false}
                         labelWidth={3}
                     />
                 </div>
             );
         }
     };

    var showPlotMode = () => {
        var options = [];

        // not showing the options if PLOT_REPLACE only
        if (plotMode && (plotMode & PLOT_REPLACE) && (plotMode !== PLOT_REPLACE)) {
            options.push({label: 'Replace Image', value: 'replace'});
        }
        if (plotMode && (plotMode & PLOT_CREATE)) {
            options.push({label: 'Create New Plot', value: 'create'});
        }
        if (plotMode && (plotMode & PLOT_CREATE3COLOR)) {
            options.push({label: 'Create New Plot - 3 Colors', value: 'create3color'});
        }

        if (options.length > 0) {
            return (
                <div className={'padding_noleft'}>
                    <RadioGroupInputField
                        initialState={{value: options[0].value,
                                       fieldKey: keyMap['plotmode'] }}
                        alignment={'horizontal'}
                        fieldKey={keyMap['plotmode']}
                        options={options}
                    />
                </div>
            );
        } else {
            return undefined;
        }
    };


    return (
        <div className={'targetpanel'}>
            { showPlotMode() }
            { showTarget() }
        </div>
    );
}

TargetPanelSetView.propsTypes={
    plotMode: PropTypes.number,
    displayEntry: PropTypes.bool
 };

TargetPanelSetView.defaultProps={
    displayEntry: false
};


/**
 * component inside each catalog tab at middle row
 *
 * @param {Object} catalog
 * @param {Object} fields
 * @returns {XML}
 * @constructor
 */
function CatalogTabView({catalog, fields}) {
    const sym = catalog.Symbol.toLowerCase();

    var listfield = (fieldname, index) => {
        var fkey = `${sym}${fieldname}`;
        var listItems = catalog[fieldname].Items.map((item) => (({'label': item.name, 'value': item.item})));
        var padding = (index === 0)? 'cataloglist' : 'cataloglist_nottop';

        return (
            <div className={padding} key={index}>
                <ListBoxInputField
                    fieldKey={keyMap[fkey]}
                    options={listItems}
                    label={ catalog[fieldname].Title }
                    labelWidth={computeLabelWidth(catalog[fieldname].Title)}
                    multiple={false}
                />
            </div>
        );
    };

    var inputfield = (fieldname, index) => {
        var fkey = `${sym}${fieldname}`;
        var padding = (index === 0)? 'tab_padding_top' : 'tab_padding_nottop';
        var style = (catalog['CatalogId'] === URL) ? {width: '50%'} : null;

        return (
            <div className={padding} key={index}>
                <ValidationField fieldKey={keyMap[fkey]}
                                 labelWidth={computeLabelWidth(catalog[fieldname].Title)}
                                 label={ catalog[fieldname].Title }
                                 style={style} />
            </div>
        );
    };

    var fieldrequest = (fieldname, index) => {
        if (fieldname.includes('types') || fieldname.includes('bands') ||
            fieldname.includes('list')) {
            return listfield(fieldname, index);
        }  else if (fieldname.includes('input')) {
            var dkey = 'dependon';

            if (catalog[fieldname].hasOwnProperty(dkey)) {
                var dependKey = Object.keys(catalog[fieldname][dkey])[0];
                var fkey = `${sym}${dependKey}`;

                if (fields && fields.hasOwnProperty(keyMap[fkey]) &&
                    fields[keyMap[fkey]].value === catalog[fieldname][dkey][dependKey]) {
                    return inputfield(fieldname, index);
                } else {
                    return undefined;
                }
            }

            return inputfield(fieldname, index);
        } else if ( fieldname.includes('upload')) {
            return (
                <div key={index} >
                    <FileUpload
                        wrapperStyle={{margin: '15px 10px 21px 10px'}}
                        fieldKey={keyMap['fitsupload']}
                        initialState= {{
                        tooltip: 'Select a file to upload' }}
                    />
                </div>
            );
        } else {
            return undefined;
        }
    };

    return (
        <div className={'tabview'}>
            {(catalog.fields).map((oneField, index) =>  fieldrequest(oneField, index))}
        </div>
    );
}

CatalogTabView.propTypes={
    catalog: PropTypes.object.isRequired,
    fields: PropTypes.object
};

