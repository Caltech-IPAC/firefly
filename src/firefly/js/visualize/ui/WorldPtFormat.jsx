import {Typography} from '@mui/joy';
import React, {Fragment} from 'react';
import {coordToString} from '../../ui/PositionFieldDef.js';
import CoordUtil from '../CoordUtil';
import Point, {isValidPoint, parseWorldPt} from '../Point';
import {sprintf} from '../../externalSource/sprintf.js';
import {isString} from 'lodash';
import {EQ_TYPE} from 'firefly/visualize/MouseReadoutCntlr';

const valToStr= (v) => sprintf('%.6f',v);

export function formatWorldPt(wp, pad=3, useBold=true) {
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    const fontWeight= useBold?'bold':'normal';
    if (!wp.objName) {
        return (
            <Typography level='body-sm' sx={{fontWeight}}> {`${valToStr(wp.x)}, ${valToStr(wp.y)} ${coordToString(wp.cSys)}`} </Typography>
        );
    }
    if (wp.resolver) {
        return (
            <Fragment>
                <Typography level='body-sm' sx={{fontWeight, paddingRight:pad+'px'}}>{wp.objName}</Typography>
                <Typography level='body-xs'> {`${wp.resolver.toString().toUpperCase()}`} </Typography>
            </Fragment>
        );
    }
    else {
        return ( <Typography size='body-sm' style={{fontWeight}}>{wp.objName}</Typography> );
    }
}

export function formatWorldPtToString(wp,addNewLIne=false) {
    const actualWp= isValidPoint(wp) ? wp : (isString(wp) ? parseWorldPt(wp) : undefined);
    if (!actualWp) return '';

    if (actualWp.type!==Point.W_PT) return `${actualWp.x}, ${actualWp.y}`;
    const lonStr = valToStr(actualWp.getLon());
    const latStr = valToStr(actualWp.getLat());
    const hmsRa = CoordUtil.convertLonToString(actualWp.getLon(), actualWp.getCoordSys());
    const hmsDec = CoordUtil.convertLatToString(actualWp.getLat(), actualWp.getCoordSys());
    const csys = coordToString(actualWp.getCoordSys());

    const coordStr= `${lonStr}, ${latStr}  or${addNewLIne?'\n':''}  ${hmsRa}, ${hmsDec} ${csys}`;
    if (!actualWp.objName) return coordStr;
    return actualWp.resolver ?
        `${actualWp.objName} - by ${actualWp.resolver.toString().toUpperCase()} - ${coordStr}` :
        `${actualWp.objName} - ${coordStr}`;
}

/**
 *
 * @param wp {WorldPt|string} World Point object or serialised as a string
 * @param eqType {EQ_TYPE|undefined} format to decimal or to HMS, or to both types if `undefined` (which is the default)
 * @returns {string}
 */
export function formatWorldPtToStringSimple(wp,eqType) {
    const actualWp= isValidPoint(wp) ? wp : (isString(wp) ? parseWorldPt(wp) : undefined);
    if (!actualWp) return '';

    if (actualWp.objName) return actualWp.objName;
    if (actualWp.type!==Point.W_PT) return `${actualWp.x}, ${actualWp.y}`;
    const lonStr = valToStr(actualWp.getLon());
    const latStr = valToStr(actualWp.getLat());
    const hmsRa = CoordUtil.convertLonToString(actualWp.getLon(), actualWp.getCoordSys());
    const hmsDec = CoordUtil.convertLatToString(actualWp.getLat(), actualWp.getCoordSys());
    const csys = coordToString(actualWp.getCoordSys());
    const dcmCoordStr = `${lonStr}, ${latStr}`;
    const hmsCoordStr = `${hmsRa}, ${hmsDec}`;

    const coordStr= eqType===EQ_TYPE.DCM ? dcmCoordStr
        : (eqType===EQ_TYPE.HMS ? hmsCoordStr
            : `${dcmCoordStr}  or  ${hmsCoordStr}`);
    return `${coordStr} ${csys}`;
}

export function formatLonLatToString(wp) {
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    return `${valToStr(wp.getLon())} ${valToStr(wp.getLat())}`;
}