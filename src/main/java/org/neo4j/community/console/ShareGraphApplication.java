package org.neo4j.community.console;

import com.google.gson.Gson;
import spark.servlet.SparkApplication;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static spark.Spark.get;
import static spark.Spark.post;

public class ShareGraphApplication implements SparkApplication {

    private static final Pattern PATTERN = Pattern.compile("^/r/([0-9a-zA-Z_-]+)$");
    private ConsoleService consoleService;

    @Override
    public void init() {
        SessionService.setDatabaseInfo(ConsoleFilter.getDatabase());
        consoleService = new ConsoleService();
        post("r/share", (request, response) -> {
            final Map input = new Gson().fromJson(request.body(), Map.class);
            return consoleService.share(request, input);
        });
        get("r/*", (request, response) -> {
            final String path = request.raw().getRequestURI();
            final Matcher matcher = PATTERN.matcher(path);
            if (matcher.find()) {
                final String id = matcher.group(1);
                response.redirect("/?id="+id);
            } else {
                response.redirect("/");
            }
            return "";
        });
    }
}
