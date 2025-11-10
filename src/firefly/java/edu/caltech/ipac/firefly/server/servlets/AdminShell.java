/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.util.Logger;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 
 * Date: 11/8/25
 *
 * @author loi
 * @version : $
 */
public class AdminShell {
    private static Logger.LoggerImpl LOG = Logger.getLogger();
    
    public static void checkAdminShell(ServletContextEvent sce) {
        String enableShell = System.getProperty("ENABLE_ADMIN_SHELL", "false");
        if (!"true".equalsIgnoreCase(enableShell)) {
            return;     // silently ignore if not enabled
        }

        var ctx = sce.getServletContext();
        LOG.debug("Admin shell proxy enabled → http://127.0.0.1:8081/");

        // Pick up IP prefixes from environment or system property
        String allowedPrefixes = System.getProperty("ALLOWED_IP_PREFIXES", "");
        LOG.debug("   - Allowed IP prefixes: " + (allowedPrefixes.isBlank() ? "(none)" : allowedPrefixes));

        // Register HTTP proxy servlet
        try {
            var reg = ctx.addServlet("AdminShellHttp", HttpProxyServlet.class);
            reg.setInitParameter("targetUri", "http://127.0.0.1:8081/");
            reg.addMapping("/admin/shell/*");
            reg.setLoadOnStartup(1);
            LOG.debug("   - Registered HTTP Proxy for /admin/shell/*");
        } catch (Exception e) {
            LOG.error(e, "Error registering HTTP Proxy Servlet: " + e.getMessage());
        }

        // Register IP restriction filter (only if prefixes provided)
        if (!allowedPrefixes.isBlank()) {
            try {
                var ipFilter = ctx.addFilter("AdminShellIpFilter", new IpFilter());
                ipFilter.setInitParameter("allowedPrefixes", allowedPrefixes);
                ipFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/admin/shell/*");
                LOG.debug("   - Added IP restriction filter for /admin/shell/* → " + allowedPrefixes);
            } catch (Exception e) {
                LOG.error(e, "Error registering IP restriction filter: " + e.getMessage());
            }
        }

        // Register WebSocket endpoint
        try {
            ServerContainer container = (ServerContainer)
                    ctx.getAttribute(ServerContainer.class.getName());

            if (container != null) {
                ServerEndpointConfig config = ServerEndpointConfig.Builder
                        .create(WebSocketProxy.class, "/admin/shell/ws")
                        .subprotocols(java.util.List.of("tty"))
                        .build();

                container.addEndpoint(config);
                LOG.debug("   - Registered WebSocket Endpoint for /admin/shell/ws");
            } else {
                System.err.println("WebSocket ServerContainer not found. Tomcat misconfiguration?");
            }
        } catch (Exception e) {
            System.err.println("Error registering WebSocket Endpoint: " + e.getMessage());
        }
    }


    //====================================================================
    //  HTTP Proxy Servlet that forwards /admin/shell/* requests
    //  ttyd at http://localhost:8081/
    //====================================================================
    public static class HttpProxyServlet extends HttpServlet {

        private static final String BACKEND_BASE = "http://localhost:8081";

        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            String targetUrl = getTargetUrl(req);

            LOG.debug("[ShellHttpProxy] → " + req.getMethod() + " " + targetUrl);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(30));

            // Copy headers except hop-by-hop ones
            req.getHeaderNames().asIterator().forEachRemaining(h -> {
                if (!h.equalsIgnoreCase("host") && !h.equalsIgnoreCase("connection")) {
                    builder.header(h, req.getHeader(h));
                }
            });

            // Forward body if present
            if (req.getContentLengthLong() > 0) {
                builder.method(req.getMethod(),
                        HttpRequest.BodyPublishers.ofInputStream(() -> {
                            try {
                                return req.getInputStream();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
            } else {
                builder.method(req.getMethod(), HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<InputStream> backendResp;
            try {
                backendResp = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.sendError(500, "Proxy interrupted");
                return;
            } catch (IOException e) {
                resp.sendError(502, "Proxy I/O error: " + e.getMessage());
                return;
            }

            resp.setStatus(backendResp.statusCode());
            backendResp.headers().map().forEach((k, vList) -> {
                for (String v : vList) {
                    if (!k.equalsIgnoreCase("transfer-encoding")) {
                        resp.addHeader(k, v);
                    }
                }
            });

            try (InputStream in = backendResp.body(); OutputStream out = resp.getOutputStream()) {
                in.transferTo(out);
            }
        }

        private static String getTargetUrl(HttpServletRequest req) {
            String context = req.getContextPath();        // e.g. "/firefly"
            String uri = req.getRequestURI();             // e.g. "/firefly/admin/shell/token"
            String prefix = context + "/admin/shell";

            // Strip prefix so backend sees '/' paths
            String backendPath = uri.startsWith(prefix)
                    ? uri.substring(prefix.length())
                    : uri;

            if (backendPath.isEmpty()) backendPath = "/";

            // Construct full backend URL
            String targetUrl = BACKEND_BASE + backendPath;
            if (req.getQueryString() != null) {
                targetUrl += "?" + req.getQueryString();
            }
            return targetUrl;
        }
    }

    //====================================================================
    //  WebSocket Proxy Endpoint
    //====================================================================
    public static class WebSocketProxy {

        private static final String BACKEND_URL = "ws://localhost:8081/ws";

        private final AtomicReference<WebSocket> backendRef = new AtomicReference<>();
        private Session clientSession;

        @OnOpen
        public void onOpen(Session session) {
            this.clientSession = session;
            session.setMaxBinaryMessageBufferSize(8192);
            session.setMaxTextMessageBufferSize(8192);

            LOG.debug("[ShellProxy] Connecting client %s to %s using subprotocol '%s'".formatted(
                    session.getId(), BACKEND_URL, session.getNegotiatedSubprotocol())
            );

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            try {
                WebSocket backend = httpClient.newWebSocketBuilder()
                        .subprotocols("tty")
                        .buildAsync(URI.create(BACKEND_URL), new WebSocket.Listener() {

                            @Override
                            public void onOpen(WebSocket ws) {
                                backendRef.set(ws);
                                LOG.debug("[ShellProxy] Connected to ttyd backend");
                                ws.request(1);
                            }

                            @Override
                            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                                LOG.trace("[ShellProxy] ← text from backend: " + data);
                                sendToClient(() -> clientSession.getBasicRemote().sendText(data.toString()));
                                ws.request(1);
                                return null;
                            }

                            @Override
                            public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                                LOG.trace("[ShellProxy] ← binary from backend: " + data.remaining() + " bytes");
                                ByteBuffer copy = data.duplicate();
                                copy.rewind();
                                sendToClient(() -> clientSession.getBasicRemote().sendBinary(copy));
                                ws.request(1);
                                return null;
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                                LOG.debug("[ShellProxy] Backend closed: " + code + " " + reason);
                                closeClient(code, reason);
                                return null;
                            }

                            @Override
                            public void onError(WebSocket ws, Throwable err) {
                                LOG.error(err, "[ShellProxy] Backend ERROR: " + err);
                                closeClient(1011, "Backend error");
                            }

                        }).join();

                backendRef.set(backend);
                LOG.debug("[ShellProxy] Backend handshake complete.");

            } catch (Exception e) {
                System.err.println("[ShellProxy] Backend connect failed: " + e);
                closeClient(CloseReason.CloseCodes.CANNOT_ACCEPT.getCode(), e.getMessage());
            }
        }

        @OnMessage
        public void onTextMessage(String msg) {
            LOG.trace("[ShellProxy] → text to backend: " + msg);
            WebSocket backend = backendRef.get();
            if (backend != null) backend.sendText(msg, true);
        }

        @OnMessage
        public void onBinaryMessage(ByteBuffer data) {
            LOG.trace("[ShellProxy] → binary to backend: " + data.remaining() + " bytes");
            WebSocket backend = backendRef.get();
            if (backend != null) {
                ByteBuffer copy = data.duplicate();
                copy.rewind();
                backend.sendBinary(copy, true);
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) {
            LOG.debug("[ShellProxy] Client closed: " + reason);
            WebSocket backend = backendRef.get();
            if (backend != null) backend.sendClose(WebSocket.NORMAL_CLOSURE, "Client closed");
        }

        @OnError
        public void onError(Session session, Throwable error) {
            LOG.error(error, "[ShellProxy] ERROR: " + error);
        }

        private void sendToClient(IOSender action) {
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    action.send();
                } catch (IOException e) {
                    LOG.error("[ShellProxy] Error sending to client: " + e);
                }
            }
        }

        private void closeClient(int code, String reason) {
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    clientSession.close(new CloseReason(
                            CloseReason.CloseCodes.getCloseCode(code), reason));
                } catch (IOException ignored) {}
            }
        }

        @FunctionalInterface
        private interface IOSender { void send() throws IOException; }
    }

    //====================================================================
    //  IP filtering
    //====================================================================

    public static class IpFilter implements Filter {
        private List<String> allowedPrefixes = List.of("127.0.0.1");

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // Try init-param first (from web.xml or dynamic registration)
            String param = filterConfig.getInitParameter("allowedPrefixes");
            if (param == null || param.isBlank()) {
                param = System.getProperty("ALLOWED_IP_PREFIXES", "");
            }

            if (!param.isBlank()) {
                allowedPrefixes = Arrays.stream(param.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }

            filterConfig.getServletContext().log("[IpRestrictionFilter] Allowed IP prefixes: " + allowedPrefixes);
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            // Try X-Real-IP → X-Forwarded-For → remoteAddr
            String ip = Optional.ofNullable(request.getHeader("X-Real-IP"))
                    .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                            .map(xff -> xff.split(",")[0].trim())
                            .orElse(request.getRemoteAddr()));

            boolean allowed = allowedPrefixes.stream().anyMatch(ip::startsWith);

            if (!allowed) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Access denied for IP: " + ip);
                return;
            }

            chain.doFilter(req, res);
        }

        @Override
        public void destroy() { }
    }

}
