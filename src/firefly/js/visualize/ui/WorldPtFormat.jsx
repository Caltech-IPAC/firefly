import {Typography} from '@mui/joy';
import React, {Fragment} from 'react';
import {coordToString} from '../../ui/PositionFieldDef.js';
import CoordUtil from '../CoordUtil';
import Point, {isValidPoint} from '../Point';
import {sprintf} from '../../externalSource/sprintf.js';

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
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    const lonStr = valToStr(wp.getLon());
    const latStr = valToStr(wp.getLat());
    const hmsRa = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
    const hmsDec = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());
    const csys = coordToString(wp.getCoordSys());

    const coordStr= `${lonStr}, ${latStr}  or${addNewLIne?'\n':''}   ${hmsRa}, ${hmsDec} ${csys}`;
    if (!wp.objName) return coordStr;
    return wp.resolver ?
        `${wp.objName} - by ${wp.resolver.toString().toUpperCase()} - ${coordStr}` :
        `${wp.objName} - ${coordStr}`;
}

export function formatWorldPtToStringSimple(wp) {
    if (!isValidPoint(wp)) return '';
    if (wp.objName) return wp.objName;
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    const lonStr = valToStr(wp.getLon());
    const latStr = valToStr(wp.getLat());
    const hmsRa = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
    const hmsDec = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());
    const csys = coordToString(wp.getCoordSys());
    return `${lonStr}, ${latStr}  or   ${hmsRa}, ${hmsDec} ${csys}`;
}

export function formatLonLatToString(wp) {
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    return `${valToStr(wp.getLon())} ${valToStr(wp.getLat())}`;
}