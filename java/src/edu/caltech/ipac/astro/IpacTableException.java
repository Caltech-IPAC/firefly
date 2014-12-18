package edu.caltech.ipac.astro;


public class IpacTableException extends Exception {

    public IpacTableException () {
        super();
    }

    public IpacTableException (String msg) {
        super(msg);
    }

    public IpacTableException (String msg, Throwable t) {
        super(msg, t);
    }

}



