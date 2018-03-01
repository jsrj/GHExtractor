import java.io.File;
import java.io.IOException;

public class Tests {
        // Global GHExtractor Object
        private static GHExtractor test_extractor;

        // -- SETUP -- START
        // Case 1
        private static void GetAllFilesFromDemoDB(){
            try {
                test_extractor.GetFileFromGithub("*", "demo.db", "files-from-github");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Case 2
        public static void GetOneFileFromDemoDB() {
            try {
                test_extractor.GetFileFromGithub("dept.ddl", "demo.db/dept", "files-from-github/anotherDirectory");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Case 3
        public static void DownloadEntireRepo() {
            try {
                test_extractor.GetFileFromGithub("*", "", "files-from-github");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Case 4
        public static void DownloadDifferentRepoToNested() {
            try {
                test_extractor.setTargetRepository("GHExtractor");
                test_extractor.GetFileFromGithub("*", "", "files-from-github/Secondary-outDirectory");
            }
            catch (Exception e) {

            }
        }
        // -- SETUP -- END

    public static void main(String[] args) {
        // TODO: Add outputs and assertions to test cases.




        // -- Preconfigure -- START
        // Load System properties for all test cases.
        try {
            PropertiesConfig props = new PropertiesConfig(".env");
            props.Set();

            test_extractor = new GHExtractor(
                    "test-repo",
                    System.getProperty("userName"),
                    System.getProperty("OAuth"   )
            );
            test_extractor.toggleVerbose();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // -- Preconfigure -- END




        // -- Tests -- START
        // Case 1 - Get all files from a specific directory inside of repository.
        GetAllFilesFromDemoDB();

        // Case 2 - Get One file by specifically entering it's full path and name.
        //GetOneFileFromDemoDB();

        // Case 3 - Completely clone entire target repository to the specified output directory.
        //DownloadEntireRepo();

        // Case 4 - Change target repository and download it into a nested parent directory.
        //DownloadDifferentRepoToNested();
        // -- Tests -- END

    }
}
