/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {flux} from '../../Firefly.js';
import FieldGroupToStoreMixin from '../../fieldGroup/FieldGroupToStoreMixin.js';
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
import InputGroup from '../../ui/InputGroup.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import TargetPanel from '../../ui/TargetPanel.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import CheckboxGroupInputField from '../../ui/CheckboxGroupInputField.jsx';
import panelCatalogs from './ImageSelectPanelProp.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {convertAngle} from '../VisUtil.js';

import './ImageSelectPanel.css';

const popupId = 'ImageSelectPopup';
const panelKey = 'SELECTIMAGEPANEL';

const rS = {  padding: 10 };

// class name for styling
const left = 'left';
const rightpadding = 'rightpadding';
const leftpadding = 'leftpadding';

const unitSign = { 'arcsec':'"', 'arcmin':'\'', 'degree': 'Deg' };

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
const [IRSA, TMASS, WISE] = [0, 1, 2];
const plotMap = {
    'IRSA':  plotIRSA,
    '2MASS': plot2MASS,
    'WISE': plotWISE
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
 *
 * image select pane initial state for all fields
 */
var ImageSelPanelReducer = function (inFields, action) {
    var size = 'Size:';


    if (!inFields) {
        return {
            [keyMap['targettry']]: {
                fieldKey: keyMap['targettry'],
                value:    'NED',
                label:    '',
                multiple: false
            },
            [keyMap['irsatypes']]: {
                fieldKey: keyMap['irsatypes'],
                label: panelCatalogs[IRSA].types.Title,
                value: panelCatalogs[IRSA].types.Default,
                multiple: false
            },
            [keyMap['2masstypes']]: {
                fieldKey: keyMap['2masstypes'],
                label: panelCatalogs[TMASS].types.Title,
                value: panelCatalogs[TMASS].types.Default,
                multiple: false
            },
            [keyMap['wisetypes']]: {
                fieldKey: keyMap['wisetypes'],
                label: panelCatalogs[WISE].types.Title,
                value: panelCatalogs[WISE].types.Default,
                multiple: false
            },
            [keyMap['wisebands']]: {
                fieldKey: keyMap['wisebands'],
                label: panelCatalogs[WISE].bands.Title,
                value: panelCatalogs[WISE].bands.Default,
                multiple: false
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
                label: '',
                multiple: false
            },
            [keyMap['colorfield']]: {
                fieldKey: keyMap['colorfield'],
                value: '_none_'
            },
            [keyMap['selTab']]: {
                fieldKey: keyMap['selTab'],
                value: 'IRSA'
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
         FieldGroupUtils.initFieldGroup(this.groupKey, ImageSelPanelReducer);
         this.state = {currentCatalogIdx: 0, currentUnit: 'arcsec'};
     }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.stateUpdate());
    }

    stateUpdate() {
        this.setState({fields: FieldGroupUtils.getResults(this.groupKey)});

        if (this.state.fields) {
            var crtCatalogId = computeCurrentCatalogId(Object.keys(this.state.fields));
            this.setState({currentCatalogIdx: crtCatalogId});
            this.setState({currentUnit: this.state.fields[keyMap['unitfield']]});
        }
    }


    render() {
         return <ImageSelectionView {...this.state}/>;
    }
}

/**
 *  presentation container for image select panel
 *  @param {number} currentCatalog
 *  @param {string} currentUnit
 */
class ImageSelectionView extends Component {

    constructor(props) {
        super(props);
        this.state = props;
    }

    changeCatalog(index) {
        this.setState({currentCatalog: index});
    }

    render() {

        // tabs for each catalog
        var categoryTabs = panelCatalogs.map((item, index) => {
            var title = item.Title;

            return (<Tab key={index} name={title.toUpperCase()}>
                        <CatalogSelectListsView catalog={item} />
                    </Tab>);
        });

        var crtCatalog = panelCatalogs[this.state.currentCatalogIdx];
        var helpId = 'basics.catalog';

        /*
         * top: target panel
         * middle: tab panel for catalogs
         * bottom: size panel
         *
         * Load button and help icon
         */
        return (
            <div style={rS} >
            <FieldGroup  groupKey={panelKey} reducerFunc={ImageSelPanelReducer} keepState={true}>
                <table className={'imagepanel'}>
                     <tbody>
                        <tr>
                            <td> <TargetPanelSetView /> </td>
                        </tr>
                        <tr>
                            <td>
                                <Tabs onTabSelect={this.changeCatalog.bind(this)} defaultSelected={2} >
                                    {categoryTabs}
                                </Tabs>
                            </td>
                        </tr>
                        <tr>
                            <td className={'sizerow'}>
                                <div className={'sizerow'}>
                                    <ImageFeaturesView currentCatalog={crtCatalog}
                                                       currentUnit={this.state.currentUnit}/>
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
     currentUnit: PropTypes.string
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
 * @param {object} currentCatalog
 * @param {string} currentUnit
 * @returns {XML}
 * @constructor
 */
function ImageFeaturesView ({currentCatalog, currentUnit}) {
    var {min, max, unit} = currentCatalog.range;
    var unitS = unitSign[currentUnit];
    var rangeMsg;

    min = convertAngle(unit, currentUnit, min);
    max = convertAngle(unit, currentUnit, max);
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
                                    [{label: 'Arc Seconds', value: 'arcsec'},
                                     {label: 'Arc Minutes', value: 'arcmin'},
                                     {label: 'Degree', value: 'deg'}
                                    ]
                                }
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
    currentCatalog: PropTypes.object.isRequired,
    currentUnit: PropTypes.string

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
                 label={Title}
                 labelWidth={computeLabelWidth(Title)}
             />
         </div>
    );
}

SelectionList.propTypes = {
    catalog: PropTypes.object.isRequired,
    method: PropTypes.string.isRequired
};

