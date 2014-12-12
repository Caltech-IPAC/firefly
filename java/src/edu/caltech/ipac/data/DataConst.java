package edu.caltech.ipac.data;

/**
 * SODB package constants.
 * <BR>
 * Copyright (C) 1999 California Institute of Technology. All rights reserved.<BR>
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged.
 * <BR>
 * @version $Id: DataConst.java,v 1.3 2011/12/23 18:22:51 booth Exp $
 * @author <a href="mailto:jchavez@ipac.caltech.edu?subject=Java Docs">Joe Chavez</a>
 */
public class DataConst {
    private DataConst() {
    } // no constructor

    /**
     * Value representing "NULL" for short's (java.lang.Short.MIN_VALUE)
     */
    public static final short NULL_SHORT = Short.MIN_VALUE;

    /**
     * Value representing "NULL" for int's (java.lang.Integer.MIN_VALUE)
     */
    public static final int NULL_INT = Integer.MIN_VALUE;

    /**
     * Value representing "NULL" for long's (java.lang.Long.MIN_VALUE)
     */
    public static final long NULL_LONG = Long.MIN_VALUE;

    /**
     * Value representing "NULL" for float's (java.lang.Float.NaN)
     *   (Do not compare with value==NULL_FLOAT.  It won't work since nothing
     *   is EVER considered equal to NaN.  Use Float.isNaN(value) instead.
     */
    public static final float NULL_FLOAT = Float.NaN;

    /**
     * Value representing "NULL" for double's (java.lang.Double.NaN)
     *   (Do not compare with value==NULL_DOUBLE.  It won't work since nothing
     *   is EVER considered equal to NaN.  Use Double.isNaN(value) instead.
     */
    public static final double NULL_DOUBLE = Double.NaN;

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
