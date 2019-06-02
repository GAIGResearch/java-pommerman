package network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import core.GameState;
import players.Player;
import players.SimplePlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rhea.RHEAPlayer;
import utils.Types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Server {
    static int id = -1;
    static int game_type = -1;
    static Player agent;


    public static void main(String[] args) throws IOException {
        // todo remove static keyword and maybe from this out from the main function
        HttpServer server = HttpServer.create(new InetSocketAddress(12345), 0);
        HttpContext context = server.createContext("/");
        HttpContext actionContext = server.createContext("/action");
        HttpContext initContext = server.createContext("/init_agent");
        HttpContext episodeEndContext = server.createContext("/episode_end");
        HttpContext shutdownContext = server.createContext("/shutdown");
        context.setHandler(Server::handleRequest);
        actionContext.setHandler(Server::getAction);
        initContext.setHandler(Server::initAgent);
        episodeEndContext.setHandler(Server::episodeEnd);
        shutdownContext.setHandler(Server::shutdown);
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        // httpagent calls this first, maybe initialize agents here?
        System.out.println("default");
        String response = "";
        exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void getAction(HttpExchange exchange) throws IOException {
        String state = getMessageBody(exchange);
        GameState gs = new GameState(state);
        try {
            Types.ACTIONS action = agent.act(gs);
            //System.out.println("selected action = " + action);

            String response = "{\"action\": " + action.getKey() + "}";
            exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void initAgent(HttpExchange exchange) throws IOException {
        // httpagent calls this second
        System.out.println("initAgent");

        Gson gson = new Gson();
        String value = getMessageBody(exchange);
        System.out.println(value);
        JsonParser parser = new JsonParser();
        JsonElement e = parser.parse(value);
        JsonObject obj = e.getAsJsonObject();
        id = gson.fromJson(obj.get("id"), int.class) + 10; // todo pommerman agent starts from 0
        game_type = gson.fromJson(obj.get("game_type"), int.class);
        agent = new MCTSPlayer(0, id, new MCTSParams());

        System.out.println("id = " + id);
        System.out.println("game_type = " + game_type);

        String response = "";
        exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void episodeEnd(HttpExchange exchange) throws IOException {
        System.out.println("episodeend");
        String message = getMessageBody(exchange);
        System.out.println(message);
        String response = "";
        exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void shutdown(HttpExchange exchange) throws IOException {
        System.out.println("shutdown");
        String message = getMessageBody(exchange);
        System.out.println(message);
        String response = "";
        exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static String getMessageBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr =  new InputStreamReader(exchange.getRequestBody(),"utf-8");
        BufferedReader br = new BufferedReader(isr);
        return br.readLine();
    }

}
