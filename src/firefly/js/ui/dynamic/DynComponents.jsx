/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {TargetPanel} from '../TargetPanel.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {AREA, CHECKBOX, CIRCLE, CONE_AREA_KEY, ENUM, FLOAT, INT, POLYGON, POSITION, UNKNOWN} from './DynamicDef.js';
import {findFieldDefType} from './ServiceDefTools.js';

const DEF_LABEL_WIDTH = 100;
const DEF_AREA_EXAMPLE = 'Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5';
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
 *
 * @param params
 * @param params.fieldDefAry
 * @param params.noLabels
 * @param params.popupHiPS
 * @param params.plotId
 * @param params.insetSpacial
 * @returns {{}}
 */
export function makeAllFields({fieldDefAry, noLabels=false, popupHiPS, toolbarHelpId,
                                  plotId='defaultHiPSTargetSearch', insetSpacial=false} )  {

    // polygon is not created directly, we need to determine who will create creat a polygon field if it exist
    const workingFieldDefAry= fieldDefAry.filter( ({hide}) => !hide);
    const hasPoly = Boolean(findFieldDefType(workingFieldDefAry,POLYGON));
    const circle= findFieldDefType(workingFieldDefAry,CIRCLE);
    const hasCircle= Boolean(circle);
    const hasSpacialAndArea= Boolean(findFieldDefType(workingFieldDefAry,POSITION) && findFieldDefType(workingFieldDefAry,AREA));
    const spacialManagesArea= hasSpacialAndArea && countFieldDefType(workingFieldDefAry,AREA)===1;

    let dynSpacialPanel= undefined;
    if (hasCircle || hasSpacialAndArea || hasCircle) {
        dynSpacialPanel= popupHiPS ?
                        makeDynSpacialPanel({fieldDefAry:workingFieldDefAry, popupHiPS, plotId, toolbarHelpId}) :
                        makeDynSpacialPanel({fieldDefAry:workingFieldDefAry, manageAllSpacial:hasCircle && hasPoly, popupHiPS,
                            plotId,toolbarHelpId, insetSpacial});
    }

    const panels = {
        DynSpacialPanel: dynSpacialPanel,
        areaFields: spacialManagesArea ? [] : makeAllAreaFields(workingFieldDefAry),
        checkBoxFields: makeCheckboxFields(workingFieldDefAry),
        ...makeInputFields(workingFieldDefAry, noLabels)
    };
    panels.useArea = Boolean(panels.areaFields.length);
    panels.useSpacial = Boolean(dynSpacialPanel);
    return panels;
};

const countFieldDefType= (fieldDefAry, type) => fieldDefAry.filter( (entry) => entry.type===type).length;

function makeInputFields(fieldDefAry, noLabels) {
    const noLabelOp= noLabels ? {labelWidth:0,desc:''} : {};
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
            inline: true, fieldKey: CONE_AREA_KEY, wrapperStyle: {paddingBottom: 10},
            tooltip: 'Chose type of search', initialState: {value: CONE_CHOICE_KEY}, options: CONE_AREA_OPTIONS
        }} />);

    return (
        <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: cirType ? 650 : 450}}>
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
        </div>
    );
}

function PositionAndPolyFieldEmbed({fieldDefAry, plotId, toolbarHelpId, insetSpacial,
                                       otherComponents, WrapperComponent}) {

    const polyType = findFieldDefType(fieldDefAry, POLYGON);
    const posType = findFieldDefType(fieldDefAry, POSITION);
    const areaType = findFieldDefType(fieldDefAry, AREA);
    const circleType= findFieldDefType(fieldDefAry, CIRCLE);
    const {targetDetails, nullAllowed = false} = posType ?? circleType ?? {};
    const [getConeAreaOp, setConeAreaOp] = useFieldGroupValue(CONE_AREA_KEY);



    const {
        hipsUrl, centerPt, hipsFOVInDeg = 240, coordinateSys: csysStr = 'EQ_J2000', mocList, sRegion,
        targetPanelExampleRow1, targetPanelExampleRow2} = targetDetails ?? polyType?.targetDetails ?? {};
    const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;

    let { minValue = 1 / 3600, maxValue = 1 } = areaType ?? circleType ?? {} ;// eslint-disable-line prefer-const
    if (!minValue) minValue= 1/3600;

    const searchAreaInDeg= areaType?.initValue ?? circleType?.initValue ?? .005;
    const sizeKey= areaType?.key ?? circleType?.targetDetails.sizeKey;
    const targetKey= posType?.targetDetails.targetKey ?? circleType.targetDetails.targetKey;
    const polygonKey= polyType?.targetDetails.polygonKey;

    if (!targetKey && !polygonKey) return false;
    if (targetKey && !sizeKey) return false;
    if (!targetKey && sizeKey) return false;

    const doToggle= polyType && (posType||circleType);
    const initToggle= polyType?.initValue  ? POLY_CHOICE_KEY : CONE_CHOICE_KEY;
    const initCenterPt= initToggle===POLY_CHOICE_KEY  ? polyType?.targetDetails?.centerPt : centerPt;


    const doGetConeAreaOp= () => {
        if (doToggle) return getConeAreaOp() ?? initToggle;
        if (polyType) return POLY_CHOICE_KEY;
        return CONE_CHOICE_KEY;
    };

    const insetStyle= insetSpacial ? {
        borderRadius: '5px 5px 2px 2px',
        background: 'white',
        border: '3px solid rgba(0,0,0,.3)',
        alignSelf: 'auto',
        position:'absolute',
        padding: '0 4px 0 4px',
        bottom: 30,
        left: 7
    } : {};

    const internals= (
        <>
            {!insetSpacial && <div style={{paddingTop:10}}/>}
            {doToggle && <RadioGroupInputField {...{
                inline: true, fieldKey: CONE_AREA_KEY, wrapperStyle: {paddingBottom: 5, paddingTop: 5},
                tooltip: 'Chose type of search', initialState: {value: initToggle}, options: CONE_AREA_OPTIONS
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
                    hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey, ...polyType,
                    desc: 'Coordinates',
                    targetKey, sizeKey, manageHiPS:false,
                }} />}
            {otherComponents && otherComponents}
        </>
    );

    const wrappedInternals= WrapperComponent ? <WrapperComponent>{internals}</WrapperComponent> : internals;

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
            <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch', ...insetStyle}}>
                {wrappedInternals}
            </div>
        </div>
    );
}

function makeAllAreaFields(fieldDefAry) {
    return fieldDefAry.filter( ({type}) => type===AREA).map( (param) => makeAreaField(param));
}

function makeAreaField({ key, desc = 'Search Radius', initValue, tooltip, nullAllowed = false,
                           minValue = 1 / 3600, maxValue = 1, labelWidth = DEF_LABEL_WIDTH }) {
    return (
        <SizeInputFields {...{
            key, fieldKey: key, showFeedback: true, labelWidth, nullAllowed, label: desc, tooltip: tooltip ?? desc,
            labelStyle:{textAlign:'right', paddingRight:4},
            initialState: {unit: 'arcsec', value: initValue + '', min: minValue, max: maxValue}
        }}/>
    );
}

function makeValidationField({ key, type, desc, tooltip = '', initValue, minValue, maxValue, units = '',
                                        precision = 2, labelWidth = DEF_LABEL_WIDTH }) {
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
            fieldKey: key, tooltip: tooltip + unitsStr, label:label+':', labelWidth,
            labelStyle:{textAlign:'right', paddingRight:4},
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
            key: cbFieldKey, fieldKey: cbFieldKey, label: '', labelWidth: 0,
            alignment: 'vertical', options,
            initialState: {value},
            wrapperStyle: {marginRight: '15px', padding: '3px 0 5px 0'}
        }}
        />
    );
}

function makeListBoxField({ key, desc, tooltip = '', initValue, enumValues, units, labelWidth = DEF_LABEL_WIDTH }) {
    const unitsStr = desc ? makeUnitsStr(units) : '';
    const label= desc ?  desc + unitsStr : '';
    return (
        <ListBoxInputField {...{
            key, fieldKey: key, label:label+':', tooltip: tooltip + unitsStr, labelWidth,
            initialState: {value: initValue},
            options: enumValues.map((e) => isString(e) ? {label: e, value: e} : e),
            labelStyle:{textAlign:'right', paddingRight:4},
            wrapperStyle: {marginRight: '15px', padding: '3px 0 5px 0'},
            multiple: false
        }} />
    );
}

function CircleField({ fieldKey, desc, tooltip = '', initValue, minValue, maxValue, targetKey, sizeKey, polygonKey,
                         style={}, labelWidth = DEF_LABEL_WIDTH, label='Coords or Object:', targetDetails, ...restOfProps }) {
    return (
        <div key={fieldKey} style={{display: 'flex', alignSelf:'start', flexDirection:'column', ...style}}>
            <div title={tooltip} style={{width: labelWidth, alignSelf: 'start', paddingBottom: 10}}>{desc}</div>
            <TargetHiPSRadiusPopupPanel {...{
                style: {marginLeft: 0, textAlign:'right'},
                key: fieldKey, searchAreaInDeg: initValue, labelWidth, label,
                minValue, maxValue, ...targetDetails, ...restOfProps,
                targetLabelStyle:{paddingRight:4},
                targetKey:targetKey??targetDetails.targetKey,
                sizeKey:sizeKey??targetDetails.sizeKey,
                sizeLabel:'Search Radius:',
                polygonKey:polygonKey??targetDetails.polygonKey,
                sizeFeedbackStyle:{textAlign:'center', marginLeft:0}
            }} />
        </div>
    );
}


function PolygonField({ fieldKey, desc = 'Coordinates', initValue = '', style={},
                          labelWidth = DEF_LABEL_WIDTH, tooltip = 'Enter polygon coordinates search',
                          targetDetails: {targetPanelExampleRow1 = DEF_AREA_EXAMPLE, targetPanelExampleRow2, sRegion}, ...restOfProps }) {

    const helpRow2= isArray(targetPanelExampleRow2) && targetPanelExampleRow2.length ?
        targetPanelExampleRow2[0] : isString(targetPanelExampleRow2) ?
            targetPanelExampleRow2 : undefined;

    const help = [
        'Each vertex is defined by a J2000 RA and Dec position pair',
        '3 to 15 vertices is allowed, separated by a comma (,)',
        isArray(targetPanelExampleRow1) ? targetPanelExampleRow1[0] :targetPanelExampleRow1,
        helpRow2
    ];
    return (
        <div key={fieldKey} style={style}>
            <VisualPolygonPanel {...{
                fieldKey,
                wrapperStyle: {display: 'flex', alignItems: 'center'},
                style: {width: 350, maxWidth: 420},
                initValue,
                label: desc,
                labelStyle: {},
                labelWidth,
                tooltip,
                sRegion,
                ...restOfProps
            }}
            />
            <ul style={{marginTop: 7}}>
                {help.filter((h) => h).map((h) => <li key={h} style={{paddingBottom: 2, listStyleType: `'${BULLET}'`}}>{h}</li>)}
            </ul>
        </div>
    );
}

function makeDynSpacialPanel({fieldDefAry, manageAllSpacial= true, popupHiPS= false,
                             plotId= 'defaultHiPSTargetSearch', toolbarHelpId, insetSpacial}) {
    const DynSpacialPanel= ({otherComponents, WrapperComponent}) => {
        const posType = findFieldDefType(fieldDefAry, POSITION);
        const areaType = findFieldDefType(fieldDefAry, AREA);
        const circleType = findFieldDefType(fieldDefAry, CIRCLE);
        const polyType= manageAllSpacial && findFieldDefType(fieldDefAry, POLYGON);
        if (!posType && !circleType) return <div/>;
        const {targetDetails, nullAllowed = false, minValue, maxValue} = posType ?? circleType;
        const {labelWidth = DEF_LABEL_WIDTH} = posType ?? circleType;

        const {
            hipsUrl, centerPt, hipsFOVInDeg = 240, coordinateSys: csysStr = 'EQ_J2000', mocList,
            targetPanelExampleRow1, targetPanelExampleRow2
        } = targetDetails ?? polyType?.targetDetails ?? {};
        const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;
        const sizeKey = areaType?.key ?? circleType?.targetDetails.sizeKey;

        if (!hipsUrl && !targetDetails) {
            return <TargetPanel {...{labelWidth, nullAllowed, targetPanelExampleRow1, targetPanelExampleRow2}}/>;
        }

        if (manageAllSpacial && sizeKey) {
            return popupHiPS ?
                <CircleAndPolyFieldPopup {...{fieldDefAry,typeForCircle:circleType?CIRCLE:POSITION, plotId, toolbarHelpId}}/> :
                <PositionAndPolyFieldEmbed {...{fieldDefAry, plotId, insetSpacial, otherComponents, WrapperComponent, toolbarHelpId}}/>;
        }
        else {
            if (popupHiPS) {
                return (<VisualTargetPanel {...{
                    hipsUrl, centerPt, hipsFOVInDeg, mocList, nullAllowed, coordinateSys, sizeKey, plotId,
                    minSize: minValue, maxSize: maxValue, toolbarHelpId,
                    targetPanelExampleRow1, targetPanelExampleRow2 }} />);
            }
            else {
                return (
                    <div key='targetGroup'
                         style={{display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch', height:'100%'}}>
                        <HiPSTargetView {...{
                            hipsUrl, centerPt, hipsFOVInDeg, mocList, coordinateSys,
                            minSize: minValue, maxSize: maxValue,
                            plotId, toolbarHelpId,
                            targetKey: 'UserTargetWorldPt', sizeKey, style: {flexGrow:1, alignSelf: 'stretch'}
                        }}/>
                        <div style={{display: 'flex', flexDirection: 'column'}}>
                            <TargetPanel {...{
                                style: {paddingTop: 10}, key: 'targetPanel', labelWidth, nullAllowed,
                                targetPanelExampleRow1, targetPanelExampleRow2
                            }}/>
                        </div>
                    </div> );
            }
        }
    };
    return DynSpacialPanel;
}

