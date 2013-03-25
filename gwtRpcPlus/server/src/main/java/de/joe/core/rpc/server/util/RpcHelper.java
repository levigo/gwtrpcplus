package de.joe.core.rpc.server.util;

import java.lang.reflect.Field;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.google.gwt.user.server.rpc.AbstractRemoteServiceServlet;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.joe.core.rpc.server.ServletList;

@Singleton
public class RpcHelper {
  private final static Logger logger = new Logger(RpcHelper.class);

  private final Injector injector;
  private final ServletList servletList;

  @Inject
  public RpcHelper(Injector injector, ServletList servletList) {
    this.injector = injector;
    this.servletList = servletList;
  }

  public RemoteServiceServlet getServlet(String name) {
    return (RemoteServiceServlet) injector.getInstance(getServletClass(name));
  }

  private Class<? extends RemoteServiceServlet> getServletClass(String name) {
    for (Class<? extends RemoteServiceServlet> servlet : servletList.getServletClasses())
      for (Class<?> iface : servlet.getInterfaces())
        if (iface.getSimpleName().equals(name))
          return servlet;
    logger.error("Servlet {} was not found in GwtRpcProcessor.", name);
    throw new IllegalArgumentException("Servlet \"" + name + "\" was not found in GwtRpcProcessor.");
  }

  @SuppressWarnings("unchecked")
  public void setThreadLocals(RemoteServiceServlet target, HttpServletRequest srcReq) {
    try {
      Field req = AbstractRemoteServiceServlet.class.getDeclaredField("perThreadRequest");
      // Give us access to hack them
      req.setAccessible(true);
      // Get the Attributes
      ThreadLocal<HttpServletRequest> targetReq = (ThreadLocal<HttpServletRequest>) req.get(target);
      // Default init
      if (targetReq == null) {
        synchronized (target) {
          targetReq = (ThreadLocal<HttpServletRequest>) req.get(target);
          if (targetReq == null)
            req.set(target, targetReq = new ThreadLocal<HttpServletRequest>());
        }
      }
      // Set the values
      targetReq.set(srcReq);
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

}
