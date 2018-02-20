import java.sql.*;
import oracle.jdbc.OracleDriver;

public class dbConnect {
    private Connection connection;
    private Statement  statement;

    public void Connect() {
        // Create a connection to the database
            try {
            String dbDriver       = "oracle.jdbc.driver.OracleDriver";

            String serverName     = "localhost";
            String portNumber     = "1521";
            String serviceName    = "xe";
            String url            = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + "/" + serviceName;
            String username       = "system";
            String password       = "oracle";

            System.out.println(url);

            Class.forName(dbDriver);
            OracleDriver ora = new OracleDriver();

            Connection connection = DriverManager.getConnection(url, username, password);

            ora.acceptsURL(url);
            //ora.getClass(connection);
            statement  = connection.createStatement();

            // Get all the things from the DB
            ResultSet allTheThings = statement.executeQuery("SELECT * FROM emp, dept;");
            System.out.println(allTheThings);

            // Close the DB connection
            statement.close();
            connection.close();
        }
            catch (ClassNotFoundException e) {
            // Could not find the database driver
            System.out.println("Database driver not found.");
        }
            catch (SQLException e) {
            // Could not connect to the database
            System.out.println("Could not connect to the database with information provided.");
        }



    }

}

