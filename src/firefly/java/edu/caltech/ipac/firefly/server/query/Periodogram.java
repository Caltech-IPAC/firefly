package edu.caltech.ipac.firefly.server.query;

/**
 * Created by ejoliet on 8/19/16.
 */
public interface Periodogram {


    /**
     * @return algo definition used to compute this
     */
    public AlgorithmDefinition getAlgoDef();

    /**
     * @return number of peaks
     */
    public int getNPeaks();

    /**
     * @return the Period object
     */
    public Period getPeriod(); // using min, max, see Period

    /**
     * @return array of values of size defined by AlgorithmDefinition#getNParameters
     */
    public double[] getAlgoValues(); // see AlgorithmDefinition#getNParameters()

    public StepMethod getStepMethod(StepMethod.STEPMETHOD_NAME sName);
}

/**
 * Period interface to handle periodogram period range used and value peak result
 */
interface Period {

    float getMin();

    float getMax();

    float getPeakValue();
}

/**
 * Handle step method definition, expecting a name and a step size associated
 */
interface StepMethod {
    enum STEPMETHOD_NAME {FIXED_PERIOD, FIXED_FREQUENCY}

    String getName();

    float getValue(); //Step size for fixed step methods
}
