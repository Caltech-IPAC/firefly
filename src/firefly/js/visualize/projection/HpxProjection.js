/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {DtoR, RtoD, MAX_SIP_LENGTH, celestialToNative, nativeToCelestial} from './ProjectionUtil.js';
// import {nativeToCelestial1} from './ProjectionUtilAlt.js';
import {makeProjectionPt, makeImagePt} from '../Point.js';


/**
 * Get H parameter (number of facets around the equator) from header
 * PV2_1 = header.pv2[0], default 4
 * @param header
 * @returns {*|number}
 */
function getH(header) {
    return header.pv2?.[0] || 4.0;
}

/**
 * Get K parameter (vertical scaling) from header
 * PV2_2 = header.pv2[1], default 3
 * @param header
 * @returns {*|number}
 */
function getK(header) {
    return header.pv2?.[1] || 3.0;
}

/**
 * The HPX projection is defined in:
 *   Calabretta, M. R. & Roukema, B. F. (2007),
 *   "Mapping on the HEALPix grid".
 *
 * The reference C implementation is in WCSLIB (prj.c):
 *   hpxset(), hpxs2x() [sphere to plane], and hpxx2s() [plane to sphere].
 * @type {{revProject(*, *, *): (Array|null), fwdProject(*, *, *): (Array|null)}}
 */
const HpxProjection = {

    revProject(ra, dec, header) {
        // crval2 (reference latitude) is not applicable
        const {crval1, crval2, crpix1, crpix2, cdelt1, cdelt2, map_distortion} = header;
        const {dc1_1, dc1_2, dc2_1, dc2_2, using_cd, crota2: twist} = header;

        // Get H and K parameters
        const H = getH(header);
        const K = getK(header);

        // HPX projection formulas
        const result = celestialToNative(ra, dec, crval1, crval2);
        if (result === null) return result;
        const theta = result[1] * DtoR;
        let phi = result[0] * DtoR;

        // Normalize phi to [-pi, pi]
        while (phi > Math.PI) phi -= 2.0 * Math.PI;
        while (phi < -Math.PI) phi += 2.0 * Math.PI;

        const abs_theta = Math.abs(theta);

        let fsamp, fline;

        // START: HPX projection implementation
        // HPX forward formulas match hpxset() and hpxs2x() in wcslib

        // Transition latitude depends on K
        const theta_transition = Math.asin((K - 1.0) / K);

        // Check which region we're in
        if (abs_theta <= theta_transition) {
            // Equatorial region
            // Output in degrees to match the paper and wcslib
            // and for numerical precision: degrees avoid very small numbers near the reference point
            fsamp = phi * RtoD;   // (15)
            fline = (90.0 * K / H) * Math.sin(theta);   // (16)
        } else {
            // Polar regions (matches wcslib implementation)
            const sigma = Math.sqrt(K * (1.0 - Math.abs(Math.sin(theta))));  // (19)
            const sign_theta = (theta >= 0) ? 1 : -1;

            // facet center - phi_c for K odd or theta > 0
            // Work in degrees
            const phi_deg = phi * RtoD;
            // phi_c for K odd or theta > 0, eq. (20)
            const phi_c = -180.0 + (2 * Math.floor((phi_deg + 180.0) * H / 360.0) + 1.0) * 180.0 / H;

            // (phi - phi_c) in degrees
            let phi_minus_phic = phi_deg - phi_c;

            // Apply offset adjustment for southern polar half-facets when K is even
            const odd_k = (K % 2) === 1;
            const odd_h = (H % 2) === 1;
            const offset = (odd_k || theta > 0.0) ? 0 : 1;

            if (offset) {
                // Offset the southern polar half-facets for even K
                const facet_width = 180.0 / H;
                const h = Math.floor(phi_deg / facet_width) + (odd_h ? 1 : 0);
                if (h % 2) {
                    phi_minus_phic -= facet_width;
                } else {
                    phi_minus_phic += facet_width;
                }
            }

            // Compute final coordinates
            // x = phi_c + (phi - phi_c) * sigma = phi + (phi - phi_c) * (sigma - 1)  // (17)
            const xi = sigma - 1.0;
            fsamp = phi_deg + phi_minus_phic * xi;
            fline = sign_theta * (180.0 / H) * ((K + 1)/2 - sigma);  // (18)

            // Put the phi = 180 meridian in the expected place
            if (fsamp > 180.0) fsamp = 360.0 - fsamp;
        }

        // END: HPX projection implementation
        // fsamp and fline are now in degrees (matching wcslib output)

        // Linear transformation from projection plane coordinates to pixel coordinates
        if (using_cd) {
            // CD matrix expects degrees, outputs degrees
            const temp = dc1_1 * fsamp + dc1_2 * fline;
            fline = dc2_1 * fsamp + dc2_2 * fline;
            fsamp = temp;
        } else {
            // CDELT expects degrees
            const rtwist = twist * DtoR;
            const temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
            fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
            fsamp = temp;

            // cdelt is in degrees/pixel, so divide degrees by degrees/pixel to get pixels
            fsamp = fsamp / cdelt1;
            fline = fline / cdelt2;
        }

        // Apply inverse SIP distortion corrections if present
        if (map_distortion) {
            let fsamp_correction = 0.0;
            let len = Math.floor(Math.min(header.ap_order + 1, MAX_SIP_LENGTH));
            for (let i = 0; i < len; i++) {
                for (let j = 0; j < len; j++) {
                    if (i + j <= header.ap_order) {
                        fsamp_correction += header.ap[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }

            let fline_correction = 0.0;
            len = Math.floor(Math.min(header.bp_order + 1, MAX_SIP_LENGTH));
            for (let i = 0; i < len; i++) {
                for (let j = 0; j < len; j++) {
                    if (i + j <= header.bp_order) {
                        fline_correction += header.bp[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }
            fsamp += fsamp_correction;
            fline += fline_correction;
        }

        const x = fsamp + crpix1 - 1;
        const y = fline + crpix2 - 1;

        return makeImagePt(x, y);
    },


    
    fwdProject(x, y, header) {
        const {crval1, crval2, crpix1, crpix2, cdelt1, cdelt2, map_distortion} = header;
        const {cd1_1, cd1_2, cd2_1, cd2_2, using_cd, crota2: twist} = header;

        let rtwist, temp;
        let xx, yy;

        // Convert from pixel coordinates to intermediate pixel coordinates
        let fsamp = x - crpix1 + 1;
        let fline = y - crpix2 + 1;

        // Apply SIP distortion corrections if present
        if (map_distortion) {
            let fsamp_correction = 0.0;
            let len = Math.floor(Math.min(header.a_order + 1, MAX_SIP_LENGTH));
            for (let i = 0; i < len; i++) {
                for (let j = 0; j < len; j++) {
                    if (i + j <= header.a_order) {
                        fsamp_correction += header.a[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }

            let fline_correction = 0.0;
            len = Math.floor(Math.min(header.b_order + 1, MAX_SIP_LENGTH));
            for (let i = 0; i < len; i++) {
                for (let j = 0; j < len; j++) {
                    if (i + j <= header.b_order) {
                        fline_correction += header.b[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }
            fsamp += fsamp_correction;
            fline += fline_correction;
        }

        // Linear transformation to intermediate world (projection plane) coordinates
        if (using_cd) {
            // CD matrix: pixels to degrees
            xx = cd1_1 * fsamp + cd1_2 * fline;
            yy = cd2_1 * fsamp + cd2_2 * fline;
        } else {
            // cdelt is in degrees/pixel, so multiply pixels by degrees/pixel to get degrees
            xx = fsamp * cdelt1;
            yy = fline * cdelt2;

            rtwist = twist * DtoR;
            temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist);
            yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
            xx = temp;
        }
        // xx and yy are now in degrees (matching wcslib input)

        // START: HPX projection implementation
        // HPX inverse formulas match hpxset(), hpxx2s() in wcslib

        // Get H and K parameters
        const H = getH(header);
        const K = getK(header);

        const x_proj = xx;  // degrees
        const y_proj = yy;  // degrees

        const abs_y = Math.abs(y_proj);

        let theta, phi;

        // Transition value
        const y_transition = 90.0 * (K - 1) / H;

        // Determine region and compute (phi, theta)
        if (abs_y <= y_transition) {
            // Equatorial region
            phi = x_proj;  // (22), degrees
            theta = Math.asin(y_proj / (90.0 * K / H)) * RtoD;  // (23), degrees
        } else {
            // Polar regions (matches wcslib implementation)
            const sign_y = (y_proj >= 0) ? 1 : -1;
            const sigma =  (K + 1) / 2.0 - abs_y / (180.0 / H);

            const odd_k = (K % 2) === 1;
            const odd_h = (H % 2) === 1;
            // offset = 0 for K odd or y > 0, otherwise 1 (for southern polar half-facets with even K)
            const offset = (odd_k || y_proj > 0.0) ? 0 : 1;

            // x_c for K odd or theta > 0, eq. (27)
            const x_c = -180.0 + (2 * Math.floor((x_proj + 180.0) * H / 360.0) + 1.0) * 180.0 / H;

            // (x - x_c) in degrees
            let x_minus_xc = x_proj - x_c;

            // Apply offset adjustment for southern polar half-facets when K is even
            if (offset) {
                const facet_width = 180.0 / H;
                const h = Math.floor(x_proj / facet_width) + (odd_h ? 1 : 0);
                if (h % 2) {
                    x_minus_xc -= facet_width;
                } else {
                    x_minus_xc += facet_width;
                }
            }

            // Compute phi using the adjusted (x - x_c), eq. (24)
            const r = (1.0/sigma) * x_minus_xc;
            phi = x_proj + (r !== 0.0 ? r - x_minus_xc : 0.0);  // degrees
            theta = sign_y * Math.asin(1 - sigma*sigma/K) * RtoD;  // (25), degrees
        }

        // END: HPX projection implementation
        // phi and theta are now in degrees (native coordinates)

        // Convert to celestial coordinates
        // const result = nativeToCelestial(phi, theta, crval1, crval2);
        const result = nativeToCelestial(phi, theta, crval1, crval2);
        if (result === null) return result;
        const [lon, lat] = result;

        return makeProjectionPt(lon, lat);
    },

};

export {HpxProjection};
