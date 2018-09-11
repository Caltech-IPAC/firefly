/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {has} from 'lodash';
import Point, {makeWorldPt, makeImagePt} from '../Point.js';
import {RegionFactory} from '../region/RegionFactory.js';
import {drawRegions} from '../region/RegionDrawer.js';
import VisUtil from '../VisUtil.js';

const PRELIM = 'prelim.';
const NoPRELIM = '';

// SPITZER is not included  06/24/2016
export const FootprintList = ['HST', 'JWST', 'WFIRST', 'SPITZER'];
export const FOOTPRINT = new Enum({HST: NoPRELIM, SPITZER: NoPRELIM, JWST: PRELIM, WFIRST: NoPRELIM});
const JWST_INST = new Enum(['FGS', 'MIRI', 'NIRCAM', 'NIS', 'NIRSPEC']);
const HST_INST = new Enum(['NICMOS', 'WFPC2', 'ACS/WFC', 'ACS/HRC', 'ACS/SBC', 'WFC3/UVIS', 'WFC3/IR']);
const SPITZER_INST = new Enum(['IRAC36', 'IRAC45']);


export const INSTRUMENTS = {
    [FOOTPRINT.HST.key]: HST_INST,
    [FOOTPRINT.SPITZER.key]: SPITZER_INST,
    [FOOTPRINT.JWST.key]: JWST_INST
};


export class FootprintFactory {
    /**
     * get list of instrument string undre mission
     * @param mission
     * @returns {array}
     */
    static getInstruments(mission) {
        var enumFP = FOOTPRINT.enums.find( (fp) => fp.key === mission);
        return (enumFP&&has(INSTRUMENTS, mission)) ? INSTRUMENTS[mission].enums.map((inst) => inst.key) : [];
    }

    /**
     * get command string on the dropdown list given mission string
     * @param mission
     * @returns {string} null for no content
     */
    static footprintDesc(mission ) {
        var label = '';
        var enumFP = FOOTPRINT.enums.find( (fp) => fp.key === mission);

        if (enumFP) {
            label = enumFP.key + (enumFP.value ? ' ' + enumFP.value : '');
        }
        return label;
    }

    static getOriginalRegionsFromStc(defAry, isInstrument, bAllowHeader = false) {
        var regions = RegionFactory.parseRegionDS9(defAry, bAllowHeader);

        regions.forEach( (oneRegion) => Object.assign(oneRegion, {isInstrument}));
        return regions;
    }

    /**
     * get drawObj from region description, move the center from instrument center or worldPt (0, 0) to reference center
     * @param regions
     * @param refCenter
     * @param moveToRelativeCenter
     * @param cc
     * @returns {*}
     */
    static getDrawObjFromOriginalRegion(regions, refCenter, moveToRelativeCenter, cc) {
        const getCenter = (wpAry) => {
            if (wpAry[0].type === Point.W_PT) {
                return VisUtil.computeCentralPointAndRadius(wpAry).centralPoint;
            } else {
                return getImageCenter(wpAry);
            }
        };

        const getImageCenter = (wpAry) => {
            const total = wpAry.reduce((prev, onePt) => {
                prev.totalX += onePt.x;
                prev.totalY += onePt.y;
                return prev;
            }, {totalX: 0.0, totalY : 0.0} );

            return makeImagePt(total.totalX/wpAry.length, total.totalY/wpAry.length);
        };

        var moveRegion = (region, refCenter, moveToRelativeCenter, instCenter) => {
                if (!region.wpAry || region.wpAry.length <= 0 ) return;
                if (region.wpAry[0].type === Point.W_PT) {
                    const centerWpt = moveToRelativeCenter && instCenter ? instCenter : makeWorldPt(0, 0);
                    region.wpAry = region.wpAry.map((wp) => VisUtil.getTranslateAndRotatePosition(centerWpt, refCenter, wp));
                } else {
                    // move center of footprint or image pt (0,0) to refCenter
                    const refCenterImg = cc.getImageCoords(refCenter);
                    const centerImgPt = moveToRelativeCenter && instCenter ? instCenter : makeImagePt(0, 0);
                    const deltaX = refCenterImg.x - centerImgPt.x;
                    const deltaY = refCenterImg.y - centerImgPt.y;

                    region.wpAry = region.wpAry.map((wp) => {
                        const newPt = Object.assign({}, wp);
                        newPt.x += deltaX;
                        newPt.y += deltaY;

                        return newPt;
                    });
                }
        };

        if (regions) {
            const newRegions = regions.map( (oneRegion) => Object.assign({}, oneRegion));
            let   instCenter = null;

            if (moveToRelativeCenter) {
                const vertices = regions.reduce((prev, oneRegion) => {
                    prev = prev.concat(oneRegion.wpAry);
                    return prev;
                }, []);

                instCenter = getCenter(vertices);
            }
            newRegions.forEach( (oneRegion) => moveRegion(oneRegion, refCenter, moveToRelativeCenter, instCenter));

            return drawRegions(newRegions);
        } else {
            return [];
        }
    }
}

