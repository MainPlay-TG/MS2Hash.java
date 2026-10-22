package ru.mainplay_tg.ms2sign;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "export-key", description = "Экспортировать ключ", mixinStandardHelpOptions = false)
public class ExportKeyCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
  @Option(names = { "-a", "--alias" }, description = "добавить алиас")
  String alias;
  @Parameters(description = "UUID или алиас ключа")
  String uuid;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    SignKeysDatabase db = cfg.getDatabase();
    String url = db.exportPubKey(db.resolveUuid(uuid), alias);
    System.out.println(url);
    return 0;
  }
}
