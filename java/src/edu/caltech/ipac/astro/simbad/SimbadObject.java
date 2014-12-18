package edu.caltech.ipac.astro.simbad;


/**
 * Contains data returned by Simbad for a single astronomical
 * object.
 *
 * @author	Xiuqin Wu
**/
public class SimbadObject
{
	private String _name; 
	private String _typeString = "";
	private String _morphology = null;
        private double _ra = Double.NaN;
        private double _dec = Double.NaN;
        private float  _raPM = Float.NaN;
        private float  _decPM = Float.NaN;
        private double _magnitude = Double.NaN;
        private String _magBand = "V";
        private double _redshift = Double.NaN;
        private double _radialVelocity = Double.NaN;
        private double _majorAxis = Double.NaN;
        private double _minorAxis = Double.NaN;
        private String _spectralType = "";
        private double _parallax = Double.NaN;
        private double _magnitudeB;

	/**
	 * Constructs a new SimbadObject initialized with empty values.
	**/
	public SimbadObject()
	{
	   _name = ""; 
	   _typeString = "";
	   _morphology = "";
	   _ra = 0.0;
	   _dec = 0.0;
	   _magnitude = 0.0;
	   _magBand = "V";
	   _redshift = 0.0;
	   _radialVelocity = 0.0;
	   _majorAxis = 0.0;
	   _minorAxis = 0.0;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	
	public String getType()
	{
		return _typeString;
	}
	
	public void setType(String type)
	{
		_typeString = type;
	}

	public String getMorphology()
	{
		return _morphology;
	}
	
	public void setMorphology(String morphology)
	{
		_morphology = morphology;
	}

	public double getRa()
	{
		return _ra;
	}
	
	public void setRa(double ra)
	{
		_ra = ra;
	}
	
	public double getDec()
	{
		return _dec;
	}
	
	public void setDec(double dec)
	{
		_dec = dec;
	}
	public float getRaPM()
	{
		return _raPM;
	}
	
	public void setRaPM(float raPM)
	{
		_raPM = raPM;
	}
	
	public float getDecPM()
	{
		return _decPM;
	}
	
	public void setDecPM(float decPM)
	{
		_decPM = decPM;
	}

	public double getMagnitude()
	{
		return _magnitude;
	}
	
	public void setMagnitude(double mag)
	{
		_magnitude = mag;
	}
	
	public String getMagBand()
	{
		return _magBand;
	}
	
	public void setMagBand(String mBand)
	{
		_magBand = mBand;
	}

        public double getBMagnitude() {
            return _magnitudeB;
        }

        public void setBMagnitude(double mag) {
            _magnitudeB = mag;
        }

	public double getRedshift()
	{
		return _redshift;
	}
	
	public void setRedshift(double redshift)
	{
		_redshift = redshift;
	}
	
	public double getRadialVelocity()
	{
		return _radialVelocity;
	}
	
	public void setRadialVelocity(double radialVelocity)
	{
		_radialVelocity = radialVelocity;
	}
	public double getMajorAxis()
	{
		return _majorAxis;
	}
	
	public void setMajorAxis(double majorAxis)
	{
		_majorAxis = majorAxis;
	}
	public double getMinorAxis()
	{
		return _minorAxis;
	}
	
	public void setMinorAxis(double minorAxis)
	{
		_minorAxis = minorAxis;
	}

        public String getSpectralType() {
            return _spectralType;
        }

        public void setSpectralType(String spectralType) {
            _spectralType = spectralType;
        }

        public double getParallax() {
            return _parallax;
        }

        public void setParallax(double parallax) {
            _parallax = parallax;
        }


        public String toString() {
            return "SimbadObject{" +
                    "_name='" + _name + "'" +
                    ", _typeString='" + _typeString + "'" +
                    ", _morphology='" + _morphology + "'" +
                    ", _ra=" + _ra +
                    ", _dec=" + _dec +
                    ", _raPM=" + _raPM +
                    ", _decPM=" + _decPM +
                    ", _magnitude=" + _magnitude +
                    ", _magBand='" + _magBand + "'" +
                    ", _redshift=" + _redshift +
                    ", _radialVelocity=" + _radialVelocity +
                    ", _majorAxis=" + _majorAxis +
                    ", _minorAxis=" + _minorAxis +
                    ", _spectralType='" + _spectralType + "'" +
                    ", _parallax=" + _parallax +
                    ", _magnitudeB=" + _magnitudeB +
                    "}";
        }
}


