import com.google.gson.*;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class GHExtractor
{
    // -- Props --
    private String         username;
    private String         authToken;
    private String         targetRepository;
    private String         contentsURL;

    private List<ContentDetails> filePaths = new ArrayList<>();
    private List<String>         repos     = new ArrayList<>();
    private String               errMsg    = "None";
    private boolean              verbose   = false;

    // GHExtractor - For changing target repo
    public void setTargetRepository(String targetRepository) {
        this.targetRepository = targetRepository;
    }

    // GHExtractor - For toggling console output on and off
    public void toggleVerbose() {
        this.verbose = !this.verbose;
    }

    // GHExtractor - Initializer
    public GHExtractor(String tRepo, String uName, String authT) {
        this.username         = uName;
        this.authToken        = (authT == null)? authT : "";
        this.setTargetRepository(tRepo);

        if(this.verbose) {
            // Ensures that verbose mode is defaulted to off.
            this.toggleVerbose();
        }
    }

    // TODO [NEXT]: Implement a log file that documents what files were downloaded and when.
        // 1: Creates file if one does not already exist
        // 2: If one already exists, read its contents and store to a local list
        // 3: Log filepath to list.
        // 4: If filepath has changed, but filename has not, then update filepath instead of new entry.
        // 5: Iterate through list and update any prelogged file dates
        // 6: Add new files and dates if new ones were added.
        // 7: Return a list of files that have changed in GitHub for other method to target for download.
    // TODO [CURRENT]: Implement a short method that can be called from the download method to log each file downloaded.
        // 1: Should be able to have just a ContentDetails object passed into it.
        // 2: Should internally create a timestamp at the time it was called.

    // TODO [3]: Implement a pre-check that queries the GitHub API for any file changes since last download logged.
        // Read the previously generated log file for update dates
        // If the log file does not exist, end method and begin download.
        // If the log file does exist, load contents to a list, and iterate through it for specific filename.
        // Send conditional request to github to check if a file has changed.
        // If Github returns a 304 status, then check for the file in local directory instead.

    // TODO [4]: Implement a repo ping that checks if repo returns 200. If 404, then regex search it in all user's repos.
        // Utilize startSession to probe for direct URI of a repository
        // If 200, then return the JSON response
        // If 404, then call FindTargetRepo()

    // TODO [5]: Implement user input option for GetFileFromGithub()
        // Void method with no parameters that will toggle a user input mode for GFFG.
        // Option: Toggle Verbose
        // Option: Filename with guide
        // Option: inDirectory with guide
        // Option: outDirectory with guide

    // TODO [DONE]: Make all sout logs Verbose-Mode compliant.

    // Initiates GET request to Github API
    private String startSession(String Mode, String URI) throws Exception {

        System.out.println((this.verbose)? "Establishing connection to "+URI : "");
        HttpsURLConnection session;
        try {
            session = (HttpsURLConnection) new URL(URI).openConnection();
        }
        catch (Exception hostErr) {
            System.out.println((this.verbose)? "Error: Unable to establish connection-- "+hostErr.getMessage()+"." : null);
            System.out.println((this.verbose)? "Will attempt to reconnect in 10 Seconds..." : null);

            Thread.sleep(10000);
            return this.startSession(Mode, URI);
        }

        session.setRequestMethod     (Mode                                               );
        session.setRequestProperty   ("User-Agent"    , "GHExtractor"                    );
        session.setRequestProperty   ("Accept"        , "application/vnd.github.v3+json," +
                                                        "text/html,"                      +
                                                        "application/xhtml+xml,"          +
                                                        "image/jxr,"                      +
                                                        "*/*"                            );
        session.setRequestProperty   ("Username"      ,  this.username                   );
        session.setRequestProperty   ("Authorization" ,  this.authToken                  );

        // Handler for API Rate-Limits -- START
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
            // This output will be logged regardless of verbosity setting.
            System.out.println("Warning: API Rate Limit Reached.");
            System.out.println("Reset Time: "+formattedReset);
            System.out.println("Local Time: "+formattedLocal);
            System.out.println("Will re-attempt connection once rate limit resets...");

            Thread.sleep(timeDifference*1000);
            System.out.println("Re-attempting connection...");

            // Re-attempts Session with the last known URI
            return this.startSession(Mode, URI);
        }
        else if (queriesRemaining < 5) {
            // This output will be logged regardless of verbosity setting.
            System.out.println("Rate Limit Warning: 5 or less API calls remaining.");
            System.out.println("Continue or wait until reset? : [C]ontinue | [W]ait");
            String userInput = System.console().readLine().toUpperCase();

            // If the user requests tool to wait until Rate reset specified by API...
            if (userInput.matches("W")) {
                Thread.sleep(timeDifference*1000);
                return this.startSession(Mode, URI);
            }
        }
        // Handler for API Rate-Limits -- END


        int responseCode = session.getResponseCode();
        try {
            switch (responseCode) {

                // Should be extended to account for all Server codes.
                case 404:
                    this.errMsg = "Error "+responseCode+": "+URI+" "+session.getResponseMessage()+". Check route path.\n";
                    System.out.println((this.verbose)? this.errMsg : "");
                    break;//return("-1");

                case 403:
                    this.errMsg = responseCode+": "+session.getResponseMessage();
                    break;//return("-1");

                case 401:
                    this.errMsg = "Error "+responseCode+": "+session.getResponseMessage()+". Bad credentials?\n";
                    System.out.println((this.verbose)? this.errMsg : "");
                    break;//return("-1");

                case 200:
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

                    // Print response in JSON, if possible, otherwise print it in plaintext
                    try {
                        JsonElement elem = json.parse(response.toString());
                        return gson.toJson(elem);
                    }
                    catch (JsonSyntaxException e) {
                        //System.out.println((this.verbose)? response.toString() : ""); <-- can log rawdata.
                        return response.toString();
                    }

                default:
                    this.errMsg = "Error Encountered";
                    System.out.println((this.verbose)? this.errMsg : "");
                    return "-1";
            }
        } catch (Exception e) {
            this.errMsg = e.toString();
            throw e;
        }

        // If API responds with an unhandled response code.
        this.errMsg = "Unhandled Response Code -- '"+responseCode+": "+session.getResponseMessage()+"'";
        System.out.println((this.verbose)? this.errMsg: "");
        return this.errMsg;
    }

    // Populates a repository list object based on results of https://api.github.com/users/{username}
    private void GetReposForUser(int iteration) throws Exception {
        if (this.repos.size() > 0 && iteration == 0) {
            // Prevents from repeatedly querying the API for information that is already present, while not interfering with recursive calls..
            return;
        }
        String resultsPer  = "100";
        int    page        = 1+iteration;
        try {
            //    - pulls info on all repos for a specified user
            String apiRes = this.startSession(
                    "GET",
                    "https://api.github.com/users/"+this.username+"/repos?"
                        +"page="+page
                        +"&per_page="+resultsPer
            );

            // Provides console output in verbose mode to let user know that this is doing something.
            if (page == 1) {
                System.out.println(((this.verbose)? "Retrieving repository list for "+this.username+"..." : ""));
            }

            // Empty API return check
            if (apiRes.matches("-1") || apiRes.matches(" ") || apiRes.contains("Unhandled Response Code --")) {
                System.out.println("Warning: No data was retrieved from GitHub.");
                return;
            }

            // Convert the GSON from API response to a JsonArray
            if (!apiRes.contains(this.errMsg)) {

                JsonArray repoArray = (JsonArray) new JsonParser().parse(apiRes);

                // Parses results for repository names and stores them in the "repos" list
                int results = 0;
                for (JsonElement repo: repoArray) {

                    String repoName = repo.getAsJsonObject().get("name").toString();

                    System.out.println(((this.verbose)? "Repo "+(((iteration > 0)? results+100 : results)+1)+": "+repoName : ""));
                    this.repos.add(repoName);
                    results++;
                }

                // Since the API has a results limit of 100, this will use the pagination feature to recursively call itself for a full list.
                if (results > 0) {
                    // Recursive call to retrieve next page of repo list until no pages are left.
                    this.GetReposForUser(++iteration);
                } else {
                    System.out.println(((this.verbose)? "Repository Count: "+ ((results == 0 && page > 1)? 100+page+1 : results) : ""));
                    // Any follow up logic to execute once repo search has completed...
                }
            }
        }
        // TODO: Remove this once handlers are implemented for 404/500 errors.
        // If, for some reason, an error is encountered, this prevents the standard error from stopping program.
        catch (Exception e) {
            throw e;
        }
    }

    // Searches entire repo list of a user if first API request returns 404
    private String FindTargetRepo() throws Exception {

        // Populates Repository list
        // TODO: Execute this only after calling new method that checks direct API route, and that returns false.
        this.GetReposForUser(0);

        if (this.repos.size() == 0) {
            System.out.println("Error: Repository list empty.");
            return "";
        }
        System.out.println((this.verbose)? "Searching repository list for "+this.targetRepository+"..." : "");

        for (String repo: this.repos) {
            if (repo.contains(this.targetRepository)) {

                System.out.println("\nFound! Scanning "+this.targetRepository+"...\n");

                // TODO: Make this be the default action of this method.
                // Query API for that specific repository's information
                String apiRes = this.startSession(
                        "GET",
                        "https://api.github.com/repos/"+this.username+"/"+this.targetRepository
                );
                return apiRes;
            }
        }
        return "Could not locate "+this.targetRepository+".";
    }

    // Obtains a nested contents URL for specific files within a directory
    private void GetContentsURL() throws Exception{
        JsonParser parser = new JsonParser();
        try {
            this.contentsURL = parser.parse(this.FindTargetRepo())
                    .getAsJsonObject()
                    .get("contents_url")
                    .toString()
                    .replaceAll("\\u007B\\+path\\u007D", "") // <== replaces {+path}
                    .replaceAll("\"", "");                   // <== removes extra quotes added by JSON.
        }
        catch (Exception e) {
            throw e;
        }
    }

    // Gets "contents_url" of a directory from github API after confirming repository exists.
    private void GetFilenamesFromRepo(String subdirectory, boolean recursiveCall) throws Exception {
        JsonParser           parser       = new JsonParser();
        List<ContentDetails> contentsList = new ArrayList<>();
        try {
            if (!recursiveCall && subdirectory.matches("")) {
                // Queries the API for top-level directory contents url
                this.GetContentsURL();
            } else {
                this.contentsURL = "https://api.github.com/repos/"+this.username+"/"+this.targetRepository+"/contents/"+subdirectory;
            }

            subdirectory = (subdirectory.matches(""))? subdirectory : subdirectory+"/";

            // TODO: Implement a log check and conditional request before directly requesting the ContentsURL
            JsonArray repoContents = parser.parse
                    (this.startSession("GET", this.contentsURL)
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
                        System.out.println((this.verbose)? ("Name: "+contentEntry.getName()+" | Path: "+contentEntry.getPath()+" | Type: "+contentEntry.getType()) : "");
                        contentsList.add(contentEntry);
                    }
                    catch (Exception e) {
                        throw e;
                    }
                }
            // TODO: Determine if this should be verbosity compliant or not.
            System.out.println("Receiving contents from "+((recursiveCall)? this.contentsURL+subdirectory : this.contentsURL));

            for (ContentDetails entry: contentsList) {
                if (entry.getType().contains("dir")) {
                // Recursively searches through any content type marked "dir" for files
                    System.out.println("Directory found. Searching...");
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
        }
        catch (Exception e){
            throw e;
        }
    }

    // Retrieves the specified file from the repository this class was instantiated with. Using "*" will retrieve every file.
    public void GetFileFromGithub(String fileName, String inDirectory, String outDirectory) throws Exception {

        // Step 0: if user of parent app specifies a table, then use that table name, otherwise use "*" to denote all tables.
        System.out.println((this.verbose)?
                " ----------------------- \n" +
                "| Contacting Github... | \n" +
                " ----------------------- \n"
        : "");

        // Step 1: Retrieve filenames and their download paths for repository provided at instantiation.
        // Reports regardless of Verbosity setting.
        System.out.println
                (
                        "Searching for '"+((fileName != "*")? fileName : "ALL FILES")
                                +"' in '"+((inDirectory.matches(""))? this.targetRepository : inDirectory)+"'"
                );

        // If a specific file and path are provided, query contents of that path to ensure the file sits there.
        // If the "*" wildcard string is passed in as a filename...
        this.GetFilenamesFromRepo(((fileName != "*")? inDirectory : inDirectory), false);

        // Step 2: Search for file raw data from Github using filePaths objects list.
        if (this.filePaths.size() <= 0) {
                System.out.println
                        ((this.verbose)?
                                "Warning: an abnormally low amount of files were " +
                                "returned while searching target directory." : null
                        );
        }
        else {

            for (ContentDetails file: this.filePaths) {
                if (file.getDownloadUrl().matches("")){
                    System.out.println
                            ((this.verbose)?
                                    "Warning: No download URL found for '"+file.getName()+"'. \n" : null
                            );
                    continue;
                }
                    // Step 3: Parse raw data from file.
                    if ( !(file.getName() == null) && (fileName.contains("*") || file.getName().contains(fileName)) ) {

                        System.out.println
                                ((this.verbose)?
                                        "Downloading "+file.getName()+" to "+outDirectory+"..." : null
                                );

                        String rawData = this.startSession("GET", file.getDownloadUrl());

                        if(
                            !(rawData == null || rawData.matches("")
                                    || rawData.contains("Not Found")
                                    || rawData.contains("Bad Request"))) {
                            // Step 4: Save raw data as a file to provided directory. Using filename found on github..
                            try {
                                // Create a parent directory based on repo/db name inside of target output directory
                                String parentDirectory = "./"+outDirectory+"/"+file.getPath().replaceAll(file.getName(), "");
                                File newFile = new File(parentDirectory);

                                // Creates requested directores if they do not already exist.
                                boolean directoryCreated = newFile.mkdirs();
                                System.out.println
                                        ((this.verbose)? ((directoryCreated)?
                                                 "Directory created successfully"
                                                :"Target directory found, skipping creation.")
                                                : null
                                        );

                                PrintWriter writer = new PrintWriter(parentDirectory+file.getName().replaceAll("\"", ""), "UTF-8");
                                writer.print(rawData);
                                writer.close();

                                // Outputs regardless of verbosity setting
                                System.out.println(file.getName()+" downloaded.\n");
                            }
                            catch(FileNotFoundException e) {
                                System.out.println((this.verbose)? "The provided parent directory does not exist. Defaulting to current directory." : "");

                                PrintWriter writer = new PrintWriter(file.getName(), "UTF-8");
                                writer.print(rawData);
                                writer.close();

                                // Outputs regardless of verbosity setting
                                System.out.println(file.getName()+" downloaded.\n");
                            }
                        }
                        else {
                            // Outputs regardless of verbosity setting
                            System.out.println("Error downloading "+file.getName()+" from "+file.getDownloadUrl()+".\n");
                        }
                    }
                }
            }
    }
}