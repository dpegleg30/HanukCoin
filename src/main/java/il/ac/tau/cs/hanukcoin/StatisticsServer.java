package il.ac.tau.cs.hanukcoin;

// Using a simple HTTP server, for example with com.sun.net.httpserver
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class StatisticsServer {
    public static void startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/stats", (exchange -> {
            String response = CaptainAmeriminer.getStatisticsJson();
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length());
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }));
        server.setExecutor(null);
        server.start();
    }

    public static void main(String[] args) throws Exception {
        startServer();
    }
}