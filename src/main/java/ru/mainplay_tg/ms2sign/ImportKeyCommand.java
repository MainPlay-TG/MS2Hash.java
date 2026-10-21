package ru.mainplay_tg.ms2sign;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import-key", description = "Импорт ключа из URL", mixinStandardHelpOptions = false)
public class ImportKeyCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
  @Option(names = { "-n", "--no-alias", }, description = "Запретить алиасы в URL")
  Boolean noAlias;
  @Parameters(description = "URL публичного ключа")
  String url;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    SignKeysDatabase db = cfg.getDatabase();
    db.importPubKey(url, !noAlias);
    System.out.println("Ключ импортирован");
    return 0;
  }
}
