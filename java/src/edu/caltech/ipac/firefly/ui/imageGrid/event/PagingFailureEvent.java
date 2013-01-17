package edu.caltech.ipac.firefly.ui.imageGrid.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 27, 2010
 * Time: 12:36:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingFailureEvent extends GwtEvent<PagingFailureHandler>{
    public static final Type TYPE = new Type<PagingFailureHandler>();

    /**
    * The exception that caused the failure.
    */
    private Throwable exception;

    /**
    * Construct a new {@link PagingFailureEvent}.
    *
    * @param exception the exception that caused the event
    */
    public PagingFailureEvent(Throwable exception) {
        this.exception = exception;
    }

    /**
    * @return the exception that caused the failure
    */
    public Throwable getException() {
        return exception;
    }

//------------ Implementation for GwtEvent ------------   

    protected void dispatch(PagingFailureHandler h) {
        h.onPagingFailure(this);
    }

    public Type getAssociatedType() {
        return TYPE;
    }
}
