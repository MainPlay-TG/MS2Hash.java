package ru.mainplay_tg.ms2hash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Util {
  /**
   * Прочитать весь stdin
   */
  public static String read_stdin() throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    StringBuilder input = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      input.append(line);
    }
    return input.toString();
  }

  public static Gson get_gson() {
    return new GsonBuilder().setLenient().create();
  }

  /**
   * Парсить JSON в указанный класс
   */
  public static <T> T from_json(String json, Class<T> classOfT) throws JsonSyntaxException {
    return get_gson().fromJson(json, classOfT);
  }

  /**
   * Закодировать объект в JSON
   */
  public static String to_json(Object data) {
    return get_gson().toJson(data);
  }

  /**
   * Написать ошибку и закрыть процесс
   * 
   * @param msg Текст ошибки
   * @param exc Объект ошибки
   */
  public static void exit_exc(String msg, Exception exc) {
    System.err.println(msg);
    if (exc != null) {
      exc.printStackTrace();
    }
    System.exit(1);
  }

  /**
   * Написать ошибку и закрыть процесс
   * 
   * @param msg Текст ошибки
   */
  public static void exit_exc(String msg) {
    exit_exc(msg, null);
  }

  /**
   * Починить кодировку на Mustdie
   */
  public static void fixEncoding() {
    String os_name = System.getProperty("os.name").toLowerCase();
    if (!os_name.contains("win")) {
      return;
    }
    String[] preferredEncodings = { "CP866", "CP1251", "UTF-8" };
    Charset encoding = Charset.defaultCharset();
    for (String enc : preferredEncodings) {
      if (Charset.isSupported(enc)) {
        encoding = Charset.forName(enc);
        break;
      }
    }
    System.setProperty("file.encoding", StandardCharsets.UTF_8.name());
    System.setOut(new java.io.PrintStream(System.out, true, encoding));
    System.setErr(new java.io.PrintStream(System.err, true, encoding));
  }

  public static String file_suffix = ".MS2_hash";

  /**
   * Убрать суффикс .MS2_hash
   */
  public static String removeFileSuffix(String path) {
    int len = file_suffix.length();
    String lower_suffix = file_suffix.toLowerCase(Locale.ROOT);
    while (path.toLowerCase(Locale.ROOT).endsWith(lower_suffix)) {
      path = path.substring(0, path.length() - len);
    }
    return path;
  }

  public static String shortenString(String str, int maxLen) {
    if (str == null || str.length() <= maxLen) {
      return str;
    }
    int prefixLength = (maxLen - 3) / 2 + (maxLen - 3) % 2; // больше на начало, если нечётно
    int suffixLength = (maxLen - 3) / 2;
    String prefix = str.substring(0, prefixLength);
    String suffix = str.substring(str.length() - suffixLength);
    return prefix + "..." + suffix;
  }

  public static ProgressBarBuilder makeByteTransferBarBuilder(String fileName) {
    ProgressBarBuilder pbb = ProgressBar.builder();
    pbb.setStyle(ProgressBarStyle.ASCII);
    pbb.setTaskName(shortenString(fileName, 20));
    pbb.setUnit(" MiB", 1024 * 1024);
    pbb.setUpdateIntervalMillis(500);
    pbb.showSpeed();
    return pbb;
  }

  /**
   * Хешировать файл
   * 
   * @param path     путь к файлу
   * @param hash     хеш (будет сброшен)
   * @param show_bar отображать прогрессбар
   * @throws IOException
   */
  public static HashResult hashFile(Path path, MessageDigest hash, Boolean show_bar) throws IOException {
    hash.reset();
    File file = path.toFile();
    try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {
      long fileSize = channel.size();
      if (fileSize == 0) {
        return new HashResult(fileSize, hash.digest());
      }
      int bufferSize = 1024 * 1024;
      byte[] buffer = new byte[bufferSize];
      if (show_bar && fileSize > bufferSize) {
        try (InputStream input = ProgressBar.wrap(fis, makeByteTransferBarBuilder(file.getName()))) {
          while ((input.read(buffer)) != -1) {
            hash.update(buffer);
          }
        }
      } else {
        while ((fis.read(buffer)) != -1) {
          hash.update(buffer);
        }
      }
      return new HashResult(fileSize, hash.digest());
    }
  }

  public static HashResult hashFile(String path, MessageDigest hash, Boolean show_bar) throws IOException {
    return hashFile(Paths.get(path), hash, show_bar);
  }

  public static class HashResult {
    public long fileSize;
    public byte[] digest;

    public HashResult(long fileSize, byte[] digest) {
      this.fileSize = fileSize;
      this.digest = digest;
    }

    public String digestHex() {
      StringBuilder builder = new StringBuilder();
      for (byte i : digest) {
        builder.append(String.format("%02x", i & 0xff));
      }
      return builder.toString();
    }
  }

  public static String readTextFile(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  public static void writeTextFile(Path path, String text) throws IOException {
    Files.write(path, text.getBytes(StandardCharsets.UTF_8));
  }
}
