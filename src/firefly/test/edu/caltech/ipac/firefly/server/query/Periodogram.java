/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

/**
 * Utilities classes to help (pre)define periodogram algorithm
 *
 * @author ejoliet
 */
public interface Periodogram {

    public enum AlgorithmDefinition {

        LS("Lomb-Scargle");

        String name = null;
        int nParameters = 0;

        AlgorithmDefinition(String name) {
            this.name = name;
        }

        AlgorithmDefinition(String name, int npars) {
            this.name = name;
            this.nParameters = npars;
        }

        public int getNParameters() {
            return this.nParameters;
        }

        public String getName() {
            return this.name;
        }
    }

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