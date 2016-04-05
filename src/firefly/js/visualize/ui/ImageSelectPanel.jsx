/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {flux} from '../../Firefly.js';
import {visRoot } from '../ImagePlotCntlr.js';
import {getAViewFromMultiView, findViewerWithPlotId, getMultiViewRoot} from '../MultiViewCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {FieldGroupTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchShowDialog} from '../../core/DialogCntlr.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {panelCatalogs} from './ImageSelectPanelProp.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {SizeInputFields, sizeFromDeg} from '../../ui/sizeInputFields.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {resultSuccess, resultFail} from './ImageSelectPanelResult.js';
import {get} from 'lodash';

import './ImageSelectPanel.css';

const popupId = 'ImageSelectPopup';
export const panelKey = 'SELECTIMAGEPANEL';

export const keyMap = {
    'selTab':    'SELECTIMAGEPANEL_SelectedCatalog',
    'targettry': 'SELECTIMAGEPANEL_targettry',
    'targetrep': 'SELECTIMAGEPANEL_targetrep',
    'catalogtab':'SELECTIMAGEPANEL_catalogtab',
    'irsatypes': 'SELECTIMAGEPANEL_IRSA_types',
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
    'radiusfield': 'SELECTIMAGEPANEL_ImgFeature_radius',
    'plotmode':    'SELECTIMAGEPANEL_targetplot'
};

export const [IRSA, TWOMASS, WISE, MSX, DSS, SDSS, FITS, URL, BLANK] = [0, 1, 2, 3, 4, 5, 6, 7, 8];

/*
 * get the catalog id (tab id) based on stored fields of the tab
 *
 */

export function computeCurrentCatalogId( fields, catalogId = IRSA ) {

    const keytab = keyMap['catalogtab'];

    if (fields && fields.hasOwnProperty(keytab)) {
        var tval = fields[keytab];
        return parseInt( typeof tval === 'object' ? tval.value : tval);
    }

    return catalogId;
}

/**
 * show image select popup window
 * @param {string} popTitle
 */

// this version is made to add a new plot to an existing viewer if no plot is found
// or replace the plot from the active viewer

const [REPLACE, CREATE, CREATE3COLOR] = [0x100, 0x010, 0x001];
export const PlotSelectMode = {
                            'AddNewPlot': CREATE,     //change to CREATE3COLOR later
                            'ReplacePlot':REPLACE,
                            'ReplaceOrCreate': REPLACE|CREATE};

var getAViewId = () => {
    var aView;

    aView = getAViewFromMultiView();
    return aView ? aView.viewerId : '';
};


export function getPlotInfo() {
    var visroot = visRoot();
    var plotId = get(visroot, 'activePlotId');
    var viewerId = plotId ?  findViewerWithPlotId(getMultiViewRoot(), plotId) : (getAViewId());
    var plotMode;

    if (viewerId) {
        if (plotId) {
            plotMode = PlotSelectMode.ReplaceOrCreate;
        } else {
            plotMode = PlotSelectMode.AddNewPlot;
        }
    } else {
        if (plotId) {
            plotMode = PlotSelectMode.ReplacePlot;
        }
    }

    return {plotId, plotMode, viewerId};
}

export function showImageSelPanel(popTitle) {
    var {plotMode, viewerId} = getPlotInfo();

    // dispatchAddImages(plotView.viewerId, [plotId]);

    var popup = (<PopupPanel title={popTitle}>
                    <ImageSelection plotMode={plotMode} viewerId={viewerId} catalogId={TWOMASS}/>
                 </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

/*
 * compute the space for string to be displayed
 */

function computeLabelWidth(labelStr) {
    return labelStr.length * 6;
}



// get unit, min and max from the data file
var getRangeItem = (crtCatalogId, rangeItem) => {
    if (panelCatalogs[crtCatalogId].hasOwnProperty('range')) {
        return panelCatalogs[crtCatalogId]['range'][rangeItem];
    } else {
        if (rangeItem === 'unit') {
            return 'deg';
        } else {
            return 0.0;
        }
    }
};

// get default size from the data file
var getSize = (crtCatalogId) => {
    if (panelCatalogs[crtCatalogId].hasOwnProperty('size')) {
        return panelCatalogs[crtCatalogId]['size'].toString();
    } else {
        return '0.0';
    }
};


function fieldInit(crtCatalogId) {
    var size = 'Size:';

    return (
        {[keyMap['targettry']]: {
            fieldKey: keyMap['targettry'],
            value: 'NED',
            label: ''
        },
        [keyMap['plotmode']]: {
            fieldKey: keyMap['plotmode'],
            value: 'replace'
        },
        [keyMap['catalogtab']]: {
            fieldKey: keyMap['catalogtab'],
            value: panelCatalogs[crtCatalogId].CatalogId.toString()
        },
        [keyMap['irsatypes']]: {
            fieldKey: keyMap['irsatypes'],
            label: panelCatalogs[IRSA].types.Title,
            value: panelCatalogs[IRSA].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[IRSA].types.Title)
        },
        [keyMap['twomasstypes']]: {
            fieldKey: keyMap['twomasstypes'],
            label: panelCatalogs[TWOMASS].types.Title,
            value: panelCatalogs[TWOMASS].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[TWOMASS].types.Title)
        },
        [keyMap['wisetypes']]: {
            fieldKey: keyMap['wisetypes'],
            label: panelCatalogs[WISE].types.Title,
            value: panelCatalogs[WISE].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[WISE].types.Title)
        },
        [keyMap['wisebands']]: {
            fieldKey: keyMap['wisebands'],
            label: panelCatalogs[WISE].bands.Title,
            value: panelCatalogs[WISE].bands.Default,
            labelWidth: computeLabelWidth(panelCatalogs[WISE].bands.Title)
        },
        [keyMap['msxtypes']]: {
            fieldKey: keyMap['msxtypes'],
            label: panelCatalogs[MSX].types.Title,
            value: panelCatalogs[MSX].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[MSX].types.Title)
        },
        [keyMap['dsstypes']]: {
            fieldKey: keyMap['dsstypes'],
            label: panelCatalogs[DSS].types.Title,
            value: panelCatalogs[DSS].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[DSS].types.Title)
        },
        [keyMap['sdsstypes']]: {
            fieldKey: keyMap['sdsstypes'],
            label: panelCatalogs[SDSS].types.Title,
            value: panelCatalogs[SDSS].types.Default,
            labelWidth: computeLabelWidth(panelCatalogs[SDSS].types.Title)
        },
        [keyMap['fitslist']]: {
            fieldKey: keyMap['fitslist'],
            label: panelCatalogs[FITS].list.Title,
            value: panelCatalogs[FITS].list.Default,
            labelWidth: computeLabelWidth(panelCatalogs[FITS].list.Title)
        },
        [keyMap['fitsextinput']]: {
            fieldKey: keyMap['fitsextinput'],
            label: panelCatalogs[FITS].extinput.Title,
            value: '0',
            labelWidth: computeLabelWidth(panelCatalogs[FITS].extinput.Title)
        },
        [keyMap['blankinput']]: {
            fieldKey: keyMap['blankinput'],
            label: panelCatalogs[BLANK].input.Title,
            value: panelCatalogs[BLANK].input.Default.toString(),
            validator: Validate.floatRange.bind(null,
                panelCatalogs[BLANK].input.range.min,
                panelCatalogs[BLANK].input.range.max, 1.0, 'a float field'),
            labelWidth: computeLabelWidth(panelCatalogs[BLANK].input.Title)
        },
        [keyMap['urlinput']]: {
            fieldKey: keyMap['urlinput'],
            label: panelCatalogs[URL].input.Title,
            validator: Validate.validateUrl.bind(null, 'a url field'),
            value: '',
            labelWidth: computeLabelWidth(panelCatalogs[URL].input.Title)
        },
        [keyMap['urllist']]: {
            fieldKey: keyMap['urllist'],
            label: panelCatalogs[URL].list.Title,
            value: panelCatalogs[URL].list.Default,
            labelWidth: computeLabelWidth(panelCatalogs[URL].list.Title)

        },
        [keyMap['urlextinput']]: {
            fieldKey: keyMap['urlextinput'],
            label: panelCatalogs[URL].extinput.Title,
            value: '0',
            labelWidth: computeLabelWidth(panelCatalogs[URL].extinput.Title)
        },
        [keyMap['radiusfield']]: {
            fieldKey: keyMap['radiusfield'],
            label: size,
            labelWidth: computeLabelWidth(size),
            unit: getRangeItem(crtCatalogId, 'unit'),
            min:  getRangeItem(crtCatalogId, 'min'),
            max:  getRangeItem(crtCatalogId, 'max'),
            value: getSize(crtCatalogId)}
        });
}

/*
 *
 * image select pane initial state for all fields
 */
var ImageSelPanelChange = function (crtCatalogId) {
    return (inFields, action) => {
        if (!inFields) {
            return fieldInit(crtCatalogId);
        } else {

            if (action.type === FieldGroupCntlr.VALUE_CHANGE) {
                if (action.payload.fieldKey === keyMap['catalogtab']) {
                    var catalogId = parseInt(inFields[keyMap['catalogtab']].value);

                    if (catalogId !== URL && catalogId !== FITS) {
                        var originalSize = get(inFields, keyMap['radiusfield']);
                        var {value, unit} = originalSize;
                        var min = getRangeItem(catalogId, 'min');
                        var max = getRangeItem(catalogId, 'max');
                        var radiusField;

                        // update value if no value or value is out of range while changing tab
                        // otherwise keep the value and unit as being left off from previous tab
                        if (!value || !(Validate.floatRange(min, max, 1, 'value of radius size in degree', value).valid)) {
                            value = getSize(catalogId);

                            radiusField = Object.assign({}, originalSize,
                                {
                                    min,
                                    max,
                                    value
                                 });
                        } else {
                            radiusField = Object.assign({}, originalSize,
                                {
                                    min: getRangeItem(catalogId, 'min'),
                                    max: getRangeItem(catalogId, 'max')
                                });
                        }

                        radiusField = Object.assign({}, radiusField,
                                                        {'displayValue': sizeFromDeg(value, unit)});

                        return Object.assign({}, inFields, {[keyMap['radiusfield']]: radiusField});
                    }
                }
            }

            return inFields;
        }
    };
};


/**
 *  container component for image select panel
 *
 */
export class ImageSelection extends Component {
     constructor(props) {
         super(props);

         this.groupKey = panelKey;
         // if (!FieldGroupUtils.getGroupFields(this.groupKey)) {
         //     dispatchInitFieldGroup(this.groupKey, true, null, ImageSelPanelChange);
         // }

         this.state = { initCatalogId:  props.catalogId ? props.catalogId: IRSA,
                        fields:         FieldGroupUtils.getGroupFields(this.groupKey)
         };
         // this.state = {currentCatalogIdx: 0 };
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
        var fields = FieldGroupUtils.getGroupFields(this.groupKey);

        if (fields) {
            if (this.iAmMounted) {
                this.setState({ fields });
            }
        }
    }


    render() {
        var {plotId, viewerId, plotMode, loadButton} = this.props;
        var params = Object.assign({}, this.state, {plotId, viewerId, plotMode, loadButton});

        return <ImageSelectionView {...params}/>;

    }
}

ImageSelection.propTypes = {
    catalogId: PropTypes.number,
    plotId: PropTypes.string,
    viewerId: PropTypes.string,
    plotMode: PropTypes.number,
    loadButton: PropTypes.bool
};


export function isCreatePlot(pMode, fields) {
    if (!pMode || !fields) {
        return false;
    } else {
        return (
        (pMode === PlotSelectMode.AddNewPlot) ||
        ((pMode === PlotSelectMode.ReplaceOrCreate) &&
         (fields.hasOwnProperty(keyMap['plotmode']) && fields[keyMap['plotmode']].value === 'create'))
        );
    }
}

class ImageSelectionView extends Component {

    constructor(props) {
        super(props);

        this.state = {
            crtCatalogId: computeCurrentCatalogId(props.fields, props.initCatalogId),
            addPlot:  isCreatePlot(props.plotMode, props.fields)
        };
    }

    componentWillReceiveProps(nextProps) {

        this.setState ({
            crtCatalogId: computeCurrentCatalogId(nextProps.fields, nextProps.initCatalogId),
            addPlot: isCreatePlot(nextProps.plotMode, nextProps.fields)}
        );
    }

    render() {

        // tabs for each catalog
        var categoryTabs = panelCatalogs.map((item, index) =>
            (<Tab key={index} name={item.Title} id={item.CatalogId.toString()}>
                <CatalogTabView catalog={item}  fields={this.props.fields}/>
            </Tab>)
        );


        var radius = () => {
            if (this.state.crtCatalogId != URL && this.state.crtCatalogId != FITS) {
                return (
                    <SizeInputFields fieldKey={keyMap['radiusfield']} />
                );
            } else {
                return (<div></div>);
            }
        };

        var loadButtonArea = () => {
             if (!this.props.loadButton) {
                 return <div></div>;
             } else {
                return (
                    <div className={'close'}>
                        <div className={'padding'}>
                            <CompleteButton
                                dialogId={popupId}
                                groupKey={panelKey}
                                onSuccess={resultSuccess(this.state.addPlot, this.props.viewerId)}
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
                         reducerFunc={ImageSelPanelChange(this.props.initCatalogId)}
                         keepState={true}>
                <div className={'imagepanel'}>
                    <div className={'section'}>
                        <TargetPanelSetView currentCatalogIdx={this.state.crtCatalogId}
                                            plotMode={this.props.plotMode} />
                    </div>
                    <div className={'section'}>
                        <FieldGroupTabs
                            fieldKey={keyMap['catalogtab']} >
                            {categoryTabs}
                        </FieldGroupTabs>
                    </div>
                    <div className={'size'}>
                        {radius()}
                    </div>

                    { loadButtonArea() }
                </div>
            </FieldGroup>
        );
    }
}

ImageSelectionView.propTypes={
    initCatalogId: PropTypes.number.isRequired,
    fields: PropTypes.object,
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
function TargetPanelSetView({currentCatalogIdx, plotMode}) {

     var showTarget = () => {
         if ((currentCatalogIdx === URL || currentCatalogIdx === FITS)) {
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
        if ((plotMode && (plotMode === PlotSelectMode.ReplaceOrCreate))) {
            return (
                <div className={'padding_noleft'}>
                    <RadioGroupInputField
                        alignment={'horizontal'}
                        fieldKey={keyMap['plotmode']}
                        options={[
                                {label: 'Replace Image', value: 'replace'},
                                {label: 'Create New Plot', value: 'create'}
                                ]}
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
    currentCatalogIdx: PropTypes.number.isRequired,
    plotMode: PropTypes.number
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
        var padding = (index === 0)? 'padding_top' : 'padding_nottop';

        return (
            <div className={padding} key={index}>
                <ValidationField fieldKey={keyMap[fkey]}
                                 labelWidth={computeLabelWidth(catalog[fieldname].Title)}
                                 label={ catalog[fieldname].Title } />
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
                        upload_url= {catalog[fieldname].url}
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

