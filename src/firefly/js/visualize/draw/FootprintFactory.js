/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {has} from 'lodash';
import Point, {makeWorldPt, makeImagePt} from '../Point.js';
import {RegionFactory} from '../region/RegionFactory.js';
import {drawRegions} from '../region/RegionDrawer.js';
import {computeCentralPointAndRadius, getTranslateAndRotatePosition} from '../VisUtil.js';

const PRELIM = 'prelim.';
const NoPRELIM = '';

// SPITZER is not included  06/24/2016
export const FootprintList = ['Spitzer', 'SOFIA', 'HST', 'JWST', 'Roman'];
export const FOOTPRINT = new Enum({HST: NoPRELIM, Spitzer: NoPRELIM, JWST: NoPRELIM, Roman: NoPRELIM, SOFIA:NoPRELIM});
const JWST_INST = new Enum(['FGS', 'MIRI', 'NIRCAM', 'NIRISS', 'NIRSPEC']);
const HST_INST = new Enum(['NICMOS', 'WFPC2', 'ACS/WFC', 'ACS/HRC', 'ACS/SBC', 'WFC3/UVIS', 'WFC3/IR']);
const SPITZER_INST = new Enum(['IRAC36', 'IRAC45']);

//To add a new  Sofia instrument, add it in SOFIA_INST.  If the new instrument has submenu, create a menu list like
// SOFIA_INST_FORCAST_TEXT and then add it to SOFIA_INSTRUMENTS
const SOFIA_INST = new Enum(['FIFI-LS', 'FLITECAM', 'FORCAST', 'FPI+', 'HAWC+']);
export const SOFIA_INST_FORCAST_TEXT = new Enum({FORCAST_IMG:'FORCAST Imaging',
    FORCAST_GRISMS_A: 'FORCAST Grism a',FORCAST_GRISMS_B:'FORCAST Grism b' });
export const SOFIA_INST_FIFILS_TEXT = new Enum({'FIFI-LS_Blue':'FIFI-LS Blue (50-120 microns)', 'FIFI-LS_Red':'FIFI-LS Red (110-200 microns)'    });
export const SOFIA_INST_FLITECAM_TEXT = new Enum({FLITECAM_IMG:'FLITECAM Imaging',  FLITECAM_GRISMS_ABBA: 'FLITECAM Grism ABBA',
    FLITECAM_GRISMS_AB: 'FLITECAM Grism AB'});
export const SOFIA_INST_HAWC_TEXT = new Enum({
    HAWC_BAND_A_TOTAL: '53 microns (Band A), Total Intensity',
    HAWC_BAND_A_POLAR: '53 microns (Band A), Polarization',
    HAWC_BAND_C_TOTAL: '89 microns (Band C), Total Intensity',
    HAWC_BAND_C_POLAR: '89 microns (Band C), Polarization',
    HAWC_BAND_D_TOTAL: '154 microns (Band D), Total Intensity',
    HAWC_BAND_D_POLAR: '154 microns (Band D), Polarization',
    HAWC_BAND_E_TOTAL: '214 microns (Band E), Total Intensity',
    HAWC_BAND_E_POLAR: '214 microns (Band E), Polarization'
});



export const SOFIA_INSTRUMENTS = {
    'FIFI-LS': SOFIA_INST_FIFILS_TEXT,
    FLITECAM: SOFIA_INST_FLITECAM_TEXT,
    FORCAST: SOFIA_INST_FORCAST_TEXT,
    'HAWC+':SOFIA_INST_HAWC_TEXT
};

export const INSTRUMENTS = {
    [FOOTPRINT.HST.key]: HST_INST,
    [FOOTPRINT.Spitzer.key]: SPITZER_INST,
    [FOOTPRINT.JWST.key]: JWST_INST,
    [FOOTPRINT.SOFIA.key]:SOFIA_INST
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
        var regions = RegionFactory.parseRegionDS9(defAry, bAllowHeader, true);

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
                return computeCentralPointAndRadius(wpAry).centralPoint;
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
                    region.wpAry = region.wpAry.map((wp) => getTranslateAndRotatePosition(centerWpt, refCenter, wp));
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

