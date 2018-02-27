public class ContentDetails {

    // Props
    private String name;
    private String path;
    private String type;
    private String download;


    // Init
    public ContentDetails(String name, String path, String type) {
        this.setName(name);
        this.setPath(path);
        this.setType(type);
    }


    // Getters
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }
    public String getDownloadUrl() {
        // Strips the extra quotes that get added by the API
        return download.replace("\"", "");
    }


    // Setters
    private void setName(String name) {
        this.name = name.replaceAll("\"", "");
    }

    private void setPath(String path) {
        this.path = path.replaceAll("\"", "");
    }

    private void setType(String type) {
        this.type = type.replaceAll("\"", "");
    }

    public void setDownload(String url) {
        this.download = url.replaceAll("\"", "");
    }
}