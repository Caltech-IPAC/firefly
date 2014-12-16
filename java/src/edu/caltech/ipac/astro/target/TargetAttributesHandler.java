package edu.caltech.ipac.astro.target;

import java.util.Map;

/**
 * This interface provide an abstract way of handling Target's attributes.
 */
public interface TargetAttributesHandler {

    /**
     * Populate the given target with attributes created from the list
     * of keyword/value pair.
     * @param target
     * @param attributes
     * @throws InvalidTargetException   if the resulting target is not valid.
     */
    void setAttributes(Target target, Map<String, String> attributes) throws InvalidTargetException;

    /**
     * Extract the attributes from the given target and return it as a list of key/value pair.
     * @param target
     * @return a map of target name and attributes
     */
    Map<String, String> getAttributes(Target target);

    /**
     * Invoked after target name resolution.  This method should populate the
     * given target with the attributes collected from the name resolution.
     * @param target
     * @param attribute
     */
    void setResolvedAttributes(Target target, TargetAttribute attribute);

    /**
     * Invoke this method to do a validation check of this target including its attribute.
     * @param target
     * @throws InvalidTargetException
     */
    void validate(Target target) throws InvalidTargetException;

}
