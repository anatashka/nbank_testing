package middle;

import middle.generators.RandomData;
import middle.models.CreateUserRequest;
import middle.models.CreateUserResponse;
import middle.models.UserRole;
import middle.specs.ResponseSpecs;
import org.junit.jupiter.api.Test;
import middle.requests.AdminCreatesUserRequester;
import middle.specs.RequestSpecs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class CreateUserTest extends BaseTest{

    @Test
    public void adminCanCreateUserWithCorrectData(){
        //create user
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse createUserResponse = new AdminCreatesUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest).extract().as(CreateUserResponse.class);

        softly.assertThat(createUserRequest.getUsername()).isEqualTo(createUserResponse.getUsername());
        softly.assertThat(createUserRequest.getPassword()).isNotEqualTo(createUserResponse.getPassword());
        softly.assertThat(createUserRequest.getRole()).isEqualTo(createUserResponse.getRole());

    }

    public static Stream<Arguments> userInvalidData(){
        return Stream.of(
                //username field validation
                Arguments.of(" ", "Password123$", "USER", "username", "Username cannot be blank"),
                Arguments.of("ab", "Password123$", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abc$", "Password123$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"));


                //password field validation

                //role field validation
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidData(String username, String password, String role, String errorKey, String errorMessage){
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new AdminCreatesUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.responseBAD_REQUEST(errorKey, errorMessage))
                .post(createUserRequest);
    }
}
