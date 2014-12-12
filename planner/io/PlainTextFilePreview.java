package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.util.action.ClassProperties;

import javax.swing.*;
import java.beans.*;
import java.io.*;
import java.awt.*;
/**
* Class used to preview text files in SPOT IO dialogs.
*
* @author Carlos Campos
* @version $Id: PlainTextFilePreview.java,v 1.3 2006/07/10 22:16:58 tatianag Exp $
*/

public class PlainTextFilePreview extends JComponent
        implements PropertyChangeListener
{
    private final static ClassProperties prop =
        new ClassProperties(PlainTextFilePreview.class);

    private JTextArea textArea =
               new JTextArea();
    private JScrollPane scrollPane = new JScrollPane(textArea);
    private JLabel label = new JLabel();


    private final static String SELECT_FILE_MSG = prop.getName("SelectFileMsg");
    private final static String PREVIEW_HEADER_A = prop.getName("PreviewHeaderA");
    private final static String PREVIEW_HEADER_B = prop.getName("PreviewHeaderB");
    private final static int NUMBER_OF_LINES = prop.getIntValue("NumberOfLines");

    public PlainTextFilePreview (JFileChooser fc)
    {
        textArea.setEditable(false);
        textArea.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setPreferredSize(new Dimension(500,150));
        label.setPreferredSize(new Dimension(500,30));
        label.setText(SELECT_FILE_MSG);

        this.setBorder(BorderFactory.createEtchedBorder());
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(label, BorderLayout.NORTH);
        fc.addPropertyChangeListener(this);
    }

    void loadText(File selectedFile)
    {
        StringBuffer sb = new StringBuffer(50);
        if(selectedFile != null)
        {
            try {
                StringWriter sw = new StringWriter(50);
                PrintWriter pw = new PrintWriter(sw);

                BufferedReader br = new BufferedReader(new FileReader(selectedFile));
                sb.append(PREVIEW_HEADER_A);
                sb.append(NUMBER_OF_LINES);
                sb.append(PREVIEW_HEADER_B);
                label.setText(sb.toString());

                String line;
                for (int i=0; i<NUMBER_OF_LINES; i++) {
                    line = br.readLine();
                    if (line == null)
                       break;
                    pw.println(line);
                }
                pw.close();
                br.close();
                textArea.setText(sw.toString());
            }
            catch(IOException e) {
                System.out.println("PlainTextFilePreview.loadText " +
                                   "IO Exception " + e);
            }
        }
    }



    public void propertyChange (PropertyChangeEvent e) {


        if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)){
            File selectedFile = (File)e.getNewValue();
            if (isShowing()){
                loadText(selectedFile);
            }
        }
    }

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
