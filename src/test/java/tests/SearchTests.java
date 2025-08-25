package tests;

import api.ApiClient;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

public class SearchTests extends BaseApiTest{
    private ApiClient api;

    @BeforeClass
    public void init() {
        api = new ApiClient();
    }

    @Test
    public void searchCommonQueryTest() {
        Response response = api.searchBreweries("dog");
        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/brewery-list.schema.json"));
        assertThat(response.jsonPath().getList("$")).isNotEmpty();
    }

    @Test
    public void findBreweryByPartOfNameTest() {
        Response list = api.listBreweries();
        String id   = list.jsonPath().getString("[0].id");
        String name = list.jsonPath().getString("[0].name");

        String token = Arrays.stream(name.split("\\s+"))
                .map(s -> s.replaceAll("[^A-Za-z0-9]", ""))
                .filter(s -> s.length() >= 3)
                .findFirst()
                .orElse(name);

        Response response = api.searchBreweries(token);
        response.then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/brewery-list.schema.json"));

        List<String> ids = response.jsonPath().getList("id");
        assertThat(ids).as("results should contain original brewery by id").contains(id);
    }

    @Test
    public void caseInsensitiveSearchTest() {
        String token = "porter";

        List<String> lower = api.searchBreweries(token.toLowerCase())
                .then().statusCode(200)
                .extract().jsonPath().getList("id");

        List<String> upper = api.searchBreweries(token.toUpperCase())
                .then().statusCode(200)
                .extract().jsonPath().getList("id");

        assertThat(lower).isNotEmpty();
        assertThat(upper).isNotEmpty();
        assertThat(new HashSet<>(lower)).isEqualTo(new HashSet<>(upper));
    }

    @Test
    public void spacesAndUnderscoresShouldBeEquivalentTest() {
        String phrase = "san diego";

        Response withSpace = api.searchBreweries(phrase);
        Response withUnderscore = api.searchBreweries(phrase.replace(' ', '_'));

        withSpace.then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/brewery-list.schema.json"));
        withUnderscore.then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/brewery-list.schema.json"));

        Set<String> a = new HashSet<>(withSpace.jsonPath().getList("id"));
        Set<String> b = new HashSet<>(withUnderscore.jsonPath().getList("id"));

        assertThat(a).isNotEmpty();
        assertThat(b).isNotEmpty();
        assertThat(a).isEqualTo(b);
    }

    @Test
    public void noMatchesTest() {
        String gibberish = "zzzzzzzzzz_qqqqqqqqqq_1234567890";

        Response r = api.searchBreweries(gibberish);

        r.then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/brewery-list.schema.json"));

        List<?> list = r.jsonPath().getList("$");
        assertThat(list).isEmpty();
    }

    @Test
    public void notAllowedMethodTest() {
        Response response = api.searchBreweriesMethodNotAllowed("dog");
        response.then()
                .statusCode(405);
    }
}
