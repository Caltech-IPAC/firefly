/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;

/**
* Creates a histogram of an image
* @author Booth Hartley
*/
public class Histogram
{
    private static int HISTSIZ2 = 4096;  /* full size of hist array */
    private static int HISTSIZ  = 2048;     /* half size of hist array */

    private int hist[];
    private double hist_min;
    private double hist_binsiz;
    private double iraf_min;
    private double iraf_max;

    /**
    * @param pixels8  array of byte pixels
    * @param blank_value  BLANK value in DN
    */
    public Histogram(byte[] pixels8, double blank_value)
    {
	int hist_datamin, hist_datamax;
	int pixval;

	hist_datamax = -Integer.MAX_VALUE;
	hist_datamin = Integer.MAX_VALUE;
	hist = new int[HISTSIZ2+1];

	/* bitpix 8 */
	for (int k = 0; k < pixels8.length; k++)
	{
	    /* interpret 8-bit signed as unsigned */
	    pixval = pixels8[k] & 0xff;

	    if (pixval != blank_value)
	    {
		//System.out.println("pixel = " + pixval
		// 	+ "  bin = " + (pixval + HISTSIZ));
		hist[pixval + HISTSIZ] ++;
		/* increment histogram - no need to check bounds */

		if (pixval < hist_datamin)
		    hist_datamin = pixval;
		if (pixval > hist_datamax)
		    hist_datamax = pixval;
	    }
	}
	hist_binsiz = 1;
	hist_min = -HISTSIZ * hist_binsiz;
	iraf_min = hist_datamin;
	iraf_max = hist_datamax;
    }

    /**
    * @param pixels16  array of short pixels
    * @param datamin   minimum value in DN
    * @param datamax   maximum value in DN
    * @param blank_value  BLANK value in DN
    */
    public Histogram(short[] pixels16, double datamin, double datamax,
	double blank_value)
    {
	double hist_max;
	double hist_datamin, hist_datamax;
	boolean redo_flag, doing_redo; 
	int i;
	int hist_min_index, hist_max_index;
	int goodpix, low_limit, low_sum, high_sum;

	if ((Double.isNaN(datamin)) || (Double.isNaN(datamax)))
	{
//	    if (SUTDebug.isDebug())
//		System.out.println("scanning for min, max . . ");
	    datamax = -Double.MAX_VALUE;
	    datamin = Double.MAX_VALUE;
	    for (int k = 0; k < pixels16.length; k++)
	    {
		if (pixels16[k] != blank_value)
		{
		    if (pixels16[k] < datamin)
			datamin = pixels16[k];
		    if (pixels16[k] > datamax)
			datamax = pixels16[k];
		}
	    }
	}

	hist = new int[HISTSIZ2+1];

	hist_datamax = -Double.MAX_VALUE;
	hist_datamin = Double.MAX_VALUE;

	hist_min = datamin;
	hist_max = datamax;
	doing_redo = false;
	for (;;)
	{
	    for (i=0; i<=HISTSIZ2; i++) hist[i]=0;
	    int underflow_count = 0;
	    int overflow_count = 0;
	    redo_flag = false;
	    hist_binsiz = (hist_max - hist_min) / HISTSIZ2;
	    if (hist_binsiz == 0.0)
		hist_binsiz = 1.0;
	    
	    if (SUTDebug.isDebug())
	    {
//		System.out.println("hist_min = " + hist_min);
//		System.out.println("hist_max = " + hist_max);
//		System.out.println("hist_binsiz = " + hist_binsiz);
	    }

	    /* bitpix 16 */
	    for (int k = 0; k < pixels16.length; k++)
	    {
		    if (pixels16[k] != blank_value)
		    {
			i = (int) ((pixels16[k] - hist_min) / hist_binsiz);
			//System.out.println("pixel = " + pixels16[k] 
			//	+ "  bin = " + i);
			if (i<0)
			    underflow_count++;
			else if (i>HISTSIZ2)
			    overflow_count++;
			else
			{
			    hist[i] ++;
			}
			if (pixels16[k] < hist_datamin)
			    hist_datamin = pixels16[k];
			if (pixels16[k] > hist_datamax)
			    hist_datamax = pixels16[k];
		    }
	    }

	    datamin = hist_datamin;
	    datamax = hist_datamax;

	    /* redo if more than 1% of pixels fell off histogram */
	    if (underflow_count > pixels16.length / .01)
		redo_flag = true;
	    if (overflow_count > pixels16.length / .01)
		redo_flag = true;

    /* check if we got a good spread */

    if ((!redo_flag) && (!doing_redo))  /* don't bother checking if we already want a redo */
    {
	    /* see what happens if we lop off top and bottom 0.05% of hist */
	    goodpix = 0;
	    for (i=0; i<HISTSIZ2; i++)
		goodpix += hist[i];
	    low_limit = (int) (goodpix * 0.0005);

	    high_sum = 0;
	    for (i = HISTSIZ2; i >= 0; i--)
	    {
		high_sum += hist[i];
		if (high_sum > low_limit)
		    break;
	    }
	    hist_max_index = i+1;

	    low_sum = 0;
	    for (i = 0; i<HISTSIZ2; i++)
	    {
		low_sum += hist[i];
		if (low_sum > low_limit)
		    break;
	    }
	    hist_min_index = i;


	    if ((hist_max_index - hist_min_index) < HISTSIZ)
	    {
		hist_max = (hist_max_index * hist_binsiz) + hist_min;
		hist_min = (hist_min_index * hist_binsiz) + hist_min;
		redo_flag = true;   /* we can spread it out by factor of 2 */
	    }
    }
    else
    {
	if (!doing_redo)
	{
	    hist_max = datamax;
	    hist_min = datamin;
	}
    }


	    if (SUTDebug.isDebug())
		System.out.println("done");

	    if ((!doing_redo) && (redo_flag))
	    {
		if (SUTDebug.isDebug())
		    System.out.println("rebuilding histogram . . ");
		doing_redo = true;
	    }
	    else
		break;
	}

	/*
	for (i = 2272; i < 2300; i++)
	    System.out.println("hist[" + i + "] = " + hist[i]);
	*/
	
	iraf_min = datamin;
	iraf_max = datamax;


    }

    /**
    * @param pixels32  array of int pixels
    * @param datamin   minimum value in DN
    * @param datamax   maximum value in DN
    * @param blank_value  BLANK value in DN
    */
    public Histogram(int[] pixels32, double datamin, double datamax,
	double blank_value)
    {
	double hist_max;
	double hist_datamin, hist_datamax;
	boolean redo_flag, doing_redo; 
	int i;
	int hist_min_index, hist_max_index;
	int goodpix, low_limit, low_sum, high_sum;

	if ((Double.isNaN(datamin)) || (Double.isNaN(datamax)))
	{
	    if (SUTDebug.isDebug())
		System.out.println("scanning for min, max . . ");
	    datamax = -Double.MAX_VALUE;
	    datamin = Double.MAX_VALUE;
	    for (int k = 0; k < pixels32.length; k++)
	    {
		if (pixels32[k] != blank_value)
		{
		    if (pixels32[k] < datamin)
			datamin = pixels32[k];
		    if (pixels32[k] > datamax)
			datamax = pixels32[k];
		}
	    }
	}

	hist = new int[HISTSIZ2+1];

	hist_datamax = -Double.MAX_VALUE;
	hist_datamin = Double.MAX_VALUE;

	hist_min = datamin;
	hist_max = datamax;
	doing_redo = false;
	for (;;)
	{
	    for (i=0; i<=HISTSIZ2; i++) hist[i]=0;
	    int underflow_count = 0;
	    int overflow_count = 0;
	    redo_flag = false;
	    hist_binsiz = (hist_max - hist_min) / HISTSIZ2;
	    if (hist_binsiz == 0.0)
		hist_binsiz = 1.0;
	    
	    if (SUTDebug.isDebug())
	    {
		System.out.println("hist_min = " + hist_min);
		System.out.println("hist_max = " + hist_max);
		System.out.println("hist_binsiz = " + hist_binsiz);
	    }

		/* bitpix 32 */
		for (int k = 0; k < pixels32.length; k++)
		{
			if (pixels32[k] != blank_value)
			{
			    i = (int) ((pixels32[k] - hist_min) / hist_binsiz);
			    //System.out.println("pixel = " + pixels32[k] 
			    //	+ "  bin = " + i);
			    if (i<0)
				underflow_count++;
			    else if (i>HISTSIZ2)
				overflow_count++;
			    else
			    {
				hist[i] ++;
			    }
			    if (pixels32[k] < hist_datamin)
				hist_datamin = pixels32[k];
			    if (pixels32[k] > hist_datamax)
				hist_datamax = pixels32[k];
			}
		}

	    datamin = hist_datamin;
	    datamax = hist_datamax;

	    /* redo if more than 1% of pixels fell off histogram */
	    if (underflow_count > pixels32.length / .01)
		redo_flag = true;
	    if (overflow_count > pixels32.length / .01)
		redo_flag = true;


    /* check if we got a good spread */

    if ((!redo_flag) && (!doing_redo))  /* don't bother checking if we already want a redo */
    {
	    /* see what happens if we lop off top and bottom 0.05% of hist */
	    goodpix = 0;
	    for (i=0; i<HISTSIZ2; i++)
		goodpix += hist[i];
	    low_limit = (int) (goodpix * 0.0005);

	    high_sum = 0;
	    for (i = HISTSIZ2; i >= 0; i--)
	    {
		high_sum += hist[i];
		if (high_sum > low_limit)
		    break;
	    }
	    hist_max_index = i+1;

	    low_sum = 0;
	    for (i = 0; i<HISTSIZ2; i++)
	    {
		low_sum += hist[i];
		if (low_sum > low_limit)
		    break;
	    }
	    hist_min_index = i;


	    if ((hist_max_index - hist_min_index) < HISTSIZ)
	    {
		hist_max = (hist_max_index * hist_binsiz) + hist_min;
		hist_min = (hist_min_index * hist_binsiz) + hist_min;
		redo_flag = true;   /* we can spread it out by factor of 2 */
	    }
    }
    else
    {
	if (!doing_redo)
	{
	    hist_max = datamax;
	    hist_min = datamin;
	}
    }


	    if (SUTDebug.isDebug())
		System.out.println("done");

	    if ((!doing_redo) && (redo_flag))
	    {
		if (SUTDebug.isDebug())
		    System.out.println("rebuilding histogram . . ");
		doing_redo = true;
	    }
	    else
		break;
	}

	iraf_min = datamin;
	iraf_max = datamax;


    }

    /**
    * @param pixelsm32  array of float pixels
    * @param datamin   minimum value in DN
    * @param datamax   maximum value in DN
    * @param blank_value  BLANK value in DN
    */
    public Histogram(float[] pixelsm32, double datamin, double datamax,
	double blank_value)
    {
	double hist_max;
	double hist_datamin, hist_datamax;
	boolean redo_flag, doing_redo; 
	int i;
	int hist_min_index, hist_max_index;
	int goodpix, low_limit, low_sum, high_sum;

	if ((Double.isNaN(datamin)) || (Double.isNaN(datamax)))
	{
	    if (SUTDebug.isDebug())
		System.out.println("scanning for min, max . . ");
	    datamax = -Double.MAX_VALUE;
	    datamin = Double.MAX_VALUE;
	    for (int k = 0; k < pixelsm32.length; k++)
	    {
		if (!Double.isNaN(pixelsm32[k]))
		{
		    if (pixelsm32[k] < datamin)
			datamin = pixelsm32[k];
		    if (pixelsm32[k] > datamax)
			datamax = pixelsm32[k];
		}
	    }
	}

	hist = new int[HISTSIZ2+1];

	hist_datamax = -Double.MAX_VALUE;
	hist_datamin = Double.MAX_VALUE;

	hist_min = datamin;
	hist_max = datamax;
	doing_redo = false;
	for (;;)
	{
	    for (i=0; i<=HISTSIZ2; i++) hist[i]=0;
	    int underflow_count = 0;
	    int overflow_count = 0;
	    redo_flag = false;
	    hist_binsiz = (hist_max - hist_min) / HISTSIZ2;
	    if (hist_binsiz == 0.0)
		hist_binsiz = 1.0;
	    
	    if (SUTDebug.isDebug())
	    {
		System.out.println("hist_min = " + hist_min);
		System.out.println("hist_max = " + hist_max);
		System.out.println("hist_binsiz = " + hist_binsiz);
	    }

		/* bitpix 32 */
		for (int k = 0; k < pixelsm32.length; k++)
		{
			if (!Double.isNaN(pixelsm32[k]))
			{
			    i = (int) ((pixelsm32[k] - hist_min) / hist_binsiz);
			    //System.out.println("pixel = " + pixelsm32[k] 
			    //	+ "  bin = " + i);
			    if (i<0)
			    {
				//redo_flag = true;   /* hist_min was bad */
				underflow_count++;
			    }
			    else if (i>HISTSIZ2)
			    {
				//redo_flag = true;   /* hist_max was bad */
				overflow_count++;
			    }
			    else
			    {
				hist[i] ++;
			    }
			    if (pixelsm32[k] < hist_datamin)
				hist_datamin = pixelsm32[k];
			    if (pixelsm32[k] > hist_datamax)
				hist_datamax = pixelsm32[k];
			}
		}

	    datamin = hist_datamin;
	    datamax = hist_datamax;
	    if (SUTDebug.isDebug())
	    {
		System.out.println("underflow_count = " + underflow_count +
		    "   overflow_count = " + overflow_count);
	    }

	    /* redo if more than 1% of pixels fell off histogram */
	    if (underflow_count > pixelsm32.length / .01)
		redo_flag = true;
	    if (overflow_count > pixelsm32.length / .01)
		redo_flag = true;

    /* check if we got a good spread */

    if ((!redo_flag) && (!doing_redo))  /* don't bother checking if we already want a redo */
    {
	    /* see what happens if we lop off top and bottom 0.05% of hist */
	    goodpix = 0;
	    for (i=0; i<HISTSIZ2; i++)
		goodpix += hist[i];
	    low_limit = (int) (goodpix * 0.0005);

	    high_sum = 0;
	    for (i = HISTSIZ2; i >= 0; i--)
	    {
		high_sum += hist[i];
		if (high_sum > low_limit)
		    break;
	    }
	    hist_max_index = i+1;

	    low_sum = 0;
	    for (i = 0; i<HISTSIZ2; i++)
	    {
		low_sum += hist[i];
		if (low_sum > low_limit)
		    break;
	    }
	    hist_min_index = i;


	    if ((hist_max_index - hist_min_index) < HISTSIZ)
	    {
		hist_max = (hist_max_index * hist_binsiz) + hist_min;
		hist_min = (hist_min_index * hist_binsiz) + hist_min;
		redo_flag = true;   /* we can spread it out by factor of 2 */
//		System.out.println("RBH new hist_min = " + hist_min +
//		    "  hist_max = " + hist_max);
	    }
    }
    else
    {
	if (!doing_redo)
	{
	    hist_max = datamax;
	    hist_min = datamin;
	}
    }


	    if (SUTDebug.isDebug())
		System.out.println("done");

	    if ((!doing_redo) && (redo_flag))
	    {
		if (SUTDebug.isDebug())
		    System.out.println("rebuilding histogram . . ");
		doing_redo = true;
	    }
	    else
		break;
	}

	iraf_min = datamin;
	iraf_max = datamax;


    }

    /**
    * @param pixelsm64  array of double pixels
    * @param datamin   minimum value in DN
    * @param datamax   maximum value in DN
    * @param blank_value  BLANK value in DN
    */
    public Histogram(double[] pixelsm64, double datamin, double datamax,
	double blank_value)
    {
	double hist_max;
	double hist_datamin, hist_datamax;
	boolean redo_flag, doing_redo; 
	int i;
	int hist_min_index, hist_max_index;
	int goodpix, low_limit, low_sum, high_sum;

	if ((Double.isNaN(datamin)) || (Double.isNaN(datamax)))
	{
	    if (SUTDebug.isDebug())
		System.out.println("scanning for min, max . . ");
	    datamax = -Double.MAX_VALUE;
	    datamin = Double.MAX_VALUE;
	    for (int k = 0; k < pixelsm64.length; k++)
	    {
		if (!Double.isNaN(pixelsm64[k]))
		{
		    if (pixelsm64[k] < datamin)
			datamin = pixelsm64[k];
		    if (pixelsm64[k] > datamax)
			datamax = pixelsm64[k];
		}
	    }
	}

	hist = new int[HISTSIZ2+1];

	hist_datamax = -Double.MAX_VALUE;
	hist_datamin = Double.MAX_VALUE;

	hist_min = datamin;
	hist_max = datamax;
	doing_redo = false;
	for (;;)
	{
	    for (i=0; i<=HISTSIZ2; i++) hist[i]=0;
	    int underflow_count = 0;
	    int overflow_count = 0;
	    redo_flag = false;
	    hist_binsiz = (hist_max - hist_min) / HISTSIZ2;
	    if (hist_binsiz == 0.0)
		hist_binsiz = 1.0;
	    
	    if (SUTDebug.isDebug())
	    {
		System.out.println("hist_min = " + hist_min);
		System.out.println("hist_max = " + hist_max);
		System.out.println("hist_binsiz = " + hist_binsiz);
	    }

		/* bitpix 64 */
		for (int k = 0; k < pixelsm64.length; k++)
		{
			if (!Double.isNaN(pixelsm64[k]))
			{
			    i = (int) ((pixelsm64[k] - hist_min) / hist_binsiz);
			    //System.out.println("pixel = " + pixelsm64[k] 
			    //	+ "  bin = " + i);
			    if (i<0)
				underflow_count++;
			    else if (i>HISTSIZ2)
				overflow_count++;
			    else
			    {
				hist[i] ++;
			    }
			    if (pixelsm64[k] < hist_datamin)
				hist_datamin = pixelsm64[k];
			    if (pixelsm64[k] > hist_datamax)
				hist_datamax = pixelsm64[k];
			}
		}

	    datamin = hist_datamin;
	    datamax = hist_datamax;

	    /* redo if more than 1% of pixels fell off histogram */
	    if (underflow_count > pixelsm64.length / .01)
		redo_flag = true;
	    if (overflow_count > pixelsm64.length / .01)
		redo_flag = true;

    /* check if we got a good spread */

    if ((!redo_flag) && (!doing_redo))  /* don't bother checking if we already want a redo */
    {
	    /* see what happens if we lop off top and bottom 0.05% of hist */
	    goodpix = 0;
	    for (i=0; i<HISTSIZ2; i++)
		goodpix += hist[i];
	    low_limit = (int) (goodpix * 0.0005);

	    high_sum = 0;
	    for (i = HISTSIZ2; i >= 0; i--)
	    {
		high_sum += hist[i];
		if (high_sum > low_limit)
		    break;
	    }
	    hist_max_index = i+1;

	    low_sum = 0;
	    for (i = 0; i<HISTSIZ2; i++)
	    {
		low_sum += hist[i];
		if (low_sum > low_limit)
		    break;
	    }
	    hist_min_index = i;


	    if ((hist_max_index - hist_min_index) < HISTSIZ)
	    {
		hist_max = (hist_max_index * hist_binsiz) + hist_min;
		hist_min = (hist_min_index * hist_binsiz) + hist_min;
		redo_flag = true;   /* we can spread it out by factor of 2 */
	    }
    }
    else
    {
	if (!doing_redo)
	{
	    hist_max = datamax;
	    hist_min = datamin;
	}
    }


	    if (SUTDebug.isDebug())
		System.out.println("done");

	    if ((!doing_redo) && (redo_flag))
	    {
		if (SUTDebug.isDebug())
		    System.out.println("rebuilding histogram . . ");
		doing_redo = true;
	    }
	    else
		break;
	}

	iraf_min = datamin;
	iraf_max = datamax;


    }

    /**
    * get_sigma
    * set DN corresponding to a sigma on the histogram
    * Code stolen from Montage mJPEG.c from Serge Monkewitz
    * @param sigma_value  The sigma on the histogram 
    * @param round_up Use the upper edge of the bin (versus the lower edge)
    * @return    The DN value in the image corresponding to the sigma
    */
    public double get_sigma(double sigma_value, boolean round_up)
    {
	double lev16 = get_pct(16., round_up);
	double lev50 = get_pct(50., round_up);
	double lev84 = get_pct(84., round_up);
	double sigma = (lev84 - lev16) / 2;
	return(lev50 + sigma_value * sigma);
    }


    /**
    * @param ra_value  The percentile on the histogram (99.0 signifies 99%)
    * @param round_up Use the upper edge of the bin (versus the lower edge)
    * @return    The DN value in the image corresponding to the percentile
    */
    public double get_pct(double ra_value, boolean round_up)
    {
	int sum, goal, i;
	int goodpix;

	if (ra_value == 0.0)
	    return iraf_min;
	if (ra_value == 100.0)
	    return iraf_max;

	goodpix = 0;
	for (i=0; i<HISTSIZ2; i++)
	    goodpix += hist[i];
	sum = 0;
	goal = (int)  (goodpix * (ra_value) / 100);
	i = -1;
	do
	{
	    i++;
	    sum = sum + hist[i];
	} while (sum < goal);
	if (SUTDebug.isDebug())
	{
	    System.out.println("goodpix = " + goodpix
		+ "   goal = " + goal
		+ "   i = " + i
		+ "   hist_binsiz = " + hist_binsiz);
	}
	if (round_up)
	    return( (i + 1.0) * hist_binsiz + hist_min);
	else
	    return( (i) * hist_binsiz + hist_min);
    }

    /**
    * @return    A pointer to the histogram array
    */
   public int [] getHistogramArray() { 
       int retHist[] = new int[hist.length];
       System.arraycopy(hist, 0, retHist, 0, hist.length);
       return retHist;
   }

   
   
    /**
    * @return    The minimum DN value in the image 
    */
    public double getDNMin() { return iraf_min; }

    /**
    * @return    The maximum DN value in the image 
    */
   public double getDNMax() { return iraf_max; }

    /**
    * @param bin The bin index in the histogram
    * @return    The DN value in the image corresponding to the specified bin
    */
   public double getDNfromBin(int bin) 
   { 
       return (bin * hist_binsiz + hist_min); 
   }

    /**
    * @param dn The DN value
    * @return    The histogram index corresponding to the DN value
    */
   public int getBinfromDN(double dn) 
   { 
       int bin = (int) ((dn - hist_min) / hist_binsiz);
       if (bin >= HISTSIZ2)
	   bin = HISTSIZ2 - 1;
       if (bin < 0)
	   bin = 0;
       return bin;
   }

    /**
    * @param pct The percentile on the histogram (99.0 signifies 99%)
    * @param round_up Use the upper edge of the bin (versus the lower edge)
    * @return    The histogram index corresponding to the percentile
    */
   public int getBINfromPercentile(double pct, boolean round_up) 
   { 
       double dn =  get_pct(pct, round_up);
       int bin = (int) ((dn - hist_min) / hist_binsiz);
       if (bin >= HISTSIZ2)
	   bin = HISTSIZ2 - 1;
       if (bin < 0)
	   bin = 0;
       return bin;
   }

    /**
    * @param sigma The sigma multiplier (-2 signifies 2 sigma below the mean)
    * @param round_up Use the upper edge of the bin (versus the lower edge)
    * @return    The histogram index corresponding to the percentile
    */
   public int getBINfromSigma(double sigma, boolean round_up) 
   { 
       double dn =  get_sigma(sigma, round_up);
       int bin = (int) ((dn - hist_min) / hist_binsiz);
       if (bin >= HISTSIZ2)
	   bin = HISTSIZ2 - 1;
       if (bin < 0)
	   bin = 0;
       return bin;
   }

    /**
    * @param tbl An int array [256] to be filled with the histogram equalized values
    */
    public void eq_tbl(int tbl[]) 
    { 
	int goodpix, accum, tblindex, hist_index;
	double next_goal, goodpix_255;

	goodpix = 0;
	for (hist_index = 0; hist_index < HISTSIZ2; hist_index++)
	    goodpix += hist[hist_index];
	goodpix_255 = goodpix / 255.0;

	tblindex = 0;
	tbl[tblindex++] = (int) hist_min;
	next_goal = goodpix_255;
	hist_index = 0;
	accum = 0;
	while ((hist_index < HISTSIZ2) && (tblindex < 255))
	{
	    if (accum >= next_goal)
	    {
		//System.out.println("RBH setting tbl[" + tblindex + "] to " +
		//	(int) (hist_index * hist_binsiz + hist_min));
		tbl[tblindex++] = (int) (hist_index * hist_binsiz + hist_min);
		next_goal += goodpix_255;
	    }
	    else
	    {
		accum += hist[hist_index++];
	    }
	}
	while (tblindex < 255)
	    tbl[tblindex++] = (int) (hist_index * hist_binsiz + hist_min);
	tbl[255] = Integer.MAX_VALUE;
	
    }

    /**
    * @param tbl An double array [256] to be filled with the histogram equalized values
    */
    public void deq_tbl(double tbl[]) 
    { 
	int goodpix, accum, tblindex, hist_index;
	double next_goal, goodpix_255;

	goodpix = 0;
	for (hist_index = 0; hist_index < HISTSIZ2; hist_index++)
	    goodpix += hist[hist_index];
	goodpix_255 = goodpix / 255.0;

	tblindex = 0;
	tbl[tblindex++] = hist_min;
	next_goal = goodpix_255;
	hist_index = 0;
	accum = 0;
	while ((hist_index < HISTSIZ2) && (tblindex < 255))
	{
	    if (accum >= next_goal)
	    {
		//System.out.println("RBH setting tbl[" + tblindex + "] to " +
	        //	(int) (hist_index * hist_binsiz + hist_min));
		tbl[tblindex++] = (hist_index * hist_binsiz + hist_min);
		next_goal += goodpix_255;
	    }
	    else
	    {
		accum += hist[hist_index++];
	    }
	}
	while (tblindex < 255)
	    tbl[tblindex++] = (hist_index * hist_binsiz + hist_min);
	tbl[255] = Double.MAX_VALUE;
	
    }

}



