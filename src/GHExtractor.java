import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.*;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.print.attribute.URISyntax;
import javax.xml.transform.URIResolver;

import java.util.ArrayList;
import java.util.List;


public class GHExtractor {

    // Props - hardcoded for now. Should be set upon app initialization.
    private String         username;
    private String         authToken;
    private String         targetRepository;
    private List<String[]> DirectoryMap = new ArrayList<>();
    private List<String>   repos        = new ArrayList<>();
    private String         errMsg       = "None";
    private boolean        verbose      = true;

    // Initializer
    public GHExtractor(String tRepo, String uName, String authT) {
        this.username         = uName;
        this.authToken        = (authT == null)? authT : "";
        this.targetRepository = tRepo;
    }

    // Initiates HTTP session with Github API
    private String startSession(String Mode, String URI) throws Exception {

        System.out.println("Establishing connection to "+URI);

        HttpsURLConnection session = (HttpsURLConnection) new URL(URI).openConnection(  );
        session.setRequestMethod     (Mode                                              );
        session.setRequestProperty   ("User-Agent"    , "GH-Extractor/1.0"              ); // Required Always.
        session.setRequestProperty   ("Accept"        , "application/vnd.github.v3+json"); // Required Always.
        session.setRequestProperty   ("Username"      ,  this.username                  );
        session.setRequestProperty   ("Authorization" ,  this.authToken                 );

        int responseCode = session.getResponseCode();
        System.out.println("47: "+responseCode+": "+session.getResponseMessage());
        try {
            switch (responseCode) {
                // Should be extended to account for all Server codes.
                case 404:
                    this.errMsg = "Error "+responseCode+": "+URI+" "+session.getResponseMessage()+". Check route path.\n";
                    System.out.println(this.errMsg);
                    break;//return("-1");

                case 403:
                    this.errMsg = responseCode+": "+session.getResponseMessage();
                    System.out.println(errMsg);
                    break;//return("-1");

                case 401:
                    this.errMsg = "Error "+responseCode+": "+session.getResponseMessage()+". Bad credentials?\n";
                    System.out.println(this.errMsg);
                    break;//return("-1");

                case 200:
                    System.out.println("63: receiving response...");
                    this.errMsg = "None";
                    String       inputLine;
                    StringBuffer response  = new StringBuffer();
                    BufferedReader in      = new BufferedReader(new InputStreamReader(session.getInputStream()));

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    in.close();
                    // Format response to human-readable JSON
                    Gson        gson  = new  GsonBuilder().setPrettyPrinting().create();
                    JsonParser  json  = new  JsonParser();
                    // Print response in JSON, if possible, otherwise print it in plaintext..
                    try {

                        JsonElement elem = json.parse(response.toString());
                        return gson.toJson(elem);
                    }
                    catch (Exception e) {
                        this.errMsg = e.toString();
                        throw e;
                    }

                default:
                    this.errMsg = "Error Encountered";
                    System.out.println(this.errMsg);
                    break;
            }
        } catch (Exception e) {
            this.errMsg = e.toString();
            throw e;
        }

        // If API responds with an unhandled response code.
        return responseCode+": "+session.getResponseMessage();
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

    /* TODO: Replace GetDirectoryMap() with a method that... */

    private void GetReposForUser(int iteration) throws Exception { // TODO: 1) ...Gets repos by username

        String resultsPer  = "100";
        int    page        = 1+iteration;
        try {

            // GET: https://api.github.com/users/{username}/repos?page={page}&per_page={resultsPer}
            //    - pulls info on all repos for a specified user
            String apiRes = this.startSession(
                    "GET",
                    "https://api.github.com/users/"+this.username+"/repos?"
                        +"page="+page
                        +"&per_page="+resultsPer
            );

            // Provides console output in verbose mode to let user know that this is doing something.
            if (page == 1) {
                System.out.println(((verbose)? "Retrieving repository list for "+this.username+"..." : ""));
            }

            // Empty API return check
            if (apiRes.matches("-1")) {
                System.out.println("No data was retrieved from GitHub.");
                return;
            }

            // Convert the GSON from API response to a JsonArray
            if (!apiRes.contains(this.errMsg)) {

                JsonArray repoArray = (JsonArray) new JsonParser().parse(apiRes);

                // TODO: Propose standardizing directory structure naming to lowercase-pipe-case
                // Parses results for repository names and stores them in the "repos" list
                int results = 0;
                for (JsonElement repo: repoArray) {

                    String repoName = repo.getAsJsonObject().get("name").toString();

                    System.out.println(((verbose)? "Repo "+(((iteration > 0)? results+100 : results)+1)+": "+repoName : ""));
                    this.repos.add(repoName);
                    results++;
                }

                // Since the API has a results limit of 100, this will use the pagination feature to recursively call itself for a full list.
                if (results > 0) {
                    // Recursive call to retrieve next page of repo list until no pages are left.
                    this.GetReposForUser(++iteration);
                } else {
                    System.out.println(((verbose)? "Repository Count: "+ ((results == 0 && page > 1)? 100+page+1 : results) : ""));
                    // Any follow up logic to execute once repo search has completed...
                }
            }
        }
        // TODO: Remove this once handlers are implemented for Rate Limit being exceeded, or 404/500 errors.
        // If, for some reason, an error is encountered, this prevents the standard error from stopping program.
        catch (Exception e) {
            throw e;
        }
        //return this.repos;
    }

    private String FindSpecificRepo(String repoName) throws Exception {// TODO: 2) Get repo that matches a certain description

        // Populates Repository list
        this.GetReposForUser(0);

        if (this.repos.size() == 0) {
            System.out.println("Error: Repository list empty.");
            return "";
        }
        System.out.println("Searching repository list for "+repoName+"...");
        for (String repo: this.repos) {
            if (repo.contains(repoName)) {

                System.out.println("\nFound! Scanning "+repoName+"...\n");

                // Query API for that specific repository information
                String apiRes = this.startSession(
                        "GET",
                        "https://api.github.com/repos/"+this.username+"/"+repoName
                );

                System.out.println(apiRes);
                return apiRes;
            }
        }
        return "Could not locate "+repoName+".";
    }
    public void GetFilenamesFromRepo(String targetRepo, String subdirectory) throws Exception { // TODO: 3) Get repo filenames
        String sub    = "omgomgomg";

        // Gets "contents_url" of repository after confirming repository exists.
        JsonParser parser  = new JsonParser();
        try {
            String contentsURL = parser.parse(this.FindSpecificRepo(targetRepo))
                    .getAsJsonObject()
                    .get("contents_url")
                    .toString()
                    .replaceAll("\\u007B\\+path\\u007D", subdirectory) // <== replaces {+path} with actual directory.
                    .replaceAll("\"", "");                  // <== removes extra quotes added by JSON.
            System.out.println(contentsURL);

        // Queries the API for directory contents using "contents_url"
        String repoContent = this.startSession("GET", contentsURL);
            for (:) {

            }
        System.out.println("262:"+ repoContent);
        }
        catch (Exception e){
            throw e;
        }


        // TODO: Create something to store the "name", "path", and "type" of each result.

        // Recursively searches through any content type marked "dir" for files

        // Logs all content type marked "file"

        // Returns log of all files found
    }
    // Retrieves the specified file from the repository this class was instantiated with. Using "*" will retrieve every file.
    public void GetFileFromGithub(String fileName, String outDirectory) throws Exception {

        // Step 0: if user of parent app specifies a table, then use that table name, otherwise use "*" to denote all tables.
        System.out.println(
                " ----------------------- \n" +
                "| Contacting Github... | \n" +
                " ----------------------- \n"
        );
        // Step 1: Retrieve directory map for repository provided at instantiation of app.
        System.out.println("Searching for '"+((fileName != "*")? fileName : "ALL TABLES")+"' in directory map");
        this.GetDirectoryMap();

            // Step 2: Search for {tableName or all tables} script raw data from Github using directory map.
            boolean found = false;
                // Checks for empty, malformed, or non-existent directory map
            if (this.DirectoryMap.size() <= 1) {
                    System.out.println("Warning: either the directory map file is not located in the repository root, or does not exist.");
            }
            else {
            for (String[] filePath: this.DirectoryMap) {

                if ((filePath.length < 2) && (filePath.length > 0)) {
                    System.out.println("Warning: No file location provided for '"+filePath[0]+"'. Skipping...\n");
                    continue;
                }
                    String filename = filePath[0];
                    String location = filePath[1];

                    // Step 3: Parse raw data from {tableName} file.
                    if ( !(filename == null) && (fileName.contains("*") || filename.contains(fileName)) ) {
                        System.out.println("Downloading "+filename+" to "+outDirectory+"...");

                        String rawData = this.GetFileData(location, filename);
                        if(!(rawData.contains("Not Found") || rawData.contains("Bad Request"))) {

                            // Step 4: Save raw data as a file to provided directory.
                            // Note:   Filename will match what is on github.
                            try {
                                PrintWriter writer = new PrintWriter("./"+outDirectory+"/"+filename, "UTF-8");
                                writer.print(rawData);
                                writer.close();
                                System.out.println(filename+" downloaded.\n");
                                found = true;
                            }
                            catch(FileNotFoundException e) {
                                System.out.println("The provided directory does not exist. Defaulting to current directory.");

                                PrintWriter writer = new PrintWriter(filename, "UTF-8");
                                writer.print(rawData);
                                writer.close();
                                System.out.println(filename+" downloaded.\n");
                                found = true;
                            }


                        } else {
                            System.out.println("Error downloading "+filename+".\n");
                        }
                    }
                }
            }

            // Only gets output if the above conditions are not met.
        if (!found) {
            System.out.println("Sorry: "+fileName+" was not found. Either it is not located in the repository, or is itself a directory. No file downloaded.");
        }
    }
}
