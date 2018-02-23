public class Tests {

        // Test Case #1:
        public static void GetOneFileFromDemoDirectory(){
            try {
                GHExtractor extractor = new GHExtractor(
                        "test-repo",
                        "jsrj",
                        ""
                );
                extractor.GetFileFromGithub("","");

            }
            catch (Exception e) {

                e.printStackTrace();

            }
        }


        // Test Case #2:
        public static void GetAllFilesFromTestRepo(){
            try {
                GHExtractor extractor = new GHExtractor(
                        "test-repo",
                        "jsrj",
                        ""
                );
                //extractor.GetFileFromGithub("testfile7.txt","files-from-github");
                extractor.FindSpecificRepo("test-repo");
            }
            catch (Exception e) {

                e.printStackTrace();

            }
        }


        // Test Case #3:
        public static void GetNonexistentFileFromDemoDirectory(){
            try {
                GHExtractor extractor = new GHExtractor(
                        "test-repo",
                        "jsrj",
                        ""
                );
                extractor.GetFileFromGithub("","");

            }
            catch (Exception e) {

                e.printStackTrace();

            }
        }


        // Test Case #4:
        public static void PlaceFilesIntoSubdirectory(){
            try {
                GHExtractor extractor = new GHExtractor(
                        "test-repo",
                        "jsrj",
                        ""
                );
                extractor.GetFileFromGithub("","");

            }
            catch (Exception e) {

                e.printStackTrace();

            }
        }


    public static void main(String[] args) {
        // TODO: Change GHExtractor.java so that it does not need to rely on a .directorymap
        // TODO: Add outputs and assertions to test cases.

        //GetOneFileFromDemoDirectory();
        GetAllFilesFromTestRepo();
        //GetNonexistentFileFromDemoDirectory();
        //PlaceFilesIntoSubdirectory();

    }
}
