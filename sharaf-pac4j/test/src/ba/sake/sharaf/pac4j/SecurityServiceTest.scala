package ba.sake.sharaf.pac4j

import org.pac4j.core.profile.{UserProfile, CommonProfile}
import ba.sake.sharaf.session.{SessionImpl, SessionHolder, SecureSessionId, InMemorySessionStore, SessionConfig}

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.util.{Base64, LinkedHashMap}
import scala.util.Using

class SecurityServiceTest extends munit.FunSuite:

  test("profiles returns empty list when no session"):
    assertEquals(SecurityService.profiles, List.empty)
    assertEquals(SecurityService.currentUser, None)

  test("profiles returns empty list when session has no pac4j data"):
    val session = new SessionImpl("test-id", java.time.Instant.now(), java.time.Instant.now(), Map.empty)
    SessionHolder.set(session)
    try
      assertEquals(SecurityService.profiles, List.empty)
      assertEquals(SecurityService.currentUser, None)
    finally
      SessionHolder.clear()

  test("profiles returns empty list when session has invalid base64 data"):
    val session = new SessionImpl("test-id", java.time.Instant.now(), java.time.Instant.now(), Map.empty)
    session.set("pac4j.pac4jUserProfiles", "not-valid-base64!!!")
    SessionHolder.set(session)
    try
      assertEquals(SecurityService.profiles, List.empty)
    finally
      SessionHolder.clear()

  test("can extract profile serialized by SharafSessionStore"):
    // Simulate what SharafSessionStore.set() does
    val profile = CommonProfile()
    profile.setId("user123")
    profile.addAttribute("email", "user@example.com")

    val profilesMap = new LinkedHashMap[String, CommonProfile]()
    profilesMap.put("HeaderClient", profile)

    val serialized = serialize(profilesMap)
    val session = new SessionImpl("test-id", java.time.Instant.now(), java.time.Instant.now(), Map.empty)
    session.set("pac4j.pac4jUserProfiles", serialized) // uses Session.set() which JSON-encodes
    SessionHolder.set(session)
    try
      val profiles = SecurityService.profiles
      assertEquals(profiles.size, 1)
      assertEquals(profiles.head.getId, "user123")
      assertEquals(profiles.head.getAttribute("email").asInstanceOf[String], "user@example.com")
      assertEquals(SecurityService.currentUser.map(_.getId), Some("user123"))
    finally
      SessionHolder.clear()

  private def serialize(obj: AnyRef): String =
    Using(new ByteArrayOutputStream()) { baos =>
      Using(new ObjectOutputStream(baos)) { oos =>
        oos.writeObject(obj)
      }
      Base64.getEncoder.encodeToString(baos.toByteArray)
    }.get
