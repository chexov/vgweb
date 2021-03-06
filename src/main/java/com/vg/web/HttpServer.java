package com.vg.web;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.vg.web.socket.PrefixWebSocketServlet;
import com.vg.web.view.JsonView;
import com.vg.web.view.View;
import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnection;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class HttpServer {
    protected AbstractSessionManager sessionManager;
    protected Server jetty;
    private RequestLogHandler requestLogHandler;
    private String accessLogPath;
    private HttpConfiguration http_config;
    File sessionsDir;
    private int acceptors;
    private int selectors;
    private ContextHandlerCollection contexts;
    protected VirtualHost defaultHost;

    public static void debugJetty() {
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
    }

    public static void debugALPN() {
        ALPN.debug = true;
        Log.getLogger(ALPNServerConnection.class).setDebugEnabled(true);
    }

    public static void debugSSL() {
        System.setProperty("javax.net.debug", "all");
    }

    public HttpServer(int port, File sessionsDir) {
        this.sessionsDir = sessionsDir;

        acceptors = 1;
        selectors = Runtime.getRuntime().availableProcessors() * 3;
        int minThreads = acceptors + selectors + Runtime.getRuntime().availableProcessors();

        QueuedThreadPool threadPool = new QueuedThreadPool(1024, minThreads);
        threadPool.setDaemon(true);

        jetty = new Server(threadPool);
        initHTTP(port);
        initHandlers();
        sessionManager = new HashSessionManager();
    }

    public VirtualHost createVirtualHost(String[] domains) {
        ServletContextHandler handler = new ServletContextHandler(contexts, "/", true, false);
        handler.setVirtualHosts(domains);
        handler.getSessionHandler().setSessionManager(getSessionManager());
        return new VirtualHost(handler);
    }

    private void initHandlers() {
        contexts = new ContextHandlerCollection();
        defaultHandlers();
        defaultHost = createVirtualHost(null);
    }

    private void defaultHandlers() {
        HandlerCollection handlers = new HandlerCollection();
        requestLogHandler = new RequestLogHandler();
        handlers.setHandlers(new Handler[]{contexts, new DefaultHandler(), requestLogHandler});

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setIncludedMimeTypes("text/html", "text/plain", "text/xml", "text/css",
                "application/javascript", "text/javascript");
        gzipHandler.setHandler(handlers);
        jetty.setHandler(gzipHandler);
    }

    static String[] allCipherSuites() {
        try {
            SSLContext sslCtx = SSLContext.getDefault();
            SSLSocketFactory sf = sslCtx.getSocketFactory();
            String[] cipherSuites = sf.getSupportedCipherSuites();
            return cipherSuites;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    void initHTTP(int httpPort) {
        http_config = new HttpConfiguration();
        http_config.addCustomizer(new ForwardedRequestCustomizer());

        HttpConnectionFactory http = new HttpConnectionFactory(http_config);
        HTTP2CServerConnectionFactory http2c = new HTTP2CServerConnectionFactory(http_config);

        ServerConnector httpConnector; // = new ServerConnector(jetty, http, http2c);
        httpConnector = new ServerConnector(jetty, null, null, null,
                acceptors,
                selectors, http, http2c);

        httpConnector.setPort(httpPort);
        httpConnector.setIdleTimeout(500000);

        jetty.addConnector(httpConnector);

    }

    public void initHTTPS(int httpsPort, File keystore, String keystorePassword) {
        if (!(keystore != null && keystore.exists() && keystore.canRead() && keystore.isFile()
                && isNotBlank(keystorePassword))) {
            System.err.println("ERROR: cant init https keystore not readable or no password " + keystore);
            return;
        }

        http_config.setSecureScheme("https");
        http_config.setSecurePort(httpsPort);
        http_config.addCustomizer(new SecureRequestCustomizer());
        HttpConnectionFactory http = new HttpConnectionFactory(http_config);
        NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
        SslContextFactory sslContextFactory = new SslContextFactory(keystore.getAbsolutePath());
        sslContextFactory.setKeyStorePassword(keystorePassword);
        sslContextFactory.setKeyManagerPassword(keystorePassword);

        sslContextFactory.setUseCipherSuitesOrder(false);
        sslContextFactory.setIncludeCipherSuites(allCipherSuites());

        HttpConfiguration http2_config = new HttpConfiguration(http_config);

        HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(http2_config);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory(http2.getProtocol(),
                HttpVersion.HTTP_1_1.asString().toLowerCase());
        alpn.setDefaultProtocol(http.getProtocol());

        // HTTPS connector
        // We create a second ServerConnector, passing in the http configuration
        // we just made along with the previously created ssl context factory.
        // Next we set the port and a longer idle timeout.
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
        ServerConnector httpsConnector = new ServerConnector(jetty, ssl, alpn, http2, http);
        httpsConnector.setPort(httpsPort);
        httpsConnector.setIdleTimeout(500000);

        jetty.addConnector(httpsConnector);

    }

    protected void initSessionManager() {
        sessionsDir.mkdirs();

        try {
            HashSessionManager hsm = (HashSessionManager) sessionManager;
            hsm.setDeleteUnrestorableSessions(true);
            hsm.setStoreDirectory(sessionsDir);
            hsm.setSavePeriod(30);
            hsm.setMaxInactiveInterval((int) TimeUnit.DAYS.toSeconds(30));
            hsm.setLazyLoad(true);
            this.sessionManager = hsm;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    //e.g. ./logs/jetty-yyyy_mm_dd.request.log
    public void setAccessLogPath(String accessLogPath) {
        this.accessLogPath = accessLogPath;
        if (isNotBlank(accessLogPath)) {
            NCSARequestLog requestLog = new NCSARequestLog(this.accessLogPath);
            requestLog.setRetainDays(90);
            requestLog.setAppend(true);
            requestLog.setExtended(true);
            requestLog.setLogTimeZone("GMT");
            requestLogHandler.setRequestLog(requestLog);
        }
    }

    public void join() throws InterruptedException {
        jetty.join();
    }

    public void stop() throws Exception {
        jetty.stop();
    }

    public void start() throws Exception {
        initSessionManager();
        jetty.start();
    }

    public void add(String path, HttpServlet servlet) {
        defaultHost.context.addServlet(new ServletHolder(servlet), path);
    }

    public void add(PrefixWebSocketServlet webSocketController) {
        add(webSocketController.prefixPath, webSocketController);
    }

    public void addGetMethod(String path, Get servlet) {
        Controller controller = new Controller() {
            public View get(javax.servlet.http.HttpServletRequest request, StrParser pathInfo) throws Exception {
                return servlet.get(request, pathInfo);
            }
        };
        defaultHost.context.addServlet(new ServletHolder(controller), path);
    }

    public <R> void addGetJson(String path, Function<StrParser, R> p) {
        Controller controller = new Controller() {
            public View get(javax.servlet.http.HttpServletRequest request, StrParser pathInfo) throws Exception {
                return new JsonView(p.apply(pathInfo));
            }
        };
        defaultHost.context.addServlet(new ServletHolder(controller), path);
    }

    public void addFilter(String path, Filter filter) {
        addFilter(path, filter, null);
    }

    public void addFilter(String path, Filter filter, Map<String, String> initParameters) {
        FilterHolder holder = new FilterHolder(filter);
        if (initParameters != null) {
            holder.setInitParameters(initParameters);
        }
        defaultHost.context.addFilter(holder, path, null);
    }

    public AbstractSessionManager getSessionManager() {
        return this.sessionManager;
    }

}
