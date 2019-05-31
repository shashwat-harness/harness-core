package software.wings.integration;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.rule.OwnerRule.MARK;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.wings.beans.User.Builder.anUser;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteKeys;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.resources.UserResource.ResendInvitationEmailRequest;
import software.wings.security.AuthenticationFilter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;

/**
 * Created by rsingh on 4/24/17.
 */
@Slf4j
public class UserServiceIntegrationTest extends BaseIntegrationTest {
  private final String validEmail = "raghu" + System.currentTimeMillis() + "@wings.software";
  @Inject private SecretManager secretManager;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDisableEnableUser() {
    User user = userService.getUserByEmail(defaultEmail);
    String userId = user.getUuid();
    String userAccountId = user.getDefaultAccountId();

    // 1. Disable user
    WebTarget target = client.target(API_BASE + "/users/disable/" + userId + "?accountId=" + userAccountId);
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).put(
        entity(defaultEmail, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());

    // 2. User should be disabled after disable operation above
    user = userService.getUserByEmail(defaultEmail);
    assertTrue(user.isDisabled());

    // 3. Once the user is disabled, its logintype call will fail with 401 unauthorized error.
    target = client.target(API_BASE + "/users/logintype?userName=" + defaultEmail + "&accountId=" + userAccountId);
    try {
      RestResponse<LoginTypeResponse> loginTypeResponse =
          getRequestBuilder(target).get(new GenericType<RestResponse<LoginTypeResponse>>() {});
      fail("NotAuthorizedException is expected");
    } catch (NotAuthorizedException e) {
      // Expected 'HTTP 401 Unauthorized' exception here.
    }

    // 4. Re-enable user
    target = client.target(API_BASE + "/users/enable/" + userId + "?accountId=" + userAccountId);
    restResponse = getRequestBuilderWithAuthHeader(target).put(
        entity(defaultEmail, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());

    // 5. User should not be disabled after enabled above
    user = userService.getUserByEmail(defaultEmail);
    assertFalse(user.isDisabled());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testBlankEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testInvalidEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz.com");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.INVALID_EMAIL, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDomainNotAllowed() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=xyz@some-domain.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUserExists() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=admin@harness.io");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(1, restResponse.getResponseMessages().size());
    final ResponseMessage responseMessage = restResponse.getResponseMessages().get(0);
    assertEquals(ErrorCode.USER_ALREADY_REGISTERED, responseMessage.getCode());
    assertFalse(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidEmail() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=" + validEmail);
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testValidEmailWithSpace() {
    WebTarget target = client.target(API_BASE + "/users/verify-email?email=%20" + validEmail + "%20");
    RestResponse<Boolean> restResponse = getRequestBuilder(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSwitchAccount() {
    WebTarget target = client.target(API_BASE + "/users/switch-account?accountId=" + accountId);
    RestResponse<User> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<User>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    User user = restResponse.getResource();
    assertNotNull(user);
    String jwtToken = user.getToken();
    assertNotNull(jwtToken);

    // Switch again, a new token should be generated.
    restResponse = getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<User>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    user = restResponse.getResource();
    assertNotNull(user);
    String newJwtToken = user.getToken();
    assertNotEquals(jwtToken, newJwtToken);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetLoginTypeWithAccountId() {
    WebTarget target =
        client.target(API_BASE + "/users/logintype?userName=" + adminUserEmail + "&accountId=" + accountId);
    RestResponse<LoginTypeResponse> restResponse =
        target.request().get(new GenericType<RestResponse<LoginTypeResponse>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    LoginTypeResponse loginTypeResponse = restResponse.getResource();
    assertNotNull(loginTypeResponse);
    assertEquals(AuthenticationMechanism.USER_PASSWORD, loginTypeResponse.getAuthenticationMechanism());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testDefaultLoginWithAccountId() {
    WebTarget target = client.target(API_BASE + "/users/login?accountId=" + defaultAccountId);
    String basicAuthValue =
        "Basic " + encodeBase64String(format("%s:%s", adminUserEmail, new String(adminPassword)).getBytes());
    RestResponse<User> restResponse =
        target.request().header("Authorization", basicAuthValue).get(new GenericType<RestResponse<User>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    User user = restResponse.getResource();
    assertNotNull(user);
    assertNotNull(user.getToken());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSetDefaultAccount() {
    WebTarget target = client.target(API_BASE + "/users/set-default-account/" + accountId);
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).put(
        entity(accountId, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    Boolean result = restResponse.getResource();
    assertNotNull(result);
    assertTrue(result);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLoginUserUsingIdentityServiceAuth() throws IllegalAccessException {
    WebTarget target = client.target(API_BASE + "/users/user/login?email=" + adminUserEmail);

    String identityServiceToken = generateIdentityServiceJwtToken();
    RestResponse<User> response =
        target.request()
            .header(HttpHeaders.AUTHORIZATION, AuthenticationFilter.IDENTITY_SERVICE_PREFIX + identityServiceToken)
            .get(new GenericType<RestResponse<User>>() {});
    assertEquals(0, response.getResponseMessages().size());
    User user = response.getResource();
    assertNotNull(user);
    assertNotNull(user.getToken());
    assertNotNull(user.getAccounts());
    assertTrue(user.getAccounts().size() > 0);
    assertTrue(user.getSupportAccounts().size() > 0);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetUserUsingIdentityServiceAuth() throws IllegalAccessException {
    WebTarget target = client.target(API_BASE + "/users/user");

    User adminUser = userService.getUserByEmail(adminUserEmail);
    String identityServiceToken = generateIdentityServiceJwtToken();
    RestResponse<User> response =
        target.request()
            .header(HttpHeaders.AUTHORIZATION, AuthenticationFilter.IDENTITY_SERVICE_PREFIX + identityServiceToken)
            .header(AuthenticationFilter.USER_IDENTITY_HEADER, adminUser.getUuid())
            .get(new GenericType<RestResponse<User>>() {});
    assertEquals(0, response.getResponseMessages().size());
    User user = response.getResource();
    assertNotNull(user);
    assertNotNull(user.getAccounts());
    assertTrue(user.getAccounts().size() > 0);
    assertTrue(user.getSupportAccounts().size() > 0);
  }

  private String generateIdentityServiceJwtToken() throws IllegalAccessException {
    MainConfiguration configuration = new MainConfiguration();
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setJwtIdentityServiceSecret("HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhwwn0K4Ttr0uoOxSsEVYNrUU=");
    configuration.setPortal(portalConfig);
    FieldUtils.writeField(secretManager, "configuration", configuration, true);
    return secretManager.generateJWTToken(new HashMap<>(), JWT_CATEGORY.IDENTITY_SERVICE_SECRET);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testTrialSignupSuccess() {
    final String name = "Mark Lu";
    final String pwd = "somepwd";
    final String email = "mark" + System.currentTimeMillis() + "@harness.io";

    WebTarget target = client.target(API_BASE + "/users/trial");
    // Trial signup with just one email address, nothing else.
    RestResponse<Boolean> response =
        target.request().post(entity(email, TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertTrue(response.getResource());

    // Trial signup again using the same email should succeed. A new verification email will be sent.
    response = target.request().post(entity(email, TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertTrue(response.getResource());

    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get();
    assertNotNull(userInvite);
    assertFalse(userInvite.isCompleted());
    String inviteId = userInvite.getUuid();
    assertNotNull(inviteId);

    // Preparing for completing the invitation.
    final String accountName = "Harness_" + System.currentTimeMillis();
    final String companyName = "Harness_" + System.currentTimeMillis();
    userInvite.setName(name);
    userInvite.setAgreement(true);
    userInvite.setPassword(pwd.toCharArray());

    // Complete the trial invitation.
    userInvite = completeTrialUserInvite(inviteId, userInvite, accountName, companyName);
    assertTrue(userInvite.isCompleted());
    assertEquals(SourceType.TRIAL, userInvite.getSource().getType());
    String accountId = userInvite.getAccountId();
    assertNotNull(userInvite.getAccountId());
    assertTrue(userInvite.isAgreement());

    // Verify the account get created.
    Account account = accountService.get(accountId);
    assertNotNull(account);

    // Verify the trial user is created, assigned to proper account and with the account admin roles.
    User savedUser = wingsPersistence.createQuery(User.class).filter(UserKeys.email, email).get();
    assertNotNull(savedUser);
    assertTrue(savedUser.isEmailVerified());
    assertEquals(1, savedUser.getAccounts().size());

    // Trial signup a few more time using the same email will trigger the rejection, and the singup result will be
    // false.
    for (int i = 0; i < UserServiceImpl.REGISTRATION_SPAM_THRESHOLD; i++) {
      response = target.request().post(entity(email, TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    }
    assertEquals(0, response.getResponseMessages().size());
    assertFalse(response.getResource());

    // Delete the user just created as a cleanup
    userService.delete(savedUser.getAccounts().get(0).getUuid(), savedUser.getUuid());

    // Verify user is deleted
    assertNull(userService.getUserByEmail(email));
    // Verify user invite is deleted
    assertNull(wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupSuccess() {
    final String name = "Raghu Singh";
    final String email = "abc" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = "some account" + System.currentTimeMillis();
    final String companyName = "some company" + System.currentTimeMillis();
    WebTarget target = client.target(API_BASE + "/users");
    RestResponse<User> response = target.request().post(entity(anUser()
                                                                   .withName(name)
                                                                   .withEmail(email)
                                                                   .withPassword(pwd)
                                                                   .withRoles(getAccountAdminRoles())
                                                                   .withAccountName(accountName)
                                                                   .withCompanyName(companyName)
                                                                   .build(),
                                                            APPLICATION_JSON),
        new GenericType<RestResponse<User>>() {});
    assertEquals(0, response.getResponseMessages().size());
    final User savedUser = response.getResource();
    assertEquals(name, savedUser.getName());
    assertEquals(email, savedUser.getEmail());
    assertNull(savedUser.getPassword());
    assertEquals(1, savedUser.getAccounts().size());
    assertEquals(accountName, savedUser.getAccounts().get(0).getAccountName());
    assertEquals(companyName, savedUser.getAccounts().get(0).getCompanyName());

    // Delete the user just created as a cleanup
    userService.delete(savedUser.getAccounts().get(0).getUuid(), savedUser.getUuid());

    // Verify user is deleted
    assertNull(userService.getUserByEmail(email));
    // Verify user invite is deleted
    assertNull(wingsPersistence.createQuery(UserInvite.class).filter(UserInviteKeys.email, email).get());
  }

  @Test
  @Owner(emails = MARK, intermittent = true)
  @Category(IntegrationTests.class)
  public void testUserInviteSignupAndSignInSuccess() {
    final String name = "Mark Lu";
    final String email = "abc" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    UserInvite userInvite = inviteUser(accountId, name, email);
    assertNotNull(userInvite.getUuid());
    assertFalse(userInvite.isCompleted());
    assertEquals(email, userInvite.getEmail());

    userInvite.setName(name);
    userInvite.setPassword(pwd);
    userInvite.setAgreement(true);
    completeUserInviteAndSignIn(accountId, userInvite.getUuid(), userInvite);

    UserInvite retrievedUserInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    assertTrue(retrievedUserInvite.isCompleted());

    // Delete the user just created as a cleanup
    User user = userService.getUserByEmail(email);
    userService.delete(accountId, user.getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testInvalidUserInviteShouldFail() {
    String invalidInviteId = UUIDGenerator.generateUuid();
    UserInvite userInvite = new UserInvite();
    userInvite.setAccountId(accountId);
    userInvite.setUuid(invalidInviteId);
    userInvite.setName("Test Invitation");
    WebTarget target = client.target(API_BASE + "/users/invites/" + invalidInviteId + "?accountId=" + accountId);
    try {
      target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<User>>() {});
      fail("HTTP 401 not authorized exception is expected.");
    } catch (NotAuthorizedException e) {
      // HTTP 400 Bad Request exception is expected.
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupSuccessWithSpaces() throws IOException {
    final String name = "  Brad  Pitt    ";
    final String email = "xyz" + System.currentTimeMillis() + "@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   " + System.currentTimeMillis();
    final String companyName = "  star   wars    enterprise   " + System.currentTimeMillis();
    WebTarget target = client.target(API_BASE + "/users");

    RestResponse<User> response = null;
    try {
      response = target.request().post(entity(anUser()
                                                  .withName(name)
                                                  .withEmail(email)
                                                  .withPassword(pwd)
                                                  .withRoles(getAccountAdminRoles())
                                                  .withAccountName(accountName)
                                                  .withCompanyName(companyName)
                                                  .build(),
                                           APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
    } catch (BadRequestException e) {
      logger.info(new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity())));
      Assert.fail();
    }
    assertEquals(0, response.getResponseMessages().size());
    final User savedUser = response.getResource();
    assertEquals(name.trim(), savedUser.getName());
    assertEquals(email.trim(), savedUser.getEmail());
    assertNull(savedUser.getPassword());
    assertEquals(1, savedUser.getAccounts().size());
    assertEquals(accountName.trim(), savedUser.getAccounts().get(0).getAccountName());
    assertEquals(companyName.trim(), savedUser.getAccounts().get(0).getCompanyName());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupEmailWithSpace() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "  xyz@wings  ";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.INVALID_ARGUMENT, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupBadEmail() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@wings";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (BadRequestException e) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.INVALID_EMAIL, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupBadEmailDomain() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "xyz@some-email.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with bad email");
    } catch (ClientErrorException e) {
      assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.USER_DOMAIN_NOT_ALLOWED, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSignupEmailExists() throws IOException {
    final String name = "Brad  Pitt    ";
    final String email = "admin@harness.io";
    final char[] pwd = "somepwd".toCharArray();
    final String accountName = " star   wars   ";
    final String companyName = "  star   wars    enterprise   ";
    WebTarget target = client.target(API_BASE + "/users");

    try {
      RestResponse<User> response = target.request().post(entity(anUser()
                                                                     .withName(name)
                                                                     .withEmail(email)
                                                                     .withPassword(pwd)
                                                                     .withRoles(getAccountAdminRoles())
                                                                     .withAccountName(accountName)
                                                                     .withCompanyName(companyName)
                                                                     .build(),
                                                              APPLICATION_JSON),
          new GenericType<RestResponse<User>>() {});
      Assert.fail("was able to sign up with existing email");
    } catch (ClientErrorException e) {
      assertEquals(HttpStatus.SC_CONFLICT, e.getResponse().getStatus());
      final String jsonResponse = new String(ByteStreams.toByteArray((InputStream) e.getResponse().getEntity()));
      final RestResponse<User> restResponse =
          JsonUtils.asObject(jsonResponse, new TypeReference<RestResponse<User>>() {});
      assertEquals(1, restResponse.getResponseMessages().size());
      assertEquals(ErrorCode.USER_ALREADY_REGISTERED, restResponse.getResponseMessages().get(0).getCode());
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testAccountCreationWithKms() {
    loginAdminUser();
    User user = wingsPersistence.createQuery(User.class).filter(UserKeys.email, "admin@harness.io").get();

    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(user.getUuid()))
                                            .actions(Sets.newHashSet(Action.READ))
                                            .build();
    wingsPersistence.save(harnessUserGroup);

    Account account = Account.Builder.anAccount()
                          .withAccountName(UUID.randomUUID().toString())
                          .withCompanyName(UUID.randomUUID().toString())
                          .withLicenseInfo(LicenseInfo.builder()
                                               .accountType(AccountType.PAID)
                                               .accountStatus(AccountStatus.ACTIVE)
                                               .licenseUnits(InstanceLimitProvider.defaults(AccountType.PAID))
                                               .build())

                          .build();

    assertFalse(accountService.exists(account.getAccountName()));
    assertNull(accountService.getByName(account.getCompanyName()));

    WebTarget target = client.target(API_BASE + "/users/account");
    RestResponse<Account> response = getRequestBuilderWithAuthHeader(target).post(
        entity(account, APPLICATION_JSON), new GenericType<RestResponse<Account>>() {});

    assertNotNull(response.getResource());
    assertTrue(accountService.exists(account.getAccountName()));
    assertNotNull(accountService.getByName(account.getCompanyName()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testResendInvitationAndCompleteInvitation() {
    Account adminAccount =
        wingsPersistence.createQuery(Account.class).filter(AccountKeys.accountName, defaultAccountName).get();
    String accountId = adminAccount.getUuid();

    ResendInvitationEmailRequest request = new ResendInvitationEmailRequest();
    request.setEmail(adminUserEmail);

    WebTarget target = client.target(API_BASE + "/users/resend-invitation-email?accountId=" + accountId);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).post(
        entity(request, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());
    assertTrue(response.getResource());
  }

  private List<Role> getAccountAdminRoles() {
    return wingsPersistence
        .query(Role.class,
            aPageRequest()
                .addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN)
                .addFilter("accountId", Operator.EQ, accountId)
                .withLimit("2")
                .build())
        .getResponse();
  }

  private UserInvite completeTrialUserInvite(
      String inviteId, UserInvite userInvite, String accountName, String companyName) {
    WebTarget target = client.target(
        API_BASE + "/users/invites/trial/" + inviteId + "?account=" + accountName + "&company=" + companyName);
    RestResponse<UserInvite> response =
        target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<UserInvite>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());

    return response.getResource();
  }

  private UserInvite inviteUser(String accountId, String name, String email) {
    String userInviteJson =
        "{\"name\":\"" + name + "\", \"accountId\":\"" + accountId + "\", \"emails\":[\"" + email + "\"]}";
    WebTarget target = client.target(API_BASE + "/users/invites?accountId=" + accountId);
    RestResponse<List<UserInvite>> response = getRequestBuilderWithAuthHeader(target).post(
        entity(userInviteJson, APPLICATION_JSON), new GenericType<RestResponse<List<UserInvite>>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());
    assertEquals(1, response.getResource().size());

    return response.getResource().get(0);
  }

  private User completeUserInviteAndSignIn(String accountId, String inviteId, UserInvite userInvite) {
    WebTarget target = client.target(API_BASE + "/users/invites/" + inviteId + "?accountId=" + accountId);
    RestResponse<User> response =
        target.request().put(entity(userInvite, APPLICATION_JSON), new GenericType<RestResponse<User>>() {});
    assertEquals(0, response.getResponseMessages().size());
    assertNotNull(response.getResource());

    return response.getResource();
  }
}
