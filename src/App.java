import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
//import javax.xml.ws.http.HTTPException;

public class App {

    // Props - hardcoded for now. Should be set upon app initialization.
    private String         username;
    private String         authToken;
    private String         targetRepository;
    private List<String[]> DirectoryMap = new ArrayList<>();

    // Initializer
    public App(String tRepo, String uName, String authT) {
        this.username         = uName;
        this.authToken        = authT;
        this.targetRepository = tRepo;
    }

    public static void main(String[] args) {

        App http = new App(
                 "test-repo",
                "jsrj",
                 "f8f36786490e94b6ba7d9398ebec8d6cd1929f07"
        );

        System.out.println(
                " ----------------------- \n" +
                "| Opening Connection... |\n" +
                " ----------------------- \n"
        );
        try {
            http.GetDirectoryMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String startSession(String Mode, String URI) throws Exception {

        HttpsURLConnection session = (HttpsURLConnection) new URL(URI).openConnection(  );
        session.setRequestMethod     (Mode                                              );
        session.setRequestProperty   ("User-Agent"    , "GH-Extractor/1.0"              ); // Required Always.
        session.setRequestProperty   ("Accept"        , "application/vnd.github.v3+json"); // Required Always.
        session.setRequestProperty   ("Username"      ,  this.username                  );
        session.setRequestProperty   ("Authorization" ,  this.authToken                 );

        int responseCode = session.getResponseCode();
        try {
            switch (responseCode) {
                // Should be extended to account for all Server codes.
                case 404:
                    System.out.println("Error "+responseCode+": "+URI+" "+session.getResponseMessage()+". Check route path.\n");
                    break;

                case 401:
                    System.out.println("Error "+responseCode+": "+session.getResponseMessage()+". Bad credentials?\n");
                    break;

                case 200:
                    System.out.println("Status "+responseCode+": "+session.getResponseMessage()+".\n");
                    String       inputLine;
                    StringBuffer response = new StringBuffer();
                    BufferedReader in = new BufferedReader(new InputStreamReader(session.getInputStream()));

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    in.close();

                    // Format response to human-readable JSON
                    Gson        gson  = new  GsonBuilder().setPrettyPrinting().create();
                    JsonParser  json  = new  JsonParser();
                    // Print response in JSON, if possible, otherwise print it in plaintext..
                    try {
                        JsonElement elem         = json.parse(response.toString());
                        String formattedResponse = gson.toJson(elem);
                        System.out.println("res: "+formattedResponse);

                        return response.toString();
                    } catch (Exception e) {

                        return response.toString();
                    }

                default:
                    break;
            }
        } catch (Exception e) {
            throw e;
        }

        // If API responds with an unhandled response code.
        return session.getResponseMessage();
    }


    private void GetDirectoryMap() throws Exception {

        System.out.println("Retrieving directory map...");

        String filePath     = "/";
        String filename     = ".directorymap";
        String downloadPath = "https://raw.githubusercontent.com/"+this.username+"/"+this.targetRepository+"/master"+filePath+filename;
        String directoryMap = this.startSession("GET", downloadPath);

        if (directoryMap.toUpperCase().contains("NOT FOUND")) {
            System.out.println("Error: Repository does not have a "+filename+" file.");
        } else {
            // Do the things with the directory map...
            for (String map: directoryMap.split("\n")) {
                if (!map.startsWith("#")) {
                    System.out.println(map);
                    String[] mapRoute = map.split(" => ");
                    this.DirectoryMap.add(mapRoute);
                    System.out.println(mapRoute);
                }

            }
            for (String[] item: this.DirectoryMap) {
                System.out.println(item.toString());
            }
            //this.directoryMap = thing;
        }
    }

    // Read a specific file from within a repository.
    private void GetFileData() throws Exception {

        String filePath     = "/";
        String filename     = ".directorymap";
        String downloadPath = "https://raw.githubusercontent.com/"+this.username+"/"+this.targetRepository+"/master"+filePath+filename;

        System.out.println(this.startSession("GET", downloadPath));
    }
}
