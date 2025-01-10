# Implementing SSOAdapter for User Identification in Firefly

This document provides guidelines for Firefly to identify a logged-in user's information, including the user's name, email, 
and other personal details. It also describes how to obtain the user's credentials to pass along to external services. 
This functionality is achieved by defining a custom implementation of the interface `edu.caltech.ipac.firefly.server.security.SsoAdapter`.

## Implementation

### User Information

```java
UserInfo getUserInfo();
```

This method returns a `UserInfo` object containing the user's information, typically extracted from the HTTP headers of the request. 

For example, if the user's email is available in the header `OIDC_CLAIM_EMAIL`, an implementation might look like this:

```java
public UserInfo getUserInfo() {
    RequestAgent req = ServerContext.getRequestOwner().getRequestAgent();
    String email = req.getHeader("OIDC_CLAIM_EMAIL");
    return new UserInfo(email, null);
}
```

### ID Token

```java
Token getAuthToken();
```

This method returns a `Token` object that contains the user's claims and additional information required to establish the user's credentials.

For example, if the user's ID token (a JWT) is found in the `X-Remote-User` header, the implementation would be:

```java
public Token getAuthToken() {
    RequestAgent req = ServerContext.getRequestOwner().getRequestAgent();
    String idToken = req.getHeader("X-Remote-User");
    return new Token(idToken);
}
```

### Pass-along Credentials

```java
default void setAuthCredential(HttpServiceInput inputs) {}
```

This method is invoked before making a request to an external service. It allows the implementation to pass along the user's credentials (e.g., in headers) required by the service.

For example, if an external service requires a Bearer token in the `Authorization` header, you might implement it as follows:

```java
public void setAuthCredential(HttpServiceInput inputs) {
    Token token = getAuthToken();
    if (token != null && token.getId() != null) {
        if (SsoAdapter.requireAuthCredential(inputs.getRequestUrl(), ".only-for-this-domain.com")) {
            inputs.setHeader("Authorization", "Bearer " + token.getId());
        }
    }
}
```

> **Note:** Replace `.only-for-this-domain.com` with the domain of the external service that requires the Bearer token.

## Deployment

To deploy the custom implementation:

1. **Create a new class** that implements the interface `edu.caltech.ipac.firefly.server.security.SsoAdapter`.

2. **Update the `sso.framework.adapter` property** in Firefly to reference the new implementation. For example:

   - **Docker CLI**
     ```bash
     -e PROPS_sso__framework__adapter=edu.caltech.ipac.firefly.server.security.MySsoAdapter
     ```

   - **Docker Compose**
     ```yaml
     environment:
       - PROPS_sso__framework__adapter=edu.caltech.ipac.firefly.server.security.MySsoAdapter
     ```

   - **Kubernetes**
     ```yaml
     env:
       - name: PROPS_sso__framework__adapter
         value: edu.caltech.ipac.firefly.server.security.MySsoAdapter
     ```

3. Package the custom class into a JAR file and add it to Firefly. You can either place the JAR in the `WEB-INF/lib` directory or the compiled class in `WEB-INF/classes` if not using a JAR. 

**Example: Extending the Firefly Docker image with a custom JAR**

- **Dockerfile**
  ```dockerfile
  FROM ipac/firefly:latest
  COPY MySsoAdapter.jar ${CATALINA_HOME}/webapps/firefly/WEB-INF/lib/
  ```

## Bundled Example Implementation

The class **`edu.caltech.ipac.firefly.server.security.PersonalAccessToken`** is a bundled example of the `SsoAdapter` interface. 
It offers a straightforward way to authenticate users using a personal access token. In this setup, Firefly is configured to use 
a shared personal access token for accessing external services. The token is included in the `Authorization` header with each request to these services.  
This token is configured at runtime and is used for all users, which is beneficial in secure environments where every user has the same level of external service access.

### **Using the PersonalAccessToken Implementation**

**Update the `sso.framework.adapter` property** in Firefly to reference the `PersonalAccessToken` implementation. Additionally, set the necessary environment variables for your token and authorized hosts.

#### Docker CLI
```bash
-e PROPS_sso__framework__adapter=edu.caltech.ipac.firefly.server.security.PersonalAccessToken \
-e PROPS_sso__access__token=<token> \
-e PROPS_sso__req__auth__hosts=<domain>
```

#### Docker Compose
```yaml
environment:
  - PROPS_sso__framework__adapter=edu.caltech.ipac.firefly.server.security.PersonalAccessToken
  - PROPS_sso__access__token=<token>
  - PROPS_sso__req__auth__hosts=<domain>
```

#### Kubernetes
```yaml
env:
  - name: PROPS_sso__framework__adapter
    value: edu.caltech.ipac.firefly.server.security.PersonalAccessToken
  - name: PROPS_sso__access__token
    value: <token>
  - name: PROPS_sso__req__auth__hosts
    value: <domain>
```

Replace `<token>` with your personal access token and `<domain>` with the domain(s) that require authentication. 
This configuration enables Firefly to use a consistent token for authenticating requests to external services across all users.