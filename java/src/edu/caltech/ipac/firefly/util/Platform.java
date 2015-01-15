/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

public  enum Platform {
    MAC("Mac"),
    WINDOWS("Windows"),
    LINUX("Linux"),
    SOLARIS("Solaris"),
    SUNOS("SunOS"),
    HPUX("HP/UX"),
    AIX("AIX"),
    IPHONE("Iphone"),
    IPAD("Ipad"),
    ANDROID("Android"),
    FREE_BSD("FreeBSD"),
    SYMBIAN_OS("SymbianOS"),
    J2ME("J2ME"),
    BLACKBERRY("Blackberry"),
    UNKNOWN("Unknown");

    private String desc;
    Platform(String desc) { this.desc= desc;}
    public String getDesc() { return desc;  }
}