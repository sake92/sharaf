package ba.sake.sharaf.pac4j

import scala.jdk.CollectionConverters.*
import org.pac4j.core.profile.{UserProfile, CommonProfile}

class SecurityServiceTest extends munit.FunSuite:

  test("profiles returns empty list when no profiles set"):
    assertEquals(SecurityService.profiles, List.empty)
    assertEquals(SecurityService.currentUser, None)

  test("returns profiles set by Pac4jSecurityHandler"):
    val profile = CommonProfile()
    profile.setId("user123")
    profile.addAttribute("email", "user@example.com")

    Pac4jSecurityHandler.currentProfiles.set(List(profile))
    try
      val profiles = SecurityService.profiles
      assertEquals(profiles.size, 1)
      assertEquals(profiles.head.getId, "user123")
      assertEquals(profiles.head.getAttribute("email").asInstanceOf[String], "user@example.com")
      assertEquals(SecurityService.currentUser.map(_.getId), Some("user123"))
    finally
      Pac4jSecurityHandler.currentProfiles.remove()

  test("returns empty after request lifecycle"):
    // Simulate the finally block that clears profiles
    val profile = CommonProfile()
    profile.setId("user123")
    Pac4jSecurityHandler.currentProfiles.set(List(profile))
    assertEquals(SecurityService.profiles.size, 1)

    Pac4jSecurityHandler.currentProfiles.remove()
    assertEquals(SecurityService.profiles, List.empty)
