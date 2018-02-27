import com.google.gson.*;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class GHExtractor
{

    // Props - hardcoded for now. Should be set upon app initialization.
    private List<ContentDetails> filePaths = new ArrayList<>();

    private String         username;
    private String         authToken;
    private String         targetRepository;
    //private List<String[]> DirectoryMap = new ArrayList<>();
    private List<String>   repos        = new ArrayList<>();
    private String         errMsg       = "None";
    private boolean        verbose      = false;

    private String scheduledHTTPMode;
    private String getScheduledURI;

    private String contentsURL;

    // For changing target repo
    public void setTargetRepository(String targetRepository) {
        this.targetRepository = targetRepository;
    }

    // For toggling console output on and off
    public void toggleVerbose() {
        this.verbose = !this.verbose;
    }

    // Initializer
    public GHExtractor(String tRepo, String uName, String authT) {
        this.username         = uName;
        this.authToken        = (authT == null)? authT : "";
        this.setTargetRepository(tRepo);

        if(this.verbose) {
            // Ensures that verbose mode is defaulted to off.
            this.toggleVerbose();
        }
    }


    // Initiates HTTP session with Github API
    private String startSession(String Mode, String URI) throws Exception {

        System.out.println("Establishing connection to "+URI);

        HttpsURLConnection session = (HttpsURLConnection) new URL(URI).openConnection();

        session.setRequestMethod     (Mode                                              );
        session.setRequestProperty   ("User-Agent"    , "GHExtractor"                   );
        session.setRequestProperty   ("Accept"        , "application/vnd.github.v3+json," +
                                                        "text/html,"                      +
                                                        "application/xhtml+xml,"          +
                                                        "image/jxr,"                      +
                                                        "*/*"                           );
        session.setRequestProperty   ("Username"      ,  this.username                  );
        session.setRequestProperty   ("Authorization" ,  this.authToken                 );

        // Handler for Rate-Limits
        String remaining = session.getHeaderField("X-RateLimit-Remaining");
        String resetTime = session.getHeaderField("X-RateLimit-Reset");

        int queriesRemaining = (remaining != null)? Integer.decode(remaining) : 999;
        long limitReset      = (resetTime != null)? Long.decode(resetTime)    : 0L;
        Date date            = new Date();
        Date reset           = new Date(limitReset * 1000);
        DateFormat format    = new SimpleDateFormat("KK:mm:ss a");


        format.setTimeZone(TimeZone.getDefault());
        String formattedLocal = format.format(date);
        String formattedReset = format.format(reset);
        Long   timeDifference = limitReset-(date.toInstant().getEpochSecond());

        if (queriesRemaining == 0) {
            // Suspends tool until rate-limit has reset or the process is interrupted.
            System.out.println("Rate Limit Reached...");
            System.out.println("Reset Time: "+formattedReset);
            System.out.println("Local Time: "+formattedLocal);
            System.out.println("Will retry once rate limit resets...");

            Thread.sleep(timeDifference*1000);
            System.out.println("Re-attempting connection...");

            // Re-attempts HTTPS Session with the last known URI
            this.startSession(Mode, URI);
        }
        else if (queriesRemaining < 5) {
            System.out.println("Rate Limit Warning: 5 or less API calls remaining. Once this count hits 0, will wait before continuing");
        }


        int responseCode = session.getResponseCode();
        System.out.println("74: "+responseCode+": "+session.getResponseMessage());
        try {
            switch (responseCode) {

                // Should be extended to account for all Server codes.
                case 404:
                    this.errMsg = "Error "+responseCode+": "+URI+" "+session.getResponseMessage()+". Check route path.\n";
                    System.out.println(this.errMsg);
                    break;//return("-1");

                case 403:
                    this.errMsg = responseCode+": "+session.getResponseMessage();
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
                    catch (JsonSyntaxException e) {
                        System.out.println(response.toString());
                        return response.toString();
                        //throw e;
                    }

                default:
                    this.errMsg = "Error Encountered";
                    System.out.println(this.errMsg);
                    return "-1";
            }
        } catch (Exception e) {
            this.errMsg = e.toString();
            throw e;
        }

        // If API responds with an unhandled response code.
        System.out.println("ACK!");
        return new JsonObject().toString();
    }

    private void GetReposForUser(int iteration) throws Exception { // TODO: 1) ...Gets repos by username
        if (this.repos.size() > 0 && iteration == 0) {
            // Prevents from repeatedly querying the API for information that is already present, while not interfering with recursive calls..
            return;
        }
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

    private String FindTargetRepo() throws Exception {

        // Populates Repository list
        this.GetReposForUser(0);

        if (this.repos.size() == 0) {
            System.out.println("Error: Repository list empty.");
            return "";
        }

        System.out.println("Searching repository list for "+this.targetRepository+"...");
        for (String repo: this.repos) {
            if (repo.contains(this.targetRepository)) {

                System.out.println("\nFound! Scanning "+this.targetRepository+"...\n");

                // Query API for that specific repository information
                String apiRes = this.startSession(
                        "GET",
                        "https://api.github.com/repos/"+this.username+"/"+this.targetRepository
                );

                //System.out.println(apiRes);
                return apiRes;
            }
        }
        return "Could not locate "+this.targetRepository+".";
    }

    private void GetContentsURL() throws Exception{
        JsonParser parser = new JsonParser();
        try {
            this.contentsURL = parser.parse(this.FindTargetRepo())
                    .getAsJsonObject()
                    .get("contents_url")
                    .toString()
                    .replaceAll("\\u007B\\+path\\u007D", "") // <== replaces {+path}
                    .replaceAll("\"", "");        // <== removes extra quotes added by JSON.
        }
        catch (Exception e) {
            throw e;
        }
    }

    private void GetFilenamesFromRepo(String subdirectory, boolean recursiveCall) throws Exception { // TODO: 3) Get repo filenames

        // Gets "contents_url" of repository after confirming repository exists.
        JsonParser           parser       = new JsonParser();
        List<ContentDetails> contentsList = new ArrayList<>();
        try {
            if (!recursiveCall) {
                // Queries the API for top-level directory contents url
                this.GetContentsURL();
            }

            subdirectory = (subdirectory.matches(""))? subdirectory : subdirectory+"/";
            String fullRoute = (recursiveCall)? this.contentsURL+subdirectory.replace("\"", "") : this.contentsURL;
            System.out.println("234: "+fullRoute);
            JsonArray repoContents = parser.parse
                    (this.startSession("GET", fullRoute)
                    ).getAsJsonArray();

                for (JsonElement content: repoContents) {
                    try {
                        ContentDetails contentEntry = new ContentDetails(
                                content.getAsJsonObject().get("name").toString(),
                                content.getAsJsonObject().get("path").toString(),
                                content.getAsJsonObject().get("type").toString()
                        );
                        if (contentEntry.getType().contains("file")){
                            contentEntry.setDownload(content.getAsJsonObject().get("download_url").toString());
                        }
                        System.out.println("Name: "+contentEntry.getName()+" | Path: "+contentEntry.getPath()+" | Type: "+contentEntry.getType());
                        contentsList.add(contentEntry);
                    }
                    catch (Exception e) {
                        throw e;
                    }
                }

            System.out.println("Receiving contents for "+((recursiveCall)? this.contentsURL+subdirectory : this.contentsURL));



            for (ContentDetails entry: contentsList) {
                System.out.println("Current Entry type:"+entry.getType());
                if (entry.getType().contains("dir")) {
                // Recursively searches through any content type marked "dir" for files
                    System.out.println("Second Search:");
                    this.GetFilenamesFromRepo(entry.getPath(), true);
                }
                else if (entry.getType().contains("file")) {
                // Stores all "download_url" members of type "file" to the filePaths list for downloading.
                    System.out.println("File found!");
                    this.filePaths.add(entry);
                }
                else {
                    // If a malformed entry was not caught earlier...
                    System.out.println("Error proc: "+entry.getName()+" | Type: "+entry.getType()+" | Path: "+entry.getPath());
                }

            }


            // Returns log of all files found to be downloaded by GetFilesFromGithub

        }
        catch (Exception e){
            throw e;
        }
    }

    // Retrieves the specified file from the repository this class was instantiated with. Using "*" will retrieve every file.
    public void GetFileFromGithub(String fileName, String outDirectory) throws Exception {

        // Step 0: if user of parent app specifies a table, then use that table name, otherwise use "*" to denote all tables.
        System.out.println(
                " ----------------------- \n" +
                "| Contacting Github... | \n" +
                " ----------------------- \n"
        );
        // Step 1: Retrieve filenames and their download paths for repository provided at instantiation.
        System.out.println("Searching for '"+((fileName != "*")? fileName : "ALL TABLES")+"'");


        this.GetFilenamesFromRepo("", false);

            // Step 2: Search for {tableName or all tables} script raw data from Github using filePaths objects list.
            // Checks for empty, malformed, or non-existent directory map
            boolean found = false;

            if (this.filePaths.size() <= 1) {
                    System.out.println("Warning: an abnormally low amount of files were returned while searching target directory.");
            }
            else {
            for (ContentDetails file: this.filePaths) {

                if (file.getDownloadUrl().matches("")){
                    System.out.println("Warning: No download URL found for '"+file.getName()+"'. Skipping...\n");
                    continue;
                }

                    // Step 3: Parse raw data from {tableName} file.
                    if ( !(file.getName() == null) && (fileName.contains("*") || file.getName().contains(fileName)) ) {
                        System.out.println("Downloading "+file.getName()+" to "+outDirectory+"...");

                        //        String downloadPath = "https://raw.githubusercontent.com/"+this.username+"/"+this.targetRepository+"/master"+path+name;
                        //        return this.startSession("GET", downloadPath);

                        String rawData = this.startSession("GET", file.getDownloadUrl());
                        System.out.println("rawData: "+rawData);
                        if(!(rawData == null || rawData.matches("") || rawData.contains("Not Found") || rawData.contains("Bad Request"))) {

                            // Step 4: Save raw data as a file to provided directory.
                            // Note:   Filename will match what is on github.
                            try {
                                System.out.println("File Path: "+file.getPath());
                                // Create a parent directory based on repo/db name inside of target output directory
                                String parentDirectory = "./"+outDirectory+"/"+this.targetRepository+"/";
                                File newFile = new File(parentDirectory);
                                newFile.mkdir();

                                PrintWriter writer = new PrintWriter(parentDirectory+file.getName().replaceAll("\"", ""), "UTF-8");
                                writer.print(rawData);
                                writer.close();
                                System.out.println(file.getName()+" downloaded.\n");
                                //found = true;
                            }
                            catch(FileNotFoundException e) {
                                System.out.println("The provided directory does not exist. Defaulting to current directory.");

                                PrintWriter writer = new PrintWriter(file.getName(), "UTF-8");
                                writer.print(rawData);
                                writer.close();
                                System.out.println(file.getName()+" downloaded.\n");
                                //found = true;
                            }


                        } else {
                            System.out.println("Error downloading "+file.getName()+" from "+file.getDownloadUrl()+".\n");
                        }
                    }
                }
            }

            // Only gets output if the above conditions are not met.
//        if (!found) {
//            System.out.println("Sorry: "++" was not found. Either it is not located in the repository, or an unknown communications error occured. No file downloaded.");
//        }
    }
}
