package edu.caltech.ipac.firefly.messaging;

/**
 * Date: 2019-03-15
 *
 * @author loi
 * @version $Id: $
 */
public interface Subscriber {
    void onMessage(Message msg);
}
