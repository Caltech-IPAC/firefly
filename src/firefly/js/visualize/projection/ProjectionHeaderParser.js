/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isUndefined} from 'lodash';
import {DtoR, RtoD, computeDistance} from '../VisUtil.js';
import { GNOMONIC, ORTHOGRAPHIC, NCP, AITOFF, CAR, LINEAR, PLATE,
    ARC, SFL, CEA, TPV, UNSPECIFIED, UNRECOGNIZED } from './Projection.js';
import {findCoordSys, EQUATORIAL_J, EQUATORIAL_B, GALACTIC_JSYS,
    ECLIPTIC_B, SUPERGALACTIC_JSYS, ECLIPTIC_J, NONCELESTIAL} from '../CoordSys.js';
import {MAX_SIP_LENGTH} from './ProjectionUtil.js';
import {makeProjectionNew} from './Projection.js';
import {getHeader, makeHeaderParse, HdrConst} from '../FitsHeaderUtil.js';

const CD1_1_HEADERS= ['CD1_1','CD001001'];
const CD1_2_HEADERS= ['CD1_2','CD001002'];
const CD2_1_HEADERS= ['CD2_1','CD002001'];
const CD2_2_HEADERS= ['CD2_2','CD002002'];


function getHeaderListD(parse, list, def, altWcs) {
	const key= list.find( (i) =>  parse.header[i+altWcs]);
	return key ? parse.getDoubleValue(key+altWcs, def) : def;
}


function getPVArray(parse, idx, altWcs) {
    const retval= [];
    for(let i=0; i<40; i++) {
        retval[i]= parse.getDoubleValue('PV'+idx+'_'+i+altWcs, i===1?1:0);
    }
    return retval;
}

function getSIPArray(parse, rootKey, length, altWcs) {
    let keyword;
    const retAry= [];
    const len= Math.min(length+1, MAX_SIP_LENGTH);
    for (let i = 0; i < len; i++) {
        retAry[i]= [];
        for (let j = 0; j < len; j++) {
            retAry[i][j] = 0.0;
            if (i + j <= length) {
                keyword = rootKey+'_' + i + '_' + j + altWcs;
                retAry[i][j] = parse.getDoubleValue(keyword, 0.0);
            }
        }
    }
    return retAry;

}


function getKeywordArray(parse, keyRoot, start, end, def, altWcs) {
    let idx=0;
    const retval= [];
    for(let i=start; i<=end; i++ ) {
        retval[idx++] = parse.getDoubleValue(keyRoot+i+altWcs, def);
    }
    return retval;
}

const startsWithAny= (s,strAry) => Boolean(strAry.find( (sTest) => s.startsWith(sTest) ));


function getBasicHeaderValues(parse) {

    const bitpix = parse.getIntValue('BITPIX');

    return {
        naxis1: parse.getIntValue('NAXIS1'),
        naxis2: parse.getIntValue('NAXIS2'),
        cdelt2: parse.getDoubleValue('CDELT2', 0),
        bscale: parse.getDoubleValue('BSCALE', 1.0),
        bzero: parse.getDoubleValue('BZERO', 0.0),
        blank_value: bitpix > 0 ? parse.getValue('BLANK','NaN') : NaN, // blank value is only applicable to integer values (BITPIX > 0)
        bitpix,
    };
}


const ORIGIN=   'ORIGIN';
const EXPTIME=  'EXPTIME';
const IMAGEZPT= 'IMAGEZPT';
const AIRMASS=  'AIRMASS';
const EXTINCT=  'EXTINCT';
const PALOMAR_ID=  'Palomar Transient Factory';

const EQ = 0;  // Equatorial
const EC = 1;  // Ecliptic
const GA = 2;  // Galactic
const SGAL = 3;  // Supergalactic



export function parseSpacialHeaderInfo(header, altWcs='', zeroHeader) {

    if (!header) return {};



    const parse= makeHeaderParse(header, altWcs);

    const p= getBasicHeaderValues(parse);
    p.headerType= 'spacial';
    p.axes_reversed = false;


    p.crpix1 = parse.getDoubleValue('CRPIX1'+altWcs, undefined);
    p.crpix2 = parse.getDoubleValue('CRPIX2'+altWcs, undefined);
    p.crval1 = parse.getDoubleValue('CRVAL1'+altWcs, undefined);
    p.crval2 = parse.getDoubleValue('CRVAL2'+altWcs, undefined);
    p.cdelt1 = parse.getDoubleValue('CDELT1'+altWcs, 0);
    p.cdelt2 = parse.getDoubleValue('CDELT2'+altWcs,0);
    p.cunit1 = parse.getValue('CUNIT1'+altWcs, undefined);
    p.cunit2 = parse.getValue('CUNIT2'+altWcs, undefined);

    p.crota2 = parse.getDoubleValue('CROTA2'+altWcs, undefined);
    const defined_crota2 = isFinite(p.crota2);
    if (!defined_crota2) p.crota2 = 0;


    /**
     * CTYPEi indicates the coordinate type and projection. According to the standard specified by the paper: Representations of world coordinates in FITS
     * E. W. Greisen1 and M. R. Calabretta2 (paper I). The CTYPEn has linear and non-linear.  Non-linear coordinate systems will be signaled by CTYPEi
     * in “4–3” form: the first four characters specify the coordinate type, the fifth character is a ’-’, and the remaining three characters
     * specify an algorithm code for computing the world coordinate value, for example ’ABCD-XYZ’. We explicitly allow the
     * possibility that the coordinate type may augment the algorithm code, for example ’FREQ-F2W’ and ’VRAD-F2W’ may denote
     * somewhat different algorithms (see Paper III). Coordinate types with names of less than four characters are padded on the right
     * with ’-’, and algorithm codes with less than three characters are padded on the right with blanks, for example ’RA---UV ’.
     * However, we encourage the use of three-letter algorithm codes. Particular coordinate types and algorithm codes must be established by convention.
     * CTYPEi values that are not in “4–3” form should be interpreted as linear axes. It is possible that there may be old FITS files with a linear axis for
     * which CTYPEi is, by chance, in 4–3 form. However, it is very unlikely that it will match a recognized algorithm code (use of
     * three-letter codes will reduce the chances). In such a case the axis should be treated as linear.
     *
     */
    const ctype1Trim = parse.getValue('CTYPE1'+altWcs, '').trim();
    const isLinear = ctype1Trim === 'LINEAR'; // special case of linear coordinates
    const ctype1End = isLinear ? '' : ctype1Trim.substring(4,8); //NON-LINEAR: ctypei=cccc-ppp, 0-3 coordinate, 4-7 projection
    if (header['CTYPE1'+altWcs]) {
        p.ctype1 = parse.getValue('CTYPE1'+altWcs, '');
        p.ctype2 = parse.getValue('CTYPE2'+altWcs, '');

        switch (ctype1End) {
            case '-TAN': p.maptype = GNOMONIC; break;
            case '-TPV': p.maptype = TPV; break;
            case '-SIN': p.maptype = ORTHOGRAPHIC; break;
            case '-NCP': p.maptype = NCP; break;
            case '-ARC': p.maptype = ARC; break;
            case '-AIT': p.maptype = AITOFF; break;
            case '-ATF': p.maptype = AITOFF; break;
            case '-CAR': p.maptype = CAR; break;
            case '-CEA': p.maptype = CEA; break;
            case '-SFL': p.maptype = SFL; break;
            case '-GLS': p.maptype = SFL; break;
            case '----':
            case '':     p.maptype = LINEAR; break;
            default :    p.maptype = UNRECOGNIZED;
        }
        if (p.maptype===UNRECOGNIZED && !ctype1Trim.match(/[ -]/)?.[0]) { // catch some non-standard LINEAR projections that go outsize the first 4 pixels
            p.maptype = LINEAR;
        }
        p.axes_reversed = startsWithAny(ctype1Trim, ['DEC','MM','GLAT','LAT','ELAT']);
    }
    else {
        p.maptype = UNSPECIFIED;
    }

    if (header['DSKYGRID']) p.maptype = ORTHOGRAPHIC;


    if (p.maptype===CAR) { // wcs projection routines require crpix1 in -180 to 180 hemisphere
        const halfway = Math.abs(180.0 / p.cdelt1);
        if (p.crpix1 > halfway) p.crpix1 -= 2 * halfway;
        if (p.crpix1 < -halfway) p.crpix1 += 2 * halfway;
    }

    p.cd1_1= getHeaderListD(parse, CD1_1_HEADERS, p.cdelt1??0, altWcs);
    p.cd1_2= getHeaderListD(parse, CD1_2_HEADERS, 0, altWcs);
    p.cd2_1= getHeaderListD(parse, CD2_1_HEADERS, 0, altWcs);
    p.cd2_2= getHeaderListD(parse, CD2_2_HEADERS, p.cdelt2??0, altWcs);

    const defined_cd =
        parse.isDefinedHeaderList(CD1_1_HEADERS) ||
        parse.isDefinedHeaderList(CD1_2_HEADERS) ||
        parse.isDefinedHeaderList(CD2_1_HEADERS) ||
        parse.isDefinedHeaderList(CD2_2_HEADERS);


    p.pc1_1 = parse.getDoubleValue('PC1_1'+altWcs, undefined);
    p.pc1_2 = parse.getDoubleValue('PC1_2'+altWcs, undefined);
    p.pc2_1 = parse.getDoubleValue('PC2_1'+altWcs, undefined);
    p.pc2_2 = parse.getDoubleValue('PC2_2'+altWcs, undefined);

    const defined_pc = isFinite(p.pc1_1) || isFinite(p.pc1_2) || isFinite(p.pc2_1) || isFinite(p.pc2_2);

    if (!defined_cd && defined_pc) {
        /* no CD matrix values in header - look for PC matrix values */
        if (isFinite(p.pc1_1) ) p.cd1_1 = p.cdelt1 * p.pc1_1;
        if (isFinite(p.pc1_2) ) p.cd1_2 = p.cdelt1 * p.pc1_2;
        if (isFinite(p.pc2_1) ) p.cd2_1 = p.cdelt2 * p.pc2_1;
        if (isFinite(p.pc2_2) ) p.cd2_2 = p.cdelt2 * p.pc2_2;
    }

    if (p.maptype===TPV) {
        p.pv1= getPVArray(parse,1, altWcs);
        p.pv2= getPVArray(parse,2, altWcs);
    }


    p.datamax = parse.getDoubleValue('DATAMAX', NaN);
    p.datamin = parse.getDoubleValue('DATAMIN', NaN);

    p.origin = parse.getValue(ORIGIN, '');

    if (p.origin.startsWith(PALOMAR_ID)) {
        p.exptime = parse.getDoubleValue(EXPTIME, 0);
        p.imagezpt = parse.getDoubleValue(IMAGEZPT, 0);
        p.airmass = parse.getDoubleValue(AIRMASS, 0);
        p.extinct = parse.getDoubleValue(EXTINCT, 0);
    }



    p.file_equinox = parse.getDoubleValue('EQUINOX'+altWcs, 0.0) || parse.getDoubleValue('EPOCH', 2000.0);
    p.radecsys = parse.getValue('RADECSYS', '') || parse.getValue('RADESYS'+altWcs, '');
    p.imageCoordSys= findCoordSys(getJsys(p), p.file_equinox);

    // for celestial coordinate systems cunit must be degree
    if (p.imageCoordSys.isCelestial()) {
        if (!p.cunit1) p.cunit1 = 'degree';
        if (!p.cunit2) p.cunit2 = 'degree';
    }


    if ((parse.getValue('TELESCOP','').startsWith('ISO'))) { // ISO images have bad CD matrix - try not to use it
        if ( (p.cdelt1) && (p.cdelt2) ) p.cd1_1 = undefined;
    }

    if (!isNaN(p.crval2) && !isNaN(p.crval1) && !isNaN(p.crpix1) && !isNaN(p.crpix2) && (p.maptype !== UNRECOGNIZED) &&
        ( defined_cd || defined_pc) ) {
        if (p.axes_reversed) {
            let temp = p.crval1;
            p.crval1 = p.crval2;
            p.crval2 = temp;

            temp = p.cd2_2;
            p.cd2_2 = p.cd1_2;
            p.cd1_2 = p.cd1_1;
            p.cd1_1 = p.cd2_1;
            p.cd2_1 = temp;
        }
        // save values for Greisen's formulas
        p.using_cd = true;
        // invert matrix
        const determinant = p.cd1_1 * p.cd2_2 - p.cd1_2 * p.cd2_1;
        p.dc1_1 = p.cd2_2 / determinant;
        p.dc1_2 = - p.cd1_2 / determinant;
        p.dc2_1 = - p.cd2_1 / determinant;
        p.dc2_2 = p.cd1_1 / determinant;

        const twist = Math.atan2(-p.cd1_2, p.cd2_2);
        p.crota2 = twist * RtoD;
    }
    else {
        if (p.axes_reversed) {
            let temp = p.crval1;
            p.crval1 = p.crval2;
            p.crval2 = temp;

            temp = p.cdelt1;
            p.cdelt1 = p.cdelt2;
            p.cdelt2 = temp;
            /* don't know what to do with twist */
            /* will have to wait until I have a sample image */
        }
    }

    /* now do Spitzer distortion corrections */
    if (ctype1Trim.endsWith('-SIP')) {
        p.map_distortion = true;

        p.a_order = parse.getIntValue('A_ORDER'+altWcs);
        if (p.a_order>= 0) p.a= getSIPArray(parse, 'A', p.a_order,altWcs);

        p.b_order = parse.getIntValue('B_ORDER'+altWcs);
        if (p.b_order>= 0) p.b= getSIPArray(parse, 'B', p.b_order,altWcs);

        p.ap_order = parse.getIntValue('AP_ORDER');
        if (p.ap_order>= 0)p.ap= getSIPArray(parse, 'AP', p.ap_order,altWcs);

        p.bp_order = parse.getIntValue('BP_ORDER');
        if (p.bp_order>= 0) p.bp= getSIPArray(parse, 'BP', p.bp_order,altWcs);
    }



    if (p.using_cd) { // need an approximation of cdelt1 and cdelt2
        const proj= makeProjectionNew(p, p.imageCoordSys);
        const proj_center = proj.getWorldCoords(p.crpix1 - 1, p.crpix2 - 1);
        const one_to_right = proj.getWorldCoords(p.crpix1, p.crpix2 - 1);
        const one_up = proj.getWorldCoords(p.crpix1 - 1, p.crpix2);
        if (proj_center && one_to_right && one_up) {
            p.cdelt1 = -computeDistance(proj_center, one_to_right);
            p.cdelt2 = computeDistance(proj_center, one_up);
        }
        else {
            p.cdelt1 = 0;
            p.cdelt2 = 0;
        }
    }

    /* now do Digital Sky Survey plate solution coefficients */
    if  (header['PLTRAH'+altWcs] && !header['CTYPE1'+altWcs]) {
        p.maptype = PLATE;
        // p.imageCoordSys= findCoordSys( getJsys(p), p.file_equinox);
        p.rah = parse.getDoubleValue('PLTRAH'+altWcs,0);
        p.ram = parse.getDoubleValue('PLTRAM'+altWcs,0);
        p.ras = parse.getDoubleValue('PLTRAS'+altWcs,0);
        p.ra_hours = p.rah + (p.ram / 60.0) + (p.ras / 3600.0);
        p.plate_ra = p.ra_hours * 15.0 * DtoR;
        p.decsign = parse.getValue('PLTDECSN'+altWcs,0);
        const dsign= (p.decsign[0]==='-') ? -1. : 1;


        p.decd = parse.getDoubleValue('PLTDECD'+altWcs,0);
        p.decm = parse.getDoubleValue('PLTDECM'+altWcs,0);
        p.decs = parse.getDoubleValue('PLTDECS'+altWcs,0);
        p.dec_deg = dsign * (p.decd+(p.decm/60.0)+(p.decs/3600.0));
        p.plate_dec = p.dec_deg * DtoR;

        p.x_pixel_offset = parse.getDoubleValue( 'CNPIX1'+altWcs,0);
        p.y_pixel_offset = parse.getDoubleValue( 'CNPIX2'+altWcs,0);
        p.plt_scale = parse.getDoubleValue( 'PLTSCALE'+altWcs,0);
        p.x_pixel_size = parse.getDoubleValue( 'XPIXELSZ'+altWcs,0);
        p.y_pixel_size = parse.getDoubleValue( 'YPIXELSZ'+altWcs,0);

        p.ppo_coeff = getKeywordArray(parse, 'PPO',1,6,0,altWcs);
        p.amd_x_coeff = getKeywordArray(parse, 'AMDX',1,20,0,altWcs);
        p.amd_y_coeff =  getKeywordArray(parse, 'AMDY',1,20,0,altWcs);

        p.crpix1 = 0.5 - p.x_pixel_offset;
        p.crpix2 = 0.5 - p.y_pixel_offset;

        if (p.cdelt1===0) {
            p.cdelt1 = - p.plt_scale * p.x_pixel_size / 1000 / 3600;
            p.cdelt2 = p.plt_scale * p.y_pixel_size / 1000 / 3600;
        }
    }


    if (p.maptype===LINEAR && isUndefined(header['CDELT1'+altWcs])) {
        p.cdelt1 = 1;
        p.cdelt2 = 1;

        if (!defined_crota2 && !defined_cd && !defined_pc) {
            p.cd1_1 = 1;
            p.cd2_2 = 1;
            p.using_cd = true;
        }
    }


    if (p.cdelt2<0) { //todo - this assumed the pixels were flipped, determine if we want to keep doing this
        p.cdelt2 = -p.cdelt2;
        p.crpix2 = p.naxis2 - p.crpix2 + 1;
    }



    p.bunit = parse.getValue(HdrConst.BUNIT);
    if (!p.bunit) p.bunit = 'DN';
    p.fluxUnits= getFluxUnits(parse, zeroHeader);


    return p;
}


function getFluxUnits(parse, zeroHeader) {
    let bunit = parse.getValue(HdrConst.BUNIT, 'NONE');
    if (bunit==='NONE') {
        bunit= zeroHeader  ? getHeader(zeroHeader, 'BUNIT', 'DN') : 'DN';
    }
    if (bunit.startsWith('HITS')) return 'frames';

                    //todo- decide if this next line is still correct
    if (parse.getValue('ORIGIN','').startsWith('Palomar Transient Factory')) return 'mag';
    return bunit;
}

function getCoordSys(params) {
    const {ctype1} = params;

    if (!ctype1) return -1;

    /**
     * The accepted celestial-coordinate systems are: the standard equatorial (RA-- and DEC-),
     * and others of the form xLON and xLAT for longitude-latitude pairs, where x is G for Galactic,
     * E for ecliptic, H for helioecliptic and S for supergalactic coordinates.
     *
     * We support equatorial, ecliptic, galactic, and supergalactic coordinate systems.
     */
    const s = ctype1.substring(0, 2);
    switch (s) {
        case 'RA':
        case 'DE':
        case 'LL':  // special case for IRAS Band Mean images, CRTYPE=['LL', 'MM']
            return EQ;
        case 'GL':
        case 'LO':  // special case for IRAS All-Sky, Galactic center, CRTYPE=['LON--ATF', 'LAT--ATF']
            return GA;
        case 'SL':
            return SGAL;
        case 'EL':
            return EC;
        default:
            return -1;  // unrecognized
    }
}



function getJsys(params) {
    let jsys;
    const {radecsys, file_equinox } = params;

    switch (getCoordSys(params)) {
        case EQ:
            if (radecsys.startsWith('FK4')) jsys = EQUATORIAL_B;
            else if (radecsys.startsWith('FK5') || radecsys.startsWith('ICRS')) jsys = EQUATORIAL_J;
            else if (file_equinox < 2000.0) jsys = EQUATORIAL_B;
            else jsys = EQUATORIAL_J;
            break;
        case EC:
            if (radecsys.startsWith('FK4')) jsys = ECLIPTIC_B;
            else if (radecsys.startsWith('FK5')) jsys = ECLIPTIC_J;
            else if (file_equinox < 2000.0) jsys = ECLIPTIC_B;
            else jsys = ECLIPTIC_J;
            break;
        case GA:
            jsys = GALACTIC_JSYS;
            break;
        case SGAL:
            jsys = SUPERGALACTIC_JSYS;
            break;
        default:
            jsys = NONCELESTIAL;
    }
    return jsys;
}




export function makeDirectFileAccessData(header,cubePlane) {

    const parse= makeHeaderParse(header);
    const dataOffset = parse.getIntValue(HdrConst.SPOT_OFF,0)+ parse.getIntValue(HdrConst.SPOT_HS,0);
    const miniHeader= {...getBasicHeaderValues(parse), dataOffset, planeNumber:cubePlane>-1?cubePlane:0};
    miniHeader.bitpix= parse.getValue(HdrConst.SPOT_BP);

    if (parse.getValue(ORIGIN,'').startsWith(PALOMAR_ID)) {
        miniHeader[ORIGIN]= header[ORIGIN];
        miniHeader[EXPTIME]= header[EXPTIME];
        miniHeader[IMAGEZPT]= header[IMAGEZPT];
        miniHeader[AIRMASS]= header[AIRMASS];
        miniHeader[EXTINCT]= header[EXTINCT];
    }
    return miniHeader;
}






