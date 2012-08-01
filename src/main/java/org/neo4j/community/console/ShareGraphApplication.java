package org.neo4j.community.console;

import static spark.Spark.get;
import static spark.Spark.post;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import com.google.gson.Gson;

public class ShareGraphApplication implements SparkApplication {

    private static final Pattern PATTERN = Pattern.compile("^/r/([0-9a-zA-Z_-]+)$");
    private ConsoleService consoleService;

    @Override
    public void init() {
        SessionService.setDatabaseInfo(ConsoleFilter.getDatabase());
        consoleService = new ConsoleService();
        post(new spark.Route("/r/share") {
            public Object handle(Request request, Response response) {
                final Map input = new Gson().fromJson(request.body(), Map.class);
                return consoleService.share(request, input);
            }
        });
        get(new spark.Route("/r/*") {
            public Object handle(Request request, Response response) {
                final String path = request.raw().getRequestURI();
                final Matcher matcher = PATTERN.matcher(path);
                if (matcher.find()) {
                    final String id = matcher.group(1);
                    response.redirect("/?id="+id);
                } else {
                    response.redirect("/");
                }
                return "";
            }
        });
    }
}
