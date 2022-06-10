/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import {string,func, object, arrayOf} from 'prop-types';
import {sprintf} from '../../externalSource/sprintf.js';
import {
    makeCircleDef, makeEnumDef, makeFloatDef, makeIntDef, makePolygonDef, makeUnknownDef
} from '../../ui/dynamic/DynamicDef.js';
import { DynamicFieldGroupPanel, DynCompleteButton, DynLayoutPanelTypes } from '../../ui/dynamic/DynamicUISearchPanel.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {splitByWhiteSpace} from '../../util/WebUtil.js';
import {makeWorldPt} from '../Point.js';


const GROUP_KEY= 'ActivateMenu';

const titleStyle= {width: '100%', textAlign:'center', padding:'10px 0 5px 0', fontSize:'larger', fontWeight:'bold'};


export const ActivateMenu= memo(({ serviceDefRef='none', serDefParams, setSearchParams, title, makeDropDown}) => {


    const loadParams= (request) => {
        setSearchParams(request);
    };
    const fieldDefAry= makeFieldDefs(serDefParams);
    return (
        <div key={serviceDefRef}>
            {makeDropDown?.()}
            <div style={{padding: '5px 5px 5px 5px'}}>
                <div style= {titleStyle}> {title} </div>
                <DynamicFieldGroupPanel groupKey={GROUP_KEY} keepState={false}
                                        DynLayoutPanel={DynLayoutPanelTypes.Simple}
                                        fieldDefAry={fieldDefAry} />
                <DynCompleteButton style={{padding: '20px 0 0 0'}}
                                   fieldDefAry={fieldDefAry}
                                   onSuccess={(r) => loadParams(r)}
                                   onFail={() => showInfoPopup('Some field are not valid')}
                                   text={'Submit'} groupKey={GROUP_KEY} />
            </div>
        </div>
    );
});

ActivateMenu.propTypes= {
    serDefParams: arrayOf(object),
    title: string,
    serviceDefRef: string,
    setSearchParams: func,
    makeDropDown: func
};

const isFloatingType= (type) => (type==='float' || type==='double');

const isCircleField= ({type,arraySize,xtype='',units=''}) =>
    (isFloatingType(type) && Number(arraySize)===3 && xtype.toLowerCase()==='circle' && units.toLowerCase()==='deg');

const isPolygonField= ({type,xtype='',units=''}) =>
    (isFloatingType(type) && xtype.toLowerCase()==='polygon' && units.toLowerCase()==='deg');

function getCircleValues(s) {
    const strAry= splitByWhiteSpace(s);
    if (strAry.length!==3 || strAry.find( (s) => isNaN(Number(s)))) return [];
    return strAry.map( (s) => Number(s));
}

const getCircleInfo = ({minValue='' ,maxValue='', value=''}) => {
    const matchStr=[value,minValue,maxValue].find( (s) => getCircleValues(s).length===3);
    if (!matchStr) return {};
    const valueAry= getCircleValues(matchStr);
    if (valueAry?.length!==3) return {};
    const minNum= getCircleValues(minValue)[2];
    const maxNum= getCircleValues(maxValue)[2];
    return {wpt:makeWorldPt(valueAry[0],valueAry[1]), radius: valueAry[2], minValue:minNum, maxValue:maxNum};
};

const getPolygonInfo = ({minValue='' ,maxValue='', value=''}) => {
    const vStr= value || minValue || maxValue;
    const validAryStr= splitByWhiteSpace(vStr).filter( (s) => !isNaN(Number(s))).map( (s) => sprintf('%.5f',Number(s)));
    if (validAryStr.length % 2 !==0) return {value:''};
    return {value: validAryStr.reduce( (s, num, idx) => idx!==0 && idx%2===0 ? `${s}, ${num}` : `${s} ${num}`, '')};
};

const isNumberField= ({type,minValue,maxValue,value}) =>
    (type==='int' || isFloatingType(type)) ||
    (value || minValue || maxValue) &&
    (!isNaN(Number(value)) || !isNaN(Number(minValue)) || !isNaN(Number(maxValue)) );



function makeFieldDefs(serDefParams) {
    return serDefParams
        .filter( (sdP) => !sdP.ref )
        .map( (sdP) => {
            const {type, optionalParam:nullAllowed, value='', name,options,desc:tooltip, units=''} = sdP;

            if (options) {
                const fieldOps = options.split(',').map( (op) => ({label:op,value:op}));
                return makeEnumDef({key:name, desc:name, tooltip, units, initValue:fieldOps[0].value, enumValues:fieldOps});
            }
            else {
                if (isCircleField(sdP)) {
                    const {wpt:centerPt,radius=1,minValue,maxValue}= getCircleInfo(sdP);
                    return makeCircleDef({key:name, desc:name, tooltip, units,
                        targetKey:'circleTarget', sizeKey:'circleSize',
                        initValue:radius,
                        centerPt, minValue, maxValue,
                        hipsFOVInDeg:radius*2+radius*.2 });
                }
                if (isPolygonField(sdP)) {
                    const {value}= getPolygonInfo(sdP);
                    return makePolygonDef({key:name, desc:name, tooltip, units, initValue:value});
                }
                else if (isNumberField(sdP)) {
                        const minNum= Number(sdP.minValue);
                        const maxNum= Number(sdP.maxValue);
                        const vNum= Number(value);
                        if (type==='int') {
                            return makeIntDef({key:name, desc:name, tooltip, units, precision:4, nullAllowed,
                                initValue:!isNaN(vNum)?vNum : undefined,
                                minValue:!isNaN(minNum)?minNum : undefined,
                                maxValue:!isNaN(maxNum)?maxNum : undefined,
                            });
                        }
                        else if (isFloatingType(type)) {
                            return makeFloatDef({key:name, desc:name, tooltip, units, precision:4, nullAllowed,
                                initValue:!isNaN(vNum)?vNum : undefined,
                                minValue:!isNaN(minNum)?minNum : undefined,
                                maxValue:!isNaN(maxNum)?maxNum : undefined,
                            });
                        }
                        else {
                            return makeUnknownDef({key:name, desc:name, tooltip, units, initValue:value });
                        }
                }
                else {
                    return makeUnknownDef({key:name, desc:name, tooltip, units, initValue:value??'' });
                }
            }
        });
}


