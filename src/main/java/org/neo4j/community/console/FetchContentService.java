package org.neo4j.community.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author mh
 * @since 22.04.12
 */
class FetchContentService {
    public String fetch(String url) {
        BufferedReader in;
        String result = "";
        try
        {
            in = new BufferedReader(
                    new InputStreamReader(new URL(url).openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                result+=inputLine+"\n";
            in.close();
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;

    }

}
