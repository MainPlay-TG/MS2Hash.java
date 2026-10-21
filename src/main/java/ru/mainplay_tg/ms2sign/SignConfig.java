package ru.mainplay_tg.ms2sign;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import ru.mainplay_tg.Util;

public class SignConfig {
  private final Path configPath;
  public String databaseUrl;
  public String defaultKey;
  private SignKeysDatabase db;

  public SignConfig() throws IOException {
    this(getDefaultConfigPath());
  }

  public SignConfig(String configPath) throws IOException {
    this(configPath == null ? getDefaultConfigPath() : Paths.get(configPath));
  }

  public SignConfig(Path configPath) throws IOException {
    this.configPath = configPath;
    this.databaseUrl = "jdbc:sqlite:" + configPath.getParent().resolve("data.db").toAbsolutePath().toString();
    this.defaultKey = null;
    load();
  }

  public void load() throws IOException {
    if (Files.notExists(configPath)) {
      save();
      return;
    }
    try (Reader reader = Files.newBufferedReader(configPath)) {
      JustData loaded = Util.get_gson().fromJson(reader, JustData.class);
      if (loaded != null) {
        if (loaded.databaseUrl != null) {
          this.databaseUrl = loaded.databaseUrl;
        }
        if (loaded.defaultKey != null) {
          this.defaultKey = loaded.defaultKey;
        }
      }
    }
  }

  public void save() throws IOException {
    Files.createDirectories(configPath.getParent());
    JustData data = new JustData();
    data.databaseUrl = databaseUrl;
    data.defaultKey = defaultKey;
    Util.writeTextFile(configPath, Util.get_gson().toJson(data));
  }

  /**
   * Подключиться к базе данных
   */
  public SignKeysDatabase getDatabase() throws SQLException {
    if (this.db == null) {
      this.db = new SignKeysDatabase(databaseUrl);
    }
    return this.db;
  }

  private static Path getDefaultConfigPath() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      String localAppData = System.getenv("LOCALAPPDATA");
      if (localAppData != null) {
        return Paths.get(localAppData, "MainPlay_TG", "MS2Sign", "config.json");
      }
      return Paths.get(System.getProperty("user.home"), "AppData", "Local", "MainPlay_TG", "MS2Sign", "config.json");
    }
    return Paths.get(System.getProperty("user.home"), ".local", "share", "MainPlay_TG", "MS2Sign", "config.json");
  }

  public static void main(String[] args) throws IOException {
    SignConfig config = new SignConfig();
    System.out.println(config.configPath.toString());
    try {
      System.out.println("Путь к БД: " + config.databaseUrl);
      System.out.println("Ключ: " + config.defaultKey);
      config.getDatabase();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unused")
  private class JustData {
    public String databaseUrl;
    public String defaultKey;

    public JustData() {
    }
  }
}