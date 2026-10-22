package ru.mainplay_tg.ms2sign;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "add-alias", description = "Добавить алиас", mixinStandardHelpOptions = false)
public class AddAliasCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
  @Parameters(description = "UUID или алиас ключа")
  String uuid;
  @Parameters(description = "название алиаса")
  String name;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    SignKeysDatabase db = cfg.getDatabase();
    db.addAlias(db.resolveUuid(uuid), name);
    return 0;
  }
}
