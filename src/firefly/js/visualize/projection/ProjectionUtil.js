/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

export const MAX_SIP_LENGTH = 10;
export const WCSTRIG_TOL= 1e-10;
export const DtoR= Math.PI/180.0;
export const RtoD= 180.0/Math.PI;

export const cosd= (angle) => Math.cos(angle*DtoR);
export const sind = (angle) => Math.sin(angle*DtoR );
export const tand= (angle) => Math.tan(angle*DtoR);

export function atand(v) {
    if (v===-1.0) return -45.0;
    else if (v===0.0) return 0.0;
    else if (v===1.0) return 45.0;
    return Math.atan(v)*RtoD;
}

export function acosd(v) {
    if (v >= 1.0) {
        if (v-1.0 <  WCSTRIG_TOL) return 0.0;
    } else if (v===0) {
        return 90.0;
    } else if (v <= -1.0) {
        if (v+1.0 > -WCSTRIG_TOL) return 180.0;
    }
    return Math.acos(v)*RtoD;
}

export function asind(v) {
    if (v <= -1.0) {
        if (v+1.0 > -WCSTRIG_TOL) return -90.0;
    } else if (v===0) {
        return 0.0;
    } else if (v >= 1.0) {
        if (v-1.0 <  WCSTRIG_TOL) return 90.0;
    }
    return Math.asin(v)*RtoD;
}

export function atan2d(y, x) {
    if (y===0) {
        if (x >= 0) {
            return 0;
        } else if (x < 0.0) {
            return 180.0;
        }
    } else if (x===0) {
        if (y > 0.0) {
            return 90.0;
        } else if (y < 0) {
            return -90.0;
        }
    }
    return Math.atan2(y,x)*RtoD;
}

/**
 *  compute the distance between two positions (lon1, lat1)
 *  and (lon2, lat2), the lon and lat are in decimal degrees.
 *  the unit of the distance is degree
 */
export function computeDistance(lon1, lat1, lon2, lat2) {
    const lon1Radians= lon1 * DtoR;
    const lat1Radians= lat1 * DtoR;
    const lon2Radians= lon2 * DtoR;
    const lat2Radians= lat2 * DtoR;
    var cosine = Math.cos(lat1Radians)*Math.cos(lat2Radians)*
        Math.cos(lon1Radians-lon2Radians)
        + Math.sin(lat1Radians)*Math.sin(lat2Radians);

    if (Math.abs(cosine) > 1.0) cosine = cosine/Math.abs(cosine);
    return RtoD*Math.acos(cosine);
}

/**
 * 
 * @param celref - the contents of this array is modified, todo - in the future this should be part of the return
 * @param euler - the contents of this array is modified, todo - in the future this should be part of the return
 * @param useProjException
 * @return {boolean}
 */
export function celset(celref,euler,useProjException) {
    const tol = 1.0e-10;
    var latp;

    // Compute celestial coordinates of the native pole.
    // Reference point away from the native pole.
    // Set default for longitude of the celestial pole.
    if (celref[1] < 0.0) celref[2] = 180.0;
    else celref[2] = 0.0;


    const clat0 = cosd(celref[1]);
    const slat0 = sind(celref[1]);
    const cphip = cosd(celref[2]);
    const sphip = sind(celref[2]);
    const cthe0 = 1.0;
    const sthe0 = 0.0;

    var x = cthe0*cphip;
    var y = sthe0;
    var z = Math.sqrt(x*x + y*y);
    if (z == 0.0) {
        if (slat0 != 0.0) {
            if (useProjException) throw new Error('failure in projection');
            else return false;
        }
        // latp determined by LATPOLE in this case.
        latp = celref[3];
    } else {
        if (Math.abs(slat0/z) > 1.0) {
            if (useProjException) throw new Error('failure in projection');
            else return false;
        }

        const u = atan2d(y,x);
        const v = acosd(slat0/z);

        var latp1 = u + v;
        if (latp1 > 180.0) {
            latp1 -= 360.0;
        } else if (latp1 < -180.0) {
            latp1 += 360.0;
        }

        var latp2 = u - v;
        if (latp2 > 180.0) {
            latp2 -= 360.0;
        } else if (latp2 < -180.0) {
            latp2 += 360.0;
        }

        if (Math.abs(celref[3]-latp1) < Math.abs(celref[3]-latp2)) {
            if (Math.abs(latp1) < 90.0+tol) {
                latp = latp1;
            } else {
                latp = latp2;
            }
        } else {
            if (Math.abs(latp2) < 90.0+tol) {
                latp = latp2;
            } else {
                latp = latp1;
            }
        }

        celref[3] = latp;
    }

    euler[1] = 90.0 - latp;

    z = cosd(latp)*clat0;
    if (Math.abs(z) < tol) {
        if (Math.abs(clat0) < tol) {
            // Celestial pole at the reference point.
            euler[0] = celref[0];
            euler[1] = 90.0;
        } else if (latp > 0.0) {
            // Celestial pole at the native north pole.
            euler[0] = celref[0] + celref[2] - 180.0;
            euler[1] = 0.0;
        } else if (latp < 0.0) {
            // Celestial pole at the native south pole.
            euler[0] = celref[0] - celref[2];
            euler[1] = 180.0;
        }
    } else {
        x = (sthe0 - sind(latp)*slat0)/z;
        y =  sphip*cthe0/clat0;
        if (x == 0.0 && y == 0.0) {
            if (useProjException) throw new Error('failure in projection');
            else return false;
        }
        euler[0] = celref[0] - atan2d(y,x);
    }

    euler[2] = celref[2];
    euler[3] = cosd(euler[1]);
    euler[4] = sind(euler[1]);

    // Check for ill-conditioned parameters.
    if (Math.abs(latp) > 90.0+tol) {
        if (useProjException) throw new Error('ill-conditioned parameters in projection');
        else return false;

    }
    return true;
}


/**
 *
 * @param lng
 * @param lat
 * @param euler
 * @return {Array}
 */
export function sphfwd(lng, lat, euler) {
    const tol = 1.0e-5;
    var theta;
    var result = [];
    var dphi, x, y, z;

    const coslat = cosd(lat);
    const sinlat = sind(lat);

    const dlng = lng - euler[0];
    const coslng = cosd(dlng);
    const sinlng = sind(dlng);

    /* Compute native coordinates. */
    x = sinlat*euler[4] - coslat*euler[3]*coslng;
    if (Math.abs(x) < tol) {
        /* Rearrange formula to reduce roundoff errors. */
        x = -cosd(lat+euler[1]) + coslat*euler[3]*(1.0 - coslng);
    }
    y = -coslat*sinlng;
    if (x != 0.0 || y != 0.0) {
        dphi = atan2d(y, x);
    } else {
        // Change of origin of longitude.
        dphi = dlng - 180.0;
    }
    var phi = euler[2] + dphi;

    // Normalize.
    if (phi > 180.0) {
        phi -= 360.0;
    } else if (phi < -180.0) {
        phi += 360.0;
    }

    if (dlng % 180.0 == 0.0) {
        theta = lat + coslng*euler[1];
        if (theta >  90.0) theta =  180.0 - theta;
        if (theta < -90.0) theta = -180.0 - theta;
    } else {
        z = sinlat*euler[3] + coslat*euler[4]*coslng;
        if (Math.abs(z) > 0.99) {
            // Use an alternative formula for greater numerical accuracy.
            theta = acosd(Math.sqrt(x*x+y*y));
            if (z < 0.0) theta = -Math.abs(theta);
            else theta = Math.abs(theta);
        } else {
            theta = asind(z);
        }
    }

    result[0] = phi;
    result[1] = theta;
    return result;
}


/**
 * 
 * @param phi
 * @param theta
 * @param euler
 * @return {Array}
 */
export function sphrev(phi, theta, euler) {
    const tol = 1.0e-5;
    var lng, lat;
    var retval= [];
    var dlng;

    const costhe = cosd(theta);
    const sinthe = sind(theta);

    const dphi = phi - euler[2];
    const cosphi = cosd(dphi);
    const sinphi = sind(dphi);

    // Compute celestial coordinates. 
    var x = sinthe*euler[4] - costhe*euler[3]*cosphi;
    if (Math.abs(x) < tol) {
        // Rearrange formula to reduce roundoff errors.
        x = -cosd(theta+euler[1]) + costhe*euler[3]*(1.0 - cosphi);
    }
    const y = -costhe*sinphi;
    if (x !== 0.0 || y !==0.0) {
        dlng = atan2d(y, x);
    } else {
        // Change of origin of longitude. 
        dlng = dphi + 180.0;
    }
    lng = euler[0] + dlng;

    // Normalize the celestial longitude. 
    if (euler[0] >= 0.0) {
        if (lng < 0.0) lng += 360.0;
    } else {
        if (lng > 0.0) lng -= 360.0;
    }

    // Normalize.
    if (lng > 360.0) {
        lng -= 360.0;
    } else if (lng < -360.0) {
        lng += 360.0;
    }

    if (dphi % 180.0===0.0) {
        lat = theta + cosphi*euler[1];
        if (lat >  90.0) lat =  180.0 - lat;
        if (lat < -90.0) lat = -180.0 - lat;
    } else {
        const z = sinthe*euler[3] + costhe*euler[4]*cosphi;
        if (Math.abs(z) > 0.99) {
            /* Use an alternative formula for greater numerical accuracy. */
            lat = acosd(Math.sqrt(x*x+y*y));
            if (z < 0.0) lat = -Math.abs(lat);
            else lat = Math.abs(lat);
        } else {
            lat = asind(z);
        }
    }

    retval[0] = lng;
    retval[1] = lat;
    return retval;
}
