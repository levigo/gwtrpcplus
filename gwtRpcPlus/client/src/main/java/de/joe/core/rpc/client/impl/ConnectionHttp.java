package de.joe.core.rpc.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import de.joe.core.rpc.client.util.Client;

public class ConnectionHttp extends AbstractConnection {

  /**
   * Thue when the BasicConnectino should be used
   */
  private boolean connnected = false;

  /**
   * true when the Serverpush-request is pending
   */
  private boolean serverCurrentlyPending = false;

  public boolean isPolling() {
    return serverCurrentlyPending;
  }

  /**
   * true when Response of the Server is expected
   */
  private boolean requestsPending = false;

  /**
   * Amount of pending simple Callbacks (they can get multiple responses, so no serverpolling is
   * needed)
   */
  private int callbacksPending = 0;

  /**
   * Flag to not do a serverpush ehen server isnt responding
   * 
   * this causes a bug after serverrecover, not timeouting some results, because the polling
   * reschedule the ontimeout
   */
  private boolean notresponding = false;

  private void updateServerPush() {
    if (!notresponding && requestsPending && connnected && !serverCurrentlyPending && callbacksPending == 0)
      try {
        serverCurrentlyPending = true;
        // System.out.println("Sending longpoll");
        longPushService.sendRequest("", longPushCallback);
      } catch (RequestException e) {
        e.printStackTrace();
      }
  }

  @Override
  public void setPending(boolean pending) {
    this.requestsPending = pending;
    updateServerPush();
  }

  @Override
  public void connect() {
    connnected = true;
    updateServerPush();
    // Always connected
    onConnected();
  }

  @Override
  public void disconnect() {
    connnected = false;
    onDisconnect();
  }

  private final RequestCallback longPushCallback = new RequestCallback() {
    @Override
    public void onResponseReceived(Request request, Response response) {
      serverCurrentlyPending = false;

      if (response.getStatusCode() != Response.SC_OK) {
        if (response.getStatusCode() != 0)// Ignore 0 (called by server don't responsed)
          System.err.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
        else
          onTimeout();
      } else {
        final String[] resp = response.getText().split("\n");
        // long start = System.currentTimeMillis();
        for (String res : resp)
          onRecieve(res);
        // long duration = (System.currentTimeMillis() - start);
        // System.out.println("Duration: " + duration + "ms (avg:" + duration / resp.length + ")");
      }

      updateServerPush();
    }

    @Override
    public void onError(Request request, Throwable exception) {
      System.err.println("Error at the HTTPConnections longpoll");
      exception.printStackTrace();

      serverCurrentlyPending = false;
      updateServerPush();
    }
  };

  private final RequestCallback callback = new RequestCallback() {
    @Override
    public void onResponseReceived(Request request, Response response) {
      notresponding = response.getStatusCode() == 0;
      if (response.getStatusCode() != Response.SC_OK) {
        if (response.getStatusCode() != 0)// Ignore 0 (called by server don't responsed)
          System.err.println("Server responsed " + response.getStatusCode() + ": " + response.getStatusText());
        else
          onTimeout();
      } else
        onRecieve(response.getText());

      callbacksPending--;
      updateServerPush();
    }

    @Override
    public void onError(Request request, Throwable exception) {
      System.err.println("Error at the HTTPConnections callback");
      exception.printStackTrace();

      callbacksPending--;
      updateServerPush();
    }
  };

  private final RequestBuilder service;
  private final RequestBuilder longPushService;

  public ConnectionHttp() {
    service = new RpcRequestBuilder().create(GWT.getModuleBaseURL() + "gwtRpcPlusBasic").finish();
    service.setHeader("clientId", Client.id);
    longPushService = new RpcRequestBuilder().create(GWT.getModuleBaseURL() + "gwtRpcPlusBasic").finish();
    longPushService.setHeader("clientId", Client.id);
    longPushService.setHeader("longpush", "true");
  }

  @Override
  public void send(String request) {
    System.err.println("request " + request);
    try {
      service.sendRequest(request, callback);
      callbacksPending++;
    } catch (RequestException e) {
      e.printStackTrace();
    }
  }

}
