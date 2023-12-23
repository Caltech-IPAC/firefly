import React, {useEffect} from 'react';
import {oneOfType, oneOf, element, bool, string, number, arrayOf, object, func, shape} from 'prop-types';
import CoordinateSys from '../../visualize/CoordSys.js';
import {CONE_AREA_OPTIONS, CONE_AREA_OPTIONS_UPLOAD, CONE_CHOICE_KEY, POLY_CHOICE_KEY, UPLOAD_CHOICE_KEY
} from '../../visualize/ui/CommonUIKeys.js';
import {HiPSTargetView} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../TargetPanel.jsx';
import {CONE_AREA_KEY} from './DynamicDef.js';
import {DEF_AREA_EXAMPLE, PolygonField} from './DynComponents.jsx';

import './EmbeddedPositionSearchPanel.css';
import {UploadTableSelector} from 'firefly/ui/UploadTableSelector';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';


/**
 * Create search panel with HiPS Viewer with an embedded target/area selection
 * All properties are optional
 * @param {Object} props
 * @param {String} [props.toolbarHelpId] - help id for the toolbar
 * @param {JSX.Element} [props.WrapperComponent] - A component to wrap all the widgets in the embedded UI
 * @param {String} [props.hipsUrl] - url for the hips url
 * @param [props.hipsFOVInDeg] - field of view of the initial HiPS display
 * @param {WorldPt} [props.initCenterPt] - center point of the initial HiPS display - string - 1.1;2.2;EQ_J2000
 * @param {Array.<{mocUrl:String, title:String}>} [props.mocList] - a list of MOCS to display, an array of MOC URLs
 * @param {String} [props.sRegion] - an sRegion to display
 * @param {String} [props.coordinateSys] - coordinate system of HiPS - must be 'EQ_J2000' or 'GALACTIC'
 * @param {String} [props.targetKey] - field group key for the target field
 * @param {String} [props.polygonKey] - field group key for the polygon field
 * @param {String} [props.sizeKey] - field group key for the size field
 * @param {String} [props.plotId] - plotId for the HiPS display
 * @param {String} [props.initSelectToggle]
 * @param {number} [props.minValue] - min value for the search area in degrees
 * @param {number} [props.maxValue] - max value for the search area in degrees
 * @param {number} [props.searchAreaInDeg] - default value for the search area in degrees
 * @param {Array.<String>} [props.targetPanelExampleRow1] - string for examples for target for row 1, 3 max in array
 * @param {Array.<String>} [props.targetPanelExampleRow2] - string for examples for target for row 2, 3 max in array
 * @param {Array.<String>} [props.polygonExampleRow1] - string for examples for polygon for row 1, 2 max in array
 * @param {Array.<String>} [props.polygonExampleRow2] - string for examples for polygon for row 2, 2 max in array
 * @param {Boolean} [props.nullAllowed] - null input is allowed
 * @param {Boolean} [props.insetSpacial] - true if spacial layout is inset
 * @param {JSX.Element} [props.otherComponents] - More component that can be added under target/polygon components
 * @param {Boolean} [props.usePosition] - true to use the target
 * @param {Boolean} [props.usePolygon] - true to use the polygon
 * @param {Boolean} [props.useUpload] - true to allow uploads
 * @param {Object} [props.searchItem] - used for URL API (similar to SearchPanel)
 * @param {Object} [props.initArgs] - used for URL API (similar to SearchPanel)
 * @return {JSX.Element|boolean}
 * @constructor
 */
export function EmbeddedPositionSearchPanel({
                                                toolbarHelpId= undefined,
                                                WrapperComponent,
                                                hipsUrl= 'ivo://CDS/P/DSS2/color',
                                                hipsFOVInDeg = 30,
                                                initCenterPt= undefined,
                                                mocList= undefined,
                                                sRegion= undefined,
                                                coordinateSys: csysStr = 'EQ_J2000',
                                                targetKey= DEF_TARGET_PANEL_KEY,
                                                polygonKey= 'Polygon',
                                                sizeKey= 'radius',
                                                initSelectToggle= CONE_AREA_KEY,
                                                plotId= 'defaultHiPSTargetSearch',
                                                minValue= 1 / 3600,
                                                maxValue= 1,
                                                searchAreaInDeg= .005,
                                                targetPanelExampleRow1,
                                                targetPanelExampleRow2,
                                                polygonExampleRow1=DEF_AREA_EXAMPLE,
                                                polygonExampleRow2,
                                                nullAllowed= false,
                                                insetSpacial=true,
                                                otherComponents= undefined,
                                                usePosition= true,
                                                useUpload = false,
                                                usePolygon= true,
                                                searchItem,
                                                initArgs
                                            }
) {

    const [getConeAreaOp, setConeAreaOp] = useFieldGroupValue(CONE_AREA_KEY);
    const [getUploadInfo, setUploadInfo]= useFieldGroupValue('uploadInfo');
    const uploadInfo= getUploadInfo() || undefined;

    //conditionally show UploadTableChooser only when uploadInfo is empty - TAP like behavior
    useEffect(() => {
        if (doGetConeAreaOp() === UPLOAD_CHOICE_KEY) {
            if (!uploadInfo.columns) showUploadTableChooser(setUploadInfo);
            else setUploadInfo(uploadInfo);
        }
    }, [uploadInfo]);

    const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;
    if (!usePolygon && !usePosition && !useUpload) return false;
    const doToggle= usePosition && usePolygon;
    const initToggle= initSelectToggle;


    const doGetConeAreaOp= () => {
        if (doToggle) return getConeAreaOp() ?? initToggle;
        if (usePolygon) return POLY_CHOICE_KEY;
        if (useUpload) return UPLOAD_CHOICE_KEY;
        return CONE_CHOICE_KEY;
    };

    const internals= (
        <>
            {!insetSpacial && <div style={{paddingTop:10}}/>}
            {doToggle && <RadioGroupInputField {...{
                inline: true, fieldKey: CONE_AREA_KEY, wrapperStyle: {paddingBottom: 5, paddingTop: 5},
                tooltip: 'Chose type of search', initialState: {value: initToggle}, options: useUpload ? CONE_AREA_OPTIONS_UPLOAD : CONE_AREA_OPTIONS
            }} />}
            {doGetConeAreaOp() === CONE_CHOICE_KEY &&
                <div style={{paddingTop:5}}>
                    <TargetPanel {...{
                        fieldKey:targetKey, labelWidth:60, nullAllowed,
                        inputStyle:{width: 225},
                        targetPanelExampleRow1, targetPanelExampleRow2
                    }}/>
                    <SizeInputFields {...{
                        style:{paddingBottom:10},
                        fieldKey: sizeKey, showFeedback: true, labelWidth: 100, nullAllowed: false,
                        labelStyle:{textAlign:'right', paddingRight:4},
                        label: 'Search Radius:',
                        initialState: {unit: 'arcsec', value: searchAreaInDeg + '', min:minValue, max:maxValue}
                    }} />
                </div>
            }
            {doGetConeAreaOp() === POLY_CHOICE_KEY &&
                <PolygonField {...{
                    style: {paddingTop:5},
                    hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey,
                    targetDetails: {targetPanelExampleRow1: polygonExampleRow1, targetPanelExampleRow2:polygonExampleRow2},
                    desc: 'Coordinates',
                    manageHiPS:false,
                }} />}
            {otherComponents && otherComponents}

            {doGetConeAreaOp() === UPLOAD_CHOICE_KEY &&
                <UploadTableSelector {...{uploadInfo, setUploadInfo, uploadTable:true}}/>
            }
        </>
    );

    const additionalProps= {searchItem, initArgs};
    const wrappedInternals = WrapperComponent
        ? <WrapperComponent {...additionalProps}>{internals}</WrapperComponent>
        : internals;

    return (
        <div key='targetGroup'
             style={{display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch', height:'100%',
                 paddingBottom:insetSpacial?0:20, position: 'relative'}}>
            <HiPSTargetView {...{
                hipsUrl, centerPt:initCenterPt, hipsFOVInDeg, mocList, coordinateSys, sRegion, plotId,
                minSize: minValue, maxSize: maxValue, toolbarHelpId,
                whichOverlay: doGetConeAreaOp(), setWhichOverlay: doToggle ? setConeAreaOp : undefined,
                targetKey, sizeKey, polygonKey, style: {minHeight: 300, alignSelf: 'stretch', flexGrow:1}
            }}/>
            <div className={`FFepsp-content ${insetSpacial ? 'inset' : ''}`}
                style={{display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch'}}>
                {wrappedInternals}
            </div>
        </div>
    );
}

EmbeddedPositionSearchPanel.propTypes= {
    toolbarHelpId: string,
    WrapperComponent: oneOfType([func,element]),
    hipsUrl: string,
    hipsFOVInDeg: number,
    initCenterPt: object,
    mocList: arrayOf(shape( { mocUrl: string, title: string} )),
    sRegion: string,
    coordinateSys: oneOf(['EQ_J2000','GALACTIC']),
    targetKey: string,
    polygonKey: string,
    sizeKey: string,
    initSelectToggle: string,
    plotId: string,
    minValue: number,
    maxValue: number,
    searchAreaInDeg: number,
    targetPanelExampleRow1: arrayOf(string),
    targetPanelExampleRow2: arrayOf(string),
    polygonExampleRow1: arrayOf(string),
    polygonExampleRow2: arrayOf(string),
    nullAllowed: bool,
    insetSpacial: bool,
    otherComponents: oneOfType([func,element]),
    usePosition: bool,
    usePolygon: bool,
    useUpload: bool,
    searchItem: object,
    initArgs: object
};
