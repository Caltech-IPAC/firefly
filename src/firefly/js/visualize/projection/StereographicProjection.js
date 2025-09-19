/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {DtoR, RtoD, MAX_SIP_LENGTH} from './ProjectionUtil.js';
import {makeProjectionPt, makeImagePt} from '../Point.js';

const StereographicProjection = {
    
    revProject(ra, dec, header) {
        const {crval1, crval2, crpix1, crpix2, cdelt1, cdelt2, map_distortion} = header;
        const {dc1_1, dc1_2, dc2_1, dc2_2, using_cd, crota2: twist} = header;

        // Convert to radians
        const lon = ra * DtoR;
        const lat = dec * DtoR;
        const alpha0 = crval1 * DtoR;  // reference longitude
        const delta0 = crval2 * DtoR;  // reference latitude

        const cos_lat = Math.cos(lat);
        const sin_lat = Math.sin(lat);
        const cos_delta0 = Math.cos(delta0);
        const sin_delta0 = Math.sin(delta0);
        const cos_dlon = Math.cos(lon - alpha0);
        const sin_dlon = Math.sin(lon - alpha0);

        // Calculate angular distance from reference point
        const cos_c = sin_delta0 * sin_lat + cos_delta0 * cos_lat * cos_dlon;

        // Check for points on opposite hemisphere (not visible in stereographic)
        if (cos_c <= 0) {
            return null; // Point not visible
        }

        const k = 2 / (1 + cos_c);

        // Calculate intermediate / projection plane coordinates
        let fsamp = k * cos_lat * sin_dlon;  // xi
        let fline = k * (cos_delta0 * sin_lat - sin_delta0 * cos_lat * cos_dlon);  // eta

        // Linear transformation from projection plane coordinates to pixel coordinates
        if (using_cd) {
            const temp = (dc1_1 * fsamp + dc1_2 * fline) * RtoD;
            fline = (dc2_1 * fsamp + dc2_2 * fline) * RtoD;
            fsamp = temp;
        }
        else {
            // do the twist
            const rtwist = twist * DtoR;       // convert to radians
            const temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
            fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
            fsamp = temp;

            const rpp1 = cdelt1 * DtoR;
            const rpp2 = cdelt2 * DtoR;
            fsamp = (fsamp / rpp1);     // now apply cdelt
            fline = (fline / rpp2);
        }

        // Apply inverse SIP distortion corrections if present
        // Inverse SIP must be applied after the projection math but before converting to final pixel coords
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
        // variables for linear transformation from pixels coordinates to
        // intermediate world (projection plane) coordinates
        let rpp1, rpp2;
        let rtwist, temp;
        let xx, yy;

        // Convert from pixel coordinates to intermediate pixel coordinates

        // historical variable names:
        // fsamp = intermediate x-coordinate (sample direction) 
        // fline = intermediate y-coordinate (line direction)
        let fsamp = x - crpix1 + 1;
        let fline = y - crpix2 + 1;

        // Apply SIP distortion corrections if present
        // SIP must be applied in pixel space, before the linear CD/PC transform and the projection
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
            // cdelt scaling factors are included int CD matrix coefficients
            // both are in units of degrees
            xx = (cd1_1 * fsamp + cd1_2 * fline) * DtoR;
            yy = (cd2_1 * fsamp + cd2_2 * fline) * DtoR;
        }
        else {
            rpp1 = cdelt1 * DtoR;        /* radians per pixel */
            rpp2 = cdelt2 * DtoR;        /* radians per pixel */
            xx = fsamp * rpp1;
            yy = fline * rpp2;

            rtwist = twist * DtoR;       /* convert to radians */
            temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
            yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
            xx = temp;
        }

        // Convert to radians
        const xi_rad = xx;  // intermediate x-coordinate already in radians
        const eta_rad = yy;  // intermediate y-coordinate already in radians
        const alpha0 = crval1 * DtoR;  // reference longitude
        const delta0 = crval2 * DtoR;  // reference latitude

        // Stereographic projection formulas
        const rho = Math.sqrt(xi_rad * xi_rad + eta_rad * eta_rad);
        const c = 2 * Math.atan(rho / 2);

        if (rho === 0) {
            // At the reference point
            return makeProjectionPt(crval1, crval2);
        }

        const cos_c = Math.cos(c);
        const sin_c = Math.sin(c);
        const cos_delta0 = Math.cos(delta0);
        const sin_delta0 = Math.sin(delta0);

        // Calculate latitude
        let lat = Math.asin(cos_c * sin_delta0 + (eta_rad * sin_c * cos_delta0) / rho);

        // Calculate longitude
        let lon = alpha0 + Math.atan2(xi_rad * sin_c,
            rho * cos_delta0 * cos_c - eta_rad * sin_delta0 * sin_c);

        lat = lat * RtoD;
        lon = (360.0 + lon * RtoD) % 360.0; // handle negative value and greater than 360 value

        return makeProjectionPt(lon, lat);
    },

};

export {StereographicProjection};