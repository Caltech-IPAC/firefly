package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldCreator;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldPanel;
import edu.caltech.ipac.util.dd.FieldDefSource;


/**
 * Date: Nov 7, 2007
 *
 * @author loi
 * @version $Id: FormBuilder.java,v 1.13 2012/05/16 01:39:04 loi Exp $
 */
public class FormBuilder {


    public static Widget createPanel(int labelWidth, String... propNames) {
        return createPanel(new Config(labelWidth), propNames);
    }

    public static Widget createPanel(Config config, String... propNames) {
        FieldDef[] fields = new FieldDef[propNames.length];
        for(int i = 0; i < fields.length; i++) {
            fields[i] = FieldDefCreator.makeFieldDef(propNames[i]);
        }
        return createPanel(config, fields);
    }

    public static Widget createPanel(int labelWidth, FieldDefSource... fdss) {
        return createPanel(new Config(labelWidth), fdss);
    }

    public static Widget createPanel(Config config, FieldDefSource... fdss) {
        FieldDef[] fields = new FieldDef[fdss.length];
        for(int i = 0; i < fields.length; i++) {
            fields[i] = FieldDefCreator.makeFieldDef(fdss[i]);
        }
        return createPanel(config, fields);
    }

    public static Widget createPanel(int labelWidth, FieldDef... fds) {
        return createPanel(new Config(labelWidth), fds);
    }

    public static Widget createPanel(Config config, FieldDef... fds) {
        InputField[] fields = new InputField[fds.length];
        for(int i = 0; i < fds.length; i++) {
            fields[i] = createField(fds[i]);
        }
        return createPanel(config, fields);
    }

    public static Widget createPanel(int labelWidth, InputField... fields) {
        return createPanel(new Config(labelWidth), fields);
    }

    /**
     * convenience method to create a panel and layout the fields.
     * @param config configuration
     * @param fields fields
     * @return panel widget
     */
    public static Widget createPanel(Config config, InputField... fields) {
        if (config.getDirection().equals(Config.Direction.HORIZONTAL)) {
            HorizontalPanel p = new HorizontalPanel();
            for(InputField f : fields) {
                p.add(layoutFields(config, f));
            }
            return p;
        } else {
            return layoutFields(config, fields);
        }
    }

    public static Widget createWidget(int labelWidth, int padding, Widget label, Widget field) {
        HorizontalPanel hp = new HorizontalPanel();
        hp.add(label);
        hp.add(field);
        hp.setCellWidth(label, labelWidth + "px");
        GwtUtil.setStyle(hp, "paddingLeft", padding+"px");
        return hp;
    }
    
    /*
     * used for Hydra
     */
    public static Widget createPanel(Config config, Widget... widgets) {
        if (config.getDirection().equals(Config.Direction.HORIZONTAL)) {
            HorizontalPanel hp = new HorizontalPanel();
            for (Widget w : widgets) {
              if (w instanceof InputField) {
                InputField f = (InputField)w;
                hp.add(layoutFields(config, f));
              } else {
                hp.add(w);
              }
            }
            return hp;

        } else {
            VerticalPanel vp = new VerticalPanel();
            for (Widget w : widgets) {
              if (w instanceof InputField) {
                InputField f = (InputField)w;
                vp.add(layoutFields(config, f));
              } else {
                vp.add(w);
              }
            }
            return vp;
        }
    }

    private static InputFieldPanel layoutFields(Config config, InputField... fields) {
        InputFieldPanel panel = new InputFieldPanel(config.getLabelWidth());
        panel.setPadding(config.getPadding());
        //int maxWidth = 0;
        for(InputField f : fields) {
            //int width= f.getFieldDef().getPreferWidth();
            //maxWidth = Math.max(width, maxWidth);
            panel.addUserField(f, config.getLabelAlign());
        }
        //panel.setDefaultWidth(maxWidth);
        return panel;
    }



    public static InputField createField(String propName) {
        return createField(FieldDefCreator.makeFieldDef(propName));
    }

    public static InputField createField(FieldDefSource fds) {
        return createField(FieldDefCreator.makeFieldDef(fds));
    }

    public static InputField createField(FieldDef fd) {
        InputField field= InputFieldCreator.createFieldWidget(fd);
        return SimpleInputField.needsWarnings(field) ?
               new ValidationInputField(field) : field;
    }


//====================================================================
//  static public methods
//====================================================================
    public static class Config {

        public static enum Direction {
            HORIZONTAL, VERTICAL
        }

        private Direction direction;
        private int labelWidth;
        private HorizontalPanel.HorizontalAlignmentConstant labelAlign;
        private int padding = 0;

        public Config() {
            this(Direction.VERTICAL, 100, HorizontalPanel.ALIGN_LEFT);
        }

        public Config(int labelWidth, int padding) {
            this(Direction.VERTICAL, labelWidth, padding, HorizontalPanel.ALIGN_LEFT);
        }

        public Config(int labelWidth) {
            this(Direction.VERTICAL, labelWidth, HorizontalPanel.ALIGN_LEFT);
        }

        public Config(int labelWidth, HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
            this(Direction.VERTICAL, labelWidth, labelAlign);
        }

        public Config(Direction direction, int labelWidth, HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
            this(direction, labelWidth, 10, labelAlign);
        }

        public Config(Direction direction, int labelWidth, int padding, HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
            this.direction = direction;
            this.labelWidth = labelWidth;
            this.labelAlign = labelAlign;
            this.padding = padding;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        public int getLabelWidth() {
            return labelWidth;
        }

        public void setLabelWidth(int labelWidth) {
            this.labelWidth = labelWidth;
        }

        public HorizontalPanel.HorizontalAlignmentConstant getLabelAlign() {
            return labelAlign;
        }

        public void setLabelAlign(HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
            this.labelAlign = labelAlign;
        }

        public int getPadding() {
            return padding;
        }

        public void setPadding(int padding) {
            this.padding = padding;
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
