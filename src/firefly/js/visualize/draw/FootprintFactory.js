/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {has} from 'lodash';
import {makeWorldPt} from '../Point.js';
import {RegionFactory} from '../region/RegionFactory.js';
import {drawRegions} from '../region/RegionDrawer.js';
import VisUtil from '../VisUtil.js';

const PRELIM = 'prelim.';
const NoPRELIM = '';
const FPCMD = 'footprint';

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
    static footprintCommand( mission ) {
        var label = '';
        var enumFP = FOOTPRINT.enums.find( (fp) => fp.key === mission);

        if (enumFP) {
            label = `${enumFP.key}` + (enumFP.value === PRELIM ? ' ' : '') + `${enumFP.value} ${FPCMD}`;
        }
        return label;
    }

    /**
     * get command string on the dropdown given mission and instrument string
     * @param mission
     * @param inst
     * @returns {string} null for no content
     */
    static instrumentCommand( mission, inst) {
        var label = '';
        var enumFP = FOOTPRINT.enums.find( (fp) => fp.key === mission);

        if (!enumFP) return label;

        if (has(INSTRUMENTS, mission)) {
           if (INSTRUMENTS[mission].get(inst)) {
               label = `${enumFP.key} ${inst}` + (enumFP.value === PRELIM ? ' ' : '') + `${enumFP.value} ${FPCMD}`;
           }
        }
        return label;
    }

    static getOriginalRegionsFromStc(defAry, isInstrument) {
        var regions = RegionFactory.parseRegionDS9(defAry, false);

        regions.forEach( (oneRegion) => Object.assign(oneRegion, {isInstrument}));
        return regions;
    }

    static getDrawObjFromOriginalRegion(regions, refCenter, moveToRelativeCenter) {
        var getCenter = (wpAry) => VisUtil.computeCentralPointAndRadius(wpAry).centralPoint;
        var moveRegion = (region, refCenter, moveToRelativeCenter, instCenter) => {
                    var centerWpt = moveToRelativeCenter&&instCenter? instCenter : makeWorldPt(0, 0);

                    region.wpAry = region.wpAry.map((wp) => VisUtil.getTranslateAndRotatePosition(centerWpt, refCenter, wp));
        };

        if (regions) {
            var newRegions = regions.map( (oneRegion) => Object.assign({}, oneRegion));
            var instCenter = null;

            if (moveToRelativeCenter) {
                var vertices = regions.reduce((prev, oneRegion) => {
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

