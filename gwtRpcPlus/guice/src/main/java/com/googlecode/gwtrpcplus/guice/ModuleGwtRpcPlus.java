package com.googlecode.gwtrpcplus.guice;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.googlecode.gwtrpcplus.server.GwtRpcPlusContext;
import com.googlecode.gwtrpcplus.server.GwtRpcPlusFilter;

public class ModuleGwtRpcPlus extends ServletModule {
  // private final static Logger logger = new Logger(ModuleGwtRpcPlus.class);

  private final String modulename;
  private final GwtRpcPlusFilter filter;
  private Set<Class<? extends RemoteServiceServlet>> servletClasses = new HashSet<Class<? extends RemoteServiceServlet>>();

  /**
   * @param base for example the projectName
   */
  public ModuleGwtRpcPlus(String modulename) {
    this(modulename, new GwtRpcPlusFilter());
  }

  /**
   * @param base for example the projectName
   */
  public ModuleGwtRpcPlus(String modulename, GwtRpcPlusFilter filter) {
    this.modulename = modulename;
    this.filter = filter;
    this.filter.setModuleName(modulename);
  }

  /**
   * Call this to register a Class to be used in the RpcPlus.
   * 
   * @param clazz ServiceClass Implementation
   * @return itselfe (builder-Pattern)
   */
  public ModuleGwtRpcPlus register(Class<? extends RemoteServiceServlet> clazz) {
    servletClasses.add(clazz);
    return this;
  }

  /**
   * Call this to register many Class to be used in the RpcPlus.
   * 
   * @param clazz ServiceClass Implementation
   * @return itselfe (builder-Pattern)
   */
  public ModuleGwtRpcPlus register(Set<Class<? extends RemoteServiceServlet>> classes) {
    servletClasses.addAll(classes);
    return this;
  }

  private final GwtRpcPlusContext context = new GuiceGwtRpcPlusContext();

  @Override
  protected final void configureServlets() {
    filter.setContext(context, getServletContext());

    filter("/*").through(filter);
    requestInjection(context);

    // Place for the user to add custom Code, when inherit from this Module
    configureCustomServlets();
  }

  private class GuiceGwtRpcPlusContext implements GwtRpcPlusContext {
    private Set<RemoteServiceServlet> servlets;
    private Set<RemoteServiceServlet> customServlets = new HashSet<>();
    @Inject
    private Injector injector;

    @Override
    public void register(RemoteServiceServlet servlet) {
      if (customServlets == null)
        throw new IllegalStateException("You can't add Servlets yet");
      customServlets.add(servlet);
    }

    @Override
    public void register(Class<? extends RemoteServiceServlet> servlet) {
      bindGwtServlet(servlet);
    }

    @Override
    public Set<RemoteServiceServlet> getServlets() {
      if (servlets == null) {
        servlets = customServlets;
        customServlets = null;

        // TODO get Servlets from guice-servlet?
        for (Class<? extends RemoteServiceServlet> servletclass : servletClasses) {
          servlets.add(injector.getInstance(servletclass));

        }
        servletClasses = null;
      }
      return servlets;
    }
  }

  // /**
  // * Adds an Module by its name. This is used to add Modules automaticly via classpath
  // *
  // * @param className full qualified classname of the InternalGwtRpcPlusModule
  // */
  // private boolean addWebsocketModule(String className) {
  // try {
  // WebsocketModule m = (WebsocketModule) Class.forName(className).newInstance();
  // m.setModulename(modulename);
  // install(m);
  // return m.isAdded();
  // } catch (Throwable e) {
  // logger.info("WebsocketModule not added", e);
  // return false;
  // }
  // }

  protected void configureCustomServlets() {
  }

  /**
   * Optional usage for adding the Service to GwtRpcPlus and to the servlet.
   * 
   * It binds the Service as Singleton too.
   * 
   * @param clazz ServiceImpl to add
   */
  protected void bindGwtServlet(Class<? extends RemoteServiceServlet> clazz) {
    if (servletClasses == null)
      throw new IllegalStateException("You can't add Servlets yet");
    servletClasses.add(clazz);
    bind(clazz).in(Singleton.class);
    serve("/" + modulename + "/" + findName(clazz)).with(clazz);
  }

  private String findName(Class<?> clazz) {
    for (Class<?> c : clazz.getInterfaces())
      if (c.isAnnotationPresent(RemoteServiceRelativePath.class))
        return c.getAnnotation(RemoteServiceRelativePath.class).value();
    return findName(clazz.getSuperclass());
  }
}
