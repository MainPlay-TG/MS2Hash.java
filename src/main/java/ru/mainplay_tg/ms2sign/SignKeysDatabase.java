package ru.mainplay_tg.ms2sign;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import ru.mainplay_tg.Util;

public class SignKeysDatabase {
  private String url;

  public SignKeysDatabase(String url) throws SQLException {
    this.url = url;
    try (Connection conn = getConn(); Statement stmt = conn.createStatement()) {
      boolean needCommit = false;
      if (!tableExists(conn, "keys")) {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS keys (
              uuid BINARY(16) PRIMARY KEY,
              privkey BINARY(32),
              pubkey BINARY(32) NOT NULL
            );""");
        needCommit = true;
      }
      if (!tableExists(conn, "keyAliases")) {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS keyAliases (
              uuid BINARY(16) NOT NULL,
              name VARCHAR(32) NOT NULL UNIQUE
            );""");
        needCommit = true;
      }
      if (needCommit) {
        conn.commit();
      }
    }
  }

  public Connection getConn() throws SQLException {
    Connection conn = DriverManager.getConnection(url);
    conn.setAutoCommit(false);
    return conn;
  }

  private boolean tableExists(Connection conn, String tableName) throws SQLException {
    DatabaseMetaData metaData = conn.getMetaData();
    try (ResultSet rs = metaData.getTables(null, null,
        tableName, new String[] { "TABLE" })) {
      return rs.next();
    }
  }

  /**
   * Получить UUID ключа по алиасу
   */
  public byte[] findKey(String alias) throws SQLException {
    String sql = "SELECT uuid FROM keyAliases WHERE name = ?";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, alias);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBytes("uuid");
        }
      }
    }
    return null;
  }

  /**
   * Получить приватный ключ по UUID
   */
  public Ed25519PrivateKeyParameters getPrivKey(byte[] uuid) throws SQLException {
    String sql = "SELECT privkey FROM keys WHERE uuid = ?";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBytes(1, uuid);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          byte[] key = rs.getBytes("privkey");
          if (key != null) {
            return new Ed25519PrivateKeyParameters(key, 0);
          }
        }
      }
    }
    return null;
  }

  public byte[] getPubKeyBytes(byte[] uuid) throws SQLException {
    String sql = "SELECT pubkey FROM keys WHERE uuid = ?";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBytes(1, uuid);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBytes("pubkey");
        }
      }
    }
    return null;
    // throw new SQLException("Key not found");
  }

  /**
   * Получить публичный ключ по UUID
   */
  public Ed25519PublicKeyParameters getPubKey(byte[] uuid) throws SQLException {
    byte[] raw = getPubKeyBytes(uuid);
    if (raw != null) {
      return new Ed25519PublicKeyParameters(raw, 0);
    }
    return null;
  }

  /**
   * Получить публичный ключ в формате для передачи
   */
  public String exportPubKey(byte[] uuid, String alias) throws SQLException {
    StringBuilder sb = new StringBuilder()
        .append("ms2sign://pubkey/")
        .append(Util.uuidFromBytes(uuid).toString())
        .append("/")
        .append(Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(getPubKeyBytes(uuid)));
    if (alias != null && !alias.isEmpty()) {
      sb.append("?alias=");
      sb.append(alias);
    }
    return sb.toString();
  };

  public String exportPubKey(byte[] uuid) throws SQLException {
    return exportPubKey(uuid, null);
  }

  /**
   * Импортировать публичный ключ из URL
   */
  public byte[] importPubKey(String url, Boolean saveAlias) throws URISyntaxException, SQLException {
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
    byte[] pubKey = Base64.getUrlDecoder().decode(pathParts[2]);
    if (pubKey.length != 32) {
      throw new URISyntaxException(url, "Decoded pubKey length is not 32 bytes");
    }
    saveKey(uuid, pubKey);
    // Сохранить алиас
    if (saveAlias) {
      String query = uri.getQuery();
      if (query != null && query.startsWith("alias=")) {
        String alias = URLDecoder.decode(query.substring(6), StandardCharsets.UTF_8);
        addAlias(uuid, alias);
      }
    }
    return uuid;
  }

  public byte[] importPubKey(String url) throws URISyntaxException, SQLException {
    return importPubKey(url, true);
  }

  /**
   * Получить все алиасы ключа
   */
  public Set<String> getKeyAliases(byte[] uuid) throws SQLException {
    String sql = "SELECT name FROM keyAliases WHERE uuid = ?";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBytes(1, uuid);
      try (ResultSet rs = stmt.executeQuery()) {
        Set<String> result = new HashSet<>();
        while (rs.next()) {
          result.add(rs.getString("name"));
        }
        return result;
      }
    }
  }

  public Set<byte[]> listAllKeys() throws SQLException {
    String sql = "SELECT uuid FROM keys";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      Set<byte[]> result = new HashSet<>();
      while (rs.next()) {
        result.add(rs.getBytes("uuid"));
      }
      return result;
    }
  }

  /**
   * Удалить ключ
   */
  public void deleteKey(byte[] uuid) throws SQLException {
    String sql1 = "DELETE FROM keyAliases WHERE uuid = ?";
    String sql2 = "DELETE FROM keys WHERE uuid = ?";
    try (Connection conn = getConn();
        PreparedStatement stmt1 = conn.prepareStatement(sql1);
        PreparedStatement stmt2 = conn.prepareStatement(sql2)) {
      stmt1.setBytes(1, uuid);
      stmt1.executeUpdate();
      stmt2.setBytes(1, uuid);
      stmt2.executeUpdate();
      conn.commit();
    }
  }

  /**
   * Сгенерировать приватный ключ
   */
  public byte[] generateKey() throws SQLException {
    // Генерация пары ключей
    Ed25519PrivateKeyParameters privKey = new Ed25519PrivateKeyParameters(new SecureRandom());
    byte[] privKeyBytes = privKey.getEncoded();
    byte[] pubKeyBytes = privKey.generatePublicKey().getEncoded();
    // Генерация UUID
    UUID uuidObj = UUID.randomUUID();
    byte[] uuidBytes = Util.uuidToBytes(uuidObj);
    // Сохранение
    saveKeyInternal(uuidBytes, privKeyBytes, pubKeyBytes);
    return uuidBytes;
  }

  /**
   * Сохранить публичный ключ
   */
  public void saveKey(byte[] uuid, byte[] pubkey) throws SQLException {
    saveKeyInternal(uuid, null, pubkey);
  }

  private void saveKeyInternal(byte[] uuid, byte[] privKey, byte[] pubKey) throws SQLException {
    String sql = "INSERT INTO keys (uuid, privkey, pubkey) VALUES (?, ?, ?) " +
        "ON CONFLICT(uuid) DO UPDATE SET privkey = excluded.privkey, pubkey = excluded.pubkey";
    if (pubKey == null) {
      pubKey = new Ed25519PrivateKeyParameters(privKey, 0)
          .generatePublicKey().getEncoded();
    }
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBytes(1, uuid);
      stmt.setBytes(2, privKey);
      stmt.setBytes(3, pubKey);
      stmt.executeUpdate();
      conn.commit();
    }
  }

  /**
   * Добавить алиас ключа
   */
  public void addAlias(byte[] uuid, String name) throws SQLException {
    String sql = "INSERT INTO keyAliases (uuid, name) VALUES (?, ?)";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setBytes(1, uuid);
      stmt.setString(2, name);
      stmt.executeUpdate();
      conn.commit();
    }
  }

  /**
   * Удалить алиас ключа
   */
  public void removeAlias(String name) throws SQLException {
    String sql = "DELETE FROM keyAliases WHERE name = ?";
    try (Connection conn = getConn();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, name);
      stmt.executeUpdate();
      conn.commit();
    }
  }

  /**
   * Найти или конвертировать UUID ключа
   */
  public byte[] resolveUuid(String value) throws SQLException {
    try {
      return Util.uuidToBytes(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return findKey(value);
    }
  }
}
