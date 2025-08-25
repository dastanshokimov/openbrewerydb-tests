package tests;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeSuite;

public class BaseApiTest {
    @BeforeSuite
    public void setup() {
        RestAssured.baseURI = System.getProperty("API_BASE_URI", "https://api.openbrewerydb.org");
        RestAssured.basePath = "/v1";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
