package middle;

import middle.generators.RandomData;
import middle.models.CreateUserRequest;
import middle.models.LoginUserRequest;
import middle.models.UserRole;
import middle.requests.AdminCreatesUserRequester;
import middle.requests.LoginUserRequester;
import middle.specs.RequestSpecs;
import middle.specs.ResponseSpecs;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class LoginUserTest {

    @Test
    public void adminCanGenerateAuthTokenTest(){
        LoginUserRequest userRequest = LoginUserRequest.builder()
                .username("admin")
                .password("admin")
                .build();

        new LoginUserRequester(RequestSpecs.unauthSpec(), ResponseSpecs.responseOK())
                .post(userRequest);
            }
    @Test
    public void userCanGenerateAuthTokenTest(){
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreatesUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        new LoginUserRequester(RequestSpecs.unauthSpec(), ResponseSpecs.responseOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .header("Authorization", Matchers.notNullValue());

    }
}
