package com.entanglementgraph.graph;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 10/06/13
 * Time: 15:36
 * To change this template use File | Settings | File Templates.
 */
public class RevisionLogListenerException extends Exception {
  public RevisionLogListenerException() {
  }

  public RevisionLogListenerException(String message) {
    super(message);
  }

  public RevisionLogListenerException(String message, Throwable cause) {
    super(message, cause);
  }

  public RevisionLogListenerException(Throwable cause) {
    super(cause);
  }

  public RevisionLogListenerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
