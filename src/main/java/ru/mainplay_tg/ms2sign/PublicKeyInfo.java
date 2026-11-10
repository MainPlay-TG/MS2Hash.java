package ru.mainplay_tg.ms2sign;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import ru.mainplay_tg.Util;

public class PublicKeyInfo {
  public byte[] uuid;
  public byte[] pubBytes;
  public String alias;

  public PublicKeyInfo(byte[] uuid, byte[] pubBytes, String alias) {
    this.uuid = uuid;
    this.pubBytes = pubBytes;
    this.alias = alias;
  }

  public static PublicKeyInfo parseUrl(String url, boolean parseAlias) throws URISyntaxException {
    URI uri = URI.create(url);
    // Проверить схему
    if (!uri.getScheme().equalsIgnoreCase("ms2sign")) {
      throw new URISyntaxException(url, "Invalid scheme. Expected 'ms2sign'.");
    }
    // Проверить путь
    String path = uri.getPath();
    if (path == null) {
      throw new URISyntaxException(url, "Path is missing");
    }
    String[] pathParts = path.substring(1).split("/");
    if (pathParts.length != 3 || !"pubkey".equals(pathParts[0])) {
      throw new URISyntaxException(url, "Invalid path structure. Expected /pubkey/{uuid}/{pubKey}");
    }
    // Получить UUID
    byte[] uuid = Util.uuidToBytes(UUID.fromString(pathParts[1]));
    // Получить публичный ключ
    byte[] pubBytes = Base64.getUrlDecoder().decode(pathParts[2]);
    if (pubBytes.length != 32) {
      throw new URISyntaxException(url, "Decoded pubKey length is not 32 bytes");
    }
    // Получить алиас
    String alias = null;
    if (parseAlias) {
      String query = uri.getQuery();
      if (query != null && query.startsWith("alias=")) {
        alias = URLDecoder.decode(query.substring(6), StandardCharsets.UTF_8);
      }
    }
    return new PublicKeyInfo(uuid, pubBytes, alias);
  }

  public String exportUrl() {
    StringBuilder sb = new StringBuilder()
        .append("ms2sign:/pubkey/")
        .append(Util.uuidFromBytes(uuid).toString())
        .append("/")
        .append(Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(pubBytes));
    if (alias != null && !alias.isEmpty()) {
      sb.append("?alias=");
      sb.append(alias);
    }
    return sb.toString();
  }

  public UUID uuidObj() {
    return Util.uuidFromBytes(uuid);
  }
}