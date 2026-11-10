package ru.mainplay_tg.ms2sign;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import ru.mainplay_tg.Util;
import ru.mainplay_tg.Util.HashResult;

@Command(name = "verify", description = "Проверить подпись файла", mixinStandardHelpOptions = false)
public class VerifyCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
  @Option(names = { "-b", "--bar" }, description = "показывать прогрессбар")
  boolean bar;
  @Parameters(description = "пути к файлам", arity = "1..*")
  List<String> files;
  private MessageDigest digest;
  private SignKeysDatabase db;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    this.db = cfg.getDatabase();
    this.digest = MessageDigest.getInstance("SHA3-256");
    if (cfg.defaultBar) {
      this.bar = true;
    }
    int okFiles = 0;
    int totalFiles = files.size();
    for (String file : files) {
      try {
        if (verifyFile(file)) {
          okFiles += 1;
        }
      } catch (Exception e) {
        System.err.println(file + ": ошибка");
        e.printStackTrace();
      }
    }
    if (totalFiles == okFiles) {
      return 0;
    } else if (okFiles == 0) {
      return 2;
    }
    return 1;
  }

  private boolean verifyFile(String pathString) throws Exception {
    // Получить пути к файлам
    pathString = Util.removeFileSuffix(pathString, App.fileSuffix);
    Path filePath = Paths.get(pathString);
    Path signPath = Paths.get(pathString + App.fileSuffix);
    File fileFile = filePath.toFile();
    File signFile = signPath.toFile();
    // Проверить наличие подписи
    if (!signFile.exists()) {
      System.err.println(pathString + ": подпись отсутствует");
      return false;
    }
    // Прочитать подпись
    SignInfoV0 sign;
    try (InputStream fis = Files.newInputStream(signPath)) {
      sign = SignInfoV0.unpack(fis.readNBytes(SignInfoV0.TOTAL_SIZE));
    }
    // Вывести информацию о подписи
    System.out.println(pathString + ":");
    Date date = new Date(sign.date * 1000L);
    DateFormat formatter = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.MEDIUM,
        Locale.getDefault());
    System.out.println("  Дата подписи: " + formatter.format(date));
    // Размер файла
    if (sign.size < Util.KB) {
      System.out.println("  Размер файла: " + sign.size + " B");
    } else {
      System.out.println("  Размер файла: " + Util.humanizeSize(sign.size) + " (" + sign.size + " B)");
    }
    boolean sizeOk = false;
    if (fileFile.exists()) {
      sizeOk = Files.size(filePath) == sign.size;
      if (!sizeOk) {
        System.err.println("! Размер файла не совпадает");
      }
    } else {
      System.err.println("! Файл не найден");
      // Просто вывести информацию о подписи при отсутствии файла
    }
    // Хеш
    System.out.println("  " + digest.getAlgorithm() + ": " + Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(sign.digest));
    boolean hashOk = false;
    if (sizeOk) {
      HashResult hash = Util.hashFile(filePath, digest, bar);
      hashOk = Arrays.equals(sign.digest, hash.digest);
      if (!hashOk) {
        System.err.println("! Хеш файла не совпадает");
      }
    } else {
      System.err.println("! Проверка хеша пропущена");
    }
    // Подпись
    UUID keyUuid = Util.uuidFromBytes(sign.keyUuid);
    System.out.println("  Ключ: " + keyUuid.toString());
    Ed25519PublicKeyParameters key = db.getPubKey(sign.keyUuid);
    boolean signOk = false;
    if (key == null) {
      System.err.println("! Ключ отсутствует в БД. Запросите публичный ключ у отправителя файла");
    } else {
      Set<String> aliases = db.getKeyAliases(sign.keyUuid);
      if (aliases.size() == 1) {
        System.out.println("  Алиас ключа: " + String.join(", ", aliases));
      } else if (aliases.size() > 1) {
        System.out.println("  Алиасы ключа: " + String.join(", ", aliases));
      }
      if (SignKeysDatabase.builtinKeys.containsKey(keyUuid)) {
        System.out.println("  Подписано разработчиком данной программы");
      }
      Ed25519Signer signer = new Ed25519Signer();
      signer.init(false, key);
      byte[] payload = sign.getSignPayload();
      signer.update(payload, 0, payload.length);
      signOk = signer.verifySignature(sign.sign);
      if (!signOk) {
        System.err.println("! Подпись не совпадает");
      }
    }
    if (sizeOk && hashOk && signOk) {
      System.out.println("= Файл успешно проверен");
      return true;
    }
    System.err.println("! Возникли ошибки при проверке файла");
    return false;
  }

}
