package files.philosophy

import utils.*

object Authentication extends PhilosophyPage {

  override def pageSettings = super.pageSettings
    .withTitle("How To Authentication")
    .withLabel("Authentication")

  override def blogSettings =
    super.blogSettings.withSections(firstSection, pac4jSection, denyByDefaultSection)

  val firstSection = Section(
    "Authentication",
    s"""
    Some important security principles from OWASP guidelines:
    - use HTTPS
    - use random user ids to prevent enumeration and other attacks
    - use strong passwords, store them hashed, implement password recovery
    - use MFA, CAPTCHA, rate limiting etc to prevent automated attacks
    - etc.
    
    Read all of them in the [OWASP auth cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
    """.md
  )

  val pac4jSection = Section(
    "Pac4j",
    s"""
    Authentication in Sharaf is done usually by delegating it to [pac4j](https://www.pac4j.org/index.html).
    Pac4j is a battle-tested and widely used library for authentication and authorization.
    It supports many authentication mechanisms, including:
    - form based authentication (username + password)
    - OAuth2, with many providers (Google, Facebook, GitHub, etc)
    
    Pac4j has a concept of `Client`, which is a type of authentication mechanism.
    The main split is between `IndirectClient` and `DirectClient`.
      
    ### Indirect clients
    Indirect clients are used for form based authentication, OAuth2, etc.
    An important thing to mention here is the callback URL:
    - for username + password authentication, the callback URL  where the form is submitted to. Then a server-side session is created and user is signed in.
    - for OAuth2 (and similar mechanisms), the callback URL where the user is redirected to *after authentication*. 
      The server will then exchange the code for an *access token* and create a server-side session.
    
    ### Direct clients
    Direct clients are used for API authentication *on every request* (e.g. Basic Auth, JWT, etc).
    On every request, the client will extract the credentials from the request and authenticate the user.
    """.md
  )

  val denyByDefaultSection = Section(
    "Deny by Default Principle",
    """
    One important principle in security is the "deny by default" principle.
    You should use whitelisting, allow access only to what is needed.  
    This is because it is easy to forget to deny something, and it is hard to remember everything that should be denied.
    
    Concretely in pac4j, you can use `PathMatcher()`, to exclude certain paths from authentication:
    ```scala
    val publicRoutesMatcher = PathMatcher()
    publicRoutesMatcher.excludePaths("/", "/login-form")
    pac4jConfig.addMatcher("publicRoutesMatcher", publicRoutesMatcher)
    ..
    SecurityHandler.build(
      SharafHandler(..),
      pac4jConfig,
      "client1,client2...",
      null,
      "securityheaders,publicRoutesMatcher", // use publicRoutesMatcher here!
      DefaultSecurityLogic()
    )
    ```
    
    There are also:
    - `excludeBranch("/somepath")` to exclude all paths starting with "/somepath"
    - `excludeRegex("^/somepath/.*\$")` to exclude all paths matching the regex (be careful with this one!)
    """.md
  )
}
