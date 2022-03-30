import {AstroMath} from './astroMath';


export function Projection(proj, lon0, lat0) {
	this.PROJECTION = proj;
	this.ROT = this.tr_oR(lon0, lat0);
}


Projection.PROJ_SIN = 3;	// Orthographic
Projection.PROJ_AITOFF = 6;	// Aitoff Projection

Projection.prototype = {
	
	/**
	 * Computes the projection of 1 point : ra,dec => X,Y
	 * alpha, delta = longitude, latitude
	 */
	project(alpha, delta) {
		const u1 = this.tr_ou(alpha, delta);	// u1[3]
		const u2 = this.tr_uu(u1, this.ROT);	// u2[3]
		const P = this.tr_up(this.PROJECTION, u2);	// P[2] = [X,Y]
		if (!P) {
			return null;
		}

		return { x: -P[0], y: -P[1] };
	},

	/**
	 * Computes the coordinates from a projection point : X,Y => ra,dec
	 * return o = [ ra, dec ]
	 */
	unproject(X,Y) {
		X = -X;
		Y = -Y;
		const u1 = this.tr_pu(this.PROJECTION, X, Y);	// u1[3]
		if (!u1) return null;
		const u2 = this.tr_uu1(u1, this.ROT);	// u2[3]
        if (!u2) return null;
		const o = this.tr_uo(u2);	// o[2]
        if (!o) return null;

		return { ra: o[0], dec: o[1] };
	},

	/**
	 * Compute projections from unit vector
	 * The center of the projection correspond to u = [1, 0, 0)
	 * proj = projection system (integer code like _PROJ_MERCATOR_
	 * u[3] = unit vector
	 * return: an array [x,y] or null
	 */
	tr_up(proj, u) {
		const x = u[0];
		const y = u[1];
		const z = u[2];
		let den;
		let pp;
		let X,Y;

		const r = AstroMath.hypot(x,y);			// r = cos b
		if (r=== 0.0 && z=== 0.0) return null;

		switch(proj) {
			default:
				pp = null;
				break;

			case Projection.PROJ_AITOFF:
				den = Math.sqrt(r*(r+x)/2.0); 		// cos b . cos l/2
				X = Math.sqrt(2.0*r*(r-x));
				den = Math.sqrt((1.0 + den)/2.0); 
				X = X / den;
				Y = z / den;
				if (y < 0.0) X = -X;
				pp = [ X, Y];
				break;

			case Projection.PROJ_SIN:
				if (x >= 0.0) {
					X = y;
					Y = z;
					pp = [ X, Y ];
				} else {
					pp = null;
				}
				break;
	  }
	  return pp;
	},

	/**
	 * Computes Unit vector from a position in projection centered at position (0,0).
	 * proj = projection code
	 * X,Y : coordinates of the point in the projection
	 * returns : the unit vector u[3] or a face number for cube projection. 
	 *           null if the point is outside the limits, or if the projection is unknown.
	 */
	tr_pu( proj, X, Y ) {
		let r,s,x,y,z;

		switch(proj) {
			default:
			return null;

			case Projection.PROJ_AITOFF:
				// Limit is ellipse with axises 
				// a = 2 * sqrt(2) ,  b = sqrt(2)
				// Compute dir l/2, b
				r = X*X/8.e0 + Y*Y/2.e0; 	// 1 - cos b . cos l/2
				if (r > 1.0) {
	  				// Test outside domain */
					return null;
				}
				x = 1.0 - r ;	// cos b . cos l/2
				s = Math.sqrt(1.0 - r/2.0) ;	// sqrt(( 1 + cos b . cos l/2)/2)
				y = X * s / 2.0;
				z = Y * s ;
				// From (l/2,b) to (l,b)
				r = AstroMath.hypot( x, y ) ;	// cos b
				if (r !== 0.0) {
					s = x;
					x = (s*s - y*y) /r;
					y = 2.0 * s * y/r;
				}
				break;

			case Projection.PROJ_SIN:
				// Limit is circle, radius 1
				s = 1.0 - X*X - Y*Y;
				if (s < 0.0) {
					return null;
				}
				x = Math.sqrt(s);
				y = X;
				z = Y;
				break;

	  }
	  return [ x,y,z ];
	},

	/**
	 * Creates the rotation matrix R[3][3] defined as
	 * R[0] (first row) = unit vector towards Zenith
	 * R[1] (second row) = unit vector towards East
	 * R[2] (third row) = unit vector towards North
	 * o[2] original angles
	 * @return rotation matrix
	 */
	tr_oR(lon, lat) {
		const R = new Array(3);
		R[0] = new Array(3);
		R[1] = new Array(3);
		R[2] = new Array(3);
		R[2][2] =  AstroMath.cosd(lat);
		R[0][2] =  AstroMath.sind(lat);
		R[1][1] =  AstroMath.cosd(lon);
		R[1][0] =  -AstroMath.sind(lon);
		R[1][2] =  0.0;
		R[0][0] =  R[2][2] * R[1][1];  
		R[0][1] = -R[2][2] * R[1][0];
		R[2][0] = -R[0][2] * R[1][1];
		R[2][1] =  R[0][2] * R[1][0];
		return R;
	},

	/**
	 * Transformation from polar coordinates to Unit vector
	 * @return U[3]
	 */
	tr_ou(ra, dec) {
		const u = new Array(3);
		const cosdec = AstroMath.cosd(dec);

		u[0] = cosdec * AstroMath.cosd(ra);
		u[1] = cosdec * AstroMath.sind(ra);
		u[2] = AstroMath.sind(dec);

		return u;
	},

	/**
	 * Rotates the unit vector u1 using the rotation matrix
	 * u1[3] unit vector
	 * R[3][3] rotation matrix
	 * return resulting unit vector u2[3]
	 */
	tr_uu( u1, R ) {
		const u2 = new Array(3);
		const x = u1[0];
		const y = u1[1];
		const z = u1[2];

		u2[0] = R[0][0]*x + R[0][1]*y + R[0][2]*z ;
		u2[1] = R[1][0]*x + R[1][1]*y + R[1][2]*z ;
		u2[2] = R[2][0]*x + R[2][1]*y + R[2][2]*z ;

		return u2;
	},

	/**
	 * reverse rotation the unit vector u1 using the rotation matrix
	 * u1[3] unit vector
	 * R[3][3] rotation matrix
	 * return resulting unit vector u2[3]
	 */
	tr_uu1( u1 , R) {
		const u2 = new Array(3);
		const x = u1[0];
		const y = u1[1];
		const z = u1[2];

		u2[0] = R[0][0]*x + R[1][0]*y + R[2][0]*z;
		u2[1] = R[0][1]*x + R[1][1]*y + R[2][1]*z;
		u2[2] = R[0][2]*x + R[1][2]*y + R[2][2]*z;

		return u2;
	},

	/**
	 * Computes angles from direction cosines
	 * u[3] = direction cosines vector
	 * return o = [ ra, dec ]
	 */
	tr_uo(u) {
		const x = u[0];
		const y = u[1];
		const z = u[2];
		const r2 = x*x + y*y;
		let ra, dec;
		if (r2 === 0.0) {
	 		// in case of poles
			if (z===0.0) {
				return null;
			}
			ra = 0.0;
			dec = z > 0.0 ? 90.0 : -90.0;
		} else {
			dec = AstroMath.atand( z / Math.sqrt(r2));
			ra  = AstroMath.atan2d (y , x );
			if (ra < 0.0) ra += 360.0;
		}

		return [ ra, dec ];
	}
};