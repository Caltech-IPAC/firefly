package edu.caltech.ipac.client;

import javax.swing.ImageIcon;


/**
 * Get the Icons that uses throughout the program.  Each method in this 
 * interface returns a ImageIcon that is used in error dialogs or the
 * splash screen.
 *
 *
 * @author Trey Roby
 * @version $Id: OptionPaneIcons.java,v 1.1 2006/02/06 21:19:38 roby Exp $
 *
 */
public interface OptionPaneIcons {

    /**
     * This is the image used when error messages are displayed. 
     * i.e. icon to show on Error JOptionPanes
     */
   public abstract ImageIcon getErrorIcon();
    /**
     * This is the image used when information messages are displayed. 
     * i.e. icon to show on Informational JOptionPanes
     */
   public abstract ImageIcon getInfoIcon();
    /**
     * This is the image used when warning messages are displayed. 
     * i.e. icon to show on Warning JOptionPanes
     */
   public abstract ImageIcon getWarningIcon();
    /**
     * This is the image used when question messages are displayed. 
     * i.e. icon to show on Question JOptionPanes
     */
   public abstract ImageIcon getQuestionIcon();
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
