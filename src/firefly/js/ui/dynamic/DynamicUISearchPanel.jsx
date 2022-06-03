/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {CoordinateSys, parseWorldPt} from '../../api/ApiUtilImage.jsx';
import {CONE_CHOICE_KEY} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {convert} from '../../visualize/VisUtil.js';
import CompleteButton from '../CompleteButton.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {FormPanel} from '../FormPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {AREA, CHECKBOX, CIRCLE, ENUM, FLOAT, INT, POLYGON, POSITION, UNKNOWN} from './DynamicDef.js';
import {getSpacialSearchType, hasValidSpacialSearch, makeAllFields, makeUnitsStr} from './DynComponents.jsx';
import './DynamicUI.css';


const defaultOnError= () => showInfoPopup('One or more fields are not valid', 'Invalid Data');

export const DynamicFieldGroupPanel = ({DynLayoutPanel, groupKey, fieldDefAry, keepState = true}) => (
    <FieldGroup groupKey={groupKey} keepState={keepState}>
        <DynLayoutPanel fieldDefAry={fieldDefAry} style={{margin: '8px 3px 15px 3px', width: '100%'}}/>
    </FieldGroup>
);


export const DynCompleteButton= ({fieldDefAry, onSuccess, ...restOfProps}) => (
    <CompleteButton {...{...restOfProps,
        onSuccess:(r) => onSuccess(convertRequest(r,fieldDefAry))
    }}/> );

export function DynamicForm({DynLayoutPanel, groupKey,fieldDefAry, onSubmit, onError=defaultOnError, onCancel, help_id, style={}}) {

    const onSearchSubmit= (request) => {
        if (!hasValidSpacialSearch(request,fieldDefAry)) {
            showInfoPopup(
                getSpacialSearchType(request,fieldDefAry)===CONE_CHOICE_KEY ?
                    'Target is required' : 'Search Area is require and must have a least 3 points');
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


export function convertRequest(request, fieldDefAry) {
    return fieldDefAry.reduce( (out, {key,type, targetDetails:{raKey,decKey, targetKey,sizeKey}={} }) => {
        switch (type) {
            case FLOAT: case INT: case AREA: case ENUM: case POLYGON: case UNKNOWN:
                out[key]= request[key];
                return out;
            case CHECKBOX:
                const value= Object.entries(request)
                    .find( ([k]) => k.includes(key))?.[1]?.includes(key) ?? false;
                out[key]= value;
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
            default: return out;
        }
    }, {});
}


function SimpleDynSearchPanel({style={}, fieldDefAry}) {
    const { spacialPanel, areaFields, polyPanel, circlePolyField, checkBoxFields, fieldsInputAry, opsInputAry,
        useSpacial, useArea, useCirclePolyField}= makeAllFields(fieldDefAry);

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
            {useSpacial &&
                <div style={{display:'flex', flexDirection:'column', alignItems:'center'}}> {spacialPanel} </div>}
            {Boolean(polyPanel) &&
                <div style={{display:'flex', flexDirection:'column', alignItems:'center'}}> {polyPanel} </div>}
                <div style={{paddingLeft:useSpacial?60:5, display:'flex', flexDirection:'column'}}>
                    {useArea &&
                        <div key='a' style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                            {areaFields}
                        </div>}
                    {(useCirclePolyField) &&
                        <div style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                            {circlePolyField}
                        </div>}
                    <div style={{display:'flex', flexDirection:'row', alignItems:'center'}}>
                        {Boolean(iFieldLayout) && <div>{iFieldLayout}</div>}
                        {Boolean(checkBoxFields) &&
                            <div style={{padding: '5px 0 0 45px', display:'flex', flexDirection:'column', alignSelf:'center'}}>
                                {checkBoxFields}
                            </div> }
                    </div>
                </div>
        </div>
    );
}

function GridDynSearchPanel({style={}, fieldDefAry}) {
    const { spacialPanel, areaFields, checkBoxFields, fieldsInputAry, opsInputAry,
        useArea, useCirclePolyField, useSpacial}= makeAllFields(fieldDefAry, true);

    const labelAry= fieldDefAry
        .filter( ({type}) => type===INT || type===FLOAT || type===ENUM || type===UNKNOWN)
        .map( ({desc,units}) => `${desc}${makeUnitsStr(units)}` );

    let gridFieldLayout;
    if (fieldsInputAry.length || opsInputAry.length) {
        const combinedAry= [];
        [...fieldsInputAry,...opsInputAry].forEach( (f,idx) => {
            combinedAry.push((<div key={labelAry[idx]} style={{justifySelf:'end'}}>{labelAry[idx]}</div>));
            combinedAry.push(f);
        } );

        gridFieldLayout= (
            <div className='dynGrid' style={{paddingTop:20}}>
                {combinedAry}
            </div>);
    }
    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', alignSelf: 'stretch', ...style}}>
            {useSpacial &&
                <div style={{display:'flex', flexDirection:'column', alignItems:'center', alignSelf:'stretch'}}>
                    {spacialPanel}
                    {useArea && <> {areaFields} </>}
                    {useCirclePolyField && <> <div style={{paddingTop:5}}/> {useCirclePolyField} </>}
                </div>
            }
            {Boolean(checkBoxFields) && <div key='b' style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                {checkBoxFields}
            </div> }
            {Boolean(gridFieldLayout) && gridFieldLayout}
        </div>
    );
}


export const DynLayoutPanelTypes= {
    'Simple': SimpleDynSearchPanel,
    'Grid': GridDynSearchPanel,
};
