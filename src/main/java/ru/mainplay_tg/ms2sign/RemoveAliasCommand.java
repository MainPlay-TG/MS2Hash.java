package ru.mainplay_tg.ms2sign;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "remove-alias", description = "Убрать алиас", mixinStandardHelpOptions = false)
public class RemoveAliasCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
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
    db.removeAlias(name);
    return 0;
  }
}
