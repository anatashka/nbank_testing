package junior_updated.iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
            Test                                    Account         Result
 1. transfer amount is positive and less 10k        your            Succeed
 2. transfer amount is 10k                          your            Succeed
 3. transfer amount is negative                     your            BAD_REQUEST
 4. transfer amount is less than balance            your            Succeed
 5. transfer amount is more than balance            your            BAD_REQUEST
 6. transfer amount is more than 10k                your            BAD_REQUEST
 7. transfer amount is positive and less 10k        other           Succeed
 8. transfer amount is 10k                          other           Succeed
 9. transfer amount is negative                     other           BAD_REQUEST
 10. transfer amount is less than balance           other           Succeed
 11. transfer amount is more than balance           other           BAD_REQUEST
 12. transfer amount is more than 10k               other           BAD_REQUEST
 13. transfer to non-existing account               n/a             BAD_REQUEST
 14. transfer without authorization                 n/a             UNAUTHORIZED
 */


public class TransferMoneyTest_v1 {
    private static String userAuthHeader0;
    private static int accountId1;
    private static int accountId2;
    private static String userAuthHeader1;
    private static int accountId3;

    @BeforeAll
    public static void setupTests(){
        RestAssured.filters(
                new RequestLoggingFilter(),
                new ResponseLoggingFilter()
        );
        //create user1
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                            "username": "TransferUser0",
                            "password": "P@ssw0rd",
                            "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        // get user token
        userAuthHeader0 = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                            "username": "TransferUser0",
                            "password": "P@ssw0rd"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        //create account
        accountId1 = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader0)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .jsonPath()
                .getInt("id");

        // deposit 5000
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader0)
                .body(String.format("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """, accountId1))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //check that balance is 5000
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader0)
                .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId1))
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("amount", hasItem(5000f));

                //create account
        accountId2 = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader0)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .jsonPath()
                .getInt("id");

        //create user2
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                            "username": "TransferUser1",
                            "password": "P@ssw0rd",
                            "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        // get user token
        userAuthHeader1 = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                            "username": "TransferUser1",
                            "password": "P@ssw0rd"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        //create account
        accountId3 = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader1)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    // ===== TRANSFERS BETWEEN OWN ACCOUNTS =====
    @Test
    public void transferBetweenOwnAccountsLess10KShouldSucceedTest() {
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 250.7
                        }
                        """, accountId1, accountId2))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            // verify TRANSFER_OUT on sender
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId1))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_OUT' }.amount", hasItem(250.7f));

            // verify TRANSFER_IN on receiver
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId2))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_IN' }.amount", hasItem(250.7f));
        }

        @Test
        public void transferBetweenOwnAccountsExactly10KShouldSucceedTest() {
            // deposit 5000
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """, accountId1))
                    .post("http://localhost:4111/api/v1/accounts/deposit")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            // deposit 5000
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """, accountId1))
                    .post("http://localhost:4111/api/v1/accounts/deposit")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 10000
                        }
                        """, accountId1, accountId2))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            // verify TRANSFER_OUT on sender
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId1))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_OUT' }.amount", hasItem(10000f));

            // verify TRANSFER_IN on receiver
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId2))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_IN' }.amount", hasItem(10000f));
        }

        @Test
        public void transferBetweenOwnAccountsMoreThan10KShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 10001
                        }
                        """, accountId1, accountId2))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount cannot exceed 10000", response);
        }

        @Test
        public void transferBetweenOwnAccountsZeroAmountShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 0
                        }
                        """, accountId1, accountId2))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount must be at least 0.01", response);
        }

        @Test
        public void transferBetweenOwnAccountsNegativeAmountShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": -100
                        }
                        """, accountId1, accountId2))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount must be at least 0.01", response);
        }

        @Test
        public void transferBetweenOwnAccountsMoreThanBalanceShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 99999
                        }
                        """, accountId1, accountId2))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount cannot exceed 10000", response);
        }

        // ===== TRANSFERS TO ANOTHER USER'S ACCOUNT =====

        @Test
        public void transferToAnotherUserAccountLess10KShouldSucceedTest() {
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 2500.7
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            // verify TRANSFER_OUT on sender
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId1))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_OUT' }.amount", hasItem(2500.7f));

            // verify TRANSFER_IN on receiver
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader1)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId3))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_IN' }.amount", hasItem(2500.7f));
        }

        @Test
        public void transferToAnotherUserAccountExactly10KShouldSucceedTest() {
            // deposit 5000
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """, accountId1))
                    .post("http://localhost:4111/api/v1/accounts/deposit")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            // deposit 5000
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """, accountId1))
                    .post("http://localhost:4111/api/v1/accounts/deposit")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 10000
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            // verify TRANSFER_OUT on sender
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId1))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_OUT' }.amount", hasItem(10000f));

            // verify TRANSFER_IN on receiver
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader1)
                    .get(String.format("http://localhost:4111/api/v1/accounts/%d/transactions", accountId3))
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .body("findAll { it.type == 'TRANSFER_IN' }.amount", hasItem(10000f));

        }

        @Test
        public void transferToAnotherUserAccountMoreThan10KShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 10001
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount cannot exceed 10000", response);
        }

        @Test
        public void transferToAnotherUserAccountZeroAmountShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 0
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount must be at least 0.01", response);
        }

        @Test
        public void transferToAnotherUserAccountNegativeAmountShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": -100
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Transfer amount must be at least 0.01", response);
        }

        @Test
        public void transferToAnotherUserAccountMoreThanBalanceShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 9999
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Invalid transfer: insufficient funds or invalid accounts", response);
        }

        @Test
        public void transferToNonExistingAccountShouldFailTest() {
            String response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("Authorization", userAuthHeader0)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": 99999,
                              "amount": 100
                        }
                        """, accountId1))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .asString();
            assertEquals("Invalid transfer: insufficient funds or invalid accounts", response);
        }

        @Test
        public void transferWithoutAuthorizationShouldFailTest() {
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(String.format("""
                        {
                              "senderAccountId": %d,
                              "receiverAccountId": %d,
                              "amount": 100
                        }
                        """, accountId1, accountId3))
                    .post("http://localhost:4111/api/v1/accounts/transfer")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }
    }