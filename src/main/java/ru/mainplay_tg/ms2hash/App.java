// App.java
package ru.mainplay_tg.ms2hash;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.Callable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import ru.mainplay_tg.ms2hash.Util.HashResult;

@Command(name = "ms2hash", subcommands = { GenCommand.class,
    CheckCommand.class }, description = "Утилита для генерации и проверки хешей MS2_hash", synopsisSubcommandLabel = "<command>", usageHelpWidth = 80)
public class App implements Callable<Integer> {
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
    return 1;
  }
}

// Подкоманда gen
@Command(name = "gen", description = "Создание контрольной суммы для файла", mixinStandardHelpOptions = false)
class GenCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--algs" }, description = "только показать список поддерживаемых алгоритмов")
  boolean show_algs;
  @Option(names = { "-b", "--bar" }, description = "показывать прогрессбар")
  boolean bar;
  @Option(names = { "-f", "--force" }, description = "перезаписывать существующие хеши")
  boolean force;
  @Option(names = { "-t", "--type" }, description = "тип контрольной суммы", defaultValue = "sha512")
  String type;
  @Parameters(description = "пути к файлам", arity = "1..*")
  List<String> files;

  @Override
  public Integer call() {
    if (help) {
      CommandLine.usage(this, System.err);
      return 1;
    }
    if (show_algs) {
      showAllAlgs();
      return 0;
    }
    MessageDigest hash;
    try {
      hash = MessageDigest.getInstance(type);
    } catch (NoSuchAlgorithmException e) {
      System.err.println("Ошибка: алгоритм не поддерживается: " + type);
      return 2;
    }
    int okFiles = 0;
    for (String i : files) {
      try {
        int status = genFile(i, hash);
        if (status == StatusAlreadyExist) {
          System.err.println("Ошибка: хеш уже существует: " + i);
        } else if (status == StatusMissingFile) {
          System.err.println("Ошибка: файл не найден: " + i);
        } else if (status == StatusOk) {
          okFiles += 1;
        } else {
          System.err.println("Ошибка: неизвестный статус: " + i);
        }
      } catch (IOException exc) {
        System.err.println("Ошибка: не удалось прочитать файл: " + i);
        exc.printStackTrace();
      }
    }
    int totalFiles = files.size();
    if (totalFiles == okFiles) {
      return 0;
    } else if (okFiles == 0) {
      return 2;
    }
    return 1;
  }

  private void showAllAlgs() {
    Set<String> algs = new HashSet<>();
    for (Provider prov : Security.getProviders()) {
      for (Provider.Service svc : prov.getServices()) {
        if ("MessageDigest".equals(svc.getType())) {
          algs.add(svc.getAlgorithm());
        }
      }
    }
    System.err.println("Поддерживаемые алгоритмы хеширования и алиасы к ним:");
    System.out.println(algs.stream().sorted().collect(Collectors.joining(", ")));
  }

  private int StatusAlreadyExist = 0;
  private int StatusMissingFile = 1;
  private int StatusOk = 2;

  private int genFile(String path, MessageDigest hash) throws IOException {
    path = Util.removeFileSuffix(path);
    Path filePath = Paths.get(path);
    Path hashPath = Paths.get(path + Util.file_suffix);
    File fileFile = filePath.toFile();
    File hashFile = hashPath.toFile();
    if (!fileFile.exists()) {
      return StatusMissingFile;
    }
    if (!force) {
      if (hashFile.exists()) {
        return StatusAlreadyExist;
      }
    }
    HashResult result = Util.hashFile(filePath, hash, bar);
    HashInfoV1 info = new HashInfoV1(result.fileSize, result.digestHex(), type);
    Util.writeTextFile(hashPath, info.toJson());
    return StatusOk;
  }
}

// Подкоманда check
@Command(name = "check", description = "Проверка размера и контрольной суммы файла", mixinStandardHelpOptions = false)
class CheckCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "-b", "--bar" }, description = "показывать прогрессбар")
  boolean bar;
  @Parameters(description = "пути к файлам", arity = "1..*")
  List<String> files;

  @Override
  public Integer call() {
    if (help) {
      CommandLine.usage(this, System.err);
      return 1;
    }
    int okFiles = 0;
    for (String i : files) {
      try {
        int status = checkFile(i);
        if (status == StatusDiffHash) {
          System.err.println("Ошибка: хеш файла не совпадает: " + i);
        } else if (status == StatusDiffSize) {
          System.err.println("Ошибка: размер файла не совпадает: " + i);
        } else if (status == StatusInvalidHash) {
          System.err.println("Ошибка: хеш повреждён или имеет неизвестный формат: " + i);
        } else if (status == StatusMissingFile) {
          System.err.println("Ошибка: файл не найден: " + i);
        } else if (status == StatusMissingHash) {
          System.err.println("Ошибка: файл хеша не найден: " + i);
        } else if (status == StatusOk) {
          okFiles += 1;
          System.err.println("Успех: файл не изменён: " + i);
        } else {
          System.err.println("Ошибка: неизвестный статус: " + i);
        }
      } catch (NoSuchAlgorithmException exc) {
        System.err.println("Ошибка: хеш не поддерживается: " + i);
      } catch (IOException exc) {
        System.err.println("Ошибка: не удалось прочитать файл: " + i);
        exc.printStackTrace();
      }
    }
    int totalFiles = files.size();
    if (totalFiles == okFiles) {
      return 0;
    } else if (okFiles == 0) {
      return 2;
    }
    return 1;
  }

  private int StatusDiffHash = 0;
  private int StatusDiffSize = 1;
  private int StatusInvalidHash = 2;
  private int StatusMissingFile = 3;
  private int StatusMissingHash = 4;
  private int StatusOk = 5;

  private int checkFile(String path) throws IOException, NoSuchAlgorithmException {
    path = Util.removeFileSuffix(path);
    Path filePath = Paths.get(path);
    Path hashPath = Paths.get(path + Util.file_suffix);
    File fileFile = filePath.toFile();
    File hashFile = hashPath.toFile();
    if (!fileFile.exists()) {
      return StatusMissingFile;
    }
    if (!hashFile.exists()) {
      return StatusMissingHash;
    }
    HashInfoV1 info;
    try {
      info = HashInfoV1.fromJson(Util.readTextFile(hashPath));
    } catch (JsonSyntaxException exc) {
      return StatusInvalidHash;
    }
    if (info == null || info.isInvalid(false)) {
      return StatusInvalidHash;
    }
    if (info.file != null && info.file.size != null) {
      if (Files.size(filePath) != info.file.size) {
        return StatusDiffSize;
      }
    }
    HashResult hash = Util.hashFile(filePath, MessageDigest.getInstance(info.hash.type), bar);
    if (hash.digestHex().equalsIgnoreCase(info.hash.hex)) {
      return StatusOk;
    }
    return StatusDiffHash;
  }
}
