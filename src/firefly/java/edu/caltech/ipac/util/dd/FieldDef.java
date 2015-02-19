/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;

import java.io.Serializable;

public interface FieldDef extends UIAttributes, Serializable {

	public boolean validate(Object aValue) throws ValidationException;

    /**
     * Do a soft validation.  If the field has invalid input but as the use continues to type it could good then
     * mark it as valid.
     * @param aValue the object to validate
     * @return true, if might be valid; false, if invalid
     * @throws ValidationException if the soft validate fails
     */
    public boolean validateSoft(Object aValue) throws ValidationException;

	public String getDefaultValueAsString();

    public boolean isUsingPreference();

    public String getPreferenceKey();

	public String getErrMsg();

	public int getMaxWidth();

	public int getPreferWidth();

	public boolean isNullAllow();
    public void setNullAllow(boolean flg);

    public String getId();

    /**
     * The mask to use to validate key strokes in a field, the mask should be a regular expression
     * When mask is null then the mask validation is disable and
     * the validate method is called on keystrokes
     * When mask is an empty string then mask validation is disable and
     * the validate method is not called on keystrokes
     * @return the mask regular expression to compare the keyboard input to
     */
	public String getMask();

    /**
     * This is a hint to the UI that the label text and tool tip text will not change during the lifetime
     * of the field. The UI will optimize how the label and tool tip text is created
     * @return true is the text will never change, false otherwise
     */
    public boolean isTextImmutable();

}