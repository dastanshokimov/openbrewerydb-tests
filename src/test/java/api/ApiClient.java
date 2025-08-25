package api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class ApiClient {
    private final RequestSpecification spec;

    public ApiClient() {
        this.spec = new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .build();
    }

    /** GET /breweries/{obdb-id} */
    public Response getBreweryById(String id) {
        return given().spec(spec).log().all()
                .when().get("/breweries/{id}", id)
                .then().extract().response();
    }

    /** GET /breweries — список с дефолтной пагинацией */
    public Response listBreweries() {
        return given().spec(spec).log().all()
                .when().get("/breweries")
                .then().extract().response();
    }

    /** GET /breweries/search?query=... */
    public Response searchBreweries(String query) {
        return given().spec(spec)
                .queryParam("query", query).log().all()
                .when().get("/breweries/search")
                .then().extract().response().prettyPeek();
    }

    /** POST /breweries/search?query=... */
    public Response searchBreweriesMethodNotAllowed(String query) {
        return given().spec(spec)
                .queryParam("query", query).log().all()
                .when().post("/breweries/search")
                .then().extract().response().prettyPeek();
    }
}
