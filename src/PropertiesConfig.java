import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PropertiesConfig {

    // Local Property Sub-object
    private class Property {
        protected String key;
        protected String value;

        // Init -
        protected Property(String key, String val) {
            this.setKey(key);
            this.setValue(val);
        }
        // Get -
        protected String getKey() {
            return this.key;
        }
        protected String getValue() {
            return this.value;
        }
        // Set -
        protected void setKey(String k) {
            this.key = k;
        }
        protected void setValue(String v) {
            this.value = v;
        }
    }

    // Properties - These should be matched either to environment variables in System, or in a secure file
    private List<Property> properties = new ArrayList<>();

    // Initializer
    public PropertiesConfig(String envFile) throws IOException {
        this.LoadEnv(envFile);
    }

    // Getters
    private List<Property> getProperties() {
        return this.properties;
    }

    // Setters
    private void addProperty(Property newProp) {
        this.properties.add(newProp);
    }

    // This will load variables into System Properties from the Properties list
    public void Set() {
        for (Property prop: this.getProperties()) {
            String key = prop.getKey();
            String val = prop.getValue();
            System.out.println("'"+key+"' property set.");
            System.setProperty(key, val);
        }
    }

    // This method reads from a .env file.
    private void LoadEnv(String envFile) throws IOException {

        FileReader reader = new FileReader(envFile);
        char[] fileChars  = new char[2048];

        reader.read(fileChars);
        reader.close();

        String output = "";

        // Filters out null block characters
        for (char c: fileChars) {
            if (c != '\u0000') {
                output = output+c;
                }
        }

        List<String[]> envVars = new ArrayList<>();
        System.out.println("------------------------------------");
        System.out.println("| Setting Environment Variables... |");
        System.out.println("------------------------------------");
        for (String var: output.split("\n")) {
            envVars.add(var.split("="));
        }
        for (String[] keyAndValue: envVars) {

            this.addProperty(new Property(keyAndValue[0], keyAndValue[1]));
        }
    }
}
