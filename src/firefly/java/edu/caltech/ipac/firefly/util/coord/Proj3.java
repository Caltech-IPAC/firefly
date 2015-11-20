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
 * Class defining the Mathematical Projections of the Celestial Sphere.
 * This class contains only the mathematical projections ---
 * WCS and Aladin projections are in a derived class.
 * <P>The available projections are defined by the formulae, where
 * <PRE>
 *          l,b   = longitude and latitude
 *          theta = angle to center of projection 
 *          x,y   = projections (cartesian) along x (East) and y (North)
 *          r,phi = projections (polar)
 * TAN      / Standard = Gnomonic     
 *            r = tan(theta)          phi
 * TAN2     / Stereographic           
 *            r = 2.tan(theta/2)      phi
 * SIN      / Orthographic  
 *            r = sin(theta)          phi
 * SIN2     / Equal-area    
 *            r = 2.sin(theta/2)      phi
 * ARC      / Schmidt proj. 
 *            r = theta               phi
 * AITOFF   / Aitoff (equal area)     if D = sqrt(0.5*(1+cos(b)cos(l/2)))
 *            x = 2cos(b)sin(l/2)/D   y = sin(b)/D
 * SANSON   / Global Sinusoidal (equal area)
 *            x = l cos(b)            y = b
 * MERCATOR / with poles at infinity
 *            x = l                   y = atanh(b)
 * LAMBERT  / equal area projection
 *            x = l                   y = sin(b)
 * </PRE>
 * <P> The typical usage of the Proj3 class consists in:<OL>
 * <LI> Define a projection (type and the center of projection) by means of
 *	one of the constructors; the default center is the (0,0) point.
 * <LI> Compute the projection values <EM>X,Y</EM> from a position with
 *	the <B>computeXY</B> method; the projections can be retrieved
 *	either via the <B>getX</B> and <B>getY</B> methods,
 *	or in <EM>aProj3.X</EM> and <EM>aProj3.Y</EM> elements.
 * <LI> The reverse computation (from projections to coordinates)
 *	is done with the <B>computeAngles</B>  method; the angles are
 *	obtained by means of the <B>getLon</B> and <B>getLat</B> methods.
 * </OL>
 *
 * @author Pierre Fernique, Francois Ochsenbein [CDS]
 * @version 1.0 : 03-Mar-2000
 * @version 1.1 : 24-Mar-2000: better documentation
 */

public class Proj3 {
    protected byte type ;	// Projection type
    private double R[][] ;	// Rotation Matrix
    private double clon, clat;	// Center of the Projection (degrees)
    /** The values of the projections */
    protected double X,Y ;	// One point in the projection (cartesian)
    /** The corresponding polar angles */
    protected Coo point ;	// One point in the projection (original angles)

    // Constants
    public static final int NONE     = 0;
    public static final int TAN      = 1; // Standard Gnomonic (r = tan(theta))
    public static final int TAN2     = 2; // Stereographic (r = 2.tan(theta/2))
    public static final int SIN      = 3; // Orthographic  (r = sin(theta))
    public static final int SIN2     = 4; // Equal-area    (r = 2.sin(theta/2))
    public static final int ARC      = 5; // Schmidt proj. (r = theta)
    public static final int AITOFF   = 6; // Aitoff Projection
    public static final int SANSON   = 7; // Global Sinusoidal
    public static final int MERCATOR = 8; // 
    public static final int LAMBERT  = 9; // 
    public static final String [] name = { "-", 
    	"Gnomonic (TAN)", "Stereographic (TAN2)", 
	"Orthographic (SIN)", "Zenithal Equal-area (SIN2)",
	"Schmidt (ARC)", "Aitoff", "Sanson", "Mercator", "Lambert"
    };

  //  ===========================================================
  //		Constructors
  //  ===========================================================

  /** Creation of object used for Projections.
   * At creation, the center and the type of projection is specified
   * @param type     projection type -- default (standard) = TAN
   * @param lon,lat  Center of projection (coordinates of the tangent point)
   *			expressed in degrees.
   */
   public Proj3(int type, double lon, double lat) {
	this.type = (byte)type ;
	this.clon = lon ;
	this.clat = lat ;
	this.point = new Coo (lon, lat) ;
	if ((lon != 0) || (lat != 0)) 	// Keep null for unit matrix
      	    R = Coo.localMatrix(clon,clat);
   }

  /** Creation of object used for Projections from a String.
   * @param type     projection type 
   * @param text     the center in a string
   * @param equatorial  boolean "true" when text represents 
			an equatorial position
   */
   public Proj3(int type, String text, boolean equatorial) throws Exception {
	this.point = new Coo(text, equatorial) ;
	this.type  = (byte)type ;
	this.clon  = this.point.lon ;
	this.clat  = this.point.lat ;
	if ((clon != 0) || (clat != 0)) 	// Keep null for unit matrix
      	    R = Coo.localMatrix(clon,clat);
   }

  /** Projection at the Origin.
   * Projection at tangent point (lon=0, lat=0)
   * @param type     projection type 
   */
   public Proj3(int type) {
	this(type, 0., 0.) ;	// Default = Standard Projection
   }

  /** Standard projection.
   * At creation, the center and the type of projection is specified
   * @param lon,lat  Center of projection (coordinates of the tangent point)
   */
   public Proj3(double lon, double lat) {
	this(TAN, lon, lat) ;	// Default = Standard Projection
   }

  //  ===========================================================
  //			Static Methods
  //  ===========================================================

  /*  Static methods (functions) in Java are very close to C ones;
      they do not require any object instanciation.
      Typical example of static methods are in the Math class
  */

  //  ===========================================================
  //			Class Methods
  //  ===========================================================

  /** Get only the X from the object
   * @return X	    the X projection
   */
   public final double getX() {
	return(X) ;
   }

  /** Get only the Y from the object
   * @return Y	    the Y projection
   */
   public final double getY() {
	return(Y) ;
   }

  /** Get only the longitude from the object
   * @return lon the longitude in degrees of the point (point.lon)
   */
   public final double getLon() {
	return(point.lon) ;
   }

  /** Get only the Y from the object
   * @return lat the latitude in degrees of the point (point.lat)
   */
   public final double getLat() {
	return(point.lat) ;
   }

   public String toString() {
     	return(name[type] + " projection centered at " + clon + " " + clat 
	    + ": " + X + " " + Y) ;
   }

  //  ===========================================================
  //		Projections from Angles
  //  ===========================================================

  /** Compute a projection from initial coordinates.
   * <BR>
   * <B>Rem :</B> the rotation matrix was computed at Constructor
   *
   * @param lon,lat position
   * @return status true if the projection is possible 
   *     	and false when the position can't be projected.
   *		The values of the projections are in object.X and object.Y
   */
   public boolean computeXY (double lon, double lat) {
      double x,y,z, r, w ;

	/* Was this position already computed ?? */
	if ((lon == point.lon) && (lat == point.lat)) 
	    return(!Double.isNaN(X)) ;

	/* Set angles + unit vector, but X and Y are not yet computed */
	point.set(lon, lat) ;
	X = 0./0.; Y = 0./0.; 

	if (R == null) { x = point.x; y = point.y; z = point.z ; }
	else {
      	    x = R[0][0]*point.x + R[0][1]*point.y + R[0][2]*point.z ;
      	    y = R[1][0]*point.x + R[1][1]*point.y + R[1][2]*point.z ;
      	    z = R[2][0]*point.x + R[2][1]*point.y + R[2][2]*point.z ;
	}
	switch(type) {
    	  case TAN      :	// Only 1 hemisphere valid
	    if (x <= 0)  return false ; 
	    X = y/x; Y = z/x; 
	    break ;

    	  case TAN2     :	// All positions valid, just opposite pole
	    w = (1.0 + x)/2.0;
	    if (w <= 0)  { X = 0./0.; Y = 0./0.; return false; }
	    X = y/w; Y = z/w;
	    break ;

    	  case SIN      :	// Only 1 hemisphere valid, r <= 1
	    if (x <= 0)  return false ; 
	    X = y; Y = z ;
	    break ;

    	  case SIN2     :	// Whole sphere, r <= 2 (equal area)
	    w = Math.sqrt((1.0 + x)/2.0);
	    if (w > 0)  { X = y/w; Y = z/w; }
	    else        { X = 2;   Y = 0;   }
	    break ;

    	  case ARC      :	// r <= pi
	    if (x > -1.0)  { 		// Angular distance = acos(x)
	        r = Math.sqrt(y*y + z*z) ;
	    	if (x > 0) w = Coo.asinc(r);
		else       w = Math.acos(x)/r ;
		X = y*w; Y = z*w;
	    }
	    else 	{ X = Math.PI ; Y = 0 ; }
	    break ;

    	  case AITOFF   :	// Ellipse, 
	    r = Math.sqrt(x*x + y*y) ;
	    w = Math.sqrt (r*(r+x)/2.0);	// cos lat . cos lon/2
	    w = Math.sqrt ((1.0 + w)/2.0);
	    X = Math.sqrt(2.*r*(r-x)) / w ;
	    Y = z / w ;
	    if (y<0) X = -X ;
	    break ;

    	  case SANSON   :	// Sinusoidal |X| <= pi, Y <= pi/2
	    r = Math.sqrt(x*x + y*y) ;
	    Y = Math.asin(z);
	    if (r == 0) X = 0 ;
	    else X = Math.atan2(y,x) * r;
	    break ;

    	  case MERCATOR :
	    r = Math.sqrt(x*x + y*y) ;
	    if (r == 0)  return false ; 
	    X = Math.atan2(y,x);
	    Y = Coo.atanh(z);
	    break ;

    	  case LAMBERT  :	// Equal Area (lon,sin(lat))
	    r = Math.sqrt(x*x + y*y) ;
	    Y = z ;
	    if (r == 0) X = 0 ;
	    else X = Math.atan2(y,x);
	    break ;
	
	  default:
	    throw new IllegalArgumentException(
	     "****Proj3: Invalid Projection type #" + type) ;
	}
	return(true) ;
   }

  //  ===========================================================
  //		Projected values to Coordinates
  //  ===========================================================

  /** Reverse projection: compute the polar angle corresponding to (x,y)
   * <BR>
   * <B>Rem :</B> the rotation matrix was computed at Constructor
   * a la creation de l'objet
   *
   * @param  type  Projection type
   * @param  x,y   projection values
   * @return status true if the X / Y values are within the projection area
   *     	and false otherwise,
   *		The values of the angles are obtained via getLon() and getLat()
   */
   public boolean computeAngles (double px, double py) {
      double x,y,z, x0,y0,z0, r, w ;
      boolean angles_set = false ;
	/* Was this position already computed ?? */
	if ((px == this.X) && (py == this.Y)) 
	    return(!Double.isNaN(point.lon)) ;

	/* Set the projection values, bug angles not yet computed */
	X = px; Y = py; 
	point.lon = 0./0.; point.lat = 0./0. ;

	switch(type) {
    	  case TAN      :	// Only 1 hemisphere valid
	    x = 1.0 / Math.sqrt(1.0 + X*X + Y*Y);
	    y = X * x;
	    z = Y * x;
	    break ;

    	  case TAN2     :	// All positions valid, just opposite pole
	    r = (X*X + Y*Y)/4.0 ;
	    w = 1.0 + r ;
	    x = (1.0 - r)/w ;
	    y = X/w ;
	    z = Y/w ;
	    break ;

    	  case SIN      :	// Only 1 hemisphere valid, r <= 1
	    w = 1.0 - X*X - Y*Y;
	    if (w < 0) {	// Accept some rounding error
		if (w > -2.e-16) w = 0 ;
		else  return false ; 
	    }
	    x = Math.sqrt(w) ;
	    y = X;
	    z = Y;
	    break;

    	  case SIN2     :	// Whole sphere, r <= 2 (equal area)
	    r = (X*X + Y*Y)/4.0 ;
	    if (r > 1.)  return false ; 
	    w = Math.sqrt(1.0 - r) ;
	    x = 1.0 - 2.0 * r;
	    y = w * X;
	    z = w * Y;
	    break ;

    	  case ARC      :	// r <= pi
	    r = Math.sqrt(X*X + Y*Y) ;
	    if (r > Math.PI)  return false ; 
	    w = Coo.sinc(r);
	    x = Math.cos(r);
	    y = w * X;
	    z = w * Y;
	    break ;

    	  case AITOFF   :	// Ellipse, dimensions sqrt(2) x 2.sqrt(2)
	    r = X*X/8.0 + Y*Y/2.0; 	// 1 - cos b . cos l/2
	    if (r > 1.0)  return false ; 
	    x = 1. - r ;		//     cos b . cos l/2
	    w = Math.sqrt(1. - r/2.) ;	// sqrt(( 1 + cos b . cos l/2)/2)
	    y = X * w / 2. ;
	    z = Y * w ;
	    // Convert from Cartesian (l/2,b) to Cartesian (l,b) 
	    r = Math.sqrt(x*x + y*y) ;	// cos(b)
	    if (r > 0) {
	        w = x;
		x = (w*w - y*y) /r;
		y = 2.0 * w * y /r;
	    }
	    break ;

    	  case SANSON   :	// Sinusoidal |X| <= pi, Y <= pi/2
	    z = Math.sin(Y);
	    r = 1 - z*z; 	// cos^2(b)
	    if (r < 0)  return false ; 
	    r = Math.sqrt(r);	// cosb
	    if (r == 0) w = 0. ;
	    else    w = X/r;	// Longitude
	    x = r * Math.cos(w);
	    y = r * Math.sin(w);
	    break ;

    	  case MERCATOR :
	    r = 1./Coo.cosh(Y);
	    z = Coo.tanh(Y) ;
	    x = r * Math.cos(X);
	    y = r * Math.sin(X);
	    break ;

    	  case LAMBERT  :	// Equal Area (lon,sin(lat))
	    z = Y;
	    r = 1 - z*z;	// cos(b) ** 2 
	    if (r < 0)  return false ; 
	    r = Math.sqrt(r);	// cosb
	    x = r * Math.cos(X);
	    y = r * Math.sin(X);
	    break ;
	
	  default:
	    throw new IllegalArgumentException(
	     "****Proj3: Invalid Projection type #" + type) ;
	}

	/* From Cartesian: just rotate (used transposed matrix) */
	if (R != null) point.set(
      	    R[0][0]*x + R[1][0]*y + R[2][0]*z ,
      	    R[0][1]*x + R[1][1]*y + R[2][1]*z ,
      	    R[0][2]*x + R[1][2]*y + R[2][2]*z 
	) ;
	else point.set(x, y, z) ;
     	return true ;
   }
}
