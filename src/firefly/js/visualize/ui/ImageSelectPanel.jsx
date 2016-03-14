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
import AppDataCntlr from '../../core/AppDataCntlr.js';
import ListBoxInputField from '../../ui/ListBoxInputField.jsx';
import FieldGroup from '../../ui/FieldGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import TargetPanel from '../../ui/TargetPanel.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import CheckboxGroupInputField from '../../ui/CheckboxGroupInputField.jsx';
import panelCatalogs from './ImageSelectPanelProp.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {convertAngle} from '../VisUtil.js';
import {isEmpty} from 'lodash';

import './ImageSelectPanel.css';

const popupId = 'ImageSelectPopup';
const panelKey = 'SELECTIMAGEPANEL';

// class name for styling
const left = 'left';
const rightpadding = 'rightpadding';
const leftpadding = 'leftpadding';

const unitSign = { 'arcsec':'"', 'arcmin':'\'', 'deg':' Deg' };

const keyMap = {
    'selTab':   'SELECTIMAGEPANEL_SelectedCatalog',
    'targettry': 'SELECTIMAGEPANEL_targettry',
    'irsatypes': 'SELECTIMAGEPANEL_IRSASelectLists_types',
    '2masstypes': 'SELECTIMAGEPANEL_2MASSSelectLists_types',
    'wisetypes': 'SELECTIMAGEPANEL_WISESelectLists_types',
    'wisebands': 'SELECTIMAGEPANEL_WISESelectLists_bands',
    'sizefield': 'SELECTIMAGEPANEL_ImgFeature_size',
    'unitfield': 'SELECTIMAGEPANEL_ImgFeature_unit',
    'colorfield': 'SELECTIMAGEPANEL_ImgFeature_3color'
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
                    <ImageSelection />
                 </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    AppDataCntlr.showDialog(popupId);
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
                value: '18000',
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
 */

function computeCurrentCatalogId(fieldsAry) {
    var selectLists = fieldsAry.filter((item) => item.includes('SelectList'));

    if (selectLists.length > 0) {
        var selCatalog = panelCatalogs.filter((catalog) => selectLists[0].includes(catalog.Title));

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
function plotIRSA(request) {
    var worldPt = request.UserTargetWorldPt;
    var survey = request[keyMap['irsatypes']];
    var sizeInDeg = convertAngle(request[keyMap['unitfield']], 'deg',
                                 request[keyMap['sizefield']]);
    //var colorImg = request['colorfield'].value;

    var wp = parseWorldPt(worldPt);

    var wpr;

    if (survey.includes('ISSA')) {
        wpr = WebPlotRequest.makeISSARequest(wp, survey, sizeInDeg);
    } else {
        wpr = WebPlotRequest.makeIRISRequest(wp, survey, sizeInDeg);
    }
    wpr.setInitialZoomLevel(1);
    wpr.setInitialColorTable(4); //??
    dispatchPlotImage('irsaImage', wpr);
}

/*
 * image plot for 2Mass
 */
function plot2MASS(request) {
    var worldPt = request.UserTargetWorldPt;
    var survey = request[keyMap['2masstypes']];
    var sizeInDeg = convertAngle(request[keyMap['unitfield']], 'deg',
                                 request[keyMap['sizefield']]);
    //var colorImg = request['colorfield'].value;

    var wp = parseWorldPt(worldPt);
    var wpr = WebPlotRequest.makeIRISRequest(wp, survey, sizeInDeg);

    wpr.setInitialZoomLevel(1);
    wpr.setInitialColorTable(4);
    dispatchPlotImage('twomassImage', wpr);
}

/*
 * image plot for wise
 */
function plotWISE(request) {
    var worldPt = request.UserTargetWorldPt;
    var survey = request[keyMap['wisetypes']];
    var band = request[keyMap['wisebands']];
    var sizeInDeg = convertAngle(request[keyMap['unitfield']], 'deg',
                                 request[keyMap['sizefield']]);
    //var colorImg = request['colorfield'].value;

    var wp = parseWorldPt(worldPt);
    var wpr = WebPlotRequest.makeWiseRequest(wp, survey, band, sizeInDeg);

    wpr.setPlotGroupId('test-wise');
    //wpr.setInitialZoomLevel(1);
    //wpr.setInitialColorTable(4); //??
    dispatchPlotImage('wiseImage', wpr);
}

/*
 * callback for 'load' button on image select panel
 */
function showResults(success, request) {
    if (!success) {
        return;
    }

    var crtCatalogId = computeCurrentCatalogId(Object.keys(request));

    plotMap[panelCatalogs[crtCatalogId].Title](request);
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
         if (!FieldGroupUtils.getGroupFields(this.groupKey)) {
             FieldGroupUtils.initFieldGroup(this.groupKey, ImageSelPanelChange, true);
         }
         this.state = {currentCatalogIdx: 0,
                       fields: FieldGroupUtils.getResults(this.groupKey)};
     }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.stateUpdate());
    }

    stateUpdate() {

        var fields = FieldGroupUtils.getResults(this.groupKey);

        if (fields) {
            var crtCatalogId = computeCurrentCatalogId(Object.keys(fields));
            this.setState({currentCatalogIdx: crtCatalogId, fields});
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
        var categoryTabs = panelCatalogs.map((item, index) => {
            var title = item.Title;

            return (<Tab key={index} name={title.toUpperCase()}>
                        <CatalogSelectListsView catalog={item} />
                    </Tab>);
        });

        var imageFeaturesRender = () => {

            if (this.state.fields) {
                return (<ImageFeaturesView fields = {this.state.fields}
                                          currentCatalogIdx={this.state.currentCatalogIdx} />);
            } else {
                return <ImageFeaturesView currentCatalogIdx={this.state.currentCatalogIdx} />;
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
            <div >
            <FieldGroup  groupKey={panelKey} reducerFunc={ImageSelPanelChange} keepState={true}>
                <table className={'imagepanel'}>
                     <tbody>
                        <tr>
                            <td> <TargetPanelSetView /> </td>
                        </tr>
                        <tr>
                            <td>
                                <Tabs onTabSelect={this.changeCatalog.bind(this)} defaultSelected={0} >
                                    {categoryTabs}
                                </Tabs>
                            </td>
                        </tr>
                        <tr>
                            <td className={'sizerow'}>
                                <div className={'sizerow'}>
                                    <ImageFeaturesView {...this.state} />
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td className={'okrow'}>
                                <div className={rightpadding}>
                                    <HelpIcon helpId={helpId}/>
                                </div>
                                <div className={rightpadding} >
                                    <CompleteButton
                                        groupKey={panelKey}
                                        onSuccess={resultSuccess}
                                        text={'Load'}
                                        dialogId={popupId}
                                    />
                                </div>
                             </td>
                        </tr>
                    </tbody>
                </table>
            </FieldGroup>
            </div>
        );
     }
}


ImageSelectionView.propTypes={
     currentCatalogIdx: PropTypes.number.isRequired,
     fields: PropTypes.object
};

/**
 * component for top target panel
 *
 * @returns {XML}
 * @constructor
 */

function TargetPanelSetView() {
     return (
        <div>
            <div className={leftpadding}>
                <TargetPanel groupKey={panelKey}/>
            </div>
            <div className={leftpadding}>
                <ListBoxInputField
                        fieldKey={keyMap['targettry']}
                        options={
                            [{label: 'Try NED then Simbad', value: 'NED'},
                             {label: 'Try Simbad then NED', value: 'simbad'}
                            ]
                        }
                        multiple={false}
                        labelWidth={3}
                    />
            </div>
        </div>
    );
}

TargetPanelSetView.propTypes= {};


/**
 * bottom image size, size unit and color selection
 *
 * @param {number} currentCatalogIDX
 * @param {object} fields
 * @returns {XML}
 * @constructor
 */
function ImageFeaturesView ({currentCatalogIdx, fields}) {
    var {min, max, unit} = panelCatalogs[currentCatalogIdx].range;
    var currentUnit = (!isEmpty(fields))&&fields[keyMap['unitfield']]?
                                                fields[keyMap['unitfield']] : 'arcsec';
    var unitS = unitSign[currentUnit];
    var rangeMsg;


    min = toMaxFixed(convertAngle(unit, currentUnit, min), 4);
    max = toMaxFixed(convertAngle(unit, currentUnit, max), 4);
    rangeMsg = `Valid range between: ${min}${unitS} and ${max}${unitS}`;


    return (
            <div>
                <div>
                    <div className={leftpadding}>
                        <div className={left}>
                            <ValidationField fieldKey={keyMap['sizefield'] } />
                        </div>
                        <div className={left}>
                            <ListBoxInputField
                                fieldKey={keyMap['unitfield']}
                                options={
                                    [{label: 'Degree', value: 'deg'},
                                     {label: 'Arc Minutes', value: 'arcmin'},
                                     {label: 'Arc Seconds', value: 'arcsec'}
                                    ]
                                }
                                multiple={false}
                                labelWidth={3}
                             />
                        </div>
                    </div>
                    <div className={'color'} >
                        <CheckboxGroupInputField
                           fieldKey={keyMap['colorfield']}
                           options={[
                                {label: '3-color Image', value: '3color'}
                            ]}
                        />
                    </div>
                </div>
                <br/><br/>
                <p>{rangeMsg}</p>
            </div>
    );
}

ImageFeaturesView.propsTypes={
    currentCatalogIdx: PropTypes.number.isRequired,
    fields: PropTypes.object

};

/**
 * component inside each catalog tab
 *
 * @param catalog
 * @returns {XML}
 * @constructor
 */
function CatalogSelectListsView({catalog}) {
    return (
        <div>
            <SelectionList catalog={catalog} method={'types'} />
            <SelectionList catalog={catalog} method={'bands'} />
        </div>
    );
}

CatalogSelectListsView.propTypes={
    catalog: PropTypes.object.isRequired
};


function SelectionList({catalog, method}) {
    if (!catalog.hasOwnProperty(method)) {
        return <div></div>;
    }

    var listItems = catalog[method].Items.map((item) => ({'label': item.name, 'value': item.item}));
    var fkey = `${catalog.Title.toLowerCase()}${method}`;
    var {Title} = catalog[method];

    return (
         <div className={'cataloglist'}>
             <ListBoxInputField
                 fieldKey={keyMap[fkey]}
                 options={listItems}
                 labelWidth={computeLabelWidth(Title)}
                 multiple={false}
             />
         </div>
    );
}

SelectionList.propTypes = {
    catalog: PropTypes.object.isRequired,
    method: PropTypes.string.isRequired
};
