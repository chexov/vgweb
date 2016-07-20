import com.vg.web.Controller;
import com.vg.web.HttpServer;
import com.vg.web.StrParser;
import com.vg.web.db.BaseJsonRedisDao;
import com.vg.web.socket.PrefixWebSocketServlet;
import com.vg.web.view.ClassPathView;
import com.vg.web.view.JsonView;
import com.vg.web.view.View;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static redis.clients.jedis.Protocol.DEFAULT_PORT;
import static redis.clients.jedis.Protocol.DEFAULT_TIMEOUT;

public class ExampleWebServerMain {
    public static final String PIVO = "\uD83C\uDF7A";

    private Config config;
    private HttpServer s;

    public ExampleWebServerMain(Config config) {
        this.config = config;
    }

    class MyDataObject {
        String key = "";
        String value = "";
    }

    public void init() {
        JedisPool jedisPool = config.getJedisPool();

        BaseJsonRedisDao<MyDataObject> myDao = new BaseJsonRedisDao<MyDataObject>(config.getJedisPool(), "dbPrefix",
                MyDataObject.class) {
        };

        s = new HttpServer(config.httpport, config.getSessionsDir());
        s.initHTTPS(config.httpsport, config.getKeystore(), config.keystorePassword);
        s.add(new PrefixWebSocketServlet("/ws/api") {
            @Override
            public void onConnect(WebSocketSession session, StrParser pathInfo) {
                System.out.println("websocket yay!");
            }
        });

        s.addGetMethod("/s/*", (r, p) -> {
            String stripVersion = r.getPathInfo().replaceFirst(p.nextString() + "/", "");
            return new ClassPathView("static" + stripVersion);
        });
        s.add("/api/1/mydataobject/*", new Controller() {
            @Override
            public View get(HttpServletRequest request, StrParser pathInfo) throws Exception {
                String id = pathInfo.nextString();
                MyDataObject o = myDao.get(id);
                return new JsonView(o);
            }
        });

        s.addGetMethod("/static/*", (r, p) -> new ClassPathView("static" + r.getPathInfo()));
    }

    public void join() throws InterruptedException {
        s.join();
    }

    public void start() throws Exception {
        s.start();
    }

    public static void main(String[] args) throws Exception {
        List<String> list = Arrays.asList(args);
        if (list.contains("-dssl") || list.contains("-dall")) {
            HttpServer.debugSSL();
        }
        if (list.contains("-djetty") || list.contains("-dall")) {
            HttpServer.debugJetty();
        }

        if (list.contains("-dalpn") || list.contains("-dall")) {
            HttpServer.debugALPN();
        }

        File cfile = new File("config.json");
        Config config = cfile.exists() ? Config.fromFile(cfile) : Config.useDefault();

        System.out.println(config);
        ExampleWebServerMain main = new ExampleWebServerMain(config);
        main.init();
        main.start();

        selfTest(config);

        System.out.println(PIVO);
        main.join();
    }

    public static void selfTest(Config config) throws IOException {
        try (Jedis resource = config.getJedisPool().getResource()) {
            System.out.println("Redis: " + resource.ping());
        }
        //
        //        OkHttpClient c = ApiClient.likeAndroid();
        //        log.info("Testing HTTP");
        //        Response httpResponse = c.newCall(GET("http://localhost:" + config.httpport)).execute();
        //        log.info("HTTP " + httpResponse);
        //        if (config.isHTTPSEnabled()) {
        //            log.info("Testing HTTPS");
        //            Response httpsResponse = c.newCall(GET("https://localhost:" + config.httpsport)).execute();
        //            log.info("HTTPS " + httpsResponse);
        //            if (Protocol.HTTP_2.equals(httpsResponse.protocol())) {
        //                log.info("HTTP2 enabled");
        //            } else {
        //                log.error("HTTP2 not enabled but requested in config.json. Exit 42.");
        //                System.exit(42);
        //            }
        //        }
    }

    public void stop() throws Exception {
        s.stop();
    }

    private static class Config {
        public int httpport;
        public int httpsport;
        public String keystorePassword;

        public JedisPool getJedisPool() {
            return new JedisPool(Config.poolConfig(), "localhost", DEFAULT_PORT, DEFAULT_TIMEOUT, null, 0);
        }

        public static GenericObjectPoolConfig poolConfig() {
            GenericObjectPoolConfig pc = new GenericObjectPoolConfig();
            pc.setMaxTotal(160);
            pc.setTestOnBorrow(true);
            pc.setMinIdle(1);
            pc.setMaxIdle(5);
            pc.setTestWhileIdle(true);
            return pc;
        }

        public File getSessionsDir() {
            return new File("sessions");
        }

        public File getKeystore() {
            return new File("keystore");
        }

        public static Config useDefault() {
            return null;
        }

        public static Config fromFile(File cfile) {
            return null;
        }
    }
}
