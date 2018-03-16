//=================================
//            AstroMath
//=================================

// Class AstroMath having 'static' methods
export const AstroMath=  {};

/** Constant for conversion Degrees => Radians (rad = deg*AstroMath.D2R) */
AstroMath.D2R = Math.PI/180.0;
/** Constant for conversion Radians => Degrees (deg = rad*AstroMath.R2D) */
AstroMath.R2D = 180.0/Math.PI;
/**
 * Function sign
 * @param x value for checking the sign
 * @return {number} -1, 0, +1 respectively if x < 0, = 0, > 0
 */
const sign = (x) => x > 0 ? 1 : (x < 0 ? -1 : 0 );

/**
 * Function cosd(degrees)
 * @param {number} x angle in degrees
 * @returns {number} the cosine of the angle
 */
AstroMath.cosd = function(x) {
	if (x % 90===0) {
		const i = Math.abs(Math.floor(x / 90 + 0.5)) % 4;
		switch (i) {
			case 0:	return 1;
			case 1:	return 0;
			case 2:	return -1;
			case 3:	return 0;
		}
	}
	return Math.cos(x*AstroMath.D2R);
};

/**
 * Function sind(degrees)
 * @param {number} x angle in degrees
 * @returns {number} the sine of the angle
 */
AstroMath.sind = function(x) {
	if (x % 90 === 0) {
		const i = Math.abs(Math.floor(x / 90 - 0.5)) % 4;
		switch (i) {
			case 0:	return 1;
			case 1:	return 0;
			case 2:	return -1;
			case 3:	return 0;
		}
	}

	return Math.sin(x*AstroMath.D2R);
};

/**
 * Function tand(degrees)
 * @param {number} x angle in degrees
 * @returns {number} the tangent of the angle
 */
AstroMath.tand = function(x) {

	const resid = x % 360;
	if (resid === 0 || Math.abs(resid) === 180) {
		return 0;
	} else if (resid === 45 || resid === 225) {
		return 1;
	} else if (resid === -135 || resid === -315) {
		return -1;
	}

	return Math.tan(x * AstroMath.D2R);
};

/**
 * Function asin(degrees)
 * @param {number} x sine value [0,1]
 * @return {number} the angle in degrees
 */
AstroMath.asind = function(x) { return Math.asin(x)*AstroMath.R2D; };

/**
 * Function acos(degrees)
 * @param {number} x cosine value [0,1]
 * @return {number} the angle in degrees
 */
AstroMath.acosd = function(x) { return Math.acos(x)*AstroMath.R2D; };

/**
 * Function atan(degrees)
 * @param {number} x tangent value
 * @return {number} the angle in degrees
 */
AstroMath.atand = function(x) { return Math.atan(x)*AstroMath.R2D; };

/**
 * Function atan2(y,x)
 * @param {number} y y component of the vector
 * @param {number} x x component of the vector
 * @return {number} the angle in radians
 */
AstroMath.atan2 = function(y,x) {
	if (y !== 0.0) {
		const sgny = sign(y);
		if (x !== 0.0) {
			const phi = Math.atan(Math.abs(y/x));
			if (x > 0.0) return phi*sgny;
			else if (x < 0) return (Math.PI-phi)*sgny;
		} else return (Math.PI/2)*sgny;
	} else {
		return x > 0.0 ? 0.0 : (x < 0 ? Math.PI : 0.0/0.0);
	}
};

/**
 * Function atan2d(y,x)
 * @param {number} y y component of the vector
 * @param {number} x x component of the vector
 * @return {number} the angle in degrees
 */
AstroMath.atan2d = function(y,x) {
	return AstroMath.atan2(y,x)*AstroMath.R2D;
};

/*=========================================================================*/
/**
 * Computation of hyperbolic cosine
 * @param {number} x argument
 */
AstroMath.cosh = (x) => (Math.exp(x)+Math.exp(-x))/2;


/**
 * Computation of hyperbolic sine
 * @param {number} x argument
 */
AstroMath.sinh = (x) => (Math.exp(x)-Math.exp(-x))/2;


/**
 * Computation of hyperbolic tangent
 * @param {number} x argument
 */
AstroMath.tanh = (x) => (Math.exp(x)-Math.exp(-x))/(Math.exp(x)+Math.exp(-x));


/**
 * Computation of Arg cosh
 * @param {number} x argument in degrees. Must be in the range [ 1, +infinity ]
 */
AstroMath.acosh = (x) =>  Math.log(x+Math.sqrt(x*x-1.0));


/**
 * Computation of Arg sinh
 * @param {number} x argument in degrees
 */
AstroMath.asinh = (x) => Math.log(x+Math.sqrt(x*x+1.0));


/**
 * Computation of Arg tanh
 * @param {number} x argument in degrees. Must be in the range ] -1, +1 [
 */
AstroMath.atanh = (x) => 0.5*Math.log((1.0+x)/(1.0-x));


//=============================================================================
//      Special Functions using trigonometry
//=============================================================================
/**
 * Computation of sin(x)/x
 *	@param {number} x in degrees.
 * For small arguments x <= 0.001, use approximation 
 */
// AstroMath.sinc = function(x) {
// 	let ax = Math.abs(x);
// 	let y;
//
// 	if (ax <= 0.001) {
// 		ax *= ax;
// 		y = 1 - ax*(1.0-ax/20.0)/6.0;
// 	} else {
// 		y = Math.sin(ax)/ax;
// 	}
//
// 	return y;
// };

/**
 * Computes asin(x)/x
 * @param {number} x in degrees.
 * For small arguments x <= 0.001, use an approximation
 */
// AstroMath.asinc = function(x) {
// 	let ax = Math.abs(x);
// 	let y;
//
// 	if (ax <= 0.001) {
// 		ax *= ax;
// 		y = 1 + ax*(6.0 + ax*(9.0/20.0))/6.0;
// 	} else {
// 		y = Math.asin(ax)/ax;	// ???? radians ???
// 	}
//
// 	return (y);
// };


//=============================================================================
/**
 * Computes the hypotenuse of x and y
 * @param {number} x value
 * @param {number} y value
 * @return {number} sqrt(x*x+y*y)
 */
AstroMath.hypot= (x,y) => Math.sqrt(x*x+y*y);

/**
 * Generate the rotation matrix from the Euler angles
 * @param {number} z - z Euler angle
 * @param {number} theta - Euler angle
 * @param {number} zeta	- Euler angles
 * @return {iArray}  R [3][3]		the rotation matrix
 * The rotation matrix is defined by:<pre>
 *    R =      R_z(-z)      *        R_y(theta)     *     R_z(-zeta)
 *   |cos.z -sin.z  0|   |cos.the  0 -sin.the|   |cos.zet -sin.zet 0|
 * = |sin.z  cos.z  0| x |   0     1     0   | x |sin.zet  cos.zet 0|
 *   |   0      0   1|   |sin.the  0  cos.the|   |   0        0    1|
 * </pre>
 */
// AstroMath.eulerMatrix = function(z, theta, zeta) {
// 	const R = new Array(3);
// 	R[0] = new Array(3);
// 	R[1] = new Array(3);
// 	R[2] = new Array(3);
// 	const cosdZ = AstroMath.cosd(z);
// 	const sindZ = AstroMath.sind(z);
// 	const cosdTheta = AstroMath.cosd(theta);
// 	const w = AstroMath.sind(theta) ;
// 	const cosdZeta = AstroMath.cosd(zeta);
// 	const sindZeta = AstroMath.sind(zeta);
//
// 	R[0][0] = cosdZeta*cosdTheta*cosdZ - sindZeta*sindZ;
// 	R[0][1] = -sindZeta*cosdTheta*cosdZ - cosdZeta*sindZ;
// 	R[0][2] = -w*cosdZ;
//
// 	R[1][0] = cosdZeta*cosdTheta*sindZ + sindZeta*cosdZ;
// 	R[1][1] = -sindZeta*cosdTheta*sindZ + cosdZeta*cosdZ;
// 	R[1][2] = -w*sindZ;
//
// 	R[2][0] = -w*cosdZeta;
// 	R[2][1] = -w*cosdZ;
// 	R[2][2] = cosdTheta;
// 	return R;
// };
