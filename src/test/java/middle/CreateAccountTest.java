package middle;

import io.restassured.http.ContentType;
import middle.generators.RandomData;
import middle.models.CreateUserRequest;
import middle.models.LoginUserRequest;
import middle.models.UserRole;
import middle.requests.AdminCreatesUserRequester;
import middle.requests.CreateAccountRequester;
import middle.requests.LoginUserRequester;
import middle.specs.RequestSpecs;
import middle.specs.ResponseSpecs;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class CreateAccountTest extends BaseTest{

    @Test
    public void userCanCreateAccountTest(){
        //create user
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreatesUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        //create account
        new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null);

        // check all user's accounts and make sure new one is there
    }
}
