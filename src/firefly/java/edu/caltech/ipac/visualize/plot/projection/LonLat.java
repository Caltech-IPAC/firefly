/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

/**
 * Represents a longitude/latitude coordinate pair in degrees.
 * This record provides a type-safe alternative to two-element arrays
 * for representing spherical coordinates.
 *
 * @param lon Longitude in degrees
 * @param lat Latitude in degrees
 */
public record LonLat(double lon, double lat) {}
