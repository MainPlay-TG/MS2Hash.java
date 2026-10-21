package ru.mainplay_tg.ms2sign;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.mainplay_tg.Util;

@Command(name = "create-key", description = "Создать новый приватный ключ и сохранить его в БД", mixinStandardHelpOptions = false)
class CreateKeyCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
  @Option(names = { "-a", "--alias" }, description = "назначить ключу алиас")
  String setAlias;
  @Option(names = { "-d", "--default" }, description = "установить ключом по умолчанию")
  Boolean setDefault;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    SignKeysDatabase db = cfg.getDatabase();
    byte[] keyUuid = db.generateKey();
    String keyUuidString = Util.uuidFromBytes(keyUuid).toString();
    System.out.println("Создан новый ключ с UUID " + keyUuidString);
    if (setAlias != null) {
      db.addAlias(keyUuid, setAlias);
      System.out.println("Ключу назначен алиас " + setAlias);
    }
    if (setDefault) {
      cfg.defaultKey = keyUuidString;
      cfg.save();
      System.out.println("Ключ будет использоваться по умолчанию при подписи");
    }
    System.out.println("URL для передачи ключа:");
    System.out.println(db.exportPubKey(keyUuid, setAlias));
    return 0;
  }
}
