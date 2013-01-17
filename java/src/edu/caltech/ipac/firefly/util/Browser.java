package edu.caltech.ipac.firefly.util;

public  enum Browser {
    FIREFOX("Firefox"),
    SEAMONKEY("Seamonkey"),
    SAFARI("Safari"),
    WEBKIT_GENERIC("Webkit-Generic"),
    IE("IE"),
    OPERA("Opera"),
    CHROME("Chrome"),
    UNKNOWN("Unknown");

    private String desc;
    Browser(String desc) { this.desc= desc;}
    public String getDesc() { return desc;  }
}