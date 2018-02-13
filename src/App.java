import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class App {

    public static void main(String[] args) {
        App http = new App();

        System.out.println(
                " ----------------------- \n" +
                        "| Opening Connection... |\n" +
                        " ----------------------- \n"
        );
        try {
            http.sendGet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // HTTP GET request - Read a specific file from within a repository.
    private void sendGet() throws Exception {

        String username     = "jsrj";
        String filename     = "/dl1-c/dl2-c1/testfile7.txt";
        String reponame     = "test-repo";

        String downloadPath = "https://raw.githubusercontent.com/"+username+"/"+reponame+"/master"+filename;
        String repoContents = "https://api.github.com/repos/"     +username+"/"+reponame+"/contents";
        String uri          = repoContents;

        URL obj = new URL(uri);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent"    , "GH-Extractor/1.0");
        con.setRequestProperty("Accept"        , "application/vnd.github.v3+json");
        con.setRequestProperty("Username"      ,  username);
        con.setRequestProperty("Authorization" , "f8f36786490e94b6ba7d9398ebec8d6cd1929f07");

        int responseCode = con.getResponseCode();
        System.out.println("\nQuerying for: "+(

                (uri == downloadPath) ?
                        (filename+"\nfrom repository: "+reponame+"\n") : (reponame+" contents.")
        ));
        System.out.println("Server Code : " + responseCode);

        switch (responseCode) {

            case 404:
                System.out.println("Error: "+uri+" Not Found. Check route path.\n");
                break;

            case 401:
                System.out.println("Error: Unauthorized. Bad credentials.\n");
                break;

            default:
                System.out.println("OK.\n");

                String       inputLine;
                StringBuffer response = new StringBuffer();

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                in.close();

                // Format response to human-readable JSON
                Gson        gson  = new  GsonBuilder().setPrettyPrinting().create();
                JsonParser  json  = new  JsonParser();

                // Print response in JSON, if possible, otherwise print it in plaintext..
                System.out.println("Response: \n");
                try {

                    JsonElement elem  = json.parse(response.toString());
                    String formattedResponse = gson.toJson(elem);
                    System.out.println("res: "+formattedResponse);
                } catch (Exception e) {

                    System.out.println(response);
                }

                break;
        }


    }
}
