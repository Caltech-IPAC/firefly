/*
	Copyright 2008 Marco Mustapic
	
    This file is part of Agilar GWT Widgets.

    Agilar GWT Widgets is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Agilar GWT Widgets is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Agilar GWT Widgets.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.caltech.ipac.firefly.visualize.ui.color;

public class Color {
    private static final double FACTOR = 0.7;

	public static void HSVToRGB(float [] hsv, float [] rgb)
	{
		
		float r = 0.0f;
		float g = 0.0f;
		float b = 0.0f;
		float h = hsv[0];
		float s = hsv[1];
		float v = hsv[2];
		
		float hf = h / 60.0f;
		int i = (int) Math.floor(hf);
		float f = hf - i;
		float pv = v * (1 - s);
		float qv = v * (1 - s * f);
		float tv = v * (1 - s * (1 - f));
		
		switch (i)
		{
		// Red is the dominant color
		case 0:
			r = v;
			g = tv;
			b = pv;
			break;
			
			// Green is the dominant color
		case 1:
			r = qv;
			g = v;
			b = pv;
			break;
		case 2:
			r = pv;
			g = v;
			b = tv;
			break;
			
			// Blue is the dominant color
		case 3:
			r = pv;
			g = qv;
			b = v;
			break;
		case 4:
			r = tv;
			g = pv;
			b = v;
			break;
			
			// Red is the dominant color
		case 5:
			r = v;
			g = pv;
			b = qv;
			break;
			
			// Just in case we overshoot on our math by a little, we put these here. Since its a switch it won't slow us down at all to put these here.
		case 6:
			r = v;
			g = tv;
			b = pv;
			break;
		case -1:
			r = v;
			g = pv;
			b = qv;
			break;
		}
		
		rgb[0] = r;
		rgb[1] = g;
		rgb[2] = b;
	}

	public static void RGBToHSV(float [] rgb, float[] hsv)
	{
		float r = rgb[0];
		float g = rgb[1];
		float b = rgb[2];
		float h, s, v;
		
		float min = min3(r, g, b);
		float max = max3(r, g, b);
		
		// calculate value
		v = max; // value
		if (v == 0)
		{
			h = 0;
			s = 0;
		}
		
		// calculate saturation
		s = max - min;
		if (s == 0)
		{
			h = 0;
		}
		
		// calculate hue
		if (max == r)
		{
			h = 0.0f + 60.0f * (g - b);
			if (h < 0.0)
			{
				h += 360.0f;
			}
		}
		else if (max == g)
		{
			h = 120.0f + 60.0f * (b - r);
		}
		else
		/* max == b */{
			h = 240.0f + 60.0f * (r - g);
		}
		
		hsv[0] = h;
		hsv[1] = s;
		hsv[2] = v;
	}

	public static float min3(float v1, float v2, float v3)
	{
		float min;

		min = v1 < v2 ? v1 : v2;
		min = min < v3 ? min : v3;

		return min;
	}

	public static float max3(float v1, float v2, float v3)
	{
		float max;

		max = v1 > v2 ? v1 : v2;
		max = max > v3 ? max : v3;

		return max;
	}
	
	public static String toHex(int [] rgb)
	{
		String hex = "";
		hex += rgb[0]>=16?Integer.toHexString(rgb[0]):"0"+Integer.toHexString(rgb[0]);
		hex += rgb[1]>=16?Integer.toHexString(rgb[1]):"0"+Integer.toHexString(rgb[1]);
		hex += rgb[2]>=16?Integer.toHexString(rgb[2]):"0"+Integer.toHexString(rgb[2]);
		
		return hex;
	}
	
	public static void toInt(float [] frgb, int [] irgb)
	{
		irgb[0] = Math.round(frgb[0] * 255.0f);
		irgb[1] = Math.round(frgb[1] * 255.0f);
		irgb[2] = Math.round(frgb[2] * 255.0f);
	}

	public static void toFloat(int [] irgb, float [] frgb)
	{
		frgb[0] = ((float)irgb[0]) / 255.0f;
		frgb[1] = ((float)irgb[1]) / 255.0f;
		frgb[2] = ((float)irgb[2]) / 255.0f;
	}

    public static boolean isHexColor(String text) {
        boolean retval= false;
        if (text.length() == 6 || (text.length()==7 && text.startsWith("#"))) {
            if (text.startsWith("#")) text=text.substring(1);
            retval= true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.digit(c, 16) == -1) {
                    retval= false;
                    break;
                }
            }
        }
        return retval;
    }

    public static int [] toRGB(String colorStr) {
        int retval[]= null;
        if (isHexColor(colorStr)) {
            try {
                //Note: subString(1) returned a new instance, so isHexColor(colorStr) cannot remove "#" in colorStr.
                if (colorStr.startsWith("#")) colorStr=colorStr.substring(1);
                String rStr= colorStr.substring(0,2);
                String gStr= colorStr.substring(2,4);
                String bStr= colorStr.substring(4);
                int red= Integer.parseInt(rStr, 16);
                int green= Integer.parseInt(gStr, 16);
                int blue= Integer.parseInt(bStr, 16);
                retval= new int[] { red, green, blue};
            } catch (NumberFormatException e) {
                retval= null;
            }
        }
        return retval;
    }

    public static String brighter(String colorStr) {
        return brighter(colorStr,FACTOR);

    }


    public static String brighter(String colorStr,double factor) {
        int rgb[]= toRGB(colorStr);
        int r = rgb[0];
        int g = rgb[1];
        int b = rgb[2];

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int)(1.0/(1.0-factor));
        if ( r == 0 && g == 0 && b == 0) {
            return toHex(new int[] {Math.min((int)(i/factor), 255),
                                    Math.min((int)(i/factor), 255),
                                    Math.min((int)(i/factor), 255) } );
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return toHex(new int[] {Math.min((int)(r/factor), 255),
                                Math.min((int)(g/factor), 255),
                                Math.min((int)(b/factor), 255) } );
    }

    /**
     * Creates a new <code>Color</code> that is a darker version of this
     * <code>Color</code>.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this <code>Color</code> to create a darker version of
     * this <code>Color</code>.  Although <code>brighter</code> and
     * <code>darker</code> are inverse operations, the results of a series
     * of invocations of these two methods might be inconsistent because
     * of rounding errors.
     * @return  a new <code>Color</code> object that is
     *                    a darker version of this <code>Color</code>.
     * @see        java.awt.Color#brighter
     * @since      JDK1.0
     */
    public static String darker(String colorStr) {
        return darker(colorStr,FACTOR);
    }

    public static String darker(String colorStr, double factor) {
        int rgb[]= toRGB(colorStr);
        int r = rgb[0];
        int g = rgb[1];
        int b = rgb[2];
        return toHex(new int[] {Math.max((int)(r *factor), 0),
                                Math.max((int)(g *factor), 0),
                                Math.max((int)(b *factor), 0)});
    }


    /**
     * Make a simple color map array of no more than 10 entries.  Map goes from darker to brighter.
     * @param baseColor
     * @param mapSize
     * @return
     */
    public static String[] makeSimpleColorMap(String baseColor, int mapSize) {
        String c[]= null;
        if (mapSize<=1) {
            return new String[] {baseColor};
        }
        if (Color.isHexColor(baseColor)) {
            if (mapSize>10) mapSize= 10;

            int baseIdx= mapSize/5;

            int rgb[]= Color.toRGB(baseColor);
            int maxCnt= 0;
            int minCnt= 0;
            for(int idx : rgb) {
                if (idx>250) maxCnt++;
                if (idx<5)   minCnt++;
            }
            if ((maxCnt==1 && minCnt==2) ||(maxCnt==2 && minCnt==1) || maxCnt==3) {
                baseIdx= mapSize-1;
            }
            else if (minCnt==3) {
                baseIdx= 0;
            }


            c= new String[mapSize];
            double factor= (mapSize>5) ? .9 : .85;

            c[baseIdx]= baseColor;
            for(int i= baseIdx+1; (i<mapSize); i++) {
                c[i]= Color.brighter(c[i-1],factor);
            }
            for(int i= baseIdx-1; (i>=0); i--) {
                c[i]= Color.darker(c[i+1], factor);
            }
        }
        return c;
    }

}
