/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {visRoot} from '../ImagePlotCntlr.js';
import {getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {DataTypes} from '../draw/DrawLayer.js';
import DrawOp from '../draw/DrawOp.js';
import Point from '../Point.js';
import {RegionType, regionPropsList, getRegionDefault, RegionValue, RegionValueUnit} from './Region.js';
import {get, has, isNil, isEmpty} from 'lodash';


const RegionPropertyName = {
    [regionPropsList.COLOR]: 'color',
    [regionPropsList.DASHLIST]: 'dashlist',
    [regionPropsList.TEXT]: 'text',
    [regionPropsList.FONT]: 'font',
    [regionPropsList.PTTYPE]: 'point',
    [regionPropsList.EDIT]: 'edit',
    [regionPropsList.MOVE]: 'move',
    [regionPropsList.ROTATE]: 'rotate',
    [regionPropsList.HIGHLITE]: 'highlite',
    [regionPropsList.SELECT]: 'select',
    [regionPropsList.DELETE]: 'delete',
    [regionPropsList.DASH]: 'dash',
    [regionPropsList.FIXED]: 'fixed',
    [regionPropsList.SOURCE]: 'source',
    [regionPropsList.INCLUDE]: 'include',
    [regionPropsList.LNWIDTH]: 'width',
    [regionPropsList.OFFX]: 'offsetx',
    [regionPropsList.OFFY]: 'offsety',
    [regionPropsList.TEXTLOC]:'textloc'
};

const DS9RegionName = {
    [RegionType.circle.key]: 'circle',
    [RegionType.annulus.key]: 'annulus',
    [RegionType.line.key]: 'line',
    [RegionType.box.key]: 'box',
    [RegionType.boxannulus.key]: 'box',
    [RegionType.ellipse.key]: 'ellipse',
    [RegionType.ellipseannulus.key]: 'ellipse',
    [RegionType.point.key]: 'point',
    [RegionType.polygon.key]: 'polygon',
    [RegionType.text.key]: 'text'
};

var fontObjToStr = (v) => (`"${v.name} ${v.point} ${v.weight} ${v.slant}"` );

/**
 * make region descript array from plot DrawObj of all visible DrawLayer
 * @param plot
 */
export function makeRegionsFromPlot(plot)
{
    var plotId = plot? plot.plotId : get(visRoot(), 'avtivePlotId');
    var dlAry = getAllDrawLayersForPlot(getDlAry(), plotId, true );
    var oneRegionDes;
    var rgDesAry = [];
    /*
    const DS9Ver = '# Region file format: DS9 version 4.1';
    var DS9Global = 'global';
    var propSet = [regionPropsList.COLOR, regionPropsList.DASHLIST, regionPropsList.LNWIDTH, regionPropsList.FONT,
                   regionPropsList.SELECT, regionPropsList.HIGHLITE, regionPropsList.DASH, regionPropsList.FIXED,
                   regionPropsList.EDIT, regionPropsList.MOVE, regionPropsList.DELETE,  regionPropsList.INCLUDE,
                   regionPropsList.ROTATE, regionPropsList.SOURCE];

    propSet.forEach( (prop) => {
       var v = getRegionDefault(prop);

       if (prop === regionPropsList.FONT) {
           v = fontObjToStr(v);
       }
       DS9Global += ` ${RegionPropertyName[prop]}=${v}`;
    });
    */
    //rgDesAry.push(DS9Ver);
    //rgDesAry.push(DS9Global);

    dlAry.forEach( (dl) => {
        var data = ['drawData', DataTypes.DATA];
        var drawObjs = dl.hasPerPlotData ? get(dl, [...data, plotId]) : get(dl, data);


        drawObjs.forEach( (dObj) => {
            oneRegionDes = DrawOp.toRegion(dObj, plot, dl.drawingDef);
            if (!isEmpty(oneRegionDes)) {
                rgDesAry.push(...oneRegionDes);
            }
         } );
    });
    return rgDesAry;

}

/**
 * region description: [<J2000>|image|physical]; [point x y | box x y w h a | ellipse x y r1 r2 a |
 *                                                line x1 y1 x2 y2 | text x y | circle x y r]
 * @param regionType
 * @param cc
 * @param ptAry  position point (x, y) array
 * @param regionDimAry  dimenstion (r1, r2) or (width, height) array
 * @param regionValAry  value array (r1, r2, ...) for annulus
 * @param isAngle       if angle is needed
 * @param angle
 * @returns {*}         region description string
 */

export function startRegionDes(regionType, cc, ptAry,
                               regionDimAry = null,
                               regionValAry = null, isAngle = false,
                               angle = RegionValue(0, RegionValueUnit.DEGREE)) {
    var des;
    var sys;

    var valStr = (val, sys) => {
        var str;

        // sys is w_pt or im_pt
        if (val.unit === RegionValueUnit.DEGREE) {
            str = ` ${val.value}`;
        } else if (val.unit === RegionValueUnit.IMAGE_PIXEL) {
            str = ` ${Math.round(val.value)}`;
        } else if (val.unit === RegionValueUnit.SCREEN_PIXEL) {
            str = ` ${Math.round(val.value/cc.zoomFactor)}`; // change to be on IM_PT coordinate
        }

        if (val.unit === RegionValueUnit.DEGREE && sys !== Point.W_PT) {
            str += 'd';
        } else if ((val.unit === RegionValueUnit.IMAGE_PIXEL || val.unit === RegionValueUnit.SCREEN_PIXEL) &&
                    sys !== Point.IM_PT) {
            str += 'i';
        }

        return str;
    };

    switch(ptAry[0].type) {
        case Point.SPT:
        case Point.VP_PT:
            // convert physical coordinate to image coordinate
            sys = Point.IM_PT;
            des = `image;${DS9RegionName[regionType.key]}`;
            ptAry.forEach((pt) => {
                var imgPt = cc.getImageCoords(pt);

                des += ` ${imgPt.x} ${imgPt.y}`;
            });
            break;

        case Point.IM_PT:
        case Point.IM_WS_PT:
            sys = Point.IM_PT;
            des = `image;${DS9RegionName[regionType.key]}`;
            ptAry.forEach((pt) => {
                des += ` ${pt.x} ${pt.y}`;
            });
            break;

        case Point.W_PT:
            sys = Point.W_PT;
            des = `J2000;${DS9RegionName[regionType.key]}`;
            ptAry.forEach((pt) => {
                var newPt = cc.getWorldCoords(pt);
                des += ` ${newPt.x} ${newPt.y}`;
            });
            break;

        default:
            return '';
    }
    if (regionValAry) {
        regionValAry.forEach ( (val) => {
            des += valStr(val, sys);
        });
    }

    if (regionDimAry) {
        regionDimAry.forEach ( (dim) => {
           des += valStr(dim.width, sys);
           des += valStr(dim.height, sys);
        });
    }

    if (isAngle) {
        des += ` ${angle.value}`;

        if (angle.unit === RegionValueUnit.DEGREE && sys !== Point.W_PT) {
            des += 'd';
        } else if (angle.unit === RegionValueUnit.IMAGE_PIXEL || angle.unit === RegionValueUnit.SCREEN_PIXEL) {
            des += 'r';
        }
    }

    des += ' #';

    return des;
}

/**
 * region property prop=xxx or text={text} or font="xxx"
 * @param prop
 * @param value
 * @returns {string} prop=value
 */
export function setRegionPropertyDes(prop, value) {
    var pname, pval, des;
    var v;

    if (isNil(prop) || isNil(value) || !has(RegionPropertyName, prop)) {
        return '';
    }

    pname = ` ${RegionPropertyName[prop]}=`;
    v = getRegionDefault(prop);
    des = '';

    switch (prop) {
        case regionPropsList.COLOR:
        case regionPropsList.LNWIDTH:
        case regionPropsList.OFFX:
        case regionPropsList.OFFY:
            if (value !== v) {
                des = `${pname}${value}`;
            }
            break;
        case regionPropsList.TEXTLOC:
            if (value !== v) {
                des = `${pname}${value.toUpperCase()}`;
            }
            break;
        case regionPropsList.TEXT:
            des = `${pname}{${value}}`;
            break;
        case regionPropsList.FONT:
            var fontstr = fontObjToStr(v);

            pval =  `"${get(value, 'name', v.name).toLowerCase()} ` +
                    `${get(value, 'size', v.point)} ` +
                    `${get(value, 'weight', v.weight).toLowerCase()} ` +
                    `${get(value, 'style', v.slant)}"`;

            if (pval !== fontstr) {
                des = `${pname}${pval}`;
            }
            break;
        case  regionPropsList.PTTYPE:
            des = `${pname}${get(value, 'pointType', v).toLowerCase()} ${get(value, 'pointSize', '')}`;
            break;
        case regionPropsList.SELECT:
        case regionPropsList.HIGHLITE:
        case regionPropsList.DASH:
        case regionPropsList.FIXED:
        case regionPropsList.EDIT:
        case regionPropsList.MOVE:
        case regionPropsList.DELETE:
        case regionPropsList.INCLUDE:
        case regionPropsList.SOURCE:
        case regionPropsList.ROTATE:
            if (value !== v) {
                des = `${pname}${value ? 1 : 0}`;
            }
            break;
        default:
            des = '';
    }
    return des;
}

export function endRegionDes(des) {
    var rgDes = '';

    if (des) {
        rgDes = des.trim();

        if (rgDes.length > 0 && rgDes[des.length - 1] === '#') {
            rgDes = rgDes.slice(0, -1);
        }
    }
    return rgDes;
}

