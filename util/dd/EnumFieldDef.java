package edu.caltech.ipac.util.dd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumFieldDef extends StringFieldDef {

    public enum Orientation {Vertical, Horizontal}

    private ArrayList<Item> enumValues;
    private Orientation orient = Orientation.Horizontal;

    public EnumFieldDef() {
        this(null);
    }

    public EnumFieldDef(String name) {
        super(name);
        enumValues = new ArrayList<Item>();
    }

    public void setOrientation(Orientation o) {
        orient = o;
    }

    public Orientation getOrientation() {
        return orient;
    }

    public boolean containsKey(String name) {
        boolean retval= false;
        for(Item item : enumValues) {
            if (item.getName().equals(name)) {
                retval= true;
                break;
            }
        }
        return retval;
    }

    public void addItem(String name, String title) {
        enumValues.add(new Item(name, title));
    }

    public void addItems(List<Item> items) {
        enumValues.addAll(items);
    }

    public void removeItem(String name) {
        for(int i=0; (i<enumValues.size());i++) {
            if (enumValues.get(i).getName().equals(name)) {
                enumValues.remove(i);
                break;
            }
        }
    }

    public List<Item> getEnumValues() {
        return Collections.unmodifiableList(enumValues);
    }

    public boolean validate(Object aValue)  throws ValidationException { return true; }

    public static class Item implements Serializable {
        private String name;
        private String title;
        private int    intVal;

        public Item() {}

        public Item(String name) {
            this(name, name);
        }

        public Item(String name, String title) {
            this.name = name;
            this.title = title == null ? name : title;
        }

        public Item(String name, int intVal, String title) {
            this.name = name;
            this.intVal= intVal;
            this.title = title == null ? name : title;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String desc) {
            this.title = desc;
        }

        public int getIntValue() {
            return intVal;
        }

        @Override
        public boolean equals(Object obj) {
            return name == null ? super.equals(obj) : name.equals(String.valueOf(obj));
        }

        @Override
        public int hashCode() {
            return name == null ? super.hashCode() : name.hashCode();
        }
    }
}