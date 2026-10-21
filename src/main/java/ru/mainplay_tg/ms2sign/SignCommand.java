package ru.mainplay_tg.ms2sign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.List;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import ru.mainplay_tg.Util;
import ru.mainplay_tg.Util.HashResult;

@Command(name = "sign", description = "Сгенерировать подпись для файла", mixinStandardHelpOptions = false)
public class SignCommand implements Callable<Integer> {
  @Option(names = { "-h", "--help" }, description = "только показать это сообщение")
  boolean help;
  @Option(names = { "--config" }, description = "путь к файлу конфига")
  String configPath;
  @Option(names = { "-b", "--bar" }, description = "показывать прогрессбар")
  boolean bar;
  @Option(names = { "-f", "--force" }, description = "перезаписать существующую подпись")
  boolean force;
  @Option(names = { "-k", "--key" }, description = "выбрать ключ для подписи")
  String selectedKey;
  @Parameters(description = "пути к файлам", arity = "1..*")
  List<String> files;
  private byte[] keyUuid;
  private Ed25519Signer signer;
  private MessageDigest digest;

  @Override
  public Integer call() throws Exception {
    if (help) {
      CommandLine.usage(this, System.err);
      return 0;
    }
    SignConfig cfg = new SignConfig(configPath);
    SignKeysDatabase db = cfg.getDatabase();
    if (selectedKey == null) {
      selectedKey = cfg.defaultKey;
      if (selectedKey == null) {
        System.err.println("Ошибка: не выбран ключ для подписи");
        return 2;
      }
    }
    this.keyUuid = db.resolveUuid(selectedKey);
    Ed25519PrivateKeyParameters key = db.getPrivKey(keyUuid);
    if (key == null) {
      System.err.println("Ошибка: выбран публичный ключ, генерировать подпись можно только приватным ключом");
      return 2;
    }
    System.out.println("Выбран ключ " + Util.uuidFromBytes(keyUuid).toString());
    this.digest = MessageDigest.getInstance("SHA3-256");
    this.signer = new Ed25519Signer();
    signer.init(true, key);
    int okFiles = 0;
    int totalFiles = files.size();
    for (String file : files) {
      try {
        if (signFile(file)) {
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

  private boolean signFile(String pathString) throws IOException {
    // Получить пути к файлам
    pathString = Util.removeFileSuffix(pathString, App.fileSuffix);
    Path filePath = Paths.get(pathString);
    Path signPath = Paths.get(pathString + App.fileSuffix);
    File fileFile = filePath.toFile();
    File signFile = signPath.toFile();
    // Проверить наличие файлов
    if (!fileFile.exists()) {
      System.err.println(pathString + ": файл не найден");
      return false;
    }
    if ((!force) && signFile.exists()) {
      System.err.println(pathString + ": подпись уже существует");
      return false;
    }
    // Хешировать файл
    HashResult result = Util.hashFile(filePath, digest, bar);
    // Подписать файл
    SignInfoV0 sign = new SignInfoV0(
        Instant.now().getEpochSecond(),
        result.fileSize,
        result.digest,
        keyUuid,
        null);
    signer.reset();
    byte[] payload = sign.getSignPayload();
    signer.update(payload, 0, payload.length);
    sign.sign = signer.generateSignature();
    // Сохранить файл
    Files.write(signPath, sign.pack());
    return true;
  }
}
