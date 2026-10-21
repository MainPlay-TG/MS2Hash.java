package ru.mainplay_tg.ms2sign;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import ru.mainplay_tg.Util;

@Command(name = "ms2sign", subcommands = {
    CreateKeyCommand.class,
    DelKeyCommand.class,
    ExportKeyCommand.class,
    ImportKeyCommand.class,
    ListKeysCommand.class,
    RemoveAliasCommand.class,
    SignCommand.class,
    VerifyCommand.class,
}, description = "Утилита для генерации и проверки подписей MS2Sign", synopsisSubcommandLabel = "<command>", usageHelpWidth = 80)
public class App implements Callable<Integer> {
  public static final String fileSuffix = ".ms2sign";

  public static void main(String[] args) {
    Util.fixEncoding();
    int exitCode;
    try {
      CommandLine cli = new CommandLine(new App());
      exitCode = cli.execute(args);
    } catch (Exception exc) {
      exitCode = 1;
      Util.exit_exc("Unhandled exception", exc);
    }
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    CommandLine.usage(this, System.err);
    return 0;
  }
}
