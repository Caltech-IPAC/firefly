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
 * This class defines the various elements of an Astronomical Frame:
 * the type of frame, with its equinox when relevant (equatorial frame);
 * the accuracy of the coordinates for a correct edition; and one point
 * (an element of the Coo class) in the frame. <BR>
 * This class also includes basic transformations in the various kinds
 * of dates used for stellar astrometry: Julian days/years, and Besselian
 * years.
 * <P> The typical usage of the Astroframe class consists in:<OL>
 * <LI> Define an astronomical coordinate frame by means of one of 
 *	the constructors;
 * <LI> Assign a position in this frame via one of the <B>set</B> methods 
 * <LI> Convert to another frame via the  <B>convert</B> method
 * <LI> The angles (in degrees) are best extracted via <B>getLon</B> and 
 *	<B>getLat</B> methods
 * </OL>
 * The edition of the position in an Astroframe can be done in a StringBuffer
 * (<B>ed2</B> methods) with options like sexagesimal or decimal edition,
 * or as a String with one of the available <B>toString</B> methods.
 * @author Francois Ochsenbein, Pierre Fernique [CDS]
 * Constants from slaLIB (P.T. Wallace)
 * @version 1.0 : 03-Mar-2000
 * @version 1.1 : 24-Mar-2000 (Bug in ICRS Edition)
 */

public class Astroframe {
  /** One point in the frame contains * lon lat x y z + dlon/dlat (decimals) */
    private Coo point ;		// Contains lon lat x y z + dlon/dlat (decimals)
  /** Equinox in Julian Years (FK5) or Besselian (FK4). 
    * Unused for Gal, SGal, ICRS */
    protected double equinox ;	// Equinox in Julian Years (FK5) or Besselian
  /** Coordinate system is one of FK4 FK5 GAL SGAL ECL ICRS */
    protected byte frame ;	// Coordinate System as 1 to 6
  /** The precision is 0=unknown, 1=DEG, 3=ARCMIN, 5=ARCSEC, 8=MAS, etc */
    protected byte precision ;	// 0 = unknown, 1=deg, 3=arcmin, 5=arcsec, 8=mas
  /** The original precision is the precision used at Construction.
      It is taken as an upper value of the precision. */
    protected byte oprecision ;	// Original Precision
    private byte status_coo ;	// 0=no position, 1=angles, 2=xyz, 3=both
    private byte status_frame ;	// 1=matrix to FK4, 2=matrix to FK5
    private double[][] matrix ;	// Matrix to rotate from standard FK4 or FK5 
    //  protected byte sexa ;	// Edition in SEXAgesimal or Degrees

    /** Definitions of Precisions for the angles */
    static public final byte NONE = 0, DEG = 1, ARCMIN=3, ARCSEC=5, MAS=8 ;

    /** Definitions of Coordinate Frames */
    static public final byte FK4 = 1, FK5 = 5, GAL = 2, SGAL = 3, 
	               SUPERGAL = 3, ECL = 4, ICRS = 6 ;

    /** Edition options */
    static private final int ED_SIGN=0x01, ED_DECIMAL=0x10, ED_FRAME=0x80, 
	ED_FULL=0x02, ED_SEXA = 0x20, ED_RA=0x40, ED_COLON=0x04 ;
    static private final int ed_opt[] = { 
	/*   FK4    Gal        SGal        Ecl         FK5   ICRS */
	0, ED_RA, ED_DECIMAL, ED_DECIMAL, ED_DECIMAL, ED_RA, ED_DECIMAL|ED_RA
    } ;

    static final private String[] explain_frame = {
	/*      FK4    Gal    SGal    Ecl    FK5    ICRS */
       "NONE", "FK4", "Gal", "SGal", "Ecl", "FK5", "ICRS" 
    } ;
    static final private String[] explain_precision = {
	"unknown", "1degree", "0.1degree", "1arcmin", "0.1arcmin", "1arcsec",
	"0.1arcsec", "10mas", "1mas", "0.1mas", "10{mu}as", 
	"1{mu}as", "0.1{mu}as"
    } ;
    static final double[] default_equinox = {	// Default Equinoxes 
	/*      FK4    Gal    SGal    Ecl    FK5    ICRS */
       2000.0, 1950., 0./0.,  0./0., 2000.0, 2000.,  0./0. 
    } ;

    /* Default Equinoxes in Julian Years */
    /** Value of Besselian Year in days */
    static final public double Byr = 365.242198781e0 ;	// Besselian Year
    /** Value of the Julian Year in days */
    static final public double Jyr = 365.25e0 ;	// Julian Year
    /** Julian date of J2000 epoch */
    static final public double JD_J2000 = 2451545. ;	// Julian Date of J2000
    /** Julian date of B1950 epoch */
    static final public double JD_B1900 = 2415020.31352;// Julian Date of B1900
 
   // Rounding values for Decimal edition ...
   static private double gf[] = {1., 10., 100., 1.e3, 1.e4, 1.e5, 1.e6, 1.e7 } ;
   // ... and for sexagesimal edition
   static private double qf[] = {1., 10.,  60., 600., .36e4, .36e5, .36e6, 
       .36e7} ;
   // continues*10

   /* Conversion from FK4 to FK5 -- Proper motions are in arcsec/century    */
   static private final double PMF = 100.*3600.*(180./Math.PI) ;
   /** Matrix 6x1 to compute the e-term */
   static protected double[] A = {		// For e-term
      -1.62557e-6,    -0.31919e-6,    -0.13843e-6,    
         1.245e-3,      -1.580e-3,      -0.659e-3    };
   /* The interval B1950 and J2000 is  
      dtB = B2J(1950.) - 2000. = -50.0012775136654 (Besselian Years)
   */
   static private double[] A1 = {	// A1[i] = A[i] + A[i+3]*dtB/PMF
      -1.62255e-6,    -0.32302e-6,    -0.14003e-6
   };
   /** The 6x6 matrix to move from FK4 to FK5.
      Only a part of this matrix systems is used here. 
      This matrix is however totally used when proper motions are involved
   */
   static protected double[][] EM = {	// Rotation of u + v vector
     { 	 0.9999256782e0, 	-0.0111820611e0, 	-0.0048579477e0, 
		 2.42395018e-6,		-0.02710663e-6,		-0.01177656e-6},
     {	 0.0111820610e0,     	 0.9999374784e0,     	-0.0000271765e0,
		 0.02710663e-6,      	 2.42397878e-6,      	-0.00006587e-6},
     {	 0.0048579479e0,     	-0.0000271474e0,     	 0.9999881997e0,
		 0.01177656e-6,      	-0.00006582e-6,      	 2.42410173e-6},
     {	-0.000551e0,         	-0.238565e0,         	 0.435739e0,
		 0.99994704e0,       	-0.01118251e0,       	-0.00485767e0},
     {	 0.238514e0,         	-0.002667e0,         	-0.008541e0,
		 0.01118251e0,       	 0.99995883e0,       	-0.00002718e0},
     {	-0.435623e0,         	 0.012254e0,         	 0.002117e0,
		 0.00485767e0,       	-0.00002714e0,       	 1.00000956e0}
     };

   /** The 6x6 matrix to move from FK5 to FK4 */
   static protected double[][] EM1 = {	/* FK5 ==> FK4	*/
     {   0.9999256795e0,      0.0111814828e0,     0.0048590039e0,
	      -2.42389840e-6,      -0.02710544e-6,     -0.01177742e-6},
     {  -0.0111814828e0,      0.9999374849e0,    -0.0000271771e0,
	       0.02710544e-6,      -2.42392702e-6,      0.00006585e-6},
     {  -0.0048590040e0,     -0.0000271557e0,     0.9999881946e0,
	       0.01177742e-6,       0.00006585e-6,     -2.42404995e-6},
     {  -0.000551e0,          0.238509e0,        -0.435614e0,
  	       0.99990432e0,        0.01118145e0,       0.00485852e0},
     {  -0.238560e0,         -0.002667e0,         0.012254e0,
	      -0.01118145e0,        0.99991613e0,      -0.00002717e0},
     {   0.435730e0,         -0.008541e0,         0.002117e0,
	      -0.00485852e0,       -0.00002716e0,       0.99996684e0}
     };

   /* For moving from FK4 to FK5 without proper motion, the EM50 matrix
      is used
      EM50[i][j] = EM[i][j] + EM[i+3][j]*dtB/PMF ;
   */
   static private double EM50[][] = {
     { 0.9999256795356672, -0.0111814827996970, -0.0048590039655699},
     { 0.0111814828233251,  0.9999374848650175, -0.0000271557959449},
     { 0.0048590038843768, -0.0000271771046587,  0.9999881945682256}
   } ;

   //  In B1950, the Galactic Frame is defined by 
   // North Pole at (RA, Dec) = 192.25 +27.4 (12h49 +27d24')
   //    longitude of ascending node = 33 deg
   // Matrix B1950 to Gal = Euler(asc.node-90, 90-lat(Pole), -lon(Pole))
   /** Rotation matrix to move from FK4 to Galactic. */
   static public final double[][] gal_1950 = {	// Euler(-57, 62.6, -192.25)
     {-0.0669887394151508,-0.8727557658519927,-0.4835389146321842},
     { 0.4927284660753235,-0.4503469580199614, 0.7445846332830311},
     {-0.8676008111514348,-0.1883746017229204, 0.4601997847838516}
   } ;

   /*  Constants for Galactic to SuperGalactic */
   // Pole of SuperGalactic at (Glon, Glat) = 47.37 +06.32
   //   longitude of ascending node = 0.0
   /** Rotation matrix to move from Galactic to Supergalactic. */
   static public final double[][] supergal = {	// Euler(-90, 83.68, -47.37)
     {-0.7357425748043749, 0.6772612964138942,        0.},
     {-0.0745537783652337,-0.0809914713069767, 0.9939225903997749},
     { 0.6731453021092076, 0.7312711658169645, 0.1100812622247821}
   } ;
   /** Rotation matrix to move from FK5 to Galactic (approximative) */
   static public final double[][] gal_2000 = {
     {-0.054875539726e0, -0.873437108010e0, -0.483834985808e0},
     { 0.494109453312e0, -0.444829589425e0,  0.746982251810e0},
     {-0.867666135858e0, -0.198076386122e0,  0.455983795705e0}
   } ;

  //  ===========================================================
  //			Static Methods
  //  ===========================================================

  /*  Static methods (functions) in Java are very close to C ones;
      they do not require any object instanciation.
      Typical example of static methods are in the Math class
  */

   /* Conversions of Times between B(esselian Year) / J(ulian year) / JD  */
   /** Conversion of a Julian epoch to a Julian Date */
   public static final double J2JD(double y) {
      return JD_J2000 + (y-2000)*Jyr ;
   }
   /** Conversion of a Besselian epoch to a Julian Date */
   public static final double B2JD(double y) {
      return JD_B1900 + (y-1900)*Byr ;
   }

   /** Conversion of a Julian Date to a Julian  epoch */
   public static final double JD2J(double jd) {
      return 2000 + (jd-JD_J2000)/Jyr ; 
   }
   /** Conversion of a Julian Date to a Besselian epoch */
   public static final double JD2B(double jd) {
      return 1900 + (jd-JD_B1900)/Byr ; 
   }

   /** Conversion of a Besselian epoch to a Julian  epoch */
   public static final double B2J(double y) {
      return JD2J(B2JD(y)) ;
   }
   /** Conversion of a Julian epoch to a Besselian epoch */
   public static final double J2B(double y) {
      return JD2B(J2JD(y)) ;
   }

  //  ===========================================================
  //		Private Methods
  //  ===========================================================

  /** Generate the rotation matrix from the Euler angles
   * @param z, theta, zeta	the 3 Euler angles
   * @return R[3][3]		the rotation matrix
   * The rotation matrix is defined by:
   * $$ R = R_z(-z) \cdot R_y(\theta) \cdot R_z(-\zeta)$$
            |cos.z -sin.z  0|   |cos.the  0 -sin.the|   |cos.zet -sin.zet 0|
	  = |sin.z  cos.z  0| x |   0     1     0   | x |sin.zet  cos.zet 0|
	    |   0      0   1|   |sin.the  0  cos.the|   |   0        0    1|
   */
   static private double[][] EulerMatrix(double z, double theta, double zeta) {
      double R[][] = new double[3][3];
    	R[0][2] =  Coo.cosd(z);
    	R[1][2] =  Coo.sind(z);
    	R[2][2] =  Coo.cosd(theta);
      double  w =  Coo.sind(theta) ;
    	R[2][0] =  Coo.cosd(zeta);
    	R[2][1] =  Coo.sind(zeta);
    	R[0][0] =  R[2][0]*R[2][2]*R[0][2] - R[2][1]*R[1][2];
    	R[1][0] =  R[2][0]*R[2][2]*R[1][2] + R[2][1]*R[0][2];
    	R[0][1] = -R[2][1]*R[2][2]*R[0][2] - R[2][0]*R[1][2];
    	R[1][1] = -R[2][1]*R[2][2]*R[1][2] + R[2][0]*R[0][2];
    	R[2][0] =  R[2][0]*w;
    	R[2][1] = -R[2][1]*w;
    	R[0][2] = -w*R[0][2];
    	R[1][2] = -w*R[1][2];
	//* System.err.println("Compute tR . R ") ;
	//* System.err.println(Coo.toString(Coo.prod(Coo.t(R), R))) ;
      	return(R) ;
    }

   /** Fill the object: compute the angles,  or cosines
    * whatever is missing in the object
    */
    private void fill_coo() {
        if (status_coo == 0) { 
	    System.err.println ("++++Astroframe[" + getFrame() 
	       + "]: non-set position, North Pole assumed") ;
	    point.x = 0 ; point.y = 0 ; point.z = 1 ;
	    status_coo = 2 ;
	}
	if ((status_coo&2) == 0) {	// Compute the ux,uy,uz 
	    point.set(point.lon, point.lat) ;
	    status_coo |= 2 ;
	}
	if ((status_coo&1) == 0) {	// Compute lon,lat
	    point.set(point.x, point.y, point.z) ;
	    status_coo |= 1 ;
	}
   }

  /** Fill the object: compute the matrix which transforms the current 
   *  coordinate system into standard FK4 or FK5
   * @param choice	1 prefer B1950, 2 to prefer J2000
   * <BR>
   * Note that the absence of a rotation matrix implies a Unit Matrix
   */
   private void fill_frame (int choice) {
      double w, z, zeta, theta, t0, dt ;

	switch(frame) {
	  case FK4:
	    status_frame = 1 ;	// Indicates change to B1950.
	    if (equinox == 1950.) {
		if (matrix != null) matrix = null ;
		return ;
	    }
	    t0 = (1950.0  - 1900.0)/1000.0;
	    dt = (equinox - 1950.0)/1000.0 ;
	    zeta  = dt*(23042.53e0+ t0*(139.73e0+0.06e0*t0) 
		  + dt*(30.23e0+18.e0*dt-0.27e0*t0) ) /3600.e0;
	    z     = zeta + dt*dt*(79.27e0+0.66e0*t0+0.32e0*dt)/3600.e0;
	    theta = dt* (20046.85e0 - t0*(85.33e0+0.37*t0) 
		- dt*(42.67e0+0.37e0*t0+41.8e0*dt) )/3600.e0;
	    matrix = EulerMatrix(z, theta, zeta) ;
	    break ;

	  case ICRS:
	    equinox = 2000. ;	// NO BREAK
	  case FK5:
	    status_frame = 2 ;	// Indicates change to J2000.
	    if (equinox == 2000.) {
		if (matrix != null) matrix = null ;
		return ;
	    }
  	    t0 = 0 ;
  	    dt = (equinox - 2000.0)/100.e0;
  	    w = 2306.2181e0+(1.39656e0-0.000139e0*t0)*t0;	// w in arcsec
  	    zeta = (w + ( (0.30188e0-0.000344e0*t0) + 0.017998e0*dt) *dt)
		    *dt/3600.e0;  				// Degrees
  	    z    = (w + ( (1.09468e0+0.000066e0*t0) + 0.018203e0*dt) *dt)
		    *dt/3600.e0;				// Degrees
  	    theta = ( (2004.3109e0 + (-0.85330e0-0.000217e0*t0)*t0)
		  +( (-0.42665e0-0.000217e0*t0) - 0.041833e0*dt) *dt) 
		  * dt/3600.e0;
	    matrix = EulerMatrix(z, theta, zeta) ;
	    break ;

	  case GAL:
	    if (matrix == null) matrix = new double[3][3] ;
	    if (choice == 1) {
	        System.arraycopy(gal_1950, 0, matrix, 0, gal_1950.length) ;
	        status_frame = 1 ;	// Indicates change to B1950
	    } 
	    else {
	        System.arraycopy(gal_2000, 0, matrix, 0, gal_2000.length) ;
	        status_frame = 2 ;	// Indicates change to J2000
	    }
	    break ;

	  case SGAL:
	    if (choice == 1) {
	        status_frame = 1 ;	// Indicates change to B1950.
	        matrix = Coo.prod (supergal, gal_1950) ;
	    }
	    else {
	        status_frame = 2 ;	// Indicates change to J2000.
	        matrix = Coo.prod (supergal, gal_2000) ;
	    }
	    break ;

	  default:
	    throw new IllegalArgumentException(
	     "****Astroframe: frame not (yet) accepted: " + getFrame()) ;
	}

	//* System.err.println("fill_frame(" + choice + ") on " + getFrame()) ;
	//* System.err.println(Coo.toString(matrix)) ;
    }

  /* Compute FK5 position from FK4, assuming no proper motion in FK5
     and an observation in B1950
  */
  private double[] fk4to5z(double u[]) {
      double w, r, u0, u1, u2 ;
      double fk5[] = new double[3] ;
	// System.err.println("....fk4to5z: Cartesian: " + Coo.toString(u)) ;
	w  = 50.;
      	w  = u[0]*A[0] + u[1]*A[1] + u[2]*A[2] ; 	// e-term
	// System.err.println("....fk4to5z: w=" + w) ;
	//* Remove e-term **** Warning, using w1 = 1.+w looses precision !
	u0 = u[0] - A[0] + w*u[0]; 	//u[0]*w1 - A[0] ;
	u1 = u[1] - A[1] + w*u[1]; 	//u[1]*w1 - A[1] ;
	u2 = u[2] - A[2] + w*u[2]; 	//u[2]*w1 - A[2] ;
	// System.err.println("....fk4to5z:  A       : " + Coo.toString(A)) ;
	// fk5[0] = u0; fk5[1] = u1; fk5[2] = u2; 
	// System.err.println("....fk4to5z: Removed e: " + Coo.toString(fk5)) ;
	//* Modifications -------------
	// Renormalize the Unit Vector
	/**** This NEW Method just uses EM50 *********/
	fk5[0] = EM50[0][0]*u0 + EM50[0][1]*u1 + EM50[0][2]*u2 ;
	fk5[1] = EM50[1][0]*u0 + EM50[1][1]*u1 + EM50[1][2]*u2 ;
	fk5[2] = EM50[2][0]*u0 + EM50[2][1]*u1 + EM50[2][2]*u2 ;
	/******************************************/
	return(fk5) ;
  }

  /* Compute FK4 position from FK5, assuming no proper motion in FK5
     and an observation in B1950.
  */
  private double[] fk5to4z(double u[]) {
      double w, w1, r, v0, v1, v2 ;
      double fk4[] = new double[3] ;
	//* System.err.println("....fk5to4z called") ;
	// 6-vector Rotation. The velocity is zero. 
	v0 = EM1[0][0]*u[0] + EM1[0][1]*u[1] + EM1[0][2]*u[2] ;
	v1 = EM1[1][0]*u[0] + EM1[1][1]*u[1] + EM1[1][2]*u[2] ;
	v2 = EM1[2][0]*u[0] + EM1[2][1]*u[1] + EM1[2][2]*u[2] ;
	// Renormalize the Unit Vector
	r  = Math.sqrt(v0*v0 + v1*v1 + v2*v2) ;
	v0 /= r ; v1 /= r ; v2 /= r ;
	// Apply e-term
      	w  = v0*A[0] + v1*A[1] + v2*A[2] ; 	// e-term
	w1 = 1 - w ;
	//* Remove e-term (approximative)
	/*  ======== Old Code
	fk4[0] = v0*w1 + A[0]*r ;
	fk4[1] = v1*w1 + A[1]*r ;
	fk4[2] = v2*w1 + A[2]*r ;
	// recompute norm
	r  = Math.sqrt(fk4[0]*fk4[0] + fk4[1]*fk4[1] + fk4[2]*fk4[2]) ;
	//* Remove e-term (correct)
	fk4[0] = v0*w1 + A[0]*r ;
	fk4[1] = v1*w1 + A[1]*r ;
	fk4[2] = v2*w1 + A[2]*r ;
	===========*/
	fk4[0] = v0*w1 + A[0] ;
	fk4[1] = v1*w1 + A[1] ;
	fk4[2] = v2*w1 + A[2] ;
	return(fk4) ;
  }

  /** Round the value 
   * @param angle	Angle (degrees) to edit
   * @param precision	The precision
   * @param sexa	true if sexagesimal
   * @returns		Rounded value
   */
   static private double rounded (double angle, int precision, double gf[])
   {
      int nd =  precision - 1;
      double f = angle ;
      double rf ;
      int i ;
      
      	/* Round the Longitude / Latitude */
	if (nd < gf.length) rf = gf[nd] ;
	else {
	    i = nd-gf.length; 
	    rf = gf[gf.length-1];
	    while (i >= 0) { rf *= 10; i-- ; }
	}
	if (angle < 0) rf = -rf ;
	f = angle +  0.5/rf ;
	if (f < -180.0) f += 360.0 ;
	if (f >= 360.0) f -= 360.0 ;

	return(f) ;
   }

  /** Edit a Longitude or Latitude with the specified precision.
   * Rounding is executed.
   * @param buf		Buffer where the edited value is added
   * @param angle	Angle (degrees) to edit
   * @param precision	The precision of the coordinate
   * @param opt		One of the options ED_xxx
   * @returns		Number of bytes filled
   */
   static private int ed1(StringBuffer buf, double angle, int precision, 
      int opt) {
      int nd =  precision - 1;
      int len0 = buf.length() ;
      int ival, n ;
      char sep ;
      double f;
      
	// System.err.println("....ed1(" + angle + ", opt=" 
	//    + Integer.toHexString(opt) + ")") ;
      	/* Round the Longitude / Latitude */
	f = angle ;
	if ((opt&ED_FULL) == 0) 
	    f = rounded(angle, precision, (opt&ED_SEXA)!=0 ? qf : gf) ;
	else nd = 10 ;		// give 10 digits for full precision
	
	/* For a decimal number, just edit */
	if (((opt&ED_SEXA) == 0) || (precision < ARCMIN))
	    return Coo.ed1(buf, f, nd+4, nd, (opt&ED_SIGN)!=0) ;

	/* Separator may be a blank or a colon */
	sep = (opt&ED_COLON) != 0 ? ':' : ' ' ;
	/* RA: must divide by 15 */
	if ((opt&ED_RA)!=0) {
	    precision++ ; f = angle/15. ;
	    if ((opt&ED_FULL)==0) f = rounded(f, precision, qf) ;
	}

	/* Edit the sign */
	if (((opt&ED_SIGN)!=0) && (f>=0)) buf.append('+') ;
	if (f<0) { buf.append('-'); f = -f ; }

	/* Insert the degrees/hours: insert leading zeroes... */
	ival = (int)f; f -= ival ;
	n = 1000 ;
	if ((opt&(ED_SIGN|ED_RA))!=0) n = 100 ;
	if(ival >= n) { buf.append(ival/n); ival %= n; }
	while(n>1) { n /= 10; buf.append((char)('0' + ival/n)); ival %= n; }

	/* Edit the minutes */
	f *= 60.; ival = (int)f ; f -= ival ;
	buf.append(sep) ; 
	buf.append((char)('0' + ival/10)); 
	buf.append((char)('0' + ival%10)); 
	if (precision == ARCMIN+1) { 
	    f *= 10; ival = (int)f; 
	    buf.append('.'); 
	    buf.append((char)('0' + ival)) ;
	}

	/* Edit the seconds */
	if (precision >= ARCSEC) {
	    buf.append(sep) ; f *= 60.;
	    n = precision-ARCSEC ;	// Number of decimals 
	    Coo.ed1(buf, f, n==0 ? 2 : n+3, n, false) ;
	}

	return(buf.length() - len0) ;
   }

  //  ===========================================================
  //			Constructors
  //  ===========================================================

  /** Create an Astroframe objet: specify just frame, equinox, precision.
   * Actual positions are normally specified by the <B>set</B> method.
   * @param frame	FK4|FK5|ICRS|GAL|SGAL|ECL
   * @param precision	NONE|DEG|ARCMIN|ARCSEC|MAS [default ARCSEC+1]
   * @param equinox	The equinox (applicable for FK4 FK5 ECL frames)
   */
   public Astroframe(int frame, int precision, double equinox) {
	this (frame, 0., 90., precision, equinox) ;
	this.status_coo = 0 ;
   }

  /** Create a fully qualified position in a new Astroframe objet. 
   * @param frame	FK4|FK5|ICRS|GAL|SGAL|ECL
   * @param lon, lat	A position expressed in the frame
   * @param precision	NONE|DEG|ARCMIN|ARCSEC|MAS [default ARCSEC+1]
   * @param equinox	The equinox (applicable for FK4 FK5 ECL only)
   */
   public Astroframe(int frame, double lon, double lat, 
     int precision, double equinox) {
	if (Double.isNaN(equinox)) this.equinox = default_equinox[frame] ;
	else this.equinox   = equinox ;
	this.point = new Coo(lon, lat) ;
	this.status_coo = 3 ;
        this.equinox   = equinox ;
	this.frame     = (byte)frame ;
	this.oprecision= (byte)precision ;
	this.precision = this.oprecision ;
   }


  /** Create an Astroframe objet with default precision (0.1arcsec)
   * @param frame	FK4|FK5|ICRS|GAL|SGAL|ECL      
   * @param equinox	The equinox (applicable for FK4 FK5 ECL only)
   */
   public Astroframe(int frame, double equinox) {
	this(frame, ARCSEC+1, equinox) ;
   }

  /** Create an Astroframe objet with default equinox.
   * We specify in this creation the frame, 
   * @param frame	FK4|FK5|ICRS|GAL|SGAL|ECL      
   * @param precision	NONE|DEG|ARCMIN|ARCSEC|MAS [default ARCSEC]
   */
   public Astroframe(int frame, int precision) {
	this(frame, precision, default_equinox[frame]) ;
   }

  /** Create an Astroframe objet, for the standard equinox.
   * We specify in this creation the frame, 
   * @param frame	FK4|FK5|ICRS|GAL|SGAL|ECL      
   */
   public Astroframe(int frame) {
	this(frame, ARCSEC+1, default_equinox[frame]) ;
   }


  /** Create the default J2000 Astroframe
   */
   public Astroframe() {
	this(FK5, ARCSEC+1, 2000.) ;
   }

  //  ===========================================================
  //			Set a position in Astroframe
  //  ===========================================================

  /** Set a particuliar set of positions in the Astroframe.
   * @param lon,lat	Longitude + latitude in degrees (or RA + Dec)
   */
   public void set (double lon, double lat) {
       point.lon = lon ;
       point.lat = lat ;
       status_coo = 1 ;
   }

  /** Set a particuliar set of positions in the Astroframe from Cartesian 
   * @param lon,lat	Longitude + latitude in degrees (or RA + Dec)
   */
   public void set (double x, double y, double z) {
       point.x = x ;
       point.y = y ;
       point.z = z ;
       status_coo = 2 ;
   }

  /** Set a particuliar set of positions in the Astroframe.
   * The precision is adapted to the number of significant digits 
   * existing in the input text string.
   * @param text  Longitude + latitude in text
   */
   public void set (String text) throws Exception {
       point.set(text, (ed_opt[frame]&ED_RA)!=0) ;
       status_coo = 3 ;
       // Set Precision = number of decimals + 1.
       precision = (byte)(1 + (point.dlon > point.dlat ? 
           point.dlon : point.dlat)) ;
       // System.err.println("...set: precision=" + precision
       //     + ", point precisions=(" + point.dlon + "," + point.dlat + ")") ;
   }

   /** Change the precision of the data
    * @param  precision: integer number, typically one of the values NONE (0), 
    * 	DEG (1), ARCMIN (3), ARCSEC (5),  MAS (8);
    * 	use ARCSEC+1 for 0.1arcsec, MAS-1 for 10mas, etc...
    */
   public void setPrecision(int precision) {
       this.precision = (byte)precision ;
   }

  //  ===========================================================
  //			Get parts of Astroframe
  //  ===========================================================

   /** Get an explicit designation of the frame
     * @return	the explanation as a string
    */
   public final String getFrame() {
      switch(frame) {
	case FK4:  return ("B" + equinox) ;
	case FK5:  return ("J" + equinox) ;
	case ICRS: return ("ICRS") ;
	case ECL:  return ("Ecl" + equinox) ;
	default:   return explain_frame[frame] ;
      }
   }

   /** Get the precision of the current value
     * @return	the value.
    */
   public final int getPrecision() {
	return precision ;
   }

   /** Get a 3-item vector with Cartesian components (x, y, z)
    * @return	the vector with Cartesian coordinates
    */
   public double[] getUvector() {
      double u[] = new double[3] ;
      	if ((status_coo&2) == 0) fill_coo() ;
      	u[0] = point.x ;
      	u[1] = point.y ;
      	u[2] = point.z ;
      	return(u) ;
   }

   /** Get a 2-item vector with (lon, lat)
    * @return	the vector with angles in degrees
    */
   public double[] getAngles() {
      double o[] = new double[2] ;
      	if ((status_coo&1) == 0) fill_coo() ;
      	o[0] = point.lon ;
      	o[1] = point.lat ;
      	return(o) ;
   }

   /** Get the longitude part
    * @return	the longitude in degrees
    * <BR> The longitude is also accessible in the object (lon)
    */
   public double getLon() {
      	if ((status_coo&1) == 0) fill_coo() ;
      	return(point.lon) ;
   }

   /** Get the latitude part
    * @return	the latitude in degrees
    * <BR> The latitude is also accessible in the object (lat)
    */
   public double getLat() {
      	if ((status_coo&1) == 0) fill_coo() ;
      	return(point.lat) ;
   }

  //  ===========================================================
  //			Edit the Coordinates
  //  ===========================================================

  /* Convert the String of options into an integer */
  private static int getOptions(String text) throws Exception {
     char b[] = text.toCharArray();
     int i, n; int o = 0 ;
       for (i=0, n=b.length; i<n; i++) switch(b[i]) {
	   case 's': o |= ED_SEXA;    continue ;
	   case ':': o |= ED_COLON;   continue ;
	   case 'd': o |= ED_DECIMAL; continue ;
	   case 'f': o |= ED_FRAME  ; continue ;
	   case 'F': o |= ED_FULL|ED_FRAME; continue ;
	   case ' ': continue ;
	   default:  throw new Exception( //System.err.println(
	        "++++Astroframe, invalid edition option '" + b[i] 
	      + "' in \"" + text + "\"") ;
       }
       return(o) ;
  }

  /** Method to edit the Coordinates in a StringBuffer
   * @param  buf  Buffer where the result is appended
   * @param  len  The total length wished for the edition
   * @param opt   A mixture of the options ED_COLON, ED_DECIMAL, 
   *			ED_FULL, ED_SEXA, ED_FRAME
   * @returns	Number of bytes used in edition
   */
   private final int ed2 (StringBuffer buf, int len, int opt) {
      int precision = this.precision ;
      int option = opt ;
      int llon, llat, n ;	/* Length of Longitude + Latitude */
      int len0 = buf.length() ;

	// System.err.println("....ed2(opt=" + Integer.toHexString(opt) + ")") ;
	/* Fill first the missing parts ! */
      	if ((status_coo&1) == 0) fill_coo() ;
	if (len > 0) {
	    if (len<8) len = 8 ;	// Minimal accuracy
	    llat = len/2 ;
	    llon = len - llat ;
	}
	else llon = llat = 0 ;

	/* Edit in Decimal or Sexagesimal ? If not specified, use default */
	if ((option&(ED_DECIMAL|ED_SEXA)) == 0) 
	    option |= ed_opt[frame]&(ED_DECIMAL|ED_RA) ;

	if ((option&ED_DECIMAL)!=0) {
	    option &= ~(ED_SEXA|ED_RA) ;
	    if ((len > 0)  && (precision > (llat-3))) precision = llat-3 ;
	    n = ed1 (buf, point.lon, precision, option) ;	// Edit Lon
	    if (llon == 0) buf.append(' ') ;
	    else while (n < llon) { buf.append(' ') ; n++ ; }
	    n = ed1 (buf, point.lat, precision, option|ED_SIGN) ;
	}

	else { 	// Sexagesimal edition: number of bytes required 
	    option |= ED_SEXA|(ed_opt[this.frame]&ED_RA);
	    if (llat > 0) {		// Adjust the Precision from width
	        n = precision + 3 ;
	        if (precision > ARCMIN) n++ ;
	        if (precision > ARCSEC) n++ ;
	        while (n > llat) { 
		    precision-- ; 
		    n = precision + 3 ;
		    if (precision > ARCMIN) n++ ;
		    if (precision > ARCSEC) n++ ;
	        }
	    }
	    n = ed1 (buf, point.lon, precision, option) ; option &= ~ED_RA ;
	    if (llon == 0) buf.append(' ') ;
	    else while (n < llon) { buf.append(' ') ; n++ ; }
	    n = ed1 (buf, point.lat, precision, option|ED_SIGN) ;
	}
	while (n < llat) { buf.append(' ') ; n++ ; }

	/* Add the Frame designation, and the precision if necessary */
	if ((option&(ED_FRAME|ED_FULL)) != 0) {
	     buf.append(" (") ;
	    if ((option&ED_FRAME)!=0) buf.append(getFrame()) ;
	    if ((option&ED_FULL)!=0) {
	        buf.append((option&ED_FRAME)==0 ? " (" : " - ") ;
                buf.append(explain_precision[precision]) ;
	        buf.append(" precision") ;
	    }
	    buf.append(')') ;
	}
	return(buf.length() - len0) ;
   }

  /** Customized edition of Coordinates to a StringBuffer,
   *	with a specified width of the edited position.
   * @param buf	     the buffer filled with the edited position
   * @param options  a string with the option letters having the meaning:<PRE>
   *		d = edit in Decimal
   *		s = edit in Sexagesimal
   *		: = separate sexagesimal parts with the colon
   *		f = edit also the frame (system+equinox)
   *		F = edit in full precision (ignore the precision of the system)
   *</PRE>
   * @param len	  the width preferred for the edition -- precision truncated if
   *		  necessary to fit in the specified width.
   */
   public final int ed2 (StringBuffer buf, String options, int len) 
      throws Exception {
      return this.ed2(buf, len, getOptions(options)) ;
   }

  /** Customized edition of Coordinates to a StringBuffer.
   * @param buf		the buffer filled with the edited position
   * @param options	string with option letters (see other ed2 method)
   */
   public final int ed2 (StringBuffer buf, String options) 
     throws Exception {
      return this.ed2(buf, 0, getOptions(options)) ;
   }

  /** Customized edition of Coordinates to a StringBuffer
   * @param options  a string with the option letters (see ed2)
   * @param len      the width preferred for the edition
   * @see   ed2
   */
   public String toString(String options, int len) 
     throws Exception {
      StringBuffer buf = new StringBuffer(80) ;
      ed2(buf, len, getOptions(options)) ;
      return ""+buf ;
   }

  /** Customized edition of Coordinates to a StringBuffer
   * @param options  a string with the option letters (see ed2)
   * @see   ed2
   */
   public String toString(String options) throws Exception {
      return toString(options, 0) ;
   }

   /** Default edition: use what's stored */
   public String toString() {
      // System.out.println("equinox==" + equinox) ;
      // System.out.println("z=" + z) ;
      StringBuffer buf = new StringBuffer(80) ;
      ed2(buf, 0, ED_FRAME) ;
      return ""+buf ;
   }

  //  ===========================================================
  //			Convert Coordinates
  //  ===========================================================

  /** Express a celestial position in another Coordinate Frame
   * @param new:	another frame to which convert the coordinates
   */
  public void convert (Astroframe coo) {
      int i ;

	/* Verify first if frames identical -- then nothing to do ! */
	if (this.equals(coo)) return ;

	/* Precision is limited to the original one. */
	coo.precision = this.precision ;
	if (this.frame == coo.frame) {
	    if (Double.isNaN(default_equinox[this.frame])) return ;
	    if (this.equinox == coo.equinox) {
		fill_coo() ;
		coo.set(point.lon, point.lat) ;
	        return ;
	    }
	}

	/* Compute the matrices required to convert from the specified
	   frames to one of the standard B1950 or J2000 frames.
	*/
	if (this.status_frame == 0) {	// Matrix for input frame
	    /* Matrix for input frame.
	       We give the preference to J2000 standard frame if
	       the output frame is in J2000-related
	    */
	    i = coo.frame > 3 ? 2 : 1 ;	
	    fill_frame(i) ;
	}

	if (coo.status_frame == 0) {
    	    /* For the second frame, choose the standard
	       to minimize the operations.
	    */
	    coo.fill_frame (this.status_frame) ;
	}

	// Convert coordinates to Cartesian
	if ((this.status_coo&2) == 0) fill_coo() ;

	// Convert from Input to Standard with transposed Matrix
	double u[] = new double[3] ;
	//* u[0] = point.x; u[1] = point.y; u[2] = point.z;
	//* System.err.println("-0- " + Coo.toString(u)) ;
	if (matrix == null) { u[0] = point.x; u[1] = point.y; u[2] = point.z; }
	else for (i=0; i<3; i++)  {
	    // System.err.println("Rotate (1) with matrix (transposed)") ;
	    // System.err.println(Coo.toString(matrix)) ;
	    u[i] = matrix[0][i]*point.x + matrix[1][i]*point.y 
		 + matrix[2][i]*point.z ;
	}
	//* System.err.println("-1- " + Coo.toString(u)) ;
	
	// Move between FK4 / FK5 if necessary
	if (this.status_frame != coo.status_frame) {
	    if (this.status_frame == 1)  u = fk4to5z(u) ;
	    else u = fk5to4z(u) ;
	    //* System.err.println("-2- " + Coo.toString(u)) ;
	}

	// Finally, rotate to output frame to get the final position
	if (coo.matrix == null) {
	    coo.point.x = u[0] ;
	    coo.point.y = u[1] ;
	    coo.point.z = u[2] ;
	}
	else {
	    //* System.err.println("Rotate (2) with matrix") ;
	    //* System.err.println(Coo.toString(coo.matrix)) ;
	    coo.point.x = coo.matrix[0][0]*u[0] + coo.matrix[0][1]*u[1] 
	                + coo.matrix[0][2]*u[2] ;
	    coo.point.y = coo.matrix[1][0]*u[0] + coo.matrix[1][1]*u[1] 
	                + coo.matrix[1][2]*u[2] ;
	    coo.point.z = coo.matrix[2][0]*u[0] + coo.matrix[2][1]*u[1] 
	                + coo.matrix[2][2]*u[2] ;
	}
	u[0] = coo.point.x; u[1] = coo.point.y; u[2] = coo.point.z;
	//* System.err.println("-3- " + Coo.toString(u)) ;
	coo.status_coo = 2 ; 	// Cartesian Coordinates are set.
   }
}
