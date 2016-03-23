/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {flux} from '../../Firefly.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchPlotImage} from '../ImagePlotCntlr.js';
import {parseWorldPt} from '../Point.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchShowDialog} from '../../core/DialogCntlr.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {panelCatalogs} from './ImageSelectPanelProp.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {convertAngle} from '../VisUtil.js';
import {isEmpty} from 'lodash';

import './ImageSelectPanel.css';

const popupId = 'ImageSelectPopup';
const panelKey = 'SELECTIMAGEPANEL';

const unitSign = { 'arcsec':'"', 'arcmin':'\'', 'deg':' Deg' };

const keyMap = {
    'selTab':    'SELECTIMAGEPANEL_SelectedCatalog',
    'targettry': 'SELECTIMAGEPANEL_targettry',
    'irsatypes': 'SELECTIMAGEPANEL_IRSASelectLists_types',
    '2masstypes':'SELECTIMAGEPANEL_2MASSSelectLists_types',
    'wisetypes': 'SELECTIMAGEPANEL_WISESelectLists_types',
    'wisebands': 'SELECTIMAGEPANEL_WISESelectLists_bands',
    'sizefield': 'SELECTIMAGEPANEL_ImgFeature_size',
    'unitfield': 'SELECTIMAGEPANEL_ImgFeature_unit',
    'colorfield':'SELECTIMAGEPANEL_ImgFeature_3color'
};
const [IRSA, TWOMASS, WISE] = [0, 1, 2];
const plotMap = {
    'IRSA':  plotIRSA,
    '2MASS': plot2MASS,
    'WISE':  plotWISE
};

/**
 * show image select popup window
 * @param {string} popTitle
 */

export function showImageSelPanel(popTitle)
{
    var popup = (<PopupPanel title={popTitle}>
                    <ImageSelection/>
                 </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

/*
 * compute the space for string to be displayed
 */

function computeLabelWidth(labelStr) {
    return labelStr.length * 7;
}

/*
 * remove trailing zero from toFixed result
 */
function toMaxFixed(floatNum, digits) {
    return parseFloat(floatNum.toFixed(digits));
}

/*
 *
 * image select pane initial state for all fields
 */
var ImageSelPanelChange = function (inFields, action) {
    var size = 'Size:';

    if (!inFields) {
        return {
            [keyMap['targettry']]: {
                fieldKey: keyMap['targettry'],
                value:    'NED',
                label:    ''
            },
            [keyMap['irsatypes']]: {
                fieldKey: keyMap['irsatypes'],
                label: panelCatalogs[IRSA].types.Title,
                value: panelCatalogs[IRSA].types.Default,
                labelWidth: computeLabelWidth(panelCatalogs[IRSA].types.Title)
            },
            [keyMap['2masstypes']]: {
                fieldKey: keyMap['2masstypes'],
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
            [keyMap['sizefield']]: {
                fieldKey: keyMap['sizefield'],
                value: '500',
                label: size,
                labelWidth: computeLabelWidth(size)
            },
            [keyMap['unitfield']]: {
                fieldKey: keyMap['unitfield'],
                value: 'arcsec',
                label: ''
            },
            [keyMap['colorfield']]: {
                fieldKey: keyMap['colorfield'],
                value: '_none_'
            }
          };
    } else {
        return inFields;
    }
};


/*
 * get the catalog id (tab id) based on stored fields of the tab
 * TODO: temporary solution to find the tab selected
 */

function computeCurrentCatalogId(fieldsAry) {
    var selectLists = fieldsAry.filter((item) => item.includes('SelectList'));

    if (selectLists.length > 0) {
        var selCatalog = panelCatalogs.filter((catalog) => selectLists[0].includes(catalog.Symbol));

        if (selCatalog.length > 0) {
            return selCatalog[0].CatalogId;
        }
    }
}

/***********************************
 * image plots for all catalogs
 ***********************************/

/*
 * plot for IRSA
 */
function plotIRSA(request, plotId) {
    var worldPt = request.UserTargetWorldPt;
    var survey = request[keyMap['irsatypes']];
    var sizeInDeg = convertAngle(request[keyMap['unitfield']], 'deg',
                                 request[keyMap['sizefield']]);

    var wp = parseWorldPt(worldPt);

    var wpr;

    if (survey.includes('ISSA')) {
        wpr = WebPlotRequest.makeISSARequest(wp, survey, sizeInDeg);
    } else {
        wpr = WebPlotRequest.makeIRISRequest(wp, survey, sizeInDeg);
    }
    wpr.setPlotGroupId('irsa-group');
    wpr.setInitialZoomLevel(1);

    dispatchPlotImage(plotId, wpr);
}

/*
 * image plot for 2Mass
 */
function plot2MASS(request, plotId) {
    var worldPt = request.UserTargetWorldPt;
    var survey = request[keyMap['2masstypes']];
    var sizeInDeg = convertAngle(request[keyMap['unitfield']], 'deg',
                                 request[keyMap['sizefield']]);

    var wp = parseWorldPt(worldPt);
    var wpr = WebPlotRequest.make2MASSRequest(wp, survey, sizeInDeg);

    wpr.setPlotGroupId('twomass-group');
    wpr.setInitialZoomLevel(1);

    dispatchPlotImage(plotId, wpr);
}

/*
 * image plot for wise
 */
function plotWISE(request, plotId) {
    var worldPt = request.UserTargetWorldPt;
    var survey = request[keyMap['wisetypes']];
    var band = request[keyMap['wisebands']];
    var sizeInDeg = convertAngle(request[keyMap['unitfield']], 'deg',
                                 request[keyMap['sizefield']]);
    //var colorImg = request['colorfield'].value;

    var wp = parseWorldPt(worldPt);
    var wpr = WebPlotRequest.makeWiseRequest(wp, survey, band, sizeInDeg);

    wpr.setPlotGroupId('wise-group');
    wpr.setInitialZoomLevel(1);

    dispatchPlotImage(plotId, wpr);
}

/*
 * callback for 'load' button on image select panel
 */
function showResults(success, request) {
    if (!success) {
        return;
    }

    var crtCatalogId = computeCurrentCatalogId(Object.keys(request));

    plotMap[panelCatalogs[crtCatalogId].Symbol](request, 'TestImage1');
}

function resultSuccess(request) {
    showResults(true, request);
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
         this.state = {currentCatalogIdx: 0 };
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

        var fields = getFieldGroupResults(this.groupKey);

        if (fields) {
            var crtCatalogId = computeCurrentCatalogId(Object.keys(fields));
            if (this.iAmMounted) {
                this.setState({currentCatalogIdx: crtCatalogId});
            }
        }
    }


    render() {
        return <ImageSelectionView {...this.state}/>;

    }
}

/**
 *  presentation container for image select panel
 *  @param {number} currentCatalogIdx
 */
class ImageSelectionView extends Component {

    constructor(props) {
        super(props);
        this.state = props;
    }

    componentWillReceiveProps(props) {
        this.state = props;
    }

    changeCatalog(index) {
        this.setState({currentCatalogIdx: index});
    }

    render() {

        // tabs for each catalog
        var categoryTabs = panelCatalogs.map((item, index) =>
             (<Tab key={index} name={item.Title}>
                  <CatalogTabView catalog={item} />
             </Tab>)
        );

        var helpId = 'basics.catalog';

        /*
         * top: target panel
         * middle: tab panel for catalogs
         * bottom: size panel
         *
         * Load button and help icon
         */
        return (
            <FieldGroup  groupKey={panelKey} reducerFunc={ImageSelPanelChange} keepState={true}>
                <div className={'imagepanel'}>
                    <div className={'section'}>
                        <TargetPanelSetView />
                    </div>
                    <div className={'section'}>
                        <Tabs onTabSelect={this.changeCatalog.bind(this)} defaultSelected={1} >
                            {categoryTabs}
                        </Tabs>
                    </div>
                    <div className={'size'}>
                        <ImageFeaturesView {...this.state} />
                    </div>

                    <div className={'close'}>
                       <div className={'padding'}>
                           <CompleteButton
                                groupKey={panelKey}
                                onSuccess={resultSuccess}
                                text={'Load'}
                                dialogId={popupId}
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
     currentCatalogIdx: PropTypes.number.isRequired,
     fields: PropTypes.object
};

/**
 * top row for target panel
 *
 * @returns {XML}
 * @constructor
 */

function TargetPanelSetView() {
     return (
        <div className={'intarget'}>
            <TargetPanel groupKey={panelKey}/>
            <ListBoxInputField
                fieldKey={keyMap['targettry']}
                options = {[{label: 'Try NED then Simbad', value: 'NED'},
                           {label: 'Try Simbad then NED', value: 'simbad'}
                          ]}
                multiple={false}
                labelWidth={3}
            />
        </div>
    );
}

TargetPanelSetView.propTypes= {};


/**
 * bottom row for image size, size unit and color selection
 *
 * @param {number} currentCatalogIdx
 * @param {object} fields
 * @returns {XML}
 * @constructor
 */
function ImageFeaturesView ({currentCatalogIdx, fields}) {

    var showSize = () => {
        var {min, max, unit} = panelCatalogs[currentCatalogIdx].range;
        var currentUnit = (!isEmpty(fields)) && fields[keyMap['unitfield']] ?
            fields[keyMap['unitfield']] : 'arcsec';
        var unitS = unitSign[currentUnit];
        var rangeMsg;

        min = toMaxFixed(convertAngle(unit, currentUnit, min), 4);
        max = toMaxFixed(convertAngle(unit, currentUnit, max), 4);
        rangeMsg = `Valid range between: ${min}${unitS} and ${max}${unitS}`;
        return (
            <div className={'sizeline'}>
                <div className={'sizeinput'}>
                    <ValidationField fieldKey={keyMap['sizefield']}/>
                    <ListBoxInputField
                       fieldKey={keyMap['unitfield']}
                       options={
                          [{label: 'Degree', value: 'deg'},
                           {label: 'Arc Minutes', value: 'arcmin'},
                           {label: 'Arc Seconds', value: 'arcsec'}
                           ]}
                       multiple={false}
                       labelWidth={2}
                    />
                </div>
                <p>{rangeMsg}</p>
            </div>
        );
    };

    return (
            <div>
                {showSize()}
            </div>
    );
}

ImageFeaturesView.propsTypes={
    currentCatalogIdx: PropTypes.number.isRequired,
    fields: PropTypes.object

};

/**
 * component inside each catalog tab at middle row
 *
 * @param catalog
 * @returns {XML}
 * @constructor
 */
function CatalogTabView({catalog}) {
    var listfield = (fieldname) => {
        var fkey = `${catalog.Symbol.toLowerCase()}${fieldname}`;
        var listItems = catalog[fieldname].Items.map((item) => (({'label': item.name, 'value': item.item})));

        return (
            <div className={'cataloglist'}>
                <ListBoxInputField
                    fieldKey={keyMap[fkey]}
                    options={listItems}
                    labelWidth={computeLabelWidth(catalog[fieldname].Title)}
                    multiple={false}
                />
            </div>
        );
    };

    var inputfield = (fieldname) => {
        var fkey = `${catalog.Symbol.toLowerCase()}${fieldname}`;

        return (
            <div className={'padding'}>
                <ValidationField fieldKey={keyMap[fkey]}/>
                <br/>
            </div>
        );
    };

    var fieldrequest = (fieldname) => {
        if (fieldname.includes('types') || fieldname.includes('bands') ||
            fieldname.includes('list')) {
            return listfield(fieldname);
        }  else if (fieldname.includes('input')) {
            return inputfield(fieldname);
        }
    };

    return (
        <div className={'tabview'}>
            {(catalog.fields).map((oneField) =>  fieldrequest(oneField))}
        </div>
    );
}

CatalogTabView.propTypes={
    catalog: PropTypes.object.isRequired
};
