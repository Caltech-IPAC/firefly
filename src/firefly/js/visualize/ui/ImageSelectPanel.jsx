/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {flux} from '../../Firefly.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchPlotImage, visRoot } from '../ImagePlotCntlr.js';
import {primePlot, getPlotViewById} from '../PlotViewUtil.js';
import {dispatchAddImages, getAViewFromMultiView, findViewerWithPlotId, getMultiViewRoot} from '../MultiViewCntlr.js';
import {parseWorldPt} from '../Point.js';
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
import {convertAngle} from '../VisUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';

import {FileUpload} from '../../ui/FileUpload.jsx';
import {SizeInputFields} from '../../ui/sizeInputFields.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import Enum from 'enum';
import {get} from 'lodash';

import './ImageSelectPanel.css';

const popupId = 'ImageSelectPopup';
const panelKey = 'SELECTIMAGEPANEL';
const ImageId = 'imgPlot';
var   imgNo = 1;

const keyMap = {
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

const [IRSA, TWOMASS, WISE, MSX, DSS, SDSS, FITS, URL, BLANK] = [0, 1, 2, 3, 4, 5, 6, 7, 8];

/**
 * show image select popup window
 * @param {string} popTitle
 */

// this version is made to add a new plot to an existing viewer if no plot is found
// or replace the plot from the active viewer

const PlotSelectMode = new Enum(['AddNewPlot', 'ReplacePlot', 'ReplaceOrCreate']);

var getAViewId = () => {
    var aView;

    aView = getAViewFromMultiView();
    return aView ? aView.viewerId : '';
};

export function showImageSelPanel(popTitle)
{
    var visroot = visRoot();
    var plotId = get(visroot, 'activePlotId');
    var viewerId = plotId ?  findViewerWithPlotId(getMultiViewRoot(), plotId) : (getAViewId());
    var plotMode;

    if (viewerId) {
        if (plotId) {
            plotMode = PlotSelectMode.ReplaceOrCreate;
        } else {
            plotMode = PlotSelectMode.AddNewPlot;
            plotId = `${ImageId}${imgNo}`;
            imgNo++;
        }
    } else {
        if (plotId) {
            plotMode = PlotSelectMode.Replace;
        } else {
            return null;
        }
    }
    // dispatchAddImages(plotView.viewerId, [plotId]);

    var popup = (<PopupPanel title={popTitle}>
                    <ImageSelection plotId={plotId} viewerId={viewerId} plotMode={plotMode} catalogId={TWOMASS}/>
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

/*
 * get the catalog id (tab id) based on stored fields of the tab
 *
 */

function computeCurrentCatalogId( fields, catalogId = IRSA ) {

    const keytab = keyMap['catalogtab'];

    if (fields && fields.hasOwnProperty(keytab)) {
        var tval = fields[keytab];
        return parseInt( typeof tval === 'object' ? tval.value : tval);
    }

    return catalogId;
}

/*
 *
 * image select pane initial state for all fields
 */
var ImageSelPanelChange = function (crtCatalogId) {
    return (inFields, action) => {
        var size = 'Size:';

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

        var getSize = (crtCatalogId) => {
            if (panelCatalogs[crtCatalogId].hasOwnProperty('size')) {
                return panelCatalogs[crtCatalogId]['size'].toString();
            } else {
                return '0.0';
            }
        };

        if (!inFields) {
             return {
                [keyMap['targettry']]: {
                    fieldKey: keyMap['targettry'],
                    value: 'NED',
                    label: ''
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
                    value: getSize(crtCatalogId)
                }
            };
        } else {

            if (action.type === FieldGroupCntlr.VALUE_CHANGE) {
                if (action.payload.fieldKey === keyMap['catalogtab']) {
                    var catalogId = parseInt(inFields[keyMap['catalogtab']].value);

                    if (catalogId !== URL && catalogId !== FITS) {
                        var radiusKey = keyMap['radiusfield'];
                        var radiusField = Object.assign({}, inFields[radiusKey],
                            {
                                unit: getRangeItem(catalogId, 'unit'),
                                min: getRangeItem(catalogId, 'min'),
                                max: getRangeItem(catalogId, 'max'),
                                value: getSize(catalogId)
                            });

                        if (radiusField.hasOwnProperty('displayValue')) {
                            radiusField = Object.assign({}, radiusField,
                                {displayValue: radiusField.value});
                        }

                        return Object.assign({}, inFields, {[radiusKey]: radiusField});
                    }
                }
            }

            return inFields;
        }
    };
};

const loadErrorMsg = {
    'nosize': 'no valid size is specified',
    'nopixelsize': 'no valid pixel size is specified',
    'notarget': 'no valid target name or position is specified',
    'nourl': 'no valid url of fits file is specified',
    'nofits': 'no fits file uploaded',
    'failplot': 'fail to make plot request'
};


var outputMessage = (errMsg) => errMsg&&showInfoPopup(errMsg, 'Load Selected Image Error');

// image plot on specified url
function imagePlotOnURL(request) {
    var url = get(request, keyMap['urlinput']);
    var wpr = WebPlotRequest.makeURLPlotRequest(url);


    if (wpr && request[keyMap['urllist']] === 'loadOne' && request.hasOwnProperty(keyMap['urlextinput'])) {
        wpr.setMultiImageIdx(request[keyMap['urlextinput']]);
    }
    return wpr;
}

// image plot on specified upload FITS
function imagePlotOnFITS(request) {
    var fits = get(request, keyMap['fitsupload']);
    var wpr = WebPlotRequest.makeFilePlotRequest(fits);


    if (wpr && request[keyMap['fitslist']] === 'loadOne' && request.hasOwnProperty(keyMap['fitsextinput'])) {
          wpr.setMultiImageIdx(request[keyMap['fitsextinput']]);
    }
    return wpr;
}



// image plot on blank image
function imagePlotOnBlank(request) {
    var sizeV = request[keyMap['blankinput']];
    var size = request[keyMap['sizefield']];
    var unit = request[keyMap['unitfield']];

    return WebPlotRequest.makeBlankPlotRequest(
        parseWorldPt(request.UserTargetWorldPt),
        convertAngle(unit, 'arcsec', size),
        sizeV, sizeV);
}

// image plot on IRSA, 2MASS, WISE, MSX, DSS, SDSS
function imagePlotOnSurvey(crtCatalogId, request) {

    var wp = parseWorldPt(request.UserTargetWorldPt);
    var sym = panelCatalogs[crtCatalogId].Symbol.toLowerCase();

    var t = `${sym}types`;
    var b = `${sym}bands`;
    var survey = get(request, keyMap[t]);
    var band = get(request, keyMap[b]);
    var sizeInDeg = parseFloat(request[keyMap['radiusfield']]);

    var wpr = null;

    switch(crtCatalogId) {
        case IRSA:
            if (survey.includes('issa')) {
                wpr = WebPlotRequest.makeISSARequest(wp, survey, sizeInDeg);
            } else if (survey.includes('iris')) {
                wpr = WebPlotRequest.makeIRISRequest(wp, survey, sizeInDeg);
            }

            break;

        case TWOMASS:
            wpr = WebPlotRequest.make2MASSRequest(wp, survey, sizeInDeg);
            break;

        case WISE:
            if (band) {
                wpr = WebPlotRequest.makeWiseRequest(wp, survey, band, sizeInDeg);
            }
            break;

        case MSX:
            wpr = WebPlotRequest.makeMSXRequest(wp, survey, sizeInDeg);
            break;

        case DSS:
            wpr = WebPlotRequest.makeDSSRequest(wp, survey, sizeInDeg);
            break;

        case SDSS:
            wpr = WebPlotRequest.makeSloanDSSRequest(wp, survey, sizeInDeg);
            break;
        default:
            break;
    }

    return wpr;
}

/*
 * onFail callback and blank input checker for request sent to onSuccess
 */
var resultFail = (request, fromFail = true)  => {
    var crtCatalogId = computeCurrentCatalogId(request);
    var errCode = '';
    var errMsg = '';
    const skey = 'radiusfield';

    switch (crtCatalogId) {
        case URL:
            if (fromFail) {
                errMsg = 'invalid url';
            } else {
                if (!get(request, keyMap['urlinput'])) {
                    errCode = 'nourl';
                }
            }
            break;

        case FITS:
            if (fromFail) {
                errMsg = 'invalid file upload';
            } else {
                if (!get(request, keyMap['fitsupload'])) {
                    errCode = 'nofits';
                }
            }
            break;

        case BLANK:
            if (fromFail) {
                errMsg = 'invalid name or position or invalid pixel size or invalid size';
            } else {
                if (!request.hasOwnProperty('UserTargetWorldPt') || !request.UserTargetWorldPt) {
                    errCode = 'notarget';
                } else if (!get(request, keyMap['blankinput'])) {
                    errCode = 'nopixelsize';
                } else if (!get(request, keyMap[skey])) {
                    errCode = 'nosize';
                }
            }
            break;

        default:
            if (fromFail) {
                errMsg = 'invalid name or position or invalid size';
            } else {
                if (!request.hasOwnProperty('UserTargetWorldPt') || !request.UserTargetWorldPt) {
                    errCode = 'notarget';
                } else if (!get(request, keyMap[skey])) {
                    errCode = 'nosize';
                }
            }
    }

    if (errCode) {
        errMsg = loadErrorMsg[errCode];
    }
    if (errMsg) {
        outputMessage(errMsg);
    }

    return errMsg;
};

/*
 * onSucess callback for 'load' button on image select panel
 */
function resultSuccess(plotId, isAddNewPlot, viewerId) {
    return (request) => {
        var wpr = null;
        const crtCatalogId = computeCurrentCatalogId(request);

        var errMsg = resultFail(request, false);
        if (errMsg) {
            return;
        }

        switch (crtCatalogId) {
            case URL:
                wpr = imagePlotOnURL(request);
                break;
            case FITS:
                wpr = imagePlotOnFITS(request);
                break;
            case BLANK:
                wpr = imagePlotOnBlank(request);
                break;
            default:
                wpr = imagePlotOnSurvey(crtCatalogId, request);
        }

        if (wpr) {
            if (isAddNewPlot && viewerId) {
                dispatchAddImages(viewerId, [plotId]);
            }
            dispatchPlotImage(plotId, wpr);
        } else {
            outputMessage('failplot');
        }
    };
}

/**
 *  container component for image select panel
 *
 */
class ImageSelection extends Component {
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
        var {plotId, viewerId, plotMode} = this.props;
        var params = Object.assign({}, this.state, {plotId, viewerId, plotMode});

        return <ImageSelectionView {...params}/>;

    }
}

ImageSelection.propTypes = {
    catalogId: PropTypes.number,
    plotId: PropTypes.string.isRequired,
    viewerId: PropTypes.string,
    plotMode: PropTypes.object
};


class ImageSelectionView extends Component {

    constructor(props) {
        super(props);

        this.state = {
            crtCatalogId: computeCurrentCatalogId(props.fields, props.initCatalogId),
            plotId: props.plotId,
            addPlot: (props.plotMode && props.plotMode === PlotSelectMode.AddNewPlot) ? true : false
        };

    }

    componentWillReceiveProps(nextProps) {
        this.setState ({ crtCatalogId: computeCurrentCatalogId(nextProps.fields, nextProps.initCatalogId) });
    }

    onSelect(index) {
        this.setState({ crtCatalogId: index});
    }

    onChangePlotId(ev) {
        if (get(ev, 'target.value') === 'create') {
            this.setState ({plotId: `${ImageId}${imgNo}`,
                              addPlot: true});
            imgNo++;
        }
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
                var catalog = panelCatalogs[this.state.crtCatalogId];

                return (
                    <SizeInputFields fieldKey={keyMap['radiusfield']} />
                );
            } else {
                return (<div></div>);
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
                                            plotMode={this.props.plotMode}
                                            changePlot={this.onChangePlotId.bind(this)} />
                    </div>
                    <div className={'section'}>
                        <FieldGroupTabs
                            onTabSelected={this.onSelect.bind(this)}
                            fieldKey={keyMap['catalogtab']} >
                            {categoryTabs}
                        </FieldGroupTabs>
                    </div>
                    <div className={'size'}>
                        {radius()}
                    </div>

                    <div className={'close'}>
                        <div className={'padding'}>
                            <CompleteButton
                                groupKey={panelKey}
                                onSuccess={resultSuccess(this.state.plotId,
                                                         this.state.addPlot,
                                                         this.props.viewerId)}
                                onFail={resultFail}
                                text={'Load'}
                            />
                        </div>
                        <div className={'padding'}>
                            <HelpIcon helpId={helpId}/>
                        </div>
                    </div>
                </div>
            </FieldGroup>
        );
    }
}

ImageSelectionView.propTypes={
    initCatalogId: PropTypes.number.isRequired,
    fields: PropTypes.object,
                        plotId: PropTypes.string.isRequired,
                        viewerId: PropTypes.string,
                        plotMode: PropTypes.object
};

/**
 * top row for target panel
 *
 * @returns {XML}
 * @constructor
 */


function TargetPanelSetView({currentCatalogIdx, changePlot, plotMode}) {

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
                    <RadioGroupInputFieldView
                        alignment={'horizontal'}
                        fieldKey={keyMap['plotmode']}
                        value={'replace'}
                        onChange={changePlot}
                        options={[
                                {label: 'Replace Image', value: 'replace'},
                                {label: 'Create New Plot', value: 'create'}
                                ]} />
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
    changePlot: PropTypes.func,
    plotMode: PropTypes.object
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
        var fval = fields.hasOwnProperty(keyMap[fkey]) ? fields[keyMap[fkey]].value : '';

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
