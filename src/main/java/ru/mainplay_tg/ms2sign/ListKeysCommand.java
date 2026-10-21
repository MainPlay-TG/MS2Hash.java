package ru.mainplay_tg.ms2sign;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.Set;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.mainplay_tg.Util;

@Command(name = "list-keys", description = "Посмотреть все сохранённые ключи", mixinStandardHelpOptions = false)
public class ListKeysCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    SignKeysDatabase db = cfg.getDatabase();
    byte[] defaultKey = (cfg.defaultKey != null && !cfg.defaultKey.isEmpty()) ? db.resolveUuid(cfg.defaultKey) : null;
    for (byte[] uuid : db.listAllKeys()) {
      System.out.println("Ключ " + Util.uuidFromBytes(uuid).toString());
      Set<String> aliases = db.getKeyAliases(uuid);
      System.out.println("  Тип: " + (db.getPrivKey(uuid) == null ? "приватный" : "публичный"));
      if (!aliases.isEmpty()) {
        System.out.println("  Алиасы: " + aliases);
      }
      if (defaultKey != null && Arrays.equals(defaultKey, uuid)) {
        System.out.println("  Ключ по умолчанию");
      }
    }
    return 0;
  }
}
