import React, {} from 'react';
import {TargetPanel} from 'firefly/ui/TargetPanel.jsx';
import {HiPSTargetView, TargetHiPSRadiusPopupPanel, VisualTargetPanel} from 'firefly/visualize/ui/TargetHiPSPanel.jsx';
import {SizeInputFields} from 'firefly/ui/SizeInputField.jsx';
import {CoordinateSys, parseWorldPt} from 'firefly/api/ApiUtilImage.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {floatRange, intRange} from 'firefly/util/Validate.js';
import {ListBoxInputField} from 'firefly/ui/ListBoxInputField.jsx';
import {isString} from 'lodash';
import {getFormattedWaveLengthUnits} from '../visualize/PlotViewUtil.js';
import {convert} from '../visualize/VisUtil.js';
import {FieldGroup} from './FieldGroup.jsx';
import {FormPanel} from './FormPanel.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import './DynamicUI.css';

const POSITION= 'position';
const ENUM= 'enum';
const INT= 'int';
const FLOAT= 'float';
const UNKNOWN= 'unknown';
const AREA= 'area';
const POLYGON= 'polygon';
const CIRCLE= 'circle';

const DEF_LABEL_WIDTH= 100;



const defaultOnError= () => showInfoPopup('One or more fields are not valid', 'Invalid Data');

function hasValidTarget(request, fieldDefAry) {
    const posType= findType(fieldDefAry, POSITION);
    if (!posType || posType.nullAllowed) return true;
    return Boolean(parseWorldPt(request[posType.key]));
}


export function DynamicForm({DynLayoutPanel, groupKey,fieldDefAry, onSubmit, onError=defaultOnError, onCancel, help_id, style={}}) {

    const onSearchSubmit= (request) => {
        if (!hasValidTarget(request,fieldDefAry)) {
            showInfoPopup('Target is required');
            return false;
        }
        return onSubmit?.(convertRequest(request, fieldDefAry));
    };

    return (
        <div style={style}>
            <FormPanel  {...{
                inputStyle: {display: 'flex', flexDirection: 'column', backgroundColor: 'transparent', padding: 'none', border: 'none'},
                submitBarStyle:{padding: '2px 3px 3px'},
                buttonStyle:{justifyContent: 'left'},
                groupKey, onSubmit:onSearchSubmit, onCancel, onError, help_id,
                params:{hideOnInvalid: false},
            }} >
                <DynamicFieldGroupPanel {...{DynLayoutPanel, groupKey,fieldDefAry}}/>
            </FormPanel>
        </div>
    );
}

export const DynamicFieldGroupPanel= ({DynLayoutPanel, groupKey,fieldDefAry, keepState=true}) => (
    <FieldGroup groupKey={groupKey} keepState={keepState}>
        <DynLayoutPanel fieldDefAry={fieldDefAry} style={{margin:'8px 3px 15px 3px', width:'100%'}}/>
    </FieldGroup>
);


/**
 *
 * @param {Array.<FieldDef>} fieldDefAry
 * @param {string} type
 * @return {FieldDef}
 */
const findType= (fieldDefAry, type) => fieldDefAry.find( (entry) => entry.type===type);


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


function makeAllAreaFields(fieldDefAry) {
    return fieldDefAry.filter( ({type}) => type===AREA).map( (param) => makeAreaField(param));
}

function makeAllCircleFields(fieldDefAry) {
    return fieldDefAry.filter( ({type}) => type===CIRCLE).map( (param) => makeCircleField(param));
}



const makeUnitsStr= (units) => !units ? '' : ` (${getFormattedWaveLengthUnits(units)})`;


function makeAreaField({key,desc='Search Area',initValue,tooltip,
                   nullAllowed=false, minValue=1/3600, maxValue=1, labelWidth= DEF_LABEL_WIDTH}) {
    return (
        <SizeInputFields {...{
            key, fieldKey:key, showFeedback:true, labelWidth, nullAllowed, label:desc, tooltip:tooltip??desc,
            feedbackStyle:{margin: '-5px 0 0 36px'},
            initialState: {unit: 'arcsec', value: initValue+'', min:minValue, max: maxValue}
        }}/>
    );
}


function makeValidationField({key,type,desc,tooltip='',initValue,minValue,maxValue, units='',
                                 precision=2, labelWidth=DEF_LABEL_WIDTH}) {
    const unitsStr= desc ? makeUnitsStr(units) : '';
    const validator=
        type===FLOAT ?
            (value,nullAllowed) => floatRange(minValue,maxValue,precision,desc,value,nullAllowed) :
            type === INT ?
                (value,nullAllowed) => intRange(minValue,maxValue,desc,value,nullAllowed) :
                undefined;
    return (
        <ValidationField {...{
            key,
            fieldKey:key, tooltip:tooltip+unitsStr, label:desc+unitsStr, labelWidth,
            initialState: {
                value: initValue ?? (type!==UNKNOWN ? minValue??0 : ''),
                validator,
            } }}/>
    );
}

function makeListBoxField({key,desc,tooltip='',initValue,enumValues, units, labelWidth= DEF_LABEL_WIDTH}) {
    const unitsStr= desc ? makeUnitsStr(units) : '';
    return (
        <ListBoxInputField {...{
            key, fieldKey:key, label:desc+unitsStr, tooltip:tooltip+unitsStr, labelWidth,
            initialState: { value: initValue},
            options: enumValues.map( (e) => isString(e) ? {label:e,value:e} : e),
            wrapperStyle:{marginRight:'15px', padding:'3px 0 5px 0'},
            multiple:false }}
        />
    );
}

function makeCircleField({key,desc,tooltip='', initValue,minValue,maxValue,labelWidth= DEF_LABEL_WIDTH, targetDetails}) {
    return (
        <div key={key} style={{display:'flex'}}>
            <div title={tooltip} style={{width:labelWidth, alignSelf:'center'}}>{desc}</div>
            <TargetHiPSRadiusPopupPanel {...{key,searchAreaInDeg:initValue,labelWidth,
                minValue, maxValue, ...targetDetails}} />
        </div>
    );
}

function makeTargetPanel(fieldDefAry) {
    const posType= findType(fieldDefAry, POSITION);
    const areaType= findType(fieldDefAry, AREA);
    if (!posType) return;
    const {targetDetails,nullAllowed=false, minValue, maxValue}= posType;
    const {labelWidth= DEF_LABEL_WIDTH}=posType;

    if (!targetDetails) return <TargetPanel labelWidth={labelWidth}/>;
    const sizeKey= areaType.key;

    const { hipsUrl, centerPt, hipsFOVInDeg=240, coordinateSys:csysStr='EQ_J2000',
        mocList, popupHiPS=false,
        targetPanelExampleRow1, targetPanelExampleRow2} = targetDetails;

    const coordinateSys= CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;
    //todo- support targetPanelExampleRow1 and 2
    //todo- pass though nullAllowed
    if (!hipsUrl) {
        return <TargetPanel {...{labelWidth, nullAllowed, targetPanelExampleRow1, targetPanelExampleRow2}}/>;
    }

    if (popupHiPS) {
        return (
           <VisualTargetPanel {...{hipsUrl,centerPt,hipsFOVInDeg,mocList,nullAllowed,coordinateSys,sizeKey,
               minSize:minValue, maxSize:maxValue,
               targetPanelExampleRow1, targetPanelExampleRow2 }} />
        );
    }
    else {
        return (
            <div key='targetGroup' style={{display:'flex', flexDirection:'column', alignItems:'center', alignSelf:'stretch'}}>
                <HiPSTargetView {...{hipsUrl,centerPt,hipsFOVInDeg,mocList,coordinateSys,
                    minSize:minValue, maxSize:maxValue,
                    targetKey:'UserTargetWorldPt', sizeKey, style:{height:400, alignSelf:'stretch'} }}/>
                <div style={{display:'flex', flexDirection:'column'}}>
                    <TargetPanel {...{style:{paddingTop: 10}, key:'targetPanel', labelWidth, nullAllowed,
                        targetPanelExampleRow1, targetPanelExampleRow2}}/>
                </div>
            </div>
        );
    }
}


export function makeTargetDef({hipsUrl,centerPt, hipsFOVInDeg, coordinateSys, mocList, nullAllowed, popupHiPS,
                               minValue, maxValue,
                             targetPanelExampleRow1, targetPanelExampleRow2, raKey, decKey }) {
    return {
        type: POSITION,
        key: 'UserTargetWorldPt',
        nullAllowed,
        minValue,
        maxValue,
        targetDetails: {
            raKey, decKey, hipsUrl, centerPt, hipsFOVInDeg, coordinateSys, mocList,
            popupHiPS, targetPanelExampleRow1, targetPanelExampleRow2}
    };
}

export const makeEnumDef= ({key,desc,tooltip,units, initValue,enumValues}) =>
    ({ type: ENUM,  key,desc,tooltip,units, initValue,enumValues });

export const makeIntDef= ({key,minValue,maxValue, desc,tooltip,units, initValue, nullAllowed}) =>
    ({ type: INT,  key,desc,tooltip,units, initValue,minValue,maxValue, nullAllowed });

export const makeFloatDef= ({key,minValue,maxValue, precision, desc,tooltip, units ,initValue, nullAllowed}) =>
    ({ type: FLOAT,  key,desc,tooltip,units, initValue,minValue,maxValue, precision, nullAllowed});

export const makeUnknownDef= ({key,desc,tooltip,units, initValue, nullAllowed}) =>
    ({ type: UNKNOWN,  key,desc,tooltip,units, initValue, nullAllowed});

export const makeAreaDef= ({key,minValue,maxValue, desc,tooltip,initValue,nullAllowed}) =>
    ({ type: AREA,  key,desc,tooltip,initValue,minValue,maxValue,nullAllowed});

export function makeCircleDef({key,targetKey, sizeKey, minValue,maxValue, desc,tooltip,initValue,nullAllowed,
                                  centerPt, hipsFOVInDeg, coordinateSys,
                                  hipsUrl, mocList, targetPanelExampleRow1, targetPanelExampleRow2}) {
    return ({ type: CIRCLE,  key,desc,tooltip,initValue,minValue,maxValue,nullAllowed,
        targetDetails: {
            hipsUrl, centerPt, hipsFOVInDeg, coordinateSys, mocList,
            targetKey, sizeKey,
            targetPanelExampleRow1, targetPanelExampleRow2}
    });
}

export const makePolygonDef= ({key, desc,tooltip,initValue,nullAllowed}) =>
    ({ type: AREA,  key,desc,tooltip,initValue,nullAllowed});


export function convertRequest(request, fieldDefAry) {
    return fieldDefAry.reduce( (out, {key,type, targetDetails:{raKey,decKey, targetKey,sizeKey}={} }) => {
        switch (type) {
            case FLOAT: case INT: case AREA: case ENUM: case UNKNOWN:
                out[key]= request[key];
                return out;
            case POSITION:
                if (raKey && decKey) {
                    const wp= convert(parseWorldPt(request[key]), CoordinateSys.EQ_J2000);
                    out[raKey]= wp?.x;
                    out[decKey]= wp?.y;
                }
                else {
                    out[key]= request[key];
                }
                return out;
            case CIRCLE:
                const radius= request[sizeKey];
                const wp= convert(parseWorldPt(request[targetKey]), CoordinateSys.EQ_J2000);
                if (radius && wp) {
                    out[key]= `${wp.x} ${wp.y} ${radius}`;
                }
                return out;
            case 'polygon': //not implemented
                return out;
            default: return out;
        }
    }, {});
}



function SimpleDynSearchPanel({style={}, fieldDefAry}) {
    const tp= makeTargetPanel(fieldDefAry);
    const aFields= makeAllAreaFields(fieldDefAry);
    const cFields= makeAllCircleFields(fieldDefAry);
    const {fieldsInputAry,opsInputAry}= makeInputFields(fieldDefAry);

    let iFieldLayout;
    if (fieldsInputAry.length || opsInputAry.length) {
        iFieldLayout= (
            <>
                <div key='top' style={{paddingTop:5}}/>
                {fieldsInputAry}
                {Boolean(fieldsInputAry.length) && <div key='pad' style={{paddingTop:5}}/>}
                {opsInputAry}
            </>);
    }


    return (
        <div style={style}>
            {Boolean(tp) &&
                <div style={{display:'flex', flexDirection:'column', alignItems:'center'}}>
                    {tp}
                </div>
            }
            <div style={{paddingLeft:60, display:'flex', flexDirection:'column'}}>
                {Boolean(aFields.length) &&  <div key='a' style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                    {aFields}
                </div>}
                {Boolean(cFields.length) &&  <div key='b' style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                    {cFields}
                </div>}
                {Boolean(iFieldLayout) && iFieldLayout}
            </div>
        </div>
    );
}

function GridDynSearchPanel({style={}, fieldDefAry}) {
    const tp= makeTargetPanel(fieldDefAry);
    const aFields= makeAllAreaFields(fieldDefAry);
    const cFields= makeAllCircleFields(fieldDefAry);
    const {fieldsInputAry,opsInputAry}= makeInputFields(fieldDefAry,true);

    const labelAry= fieldDefAry
        .filter( ({type}) => type===INT || type===FLOAT || type===ENUM || type===UNKNOWN)
        .map( ({desc,units}) => `${desc}${makeUnitsStr(units)}` );

    let iFieldLayout;
    if (fieldsInputAry.length || opsInputAry.length) {
        const combinedAry= [];
        [...fieldsInputAry,...opsInputAry].forEach( (f,idx) => {
            combinedAry.push((<div style={{justifySelf:'end'}}>{labelAry[idx]}</div>));
            combinedAry.push(f);
        } );

        iFieldLayout= (
            <div className='dynGrid' style={{paddingTop:20}}>
                {combinedAry}
            </div>);
    }
    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch', ...style}}>
            {Boolean(tp) &&
                <div style={{display:'flex', flexDirection:'column', alignItems:'center', alignSelf:'stretch'}}>
                    {tp}
                    {Boolean(aFields.length) && <> {aFields} </>}
                    {Boolean(cFields.length) && <> <div style={{paddingTop:5}}/> {cFields} </>}
                </div>
            }
            {Boolean(iFieldLayout) && iFieldLayout}
        </div>
    );
}




export const DynLayoutPanelTypes= {

    'Simple': SimpleDynSearchPanel,
    'Grid': GridDynSearchPanel,
};

/**
 * @typedef FieldDef
 *
 * @prop {String} key - not used with uiSelect
 * @prop {string} type one of float, int, position, area, enum, unknown.  not implemented yet: polygon, circle
 * @prop {number} minValue only use with int, float, radius
 * @prop {number} maxValue  only use with int,float, radius
 * @prop {string} desc - optional  for position, polygon, radius, not used with ra, dec
 * @prop {boolean} nullAllowed - true if can be null
 * @prop {string} units - will show up in description and tooltip
 * @prop {string} tooltip
 * @prop {number} labelWidth
 * @prop {number} precision - used with float
 * @prop {number|string} initValue used with float, int, enum
 * @prop {string} areaType - only used with area - one of square, circleRadius, circleDiameter, none
 * @prop {TargetSearchDetails} - targetDetails only use with ra, dec, position, polygon
 * @prop {Array.<{label:string,value:string}|String>} enumValues only used with enum
 *
 */


/**
 * @typedef TargetSearchDetails
 *
 * @prop {String} hipsUrl
 * @prop {WorldPt} centerPt
 * @prop {number} hipsFOVInDeg
 * @prop {string} coordinateSys - one of 'GALACTIC' or 'EQ_J2000'
 * @prop {Array.<{mocUrl:String, title:String}>} mocList
 * @prop {boolean} nullAllowed
 * @prop {boolean} popupHiPS
 * @prop {String} raKey
 * @prop {String} decKey
 * @prop {Array.<String>} targetPanelExampleRow1 eg- [`'62, -37'`, `'60.4 -35.1'`, `'4h11m59s -32d51m59s equ j2000'`, `'239.2 -47.6 gal'`],
 * @prop {Array.<String>} targetPanelExampleRow2  eg= [`'NGC 1532' (NB: DC2 is a simulated sky, so names are not useful)`],
 */

