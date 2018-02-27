import java.io.File;
import java.io.IOException;

public class Tests {

        public static void GetAllFilesFromTestRepo(){
            try {
                GHExtractor extractor = new GHExtractor(
                        "test-repo",
                        "jsrj",
                        "44649e73f09ea701189441dd093c711d1f25975a"
                );
                extractor.toggleVerbose();
                extractor.GetFileFromGithub("*", "files-from-github");
            }
            catch (Exception e) {

                e.printStackTrace();

            }
        }

    public static void main(String[] args) {
        // TODO: Add outputs and assertions to test cases.

//        File testFile = new File("files-from-github/adirectory/test.txt");
//
//        System.out.println(testFile.getParentFile().mkdir());
//        try {
//            testFile.createNewFile();
//        }
//        catch (Exception e) {
//            System.out.println("Failed to create new directory or file.");
//        }
//        try {
//            testFile.createNewFile();
//        }
//        catch (Exception e) {
//            System.out.println("Failed to create new directory");
//        }
        GetAllFilesFromTestRepo();

    }
}
