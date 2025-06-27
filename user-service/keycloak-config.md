# Keycloak Configuration for Google Social Login

This document outlines the steps to configure Google as an identity provider in Keycloak for social login.

## Prerequisites

*   A running Keycloak instance.
*   A Google Cloud Platform (GCP) account.

## Step 1: Configure Google Cloud Platform OAuth 2.0 Credentials

1.  **Go to Google Cloud Console:** Navigate to [https://console.cloud.google.com/](https://console.cloud.google.com/).
2.  **Select or Create Project:** Choose an existing project or create a new one.
3.  **Navigate to Credentials:** In the left navigation menu, go to "APIs & Services" > "Credentials".
4.  **Create OAuth Client ID:**
    *   Click on "+ CREATE CREDENTIALS" at the top.
    *   Select "OAuth client ID".
    *   For "Application type", choose "Web application".
    *   Give it a "Name" (e.g., "Keycloak Social Login").
5.  **Configure Authorized Redirect URIs:**
    *   Under "Authorized redirect URIs", click "+ ADD URI".
    *   Enter your Keycloak's redirect URI for the Google broker. This typically follows the pattern:
        `YOUR_KEYCLOAK_HOST/realms/YOUR_REALM_NAME/broker/google/endpoint`
        For example, if Keycloak is running locally on port 8081 and your realm is named `kong`:
        `http://localhost:8081/realms/kong/broker/google/endpoint`
    *   Replace `YOUR_KEYCLOAK_HOST` and `YOUR_REALM_NAME` with your actual Keycloak host and realm name.
6.  **Create and Note Credentials:**
    *   Click "CREATE".
    *   A dialog will appear showing your "Client ID" and "Client Secret". **Copy these values securely.** You will need them for Keycloak configuration.

## Step 2: Configure Google as an Identity Provider in Keycloak

1.  **Log in to Keycloak Admin Console:** Access your Keycloak admin console (e.g., `http://localhost:8081/admin`).
2.  **Select Your Realm:** From the dropdown in the top-left corner, select the realm you want to configure (e.g., `kong`).
3.  **Navigate to Identity Providers:** In the left navigation menu, click on "Identity Providers".
4.  **Add Google Provider:**
    *   In the "Add provider..." dropdown, select "Google".
5.  **Configure Google Provider Settings:**
    *   **Redirect URI:** This field will display the redirect URI that Keycloak expects. Ensure this URI is one of the "Authorized redirect URIs" you configured in the Google Cloud Platform console in Step 1.5.
    *   **Client ID:** Paste the "Client ID" you obtained from GCP (Step 1.6).
    *   **Client Secret:** Paste the "Client Secret" you obtained from GCP (Step 1.6).
    *   **Default Scopes:** `openid profile email` are common defaults.
    *   **Store Tokens:** Enable if you want Keycloak to store the external Google tokens.
    *   **Store Tokens Readable:** Enable if you want to allow applications to retrieve and read the stored external Google tokens.
    *   Adjust other settings like "Sync Mode" as needed for your use case.
6.  **Save:** Click "Save".

## Step 3: Configure Mappers for Google Identity Provider

Mappers define how user attributes from Google are mapped to Keycloak user attributes upon successful authentication.

1.  **Go to Mappers Tab:** After saving the Google identity provider, click on the "Mappers" tab for that provider.
2.  **Create Mappers:** Click "Create" to add new mappers. You'll typically want to map:
    *   **Email:**
        *   Name: `Google Email`
        *   Sync Mode Override: `inherit` (or choose as needed)
        *   Mapper Type: `Attribute Importer`
        *   Social Profile JSON Field Path: `email`
        *   User Attribute Name: `email` (this is the Keycloak user attribute)
    *   **First Name:**
        *   Name: `Google First Name`
        *   Mapper Type: `Attribute Importer`
        *   Social Profile JSON Field Path: `given_name`
        *   User Attribute Name: `firstName`
    *   **Last Name:**
        *   Name: `Google Last Name`
        *   Mapper Type: `Attribute Importer`
        *   Social Profile JSON Field Path: `family_name`
        *   User Attribute Name: `lastName`
    *   **Username (Optional, choose one strategy):**
        *   **Option A: Map from Email:**
            *   Name: `Google Username from Email`
            *   Mapper Type: `Attribute Importer`
            *   Social Profile JSON Field Path: `email`
            *   User Attribute Name: `username`
        *   **Option B: Map from Name (e.g., full name or given_name):**
            *   Name: `Google Username from Name`
            *   Mapper Type: `Attribute Importer`
            *   Social Profile JSON Field Path: `name` (or `given_name`)
            *   User Attribute Name: `username`
    *   Adjust "User Attribute Name" to match the attribute names used in your Keycloak user model.
3.  **Save each mapper.**

## Step 4: Test the Login

1.  Go to your Keycloak account login page for the configured realm (e.g., `http://localhost:8081/realms/kong/account`).
2.  You should now see an option to "Sign in with Google".
3.  Clicking it should redirect you to Google for authentication and then back to Keycloak.
4.  After successful authentication, Keycloak will either create a new user (if it's the first time for this Google account) or link to an existing one, based on your configuration.

## Important Notes

*   **HTTPS for Production:** In production environments, always use HTTPS for both your application and Keycloak. Google OAuth2 requires HTTPS for redirect URIs in production.
*   **Client Secret Security:** Keep your Google Client Secret confidential. Do not embed it directly in client-side code.
*   **Realm Name:** Ensure you are working within the correct Keycloak realm throughout the configuration.
*   **Troubleshooting:** Check Keycloak server logs and browser developer tools for errors if you encounter issues.
## Step 5: Configuring for Refresh Tokens (Important for Password Grant)

When using the Password Grant type for your client in Keycloak (e.g., the `kong-oidc` client if it's used for direct username/password login from your service), you need to ensure Keycloak is configured to issue refresh tokens and that their lifetimes are appropriate.

1.  **Client Configuration in Keycloak:**
    *   Navigate to your client's settings in the Keycloak Admin Console (e.g., `kong-oidc` under your realm).
    *   Under the "Advanced" tab (or similar, depending on Keycloak version), ensure that **"Use Refresh Tokens"** is ON. This is often on by default for confidential clients.
    *   If you want refresh tokens to be available even if the user's session times out (for true "offline" access via password grant), ensure the **"Offline Access"** switch for the *client* is also enabled, or that the client's "Access Token Lifespan" and "Client Session Idle/Max" settings are configured appropriately. For password grants, the `offline_access` scope in the request is also key.

2.  **Realm Token Lifetimes:**
    *   In your realm settings, under "Tokens", you can configure:
        *   **Access Token Lifespan:** How long access tokens are valid.
        *   **Client Session Idle:** How long a session can be idle before refresh tokens associated with it might become invalid if not "offline" tokens.
        *   **Client Session Max:** Absolute maximum time a session (and potentially its refresh tokens) can last.
        *   **Offline Session Idle:** For `offline_access` tokens, how long they can be idle. This is critical for long-lived refresh tokens.
        *   **Offline Session Max Limited:** Whether there's a hard upper limit on offline token lifespan.

3.  **Scope for Password Grant:**
    *   When your service requests a token using the password grant, include the `offline_access` scope if you intend for the refresh token to outlive the regular user session. The code for `authenticateWithKeycloak` in `UserService.java` has been updated to include this scope.

By ensuring these settings, the refresh tokens obtained via the password grant flow will behave as expected, allowing clients to maintain longer sessions without requiring users to re-enter their passwords frequently.
