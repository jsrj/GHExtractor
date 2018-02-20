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

public class GHExtractor {

    // Props - hardcoded for now. Should be set upon app initialization.
    private String         username;
    private String         authToken; //<-- probably not a good idea to have published on github, but since its just a dummy repository, its whatever.
    private String         targetRepository;
    private String         tableToExtract;
    private List<String[]> DirectoryMap = new ArrayList<>();

    // Initializer
    public GHExtractor(String tRepo, String uName, String authT, String targetTable) {
        this.username         = uName;
        this.authToken        = authT;
        this.targetRepository = tRepo;
        this.tableToExtract   = targetTable;
    }

    // Only runs when testing within the GHExtractor class itself.
    public static void main(String[] args) {
        // Test connection to DB to pull tables for comparison.
        dbConnect db = new dbConnect();
        db.Connect();

        // Establishes connection to Github with specified repo/directory and credentials.
        GHExtractor extractor = new GHExtractor(
                 "GHExtractor",
                "jsrj",
                 "f8f36786490e94b6ba7d9398ebec8d6cd1929f07",
                "demo.ddl"
        );


        try {
            extractor.GetTableFromGithub();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Initiates HTTP session with Github API
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
                        return gson.toJson(elem);

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

    // Read a specific file from within a repository.
    private String GetFileData(String path, String name) throws Exception {

        String downloadPath = "https://raw.githubusercontent.com/"+this.username+"/"+this.targetRepository+"/master"+path+name;
        return this.startSession("GET", downloadPath);
    }

    // Reads .directorymap from given repository and parses for locations of files.
    private void GetDirectoryMap() throws Exception {

        System.out.println("Retrieving directory map...");

        String filePath     = "/";
        String filename     = ".directorymap";
        String directoryMap = this.GetFileData(filePath, filename);

        if (directoryMap.toUpperCase().contains("NOT FOUND")) {
            System.out.println("Error: Repository does not have a "+filename+" file.");
        } else {
            // Do the things with the directory map...
            for (String map: directoryMap.split("\n")) {

                // Ignore lines commented out with '#' or that are empty.
                if (!(map.startsWith("#")) && (map.length() > 0)) {

                    // Convert each line from .directorymap into a String array of structure [0:{filename}, 1:{location}]
                    String[] mapRoute = map.split(" => ");

                    // Add the string array to the DirectoryMap list.
                    this.DirectoryMap.add(mapRoute);
                }
            }

            // Simply outputs dialogue of all files and routes found in directory map.
            int i = 0;
            for (String[] item: this.DirectoryMap) {
                if (!(item.length <= 0)) {
                    System.out.println("Entry #"+(this.DirectoryMap.indexOf(item)+1)+": ");
                    for (String unit: item) {
                        String whatIs = (i == 0)? "Filename: " : "Location: ";
                        System.out.println(whatIs+unit);
                        i++;
                    }
                    System.out.println("\n");
                    i = 0;
                }
            }
        }
    }

    private void GetAllFromGithub() throws Exception {
        // Similar to GetTableFromGithub except that this will pull data for every single file.
    }

    private void GetTableFromGithub() throws Exception {
        String tableName = this.tableToExtract; // <-- inline refactor once code is in place.
        System.out.println(
                " ----------------------- \n" +
                "| Contacting Github... | \n" +
                " ----------------------- \n"
        );
        // Step 1: Retrieve directory map for repository provided at instantiation of app.
        this.GetDirectoryMap();

        // Step 2: Search for {tableName} script raw data from Github using directory map.
        System.out.println("Searching for '"+tableName+"' in directory map");

            for (String[] filePath: this.DirectoryMap) {
                // Checks for empty, malformed, or non-existent directory map
                if (filePath.length <= 1) {
                    System.out.println("Warning: either the directory map file is not located in the repository root, or does not exist.");

                } else {

                    String filename = filePath[0];
                    String location = filePath[1];
                    boolean found   = false;

        // Step 3: Parse raw data from {tableName} file.
                    if (!(filename == null) && filename.contains(tableName)) {
                        System.out.println("Found!");

                        System.out.println("\n-- START OF FILE FROM GH --"           );
                        System.out.println(this.GetFileData(location, filename));
                        System.out.println("-- EOF --"                 );
                        found = true;
                        return;
                    }
                }
            }
            // Only gets output if the above conditions are not met.
            System.out.println("Sorry: "+tableName+" was not found. Either it is not located in the repository, or was not listed in the directory map.");

        // Step 4: Save raw data as a file to provided directory.
        // Note:   Filename will match what is on github.
    }
}
