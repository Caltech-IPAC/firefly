//
// Copyright 1999-2000 - Universite Louis Pasteur / Centre National de la 
// Recherche Scientifique
//                      
// ------
// 
// Address: Centre de Donnees astronomiques de Strasbourg
//          11 rue de l'Universite
//          67000 STRASBOURG
//          FRANCE
// Email:   question@simbad.u-strasbg.fr
// 
// -------
// 
// In accordance with the international conventions about intellectual
// property rights this software and associated documentation files
// (the "Software") is protected. The rightholder authorizes : 
// the reproduction and representation as a private copy or for educational
// and research purposes outside any lucrative use,
// subject to the following conditions:
// 
// The above copyright notice shall be included.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON INFRINGEMENT,
// LOSS OF DATA, LOSS OF PROFIT, LOSS OF BARGAIN OR IMPOSSIBILITY
// TO USE SUCH SOFWARE. IN NO EVENT SHALL THE RIGHTHOLDER BE LIABLE
// FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
// THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// 
// For any other exploitation contact the rightholder.
// 
//                        -----------
// 
// Conformement aux conventions internationales relatives aux droits de
// propriete intellectuelle ce logiciel et sa documentation sont proteges.
// Le titulaire des droits autorise : 
// la reproduction et la representation a titre de copie privee ou des fins
// d'enseignement et de recherche et en dehors de toute utilisation lucrative.
// Cette autorisation est faite sous les conditions suivantes :
// 
// La mention du copyright portee ci-dessus devra etre clairement indiquee.
// 
// LE LOGICIEL EST LIVRE "EN L'ETAT", SANS GARANTIE D'AUCUNE SORTE.
// LE TITULAIRE DES DROITS NE SAURAIT, EN AUCUN CAS ETRE TENU CONTRACTUELLEMENT
// OU DELICTUELLEMENT POUR RESPONSABLE DES DOMMAGES DIRECTS OU INDIRECTS
// (Y COMPRIS ET A TITRE PUREMENT ILLUSTRATIF ET NON LIMITATIF,
// LA PRIVATION DE JOUISSANCE DU LOGICIEL, LA PERTE DE DONNEES,
// LE MANQUE A GAGNER OU AUGMENTATION DE COUTS ET DEPENSES, LES PERTES
// D'EXPLOITATION,LES PERTES DE MARCHES OU TOUTES ACTIONS EN CONTREFACON)
// POUVANT RESULTER DE L'UTILISATION, DE LA MAUVAISE UTILISATION
// OU DE L'IMPOSSIBILITE D'UTILISER LE LOGICIEL, ALORS MEME
// QU'IL AURAIT ETE AVISE DE LA POSSIBILITE DE SURVENANCE DE TELS DOMMAGES.
// 
// Pour toute autre utilisation contactez le titulaire des droits.
// 
package edu.caltech.ipac.firefly.util.coord ;

/**
 * Class defining the coordinates on the celestial sphere,
 * and includes the missing trigonometrical functions in degrees
 * and hyperbolic functions.
 * The class includes conversions between polar angles (lon,lat) 
 * expressed in degrees, and Cartesian 3-vectors.
 * The typical way of converting between polar and cartesian is:
 * <PRE>
 * Coo aCoo = new Coo ; double u[] = new double[3] ;
 * while (true) {
 *     aCoo.set(stdin.readLine()) ;
 *     System.out.println("Coordonnees   : " + aCoo) ;
 *     u[0] = aCoo.x; u[1] = aCoo.y; u[2] = aCoo.z;
 *     System.out.println("Cos. directeurs: " + Coo.toString(u)) ;
 * }
 * </PRE>
 * This class also deals with 3x3 matrices.
 * @author Pierre Fernique, Francois Ochsenbein [CDS]
 * @version 1.0 : 03-Mar-2000
 * @version 1.1 : 24-Mar-2000: Bug in dist
 */

public class Coo {
   // private byte status ; 	// 1 for angles, 2 for xyz
   /** Polar Angles in degrees */
   public double lon, lat ;
   /** Components of unit vector (direction cosines) */
   public double x, y, z ;
   /** Number of decimals on lon and lat */
   public byte dlon, dlat ;

  //  ===========================================================
  //		Trigonometry in Degrees
  //  ===========================================================

  /*  Static methods (functions) in Java are very close to C ones;
      they do not require any object instanciation.
      Typical example of static methods are in the Math class
      Note that the functions toDegrees and toRadians can be used
      in JDK1.2 -- we stick here strictly to JDK1.1
  */

  /** Cosine when argument in degrees
   * @param	angle in degrees
   * @return	the cosine
   */
   public static final double cosd(double x) {
      return Math.cos( x*(Math.PI/180.0) );
   }

  /** Sin   when argument in degrees
   * @param	angle in degrees
   * @return	the sine
   */
   public static final double sind(double x) {
      return Math.sin( x*(Math.PI/180.0) );
   }

  /** sin-1 (inverse function of sine), gives argument in degrees
   * @param	x argument
   * @return	y value such that sin(y) = x 
   */
   public static final double asind(double x) {
      	return Math.asin(x)*(180.0/Math.PI);
   }

  /** tan-1 (inverse function of tangent), gives argument in degrees
   * @param	argument
   * @return	angle in degrees
   */
   public static final double atand(double x) {
      	return Math.atan(x)*(180.0/Math.PI);
   }

  /** get the polar angle from 2-D cartesian coordinates
   * @param	y,x = cartesian (in that order)
   * @return	polar angle in degrees
   */
   public static final double atan2d(double y,double x) {
      	return Math.atan2(y,x)*(180.0/Math.PI);
   }

  //  ===========================================================
  //		Hyperbolic Functions (not in Math ??)
  //  ===========================================================

  /** Hyperbolic cosine cosh = (exp(x) + exp(-x))/2
   * @param 	argument
   * @return	corresponding hyperbolic cosine (>= 1)
   */
   public static final double cosh (double x) {
      double ex ;
       	ex = Math.exp(x) ;
        return 0.5 * (ex + 1./ex) ;
   }

  /** Hyperbolic tangent = (exp(x)-exp(-x))/(exp(x)+exp(-x))
   * @param	argument
   * @return	corresponding hyperbolic tangent (in range ]-1, 1[)
   */
   public static final double tanh (double x) {
      double ex, ex1 ;
	ex = Math.exp(x) ;
	ex1 = 1./ex ;
	return (ex - ex1) / (ex + ex1) ;
   }
  /** tanh-1 (inverse function of tanh)
   * @param	argument, in range ]-1, 1[ (NaN returned otherwise)
   * @return	corresponding hyperbolic inverse tangent
   */
   public static final double atanh (double x) {
        return (0.5*Math.log((1.+(x))/(1.-(x))));
   }

  //  ===========================================================
  //		sin(x)/x and Inverse
  //  ===========================================================

   /** Function sinc(x) = sin(x)/x
   * @param	argument (radians)
   * @return	corresponding value
   */
   public static final double sinc(double x) {
      double ax, y;
      	ax = Math.abs(x);
      	if (ax <= 1.e-4) {
            ax *= ax;
            y = 1 - ax*(1.0-ax/20.0)/6.0;
      	} 
	else y = Math.sin(ax)/ax;
      	return y;
   }

   /** Reciprocal */
   /** Function asinc(x), inverse function of sinc 
   * @param	x argument
   * @return	y such that sinc(y) = x
   */
   public static final double asinc(double x) {
      double ax,y;
      	ax = Math.abs(x);
      	if( ax <= 1.e-4) {
            ax *= ax;
            y = 1.0 + ax*(6.0 + ax*(9.0/20.0))/6.0;
      	} 
	else y = Math.asin(ax)/ax;
      	return (y);
   }

  //  ===========================================================
  //		Spherical distance
  //  ===========================================================

   /** 
   * Distance between 2 points on the sphere.
   * @param  lon1,lat1 = position of first point in degrees
   * @param  lon2,lat2 = position of second point in degrees
   * @return distance in degrees in range [0, 180]
   */
   public static final double dist(double lon1, double lat1, 
      double lon2, double lat2) {
      double c1 = cosd(lat1); 
      double c2 = cosd(lat2) ;
      double w, r2 ;
      	w  = c1*cosd(lon1) - c2*cosd(lon2) ;
      	r2 = w*w ;
      	w  = c1*sind(lon1) - c2*sind(lon2) ;
	r2 += w*w ;
	w  = sind(lat1) - sind(lat2) ;
	r2 += w*w ;			// 4.sin^2(r/2)
	return(2.*asind(0.5*Math.sqrt(r2))) ;
   }

  //  ===========================================================
  //		Matrices and Vectors 3x3
  //  ===========================================================

   /** 3-Matrices Products
   * @param  A,B  = 3x3 matrices
   * @return R    = A * B
   */
   public static final double[][] prod(double A[][], double B[][]) {
      double[][] R = new double[3][3];
      int i, j ;
	for (i=0; i<3; i++) for (j=0; j<3; j++) 
	    R[i][j] = A[i][0]*B[0][j] + A[i][1]*B[1][j] + A[i][2]*B[2][j] ;
      	return(R) ;
   }

   /** Transposed of a Matrix
   * @param  A 	= input matric
   * @return R  = t(A)
   */
   public static final double[][] t(double A[][]) {	// Transpose a Matrix
      double R[][] = new double[3][3];
      int i, j ;
	for (i=0; i<3; i++) for (j=0; j<3; j++) R[i][j] = A[j][i] ;
	return(R) ;
   }

  //  ===========================================================
  //		Polar angles (lon,lat) <--> Cartesian
  //  ===========================================================
  
  /** Convert (lon,lat) into its direction cosines (x,y,z) 
   * @param  lon, lat	angles in degrees
   */
   public final void set (double lon, double lat) {
      double coslat = cosd(lat);
	this.lon = lon ; this.lat = lat ;
     	x = coslat * cosd(lon);
     	y = coslat * sind(lon);
     	z = sind(lat);
   }

  /** Revert conversion of (x,y,z) into (lon,lat)
   * @param  x,y,z unit vector (direction cosines)
   *         (Note that (x,y,z) doesn't need to have a norm=1)
   */
   public final void set (double x, double y, double z) {
      double r2 = x*x + y*y ;
	this.x = x ; this.y = y ; this.z = z ;
      	lon = 0.0;
      	if( r2==0.0) {			/* in case of poles */
	    if( z==0.0 ) { lon = 0./0. ; lat = 0./0.; }
	    else lat = (z>0.0) ? 90.0 : -90.0;
     	}
	else {
     	    lon = atan2d(y, x);
     	    lat = atan2d(z, Math.sqrt(r2));
     	    if( lon<0.0) lon += 360.0;
     	}
   }

  //  ===========================================================
  //		Interpret a string for Position
  //  ===========================================================
  
  /** Define a coordinate for its angles
   * @param  text	a text containing 2 angles, in decimal or Sexagesimal
   * @param  equatorial	true when text represents equatorial coordinates
   *			(the RA in units of time)
   * <BR>Note: dlon and dlat are set to the number of decimals found
   * in the longitude and latitude parts. 
   * 
   */
   public final void set (String text, boolean equatorial) throws Exception { 
      int n = text.length() ;
      char b[] = text.toCharArray() ;
      boolean f15 = equatorial ;
      int state = 0 ;		// 0 = Skip blanks, 1=int, 2=frac
      int index = 0 ;		// 0-2 = lon, 4-6 = lat
      int nd = 0 ;		// Number of decimals
      double value = 0 ;
      double angle = 0 ;
      double ff = 1 ;		// fraction factor to get the number
      double fa = 1 ;		// fraction factor to get the angle
      boolean minus = false ;
      int i ; char c ;

	lon = 0./0. ; lat = 0./0. ;	// Initialize to NaN
	for (i=0; i<n ; i++) {
	    c = b[i] ;
	    //System.err.println("i=" + i + ", c='" + c + "' index=" + index 
	    //  + ", state=" + state); 
	    if (state == 0) {
	       	if (c == ' ') continue ;
		if (c == '+') { state = 1; continue ; }
		if (c == '-') { minus = true; state = 1; continue ; }
	    }
	    if ((c >= '0') && (c <= '9')) {	// Number
		c -= '0' ;
		if (state <= 1) { state = 1 ; value = value*10. + c ; }
		else { ff *= 10.; value += c/ff ; nd++ ; }
		continue ;
	    }
	    if (c == '.') {			// Fractional part
		if (state > 1) break ;		// Error, . repeated...
		// When the very first number contains a decimal point,
		// the RA part is in degrees (no decimal hour accepted)
		if (index == 0) f15 = false ;	
	        state = 2 ; nd = 0 ;
		ff = 1. ;
		continue ;
	    }
	    if (c == ' ') {			// New part ?
		while ((i<n) && (b[i]==' ')) i++ ;
		if (i >= n) break ;
		c = b[i--] ;
		if ((c>='0') && (c<='9')) {	// A new number or sexa part ?
		    if (state > 1) c = '+' ;
		    else c = ':' ;
		}
		else i++ ;			// It's the read byte.
	    }
	    if (c == ':') {
		if (state != 1) break ;		// : doesn't separate numbers
		if ((index&2)!=0) break ;	// Too many :
		angle += value/fa ;
		fa *= 60. ;
		index++ ; value = 0 ; state = 0 ; nd = 0 ;
		continue ;
	    }
	    if ((c == '+') || (c == '-')) {	// Second number starts !
		if (index > 3) break ;		// Too many numbers...
		angle += value/fa ;
		if (f15) { angle *= 15. ; nd-- ; }
	    	if (minus) angle = -angle ;
		lon = angle ; dlon = (byte)(nd + 1 + index*2) ;
		index = 4 ;
		state = 1 ; 
		minus = c == '-' ;
		value = 0 ; angle = 0 ; fa = 1;
		continue ;
	    }
	    break ;			// Error
	}
	if ((i == n) && (index > 3)) {
	    angle += value/fa ;
	    if (minus) angle = -angle ;
	    lat = angle ;
	    dlat = (byte)(nd + 1 + ((index&3)<<1)) ;
	}

	// Error Check
	if (i < n) throw new Exception //System.err.println
	    ("****Coo can't be interpreted at " + i + ": \"" + text + "\"") ;
	else if (index < 4) throw new Exception  //System.err.println
	    ("****Coo missing? \"" + text + "\"") ;
	set(lon, lat) ;
   }

  /** Define a coordinate for its angles
   * @param  text	a text containing 2 angles, in decimal or Sexagesimal
   * @param  equatorial	true when text represents equatorial coordinates
   *			(the RA in units of time)
   */
   public final void set (String text) throws Exception { 
	this.set(text, false) ;
   }

  /** Define equatorial coordinate for its angles
   * @param  text	a text containing 2 angles, in decimal or Sexagesimal
   * @param  equatorial	true when text represents equatorial coordinates
   *			(the RA in units of time)
   */
   public final void setEq(String text) throws Exception { 
	this.set(text, true) ;
   }

  //  ===========================================================
  //		Constructor
  //  ===========================================================
  
  /** Define a coordinate from its angles
   * @param  lon,lat	angles in degrees
   */
   public Coo(double lon, double lat) {
	this.set(lon, lat) ;
   }
  /** Define a coordinate from its direction cosines.
   * @param  x,y,z	unit vector components (direction cosines)
   */
   public Coo(double x, double y, double z) {
	this.set(x, y, z) ;
   }
  /** Define a coordinate from a string
   * @param  text	a position as a string (decimal or sexagesimal)
   * @param  equatorial	true when text represents equatorial coordinates
   */
   public Coo(String text, boolean equatorial) throws Exception {
	this.set(text, equatorial) ;
   }
  /** Define a coordinate from a string
   * @param  text	a position as a string
   */
   public Coo(String text) throws Exception {
	this.set(text) ;
   }
  /** The basic contructor: assume the North Pole */
   public Coo() {
       	x = y = 0.; z = 1.;
	lon = 0. ; lat = 90.;
   }

  //  ===========================================================
  //		Rotation Matrix to a Coo
  //  ===========================================================
  
   /** Compute the rotation matrix to transform a unit vector 
   * into the local frame with axises defined by:
   *  R[0] (first axis)  = unit vector towards Zenith
   *  R[1] (second axis) = unit vector towards East
   *  R[2] (second axis) = unit vector towards North
   * (the projected x,y are along R[1] and R[2] axises)
   * @param  lon,lat = Center of projection
   * @return R[3][3] = Rotation Matrix      (local_x,y,z = R * global_x,y,z)
   */
   public static final double[][] localMatrix(double lon,double lat) {
      double R[][] = new double[3][3];
      	R[2][2] =   cosd(lat);
      	R[0][2] =   sind(lat);
      	R[1][1] =   cosd(lon);
      	R[1][0] =  -sind(lon);
      	R[1][2] =  0.0;
      	R[0][0] =  R[2][2] * R[1][1];
      	R[0][1] = -R[2][2] * R[1][0];
      	R[2][0] = -R[0][2] * R[1][1];
      	R[2][1] =  R[0][2] * R[1][0];
	return(R) ;
   }

  //  ===========================================================
  //		Editions of Angles, uvectors and Matrices
  //  ===========================================================

   /** Edition of a single floating-point number, zero_filled.
    * @param  buf   the edition buffer to which the edited number is appended
    * @param  value the number to edit
    * @param  len   the total width
    * @param  nd    the number of decimals -- use -1 to remove the decimal point
    * @param  sign  true to edit the sign as '+' or '-'
    * @return	the number of characters effectively appended (0 for NaN)
    * Remark: the number is truncated -- not rounded.
    */
   public static final int ed1(StringBuffer buf, double value, 
      int len, int nd, boolean sign) {
      int len0 = buf.length() ;
      int n ;
      int int_part ;
      int lint = len - nd -1;	// Number of bytes for integer part
      double a = value ;
      	// System.err.println("ed1(" + value + ", index=" + index + ", len="
	//   + len + ", nd=" + nd + ")") ;
	if (Double.isNaN(value)) return(0) ;
      	if (sign && (a >= 0)) { buf.append('+') ; lint--; }
	if (a < 0)   { a = -a ; buf.append('-') ; lint--; }
	int_part = (int)a ; a -= int_part ;
	//* Edit the integer part, zero-filled
	for (n=1; lint>1; lint--) n *= 10; 
	while ((int_part/n) >= 10) n *= 10;	// In case too short len
      	//* System.err.print("....int_part=" + int_part + ", n=" + n) ;
	while (n > 0) {
	    buf.append((char)('0' + int_part / n)) ;
	    int_part %= n ;
	    n /= 10 ;
	}
	//* Add now the decimals
	if (nd >= 0) buf.append('.') ;
	for (n = (len0 + len) - buf.length() ; n > 0; n--) {
	    a *= 10. ;
	    int_part = (int)a ; a -= int_part ;
	    buf.append((char)('0' + int_part)) ;
	}
	/* Finally, return the number of chars appended */
	return(buf.length() - len0) ;
   }

   /** Edition of a single floating-point number
    * @param  value the number to edit
    * @param  len   the total width
    * @param  nd    the number of decimals
    * @param  sign  true to edit the sign as '+' or '-'
    * @return	the string 
    */
   public static final String toString(double value, int len, int nd,
      boolean sign) {
      StringBuffer b = new StringBuffer(len+2);
      	ed1(b, value, len, nd, sign) ;
      	return(""+b) ;		// Buffer converted to String
   }

  /** Default Edition of the Coordinates, as 2 numbers expressing
    * the angles in degrees.
    */
   public final String toString() {
      StringBuffer b = new StringBuffer(40) ;
      int nd ;
	nd = dlon > 0 ? dlon : 12 ;
	ed1(b, lon, nd+4, nd, false) ;  b.append(' ') ;
	nd = dlat > 0 ? dlat : 12 ;
	ed1(b, lat, nd+4, nd, true) ;  
     	return(""+b) ;		// Buffer converted to String
   }

  /** Edition of the 3 components of a vector
    * @param	the 3-vector 
    * @return	the equivalent string (edited with 16 decimals)
    */
   protected static final void edit(StringBuffer buf, double u[]) {
	ed1(buf, u[0], 19, 16, true) ; buf.append(' ') ;
	ed1(buf, u[1], 19, 16, true) ; buf.append(' ') ;
	ed1(buf, u[2], 19, 16, true) ; 
   }

  /** Edition of the 3 components of a vector
    * @param	the 3-vector 
    * @return	the equivalent string (makes use of TABs)
    */
   protected static final String toString(double u[]) {
      StringBuffer b = new StringBuffer(60) ;
	edit(b, u) ;
     	return(""+b) ;		// Buffer converted to String
   }

   /** Edition of s 3x3 matrix
    * @param	the 3x3 matrix
    * @return	the equivalent string (makes use of TABs)
    */
   protected static final String toString(double m[][]) {
     StringBuffer b = new StringBuffer(200) ;
     	b.append("    "); 
     	edit(b, m[0]) ; b.append("\n    ") ;
     	edit(b, m[1]) ; b.append("\n    ") ;
     	edit(b, m[2]) ; 
	return(""+b) ;		// Buffer converted to String
   }
}
