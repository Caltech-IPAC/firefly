/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect} from 'react';
import {isString} from 'lodash';
import {CoordinateSys} from '../../api/ApiUtilImage.jsx';
import {floatRange, intRange} from '../../util/Validate.js';
import {getFormattedWaveLengthUnits} from '../../visualize/PlotViewUtil.js';
import {parseWorldPt} from '../../visualize/Point.js';
import {CONE_AREA_OPTIONS, CONE_CHOICE_KEY, POLY_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {
    hideHiPSPopupPanel, HiPSTargetView, TargetHiPSRadiusPopupPanel,
    VisualPolygonPanel, VisualTargetPanel
} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {convertStrToWpAry} from '../../visualize/ui/VisualSearchUtils.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {TargetPanel} from '../TargetPanel.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {AREA, CHECKBOX, CIRCLE, ENUM, FLOAT, INT, POLYGON, POSITION, UNKNOWN} from './DynamicDef.js';

const DEF_LABEL_WIDTH = 100;
const CONE_AREA_KEY = 'CONE_AREA_KEY_RESERVED';
const DEF_AREA_EXAMPLE = 'Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5';
const BULLET = String.fromCharCode(0x2013) + '  ';

export const makeUnitsStr = (units) => !units ? '' : ` (${getFormattedWaveLengthUnits(units)})`;

export function hasValidSpacialSearch(request, fieldDefAry) {
    if (!request[CONE_AREA_KEY] || request[CONE_AREA_KEY]===CONE_CHOICE_KEY) {
        const posType = findType(fieldDefAry, POSITION) ?? findType(fieldDefAry, CIRCLE);
        if (!posType || posType.nullAllowed) return true;
        return Boolean(parseWorldPt(request[posType.targetDetails.targetKey]));
    }
    else if (request[CONE_AREA_KEY]===POLY_CHOICE_KEY) {
        const polygonType = findType(fieldDefAry, POLYGON);
        if (!polygonType || polygonType.nullAllowed) return true;
        return convertStrToWpAry(request[polygonType.targetDetails.polygonKey]).length>2;
    }
}

export function getSpacialSearchType(request, fieldDefAry) {
    if (request[CONE_AREA_KEY]) {
        return request[CONE_AREA_KEY];
    }
    else if (findType(fieldDefAry, POSITION) || findType(fieldDefAry, CIRCLE)){
        return CONE_CHOICE_KEY;
    }
    else if (findType(fieldDefAry, POLYGON)) {
        return POLY_CHOICE_KEY;
    }
}



/**
 * @param fieldDefAry
 * @param noLabels
 * @return {{opsInputAry, fieldsInputAry, checkBoxFields, polyPanel, spacialPanel, areaFields, fieldsInputAry, opsInputAry, useArea, useSpacial, useCirclePolyField}}
 */
export const makeAllFields = (fieldDefAry, noLabels=false) => {

       // polygon is not created directly, we need to determine who will create creat a polygon field if it exist
    const hasPoly = Boolean(findType(fieldDefAry,POLYGON));
    const hasCircle= Boolean(findType(fieldDefAry,CIRCLE));
    const hasSpacialAndArea= Boolean(findType(fieldDefAry,POSITION) && findType(fieldDefAry,AREA));
    const spacialCreatesPoly= !hasCircle && hasSpacialAndArea && hasPoly;
       // spacial should manage area fields if there is only one
    const spacialManagesArea= hasSpacialAndArea && countType(fieldDefAry,AREA)===1;


    const panels = {
        spacialPanel: makeDynSpacialPanel(fieldDefAry, spacialManagesArea && spacialCreatesPoly),
        areaFields: spacialManagesArea ? [] : makeAllAreaFields(fieldDefAry),
        circlePolyField: !spacialManagesArea && makeCirclePolyCombinedField(fieldDefAry),
        checkBoxFields: makeCheckboxFields(fieldDefAry),
        ...makeInputFields(fieldDefAry, noLabels)
    };
    panels.useArea = Boolean(panels.areaFields.length);
    panels.useSpacial = Boolean(panels.spacialPanel);
    panels.useCirclePolyField = Boolean(panels.circlePolyField);
    return panels;
};

/**
 * @param {Array.<FieldDef>} fieldDefAry
 * @param {string} type
 * @return {FieldDef}
 */
const findType= (fieldDefAry, type) => fieldDefAry.find( (entry) => entry.type===type);
const countType= (fieldDefAry, type) => fieldDefAry.filter( (entry) => entry.type===type).length;

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




function makeCirclePolyCombinedField(fieldDefAry) {
    const cdDefAry = fieldDefAry.filter(({type}) => (type === CIRCLE || type === POLYGON));
    return cdDefAry.length ? <CircleAndPolyFieldPopup fieldDefAry={cdDefAry}/> : false;
}

function CircleAndPolyFieldPopup({fieldDefAry, typeForCircle= CIRCLE}) {
    const polyType = findType(fieldDefAry, POLYGON);
    const cirType = findType(fieldDefAry, typeForCircle);
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

    const sizeType= (cirType && typeForCircle!==CIRCLE) ? findType(fieldDefAry, AREA) : undefined;

    const polygonKey= polyType?.key;
    const targetKey= typeForCircle===CIRCLE ? cirType.targetDetails.targetKey : cirType?.key;
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
                    hideHiPSPopupPanelOnDismount: false, fieldKey: targetKey, ...cirType,
                    targetKey, sizeKey, polygonKey, initValue, minValue, maxValue,
                }}/>}
            {getChoice() === POLY_CHOICE_KEY &&
                <PolygonField {...{
                    hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey, ...polyType,
                    targetKey, sizeKey, polygonKey,
                }} />}
        </div>
    );
}

function PositionAndPolyFieldEmbed({fieldDefAry}) {

    const polyType = findType(fieldDefAry, POLYGON);
    const posType = findType(fieldDefAry, POSITION);
    const areaType = findType(fieldDefAry, AREA);
    const {targetDetails, nullAllowed = false} = posType ?? {};
    const [getConeAreaOp] = useFieldGroupValue(CONE_AREA_KEY);

    const {
        hipsUrl, centerPt, hipsFOVInDeg = 240, coordinateSys: csysStr = 'EQ_J2000', mocList, sRegion,
        targetPanelExampleRow1, targetPanelExampleRow2} = targetDetails ?? polyType?.targetDetails ?? {};
    const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;

    const { minValue = 1 / 3600, maxValue = 1 } = areaType ?? {} ;

    if (!polyType || !polyType || !areaType) return false;
    const sizeKey= areaType.key;
    const searchAreaInDeg= areaType.initValue;
    const targetKey= posType.key;
    const polygonKey= polyType.key;

    return (
        <div key='targetGroup'
             style={{display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch'}}>
            <HiPSTargetView {...{
                hipsUrl, centerPt, hipsFOVInDeg, mocList, coordinateSys, sRegion,
                minSize: minValue, maxSize: maxValue, whichOverlay: getConeAreaOp(),
                targetKey, sizeKey, polygonKey, style: {height: 400, alignSelf: 'stretch'}
            }}/>
            <RadioGroupInputField {...{
                inline: true, fieldKey: CONE_AREA_KEY, wrapperStyle: {paddingBottom: 10, paddingTop: 10},
                tooltip: 'Chose type of search', initialState: {value: CONE_CHOICE_KEY}, options: CONE_AREA_OPTIONS
            }} />
            {getConeAreaOp() === CONE_CHOICE_KEY &&
                <div>
                    <TargetPanel {...{
                        key: 'targetPanel', labelWidth:100, nullAllowed,
                        targetPanelExampleRow1, targetPanelExampleRow2
                    }}/>
                    <SizeInputFields {...{
                        style:{paddingBottom:10},
                        fieldKey: sizeKey, showFeedback: true, labelWidth: 100, nullAllowed: false,
                        labelStyle:{textAlign:'right', paddingRight:4},
                        label: 'Search Area:',
                        initialState: {unit: 'arcsec', value: searchAreaInDeg + '', min:minValue, max:maxValue}
                    }} />
                </div>
            }
            {getConeAreaOp() === POLY_CHOICE_KEY &&
                <PolygonField {...{
                    hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey, ...polyType,
                    targetKey, sizeKey, manageHiPS:false,
                }} />}
        </div>
    );
}

function makeAllAreaFields(fieldDefAry) {
    return fieldDefAry.filter( ({type}) => type===AREA).map( (param) => makeAreaField(param));
}

function makeAreaField({ key, desc = 'Search Area', initValue, tooltip, nullAllowed = false,
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
                sizeLabel:'Search Area:',
                polygonKey:polygonKey??targetDetails.polygonKey,
                sizeFeedbackStyle:{textAlign:'center', marginLeft:0}
            }} />
        </div>
    );
}


function PolygonField({ fieldKey, desc = 'Coordinates', initValue = '', style={},
                          labelWidth = DEF_LABEL_WIDTH, tooltip = 'Enter polygon coordinates search',
                          targetDetails: {targetPanelExampleRow1 = DEF_AREA_EXAMPLE, sRegion}, ...restOfProps }) {

    const help = [
        'Each vertex is defined by a J2000 RA and Dec position pair',
        'A max of 15 and min of 3 vertices is allowed',
        'Vertices must be separated by a comma (,)',
        targetPanelExampleRow1
    ];
    return (
        <div key={fieldKey} style={style}>
            <VisualPolygonPanel {...{
                fieldKey,
                wrapperStyle: {display: 'flex', alignItems: 'center'},
                style: {width: 350, maxWidth: 420},
                initialState: {value: initValue, tooltip},
                label: desc,
                labelStyle: {},
                labelWidth,
                tooltip,
                sRegion,
                ...restOfProps
            }}
            />
            <ul style={{marginTop: 7}}>
                {help.map((h) => <li key={h} style={{listStyleType: `'${BULLET}'`}}>{h}</li>)}
            </ul>
        </div>
    );
}

function makeDynSpacialPanel(fieldDefAry, manageAllSpacial= true) {
    const posType = findType(fieldDefAry, POSITION);
    const areaType = findType(fieldDefAry, AREA);
    const polyType= manageAllSpacial && findType(fieldDefAry, POLYGON);
    if (!posType) return;
    const {targetDetails, nullAllowed = false, minValue, maxValue} = posType;
    const {labelWidth = DEF_LABEL_WIDTH} = posType;

    const {
        hipsUrl, centerPt, hipsFOVInDeg = 240, coordinateSys: csysStr = 'EQ_J2000', mocList, popupHiPS = false,
        targetPanelExampleRow1, targetPanelExampleRow2
    } = targetDetails ?? polyType?.targetDetails ?? {};
    const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;
    const sizeKey = areaType?.key;

    if (!hipsUrl && !targetDetails) {
        return <TargetPanel {...{labelWidth, nullAllowed, targetPanelExampleRow1, targetPanelExampleRow2}}/>;
    }

    if (manageAllSpacial) {
        return popupHiPS ?
            <CircleAndPolyFieldPopup {...{fieldDefAry,typeForCircle:POSITION}}/> : <PositionAndPolyFieldEmbed {...{fieldDefAry}}/>;
    }
    else {
        if (popupHiPS) {
            return (<VisualTargetPanel {...{
                hipsUrl, centerPt, hipsFOVInDeg, mocList, nullAllowed, coordinateSys, sizeKey,
                minSize: minValue, maxSize: maxValue,
                targetPanelExampleRow1, targetPanelExampleRow2 }} />);
        }
        else {
            return (
                <div key='targetGroup'
                     style={{display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch'}}>
                    <HiPSTargetView {...{
                        hipsUrl, centerPt, hipsFOVInDeg, mocList, coordinateSys,
                        minSize: minValue, maxSize: maxValue,
                        targetKey: 'UserTargetWorldPt', sizeKey, style: {height: 400, alignSelf: 'stretch'}
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
}

