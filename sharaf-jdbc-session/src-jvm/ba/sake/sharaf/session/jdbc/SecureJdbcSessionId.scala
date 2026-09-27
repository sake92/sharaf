package ba.sake.sharaf.session.jdbc

private[jdbc] object SecureJdbcSessionId:
  private val random = new java.security.SecureRandom()

  def generate(): String =
    val bytes = new Array[Byte](16)
    random.nextBytes(bytes)
    java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
