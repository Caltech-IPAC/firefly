/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Stack, Typography} from '@mui/joy';
import {isArray, isString} from 'lodash';
import React, {useEffect} from 'react';
import {CoordinateSys} from '../../api/ApiUtilImage.jsx';
import {floatRange, intRange} from '../../util/Validate.js';
import {getFormattedWaveLengthUnits} from '../../visualize/PlotViewUtil.js';
import {parseWorldPt} from '../../visualize/Point.js';
import {CONE_AREA_OPTIONS, CONE_CHOICE_KEY, POLY_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {
    hideHiPSPopupPanel, HiPSTargetView, TargetHiPSRadiusPopupPanel, VisualPolygonPanel, VisualTargetPanel
} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {convertStrToWpAry} from '../../visualize/ui/VisualSearchUtils.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FormPanel} from '../FormPanel';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {showInfoPopup} from '../PopupUtil';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {Slot, useFieldGroupValue} from '../SimpleComponent.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {ObsCoreSearch} from '../tap/ObsCore';
import {ObsCoreWavelengthSearch} from '../tap/WavelengthPanel';
import {TargetPanel} from '../TargetPanel.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {
    AREA, CHECKBOX, CIRCLE, CONE_AREA_KEY, ENUM, FLOAT, INT, POINT, POLYGON, POSITION, SIA_OBSCORE_OPS, UNKNOWN, UPLOAD,
    WAVELENGTH
} from './DynamicDef.js';
import {EmbeddedPositionSearchPanel} from './EmbeddedPositionSearchPanel.jsx';
import {findFieldDefType, hasType} from './ServiceDefTools.js';

const DEF_LABEL_WIDTH = 100;
export const DEF_AREA_EXAMPLE = 'Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5';
const BULLET = String.fromCharCode(0x2013) + '  ';

export const makeUnitsStr = (units) => !units ? '' : ` (${getFormattedWaveLengthUnits(units)})`;

export function hasValidSpacialSearch(request, fieldDefAry) {
    if (!request[CONE_AREA_KEY] || request[CONE_AREA_KEY]===CONE_CHOICE_KEY) {
        const posType = findFieldDefType(fieldDefAry, POSITION) ?? findFieldDefType(fieldDefAry, CIRCLE);
        if (!posType || posType.nullAllowed) return true;
        return Boolean(parseWorldPt(request[posType.targetDetails.targetKey]));
    }
    else if (request[CONE_AREA_KEY]===POLY_CHOICE_KEY) {
        const polygonType = findFieldDefType(fieldDefAry, POLYGON);
        if (!polygonType || polygonType.nullAllowed) return true;
        return convertStrToWpAry(request[polygonType.targetDetails.polygonKey]).length>2;
    }
}

export function getSpacialSearchType(request, fieldDefAry) {
    if (request[CONE_AREA_KEY]) {
        return request[CONE_AREA_KEY];
    }
    else if (findFieldDefType(fieldDefAry, POSITION) || findFieldDefType(fieldDefAry, CIRCLE)){
        return CONE_CHOICE_KEY;
    }
    else if (findFieldDefType(fieldDefAry, POLYGON)) {
        return POLY_CHOICE_KEY;
    }
}



/**
 * @param params
 * @param params.fieldDefAry
 * @param params.noLabels
 * @param params.popupHiPS
 * @param params.popupHiPS
 * @param params.toolbarHelpId
 * @param params.plotId
 * @param params.insetSpacial
 * @returns {{}}
 */
export function makeAllFields({fieldDefAry:inFieldDefAry, noLabels=false, popupHiPS, toolbarHelpId,
                                  plotId='defaultHiPSTargetSearch', insetSpacial=false, submitSearch} )  {

    const fieldDefAry= inFieldDefAry.filter( ({hide}) => !hide);
    const {hasCircle, hasPointAndArea, hasSpacialAndArea}= getFieldSpacialInfo(fieldDefAry);
    const spacialManagesArea= hasSpacialAndArea && countFieldDefType(fieldDefAry,AREA)===1;

    let dynSpacialPanel= undefined;
    let SiaWLPanel= undefined;
    let SiaObsCorePanel = undefined;
    if (hasCircle || hasSpacialAndArea || hasPointAndArea) {
        dynSpacialPanel= makeDynPanel({fieldDefAry, popupHiPS, plotId, insetSpacial, submitSearch, toolbarHelpId});
    }
    if (hasType(fieldDefAry,WAVELENGTH)) {
       SiaWLPanel= ({dataServiceId}) => <ObsCoreWavelengthSearch {...{serviceId:dataServiceId,useSIAv2:true}}/>;
    }


    if (hasType(fieldDefAry,SIA_OBSCORE_OPS)) {
        const obsDef= findFieldDefType(fieldDefAry,SIA_OBSCORE_OPS);
        SiaObsCorePanel = ({dataServiceId,obsCoreMetadataModel}) =>
            <ObsCoreSearch {...{obsCoreMetadataModel,serviceId:dataServiceId,useSIAv2:true, ...obsDef}}/>;
    }

    const panels = {
        DynSpacialPanel: dynSpacialPanel,
        areaFields: spacialManagesArea ? [] : makeAllAreaFields(fieldDefAry),
        checkBoxFields: makeCheckboxFields(fieldDefAry),
        SiaWLPanel,
        SiaObsCorePanel,
        ...makeInputFields(fieldDefAry, noLabels)
    };
    panels.useSpacial = Boolean(dynSpacialPanel);
    return panels;
}


function makeDynPanel({fieldDefAry, popupHiPS, plotId='defaultHiPSTargetSearch', insetSpacial=false, submitSearch, toolbarHelpId}) {
    const {targetDetails, manageAllSpacial}= getFieldSpacialInfo(fieldDefAry);
    const areaType = findFieldDefType(fieldDefAry, AREA);
    const circleType = findFieldDefType(fieldDefAry, CIRCLE);
    const sizeKey = areaType?.key ?? circleType?.targetDetails.sizeKey;

    if (sizeKey && manageAllSpacial && targetDetails?.hipsUrl) {
        return popupHiPS ?
            makeCircleAndPolyPopup({fieldDefAry, plotId, toolbarHelpId}) : // popup not used anymore
            makeEmbeddedPositionSearchPanelWrapper({fieldDefAry, plotId,toolbarHelpId, insetSpacial, submitSearch}); // normal case
    }
    else { // fallback - rarely used
        return targetDetails?.hipsUrl ?
            makeDynSpacialPanelFallback({fieldDefAry, popupHiPS, plotId, toolbarHelpId}) :
            makeSimpleTarget(fieldDefAry); // simple target: usually means something not set up right
    }
}

export function isSimpleTargetPanel(fieldDefAry) {
    const {targetDetails, manageAllSpacial}= getFieldSpacialInfo(fieldDefAry);
    const areaType = findFieldDefType(fieldDefAry, AREA);
    const circleType = findFieldDefType(fieldDefAry, CIRCLE);
    const sizeKey = areaType?.key ?? circleType?.targetDetails.sizeKey;
    if (sizeKey && manageAllSpacial && targetDetails?.hipsUrl) {
        return false;
    }
    return !Boolean(targetDetails?.hipsUrl);
}


function getFieldSpacialInfo(fieldDefAry) {
    const hasPoly= hasType(fieldDefAry,POLYGON);
    const hasPoint= hasType(fieldDefAry,POINT);
    const hasArea= hasType(fieldDefAry,AREA);
    const hasCircle= hasType(fieldDefAry,CIRCLE);
    const hasPointAndArea= hasPoint && hasArea;
    const manageAllSpacial= (hasCircle || hasPointAndArea);

    const targetDetails= getTargetDetails(fieldDefAry);
    return {
        hasPoly, hasPoint, hasArea, hasCircle, targetDetails,
        hasPointAndArea, manageAllSpacial,
        hasSpacialAndArea: (hasPoint || hasType(fieldDefAry,POSITION)) && hasArea
    };
}

function getTargetDetails(fieldDefAry)  {
    const hasPoint= hasType(fieldDefAry,POINT);
    const hasArea= hasType(fieldDefAry,AREA);
    const hasPointAndArea= hasPoint && hasArea;
    const hasCircle= hasType(fieldDefAry,CIRCLE);
    const manageAllSpacial= (hasCircle || hasPointAndArea);
    const posType = findFieldDefType(fieldDefAry, POINT) ?? findFieldDefType(fieldDefAry, POSITION) ;
    const circleType = findFieldDefType(fieldDefAry, CIRCLE);
    const polyType= manageAllSpacial && findFieldDefType(fieldDefAry, POLYGON);
    if (!posType && !circleType) return <div/>;
    let {targetDetails} = posType ?? circleType;
    const { hipsUrl} = targetDetails ?? polyType?.targetDetails ?? {};
    if (!targetDetails.hipsUrl && hipsUrl) targetDetails= {...targetDetails, hipsUrl};
    return targetDetails;
}





const countFieldDefType= (fieldDefAry, type) => fieldDefAry.filter( (entry) => entry.type===type).length;

function makeInputFields(fieldDefAry, noLabels) {
    const noLabelOp= noLabels ? {desc:''} : {};
    const fieldsInputAry= fieldDefAry
        .filter( ({type}) => type===FLOAT || type===INT || type===UNKNOWN)
        .map( (param) => makeValidationField({...param, ...noLabelOp}) );

    const opsInputAry= fieldDefAry
        .filter( ({type}) => type===ENUM)
        .map( (param) => makeListBoxField({...param, ...noLabelOp}) );


    return {fieldsInputAry, opsInputAry};
}

function CircleAndPolyFieldPopup({fieldDefAry, typeForCircle= CIRCLE, plotId='defaultHiPSTargetSearch', toolbarHelpId}) {
    const polyType = findFieldDefType(fieldDefAry, POLYGON);
    const cirType = findFieldDefType(fieldDefAry, typeForCircle);
    const [getConeAreaOp] = useFieldGroupValue(CONE_AREA_KEY);

    useEffect(() => {
        return () => {
            hideHiPSPopupPanel();
        };
    }, []);


    if (!polyType && !cirType) return false;
    const usingToggle = Boolean(cirType && polyType);

    const getChoice = () => {
        if (usingToggle) return getConeAreaOp();
        return cirType ? CONE_CHOICE_KEY : POLY_CHOICE_KEY;
    };

    const sizeType= (cirType && typeForCircle!==CIRCLE) ? findFieldDefType(fieldDefAry, AREA) : undefined;

    const polygonKey= polyType?.targetDetails.polygonKey;
    const targetKey= typeForCircle===CIRCLE ? cirType.targetDetails.targetKey : cirType?.targetDetails.targetKey;
    const sizeKey= sizeType?.key || cirType.targetDetails.sizeKey;
    const initValue= sizeType?.initValue ?? cirType.initValue;
    const minValue= sizeType?.minValue ?? cirType.minValue;
    const maxValue= sizeType?.maxValue ?? cirType.maxValue;


    const taToggle = usingToggle &&
        (<RadioGroupInputField {...{
            inline: true, fieldKey: CONE_AREA_KEY, sx: {pb: 1},
            tooltip: 'Chose type of search', initialState: {value: CONE_CHOICE_KEY}, options: CONE_AREA_OPTIONS
        }} />);

    return (
        <Stack {...{alignItems: 'center', minWidth: cirType ? 650 : 450}}>
            {taToggle}
            {getChoice() === CONE_CHOICE_KEY &&
                <CircleField {...{
                    hideHiPSPopupPanelOnDismount: false, fieldKey: targetKey, ...cirType, plotId,
                    targetKey, sizeKey, polygonKey, initValue, minValue, maxValue, toolbarHelpId
                }}/>}
            {getChoice() === POLY_CHOICE_KEY &&
                <PolygonField {...{
                    hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey, ...polyType,
                    desc: 'Coordinates',
                    targetKey, sizeKey, polygonKey,
                }} />}
        </Stack>
    );
}


export function EmbeddedPositionSearchPanelWrapper({fieldDefAry, plotId, toolbarHelpId, insetSpacial,
                                       slotProps={}, children}) {

    const polyType = findFieldDefType(fieldDefAry, POLYGON);
    const posType = findFieldDefType(fieldDefAry, POINT) ?? findFieldDefType(fieldDefAry, POSITION);
    const areaType = findFieldDefType(fieldDefAry, AREA);
    const circleType= findFieldDefType(fieldDefAry, CIRCLE);
    const {targetDetails, nullAllowed = false} = posType ?? circleType ?? {};

    const {
        hipsUrl, centerPt, hipsFOVInDeg = 240, coordinateSys: csysStr = 'EQ_J2000', mocList, sRegion,
        targetPanelExampleRow1, targetPanelExampleRow2} = targetDetails ?? polyType?.targetDetails ?? {};

    let { minValue = 1 / 3600, maxValue = 1 } = areaType ?? circleType ?? {} ;// eslint-disable-line prefer-const
    if (!minValue) minValue= 1/3600;

    const searchAreaInDeg= areaType?.initValue ?? circleType?.initValue ?? .005;
    const sizeKey= areaType?.key ?? circleType?.targetDetails.sizeKey;
    const targetKey= posType?.targetDetails.targetKey ?? circleType.targetDetails.targetKey;
    const polygonKey= polyType?.targetDetails.polygonKey;

    if (!targetKey && !polygonKey) return false;
    if (targetKey && !sizeKey) return false;
    if (!targetKey && sizeKey) return false;

    const usePosition= Boolean(posType||circleType);
    const usePolygon= Boolean(polyType);
    const initToggle= polyType?.initValue  ? POLY_CHOICE_KEY : CONE_CHOICE_KEY;
    const initCenterPt= initToggle===POLY_CHOICE_KEY  ? polyType?.targetDetails?.centerPt : centerPt;

    return (
        <EmbeddedPositionSearchPanel {...{
            initSelectToggle: initToggle,
            usePosition,
            usePolygon,
            targetPanelExampleRow1, targetPanelExampleRow2,
            nullAllowed,
            insetSpacial,
            slotProps: {
                hipsTargetView: {
                    mocList, sRegion, plotId, toolbarHelpId, hipsUrl, hipsFOVInDeg, initCenterPt,
                    coordinateSys: csysStr,
                },
                targetPanel: { targetKey, targetPanelExampleRow1, targetPanelExampleRow2, },
                sizeInput: { ...slotProps.sizeInput, min:minValue,max:maxValue,sizeKey, initValue: searchAreaInDeg},
                polygonField: {
                    polygonKey,
                    polygonExampleRow1:polyType?.targetDetails?.targetPanelExampleRow1,
                    polygonExampleRow2:polyType?.targetDetails?.targetPanelExampleRow2,
                },
                formPanel: slotProps.FormPanel,
                spatialSearch: slotProps.spatialSearch,
            }
            }}>
            {children}
        </EmbeddedPositionSearchPanel>

    );

}

function makeAllAreaFields(fieldDefAry) {
    return fieldDefAry.filter( ({type}) => type===AREA).map( (param) => makeAreaField(param));
}

function makeAreaField({ key, desc = 'Search Radius', initValue, tooltip, nullAllowed = false,
                           minValue = 1 / 3600, maxValue = 1}) {
    return (
        <SizeInputFields {...{
            key, fieldKey: key, showFeedback: true, nullAllowed, label: desc, tooltip: tooltip ?? desc,
            initialState: {unit: 'arcsec', value: initValue + '', min: minValue, max: maxValue}
        }}/>
    );
}

function makeValidationField({ key, type, desc, tooltip = '', initValue, minValue, maxValue, units = '',
                                        precision = 2}) {
    const unitsStr = desc ? makeUnitsStr(units): '';
    const label= desc ?  desc + unitsStr : '';
    const validator =
        type === FLOAT ?
            (value, nullAllowed) => floatRange(minValue, maxValue, precision, desc, value, nullAllowed) :
            type === INT ?
                (value, nullAllowed) => intRange(minValue, maxValue, desc, value, nullAllowed) :
                undefined;
    return (
        <ValidationField {...{
            key,
            fieldKey: key, tooltip: tooltip + unitsStr, label,
            initialState: {
                value: initValue ?? (type !== UNKNOWN ? minValue ?? 0 : ''),
                validator,
            }
        }}/>
    );
}

function makeCheckboxFields(fieldDefAry) {
    const cbFields = fieldDefAry.filter(({type}) => type === CHECKBOX);
    if (!cbFields?.length) return null;
    const cbFieldKey = cbFields.reduce((s, {key}) => s + key, '');
    const options = cbFields.map(({desc, key: value, units}) => ({label: desc + makeUnitsStr(units), value}));
    const value = cbFields.reduce((s, {key, initValue}) => initValue ? s ? s + ',' + key : key : s, '');

    return (
        <CheckboxGroupInputField {...{
            key: cbFieldKey, fieldKey: cbFieldKey, label: '',
            alignment: 'vertical', options, initialState: {value}, sx: {mr: 2, py:1/2, }
        }}
        />
    );
}

function makeListBoxField({ key, desc, tooltip = '', initValue, enumValues, units}) {
    const unitsStr = desc ? makeUnitsStr(units) : '';
    const label= desc ?  desc + unitsStr : '';
    return (
        <ListBoxInputField {...{
            key, fieldKey: key, label:label+':', tooltip: tooltip + unitsStr,
            initialState: {value: initValue},
            options: enumValues.map((e) => isString(e) ? {label: e, value: e} : e),
            sx: {mr: 2, py:1/2, },
        }} />
    );
}

function CircleField({ fieldKey, desc, tooltip = '', initValue, minValue, maxValue, targetKey, sizeKey, polygonKey,
                         sx={}, label='Coords or Object:', targetDetails, ...restOfProps }) {
    return (
        <Stack {...{key:fieldKey, alignSelf:'start', ...sx}}>
            <Typography title={tooltip} sx={{alignSelf: 'start', pb: 1}}>{desc}</Typography>
            <TargetHiPSRadiusPopupPanel {...{
                sx: {textAlign:'right'},
                key: fieldKey, searchAreaInDeg: initValue, label,
                minValue, maxValue, ...targetDetails, ...restOfProps,
                targetKey:targetKey??targetDetails.targetKey,
                sizeKey:sizeKey??targetDetails.sizeKey,
                sizeLabel:'Search Radius:',
                polygonKey:polygonKey??targetDetails.polygonKey,
            }} />
        </Stack>
    );
}


export function PolygonField({ fieldKey, desc = 'Coordinates', initValue = '', style={},
                          tooltip = 'Enter polygon coordinates search',
                          targetDetails: {targetPanelExampleRow1 = DEF_AREA_EXAMPLE, targetPanelExampleRow2, sRegion}, ...restOfProps }) {

    const helpRow2= isArray(targetPanelExampleRow2) && targetPanelExampleRow2.length ?
        targetPanelExampleRow2[0] : isString(targetPanelExampleRow2) ?
            targetPanelExampleRow2 : undefined;

    const help = [
        'Each vertex is defined by a J2000 RA and Dec position pair',
        '3 to 15 vertices is allowed, optionally separated by a comma (,)',
        isArray(targetPanelExampleRow1) ? targetPanelExampleRow1[0] :targetPanelExampleRow1,
        helpRow2
    ];
    return (
        <div key={fieldKey} style={style}>
            <VisualPolygonPanel {...{
                fieldKey,
                style: {width: 350, maxWidth: 420},
                initValue,
                placeholder: desc,
                placeholderHighlight: true,
                labelStyle: {},
                tooltip,
                sRegion,
                ...restOfProps
            }}
            />
            <ul style={{marginTop: 7}}>
                {help.filter((h) => h).map((h) => (
                    <Typography key={h} level='body-xs'>
                        <li key={h} style={{listStyleType: `'${BULLET}'`}}>{h}</li>
                    </Typography>
                ))}
            </ul>
        </div>
    );
}


function makeEmbeddedPositionSearchPanelWrapper({fieldDefAry,
                             plotId= 'defaultHiPSTargetSearch', toolbarHelpId, insetSpacial, submitSearch}) {

    const DynSpacialPanel= ({slotProps, children}) => {
        return (
            <EmbeddedPositionSearchPanelWrapper {...{fieldDefAry, plotId, insetSpacial, slotProps, toolbarHelpId, submitSearch}}>
                {children}
            </EmbeddedPositionSearchPanelWrapper>
        );
    };
    return DynSpacialPanel;
}

function makeCircleAndPolyPopup({fieldDefAry, plotId= 'defaultHiPSTargetSearch', toolbarHelpId}) {
    const circleType = findFieldDefType(fieldDefAry, CIRCLE);
    const DynSpacialPanel= () => {
        return (
            <CircleAndPolyFieldPopup {...{fieldDefAry,typeForCircle:circleType?CIRCLE:POSITION, plotId, toolbarHelpId}}/>
        );
    };
    return DynSpacialPanel;
}

function makeSimpleTarget(fieldDefAry) {
    const posType = findFieldDefType(fieldDefAry, POINT) ??
        findFieldDefType(fieldDefAry, POSITION) ??
        findFieldDefType(fieldDefAry, CIRCLE);
    const {nullAllowed = false} = posType;
    const {targetPanelExampleRow1, targetPanelExampleRow2}=  getTargetDetails(fieldDefAry) ?? {};
    const DynSpacialPanel= ({slotProps={}}) => {
        return (
            <Slot {...{ component: FormPanel,
                slotProps: slotProps?.FormPanel, help_id: 'dynDefaultSearchPanelHelp',
                onError:() => showInfoPopup('Fix errors and search again', 'Error'),
                cancelText:'', completeText:'Submit'}} >
                <TargetPanel {...{nullAllowed, targetPanelExampleRow1, targetPanelExampleRow2}}/>
            </Slot>
            );
    };
    return DynSpacialPanel;

}



function makeDynSpacialPanelFallback({fieldDefAry, popupHiPS= false, plotId= 'defaultHiPSTargetSearch', toolbarHelpId}) {
    const DynSpacialPanel= () => {
        const {targetDetails, manageAllSpacial}= getFieldSpacialInfo(fieldDefAry);
        const posType = findFieldDefType(fieldDefAry, POINT) ?? findFieldDefType(fieldDefAry, POSITION) ;
        const areaType = findFieldDefType(fieldDefAry, AREA);
        const circleType = findFieldDefType(fieldDefAry, CIRCLE);
        const polyType= manageAllSpacial && findFieldDefType(fieldDefAry, POLYGON);
        if (!posType && !circleType) return <div/>;
        const {nullAllowed = false, minValue, maxValue} = posType ?? circleType;

        const {
            hipsUrl, centerPt, hipsFOVInDeg = 240, coordinateSys: csysStr = 'EQ_J2000', mocList,
            targetPanelExampleRow1, targetPanelExampleRow2
        } = targetDetails ?? polyType?.targetDetails ?? {};
        const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;
        const sizeKey = areaType?.key ?? circleType?.targetDetails.sizeKey;

        if (popupHiPS) {
            return (<VisualTargetPanel {...{
                hipsUrl, centerPt, hipsFOVInDeg, mocList, nullAllowed, coordinateSys, sizeKey, plotId,
                minSize: minValue, maxSize: maxValue, toolbarHelpId,
                targetPanelExampleRow1, targetPanelExampleRow2 }} />);
        }
        else {
            return (
                <Stack key='targetGroup' {...{alignItems: 'center', alignSelf: 'stretch', height:1}}>
                    <HiPSTargetView {...{
                        hipsUrl, centerPt, hipsFOVInDeg, mocList, coordinateSys,
                        minSize: minValue, maxSize: maxValue,
                        plotId, toolbarHelpId,
                        targetKey: 'UserTargetWorldPt', sizeKey, sx: {flexGrow:1, alignSelf: 'stretch'}
                    }}/>
                    <div style={{display: 'flex', flexDirection: 'column'}}>
                        <TargetPanel {...{
                            sx: {pt: 1}, key: 'targetPanel', nullAllowed,
                            targetPanelExampleRow1, targetPanelExampleRow2
                        }}/>
                    </div>
                </Stack> );
        }
    };
    return DynSpacialPanel;
}

