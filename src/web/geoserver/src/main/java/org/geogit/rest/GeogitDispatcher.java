/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.rest;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.BeanDelegatingRestlet;
import org.geoserver.rest.BeanResourceFinder;
import org.geoserver.rest.DispatcherCallback;
import org.geoserver.rest.GeoServerServletConverter;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RESTDispatcher;
import org.geoserver.rest.RESTMapping;
import org.geoserver.rest.RestletException;
import org.geotools.util.logging.Logging;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.noelios.restlet.ext.servlet.ServletConverter;

/**
 * Simple AbstractController implementation that does the translation between Spring requests and
 * Restlet requests.
 * <p>
 * Almost a verbatim copy of {@link RESTDispatcher} but looks for {@link GeogitRESTMapping} instead
 * of {@link RESTMapping} so that our mappings don't get added to the regular geoserver rest
 * dispatcher.
 */
public class GeogitDispatcher extends AbstractController {
    /** HTTP method "PUT" */
    public static final String METHOD_PUT = "PUT";

    /** HTTP method "DELETE" */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * logger
     */
    static Logger LOG = Logging.getLogger(GeogitDispatcher.class);

    /**
     * converter for turning servlet requests into resetlet requests.
     */
    ServletConverter myConverter;

    /**
     * the root restlet router
     */
    Router myRouter;

    /**
     * rest request callbacks
     */
    List<DispatcherCallback> callbacks;

    public GeogitDispatcher() {
        setSupportedMethods(new String[] { METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_DELETE,
                METHOD_HEAD });
    }

    protected void initApplicationContext() throws BeansException {
        super.initApplicationContext();

        myConverter = new GeoServerServletConverter(getServletContext());
        myConverter.setTarget(createRoot());

        callbacks = GeoServerExtensions.extensions(DispatcherCallback.class,
                getApplicationContext());
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        try {
            myConverter.service(req, resp);
        } catch (Exception e) {
            RestletException re = null;
            if (e instanceof RestletException) {
                re = (RestletException) e;
            }
            if (re == null && e.getCause() instanceof RestletException) {
                re = (RestletException) e.getCause();
            }

            if (re != null) {
                resp.setStatus(re.getStatus().getCode());

                String reStr = re.getRepresentation().getText();
                if (reStr != null) {
                    LOG.severe(reStr);
                    resp.setContentType("text/plain");
                    resp.getOutputStream().write(reStr.getBytes());
                }

                // log the full exception at a higher level
                LOG.log(Level.SEVERE, "", re);
            } else {
                LOG.log(Level.SEVERE, "", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                if (e.getMessage() != null) {
                    resp.getOutputStream().write(e.getMessage().getBytes());
                }
            }
            resp.getOutputStream().flush();
        }

        return null;
    }

    public void addRoutes(Map<String, Object> m, Router r) {
        Iterator<Entry<String, Object>> it = m.entrySet().iterator();

        ApplicationContext applicationContext = getApplicationContext();

        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();

            // LOG.info("Found mapping: " + entry.getKey().toString());
            Restlet restlet = (applicationContext.getBean(entry.getValue().toString()) instanceof Resource) ? new BeanResourceFinder(
                    applicationContext, entry.getValue().toString()) : new BeanDelegatingRestlet(
                    applicationContext, entry.getValue().toString());

            String path = entry.getKey().toString();

            r.attach(path, restlet);

            if (path.indexOf("?") == -1) {
                r.attach(path + "?{q}", restlet);
            } else
                LOG.fine("Query string already listed in restlet mapping: " + path);
        }
    }

    public Restlet createRoot() {
        if (myRouter == null) {
            myRouter = new Router() {

                @Override
                protected synchronized void init(Request request, Response response) {
                    super.init(request, response);

                    // set the page uri's

                    // http://host:port/appName
                    String baseURL = request.getRootRef().getParentRef().toString();
                    String rootPath = request.getRootRef().toString().substring(baseURL.length());
                    String pagePath = request.getResourceRef().toString()
                            .substring(baseURL.length());
                    String basePath = null;
                    if (request.getResourceRef().getBaseRef() != null) {
                        basePath = request.getResourceRef().getBaseRef().toString()
                                .substring(baseURL.length());
                    }

                    // strip off the extension
                    String extension = ResponseUtils.getExtension(pagePath);
                    if (extension != null) {
                        pagePath = pagePath
                                .substring(0, pagePath.length() - extension.length() - 1);
                    }

                    // trim leading slash
                    if (pagePath.endsWith("/")) {
                        pagePath = pagePath.substring(0, pagePath.length() - 1);
                    }
                    // create a page info object and put it into a request attribute
                    PageInfo pageInfo = new PageInfo();
                    pageInfo.setBaseURL(baseURL);
                    pageInfo.setRootPath(rootPath);
                    pageInfo.setBasePath(basePath);
                    pageInfo.setPagePath(pagePath);
                    pageInfo.setExtension(extension);
                    request.getAttributes().put(PageInfo.KEY, pageInfo);

                    for (DispatcherCallback callback : callbacks) {
                        callback.init(request, response);
                    }
                }

                @Override
                public Restlet getNext(Request request, Response response) {
                    Restlet next = super.getNext(request, response);
                    if (next != null) {
                        for (DispatcherCallback callback : callbacks) {
                            callback.dispatched(request, response, next);
                        }
                    }
                    return next;
                };

                @Override
                public void handle(Request request, Response response) {
                    try {
                        super.handle(request, response);
                    } catch (Exception e) {
                        // execute the exception callback
                        for (DispatcherCallback callback : callbacks) {
                            callback.exception(request, response, e);
                        }
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        }
                        throw new RuntimeException(e);
                    } finally {
                        // execute the finished callback
                        for (DispatcherCallback callback : callbacks) {
                            callback.finished(request, response);
                        }
                    }
                };
            };

            // load all the rest mappings and register them with the router
            Iterator<GeogitRESTMapping> i = GeoServerExtensions.extensions(GeogitRESTMapping.class)
                    .iterator();

            while (i.hasNext()) {
                GeogitRESTMapping rm = i.next();
                addRoutes(rm.getRoutes(), myRouter);
            }
        }

        return myRouter;
    }
}
