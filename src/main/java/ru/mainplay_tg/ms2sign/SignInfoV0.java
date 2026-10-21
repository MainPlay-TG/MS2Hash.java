package ru.mainplay_tg.ms2sign;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Информация из файла MS2Sign v0
 */
public class SignInfoV0 {
  /**
   * Дата генерации подписи (Unix timestamp)
   */
  public long date;
  /**
   * Размер файла
   */
  public long size;
  /**
   * SHA3-256 хеш
   */
  public byte[] digest;
  /**
   * UUID публичного ключа
   */
  public byte[] keyUuid;
  /**
   * Подпись даты+размера+хеша
   */
  public byte[] sign;
  public static final byte[] MAGIC_HEAD = "MS2S".getBytes(StandardCharsets.UTF_8);
  public static final int TOTAL_SIZE = 133;

  public SignInfoV0(long date, long size, byte[] digest, byte[] keyUuid, byte[] sign) {
    this.date = date;
    this.size = size;
    this.digest = digest.clone();
    this.keyUuid = keyUuid.clone();
    if (sign != null) {
      this.sign = sign.clone();
    }
  }

  public void getSignPayload(ByteBuffer buf) {
    buf.putLong(date);
    buf.putLong(size);
    buf.put(digest);
  }

  public byte[] getSignPayload() {
    ByteBuffer buf = ByteBuffer.allocate(48);
    buf.order(ByteOrder.BIG_ENDIAN);
    getSignPayload(buf);
    return buf.array();
  }

  public byte[] pack() {
    ByteBuffer buf = ByteBuffer.allocate(TOTAL_SIZE);
    buf.order(ByteOrder.BIG_ENDIAN);
    // Заголовок
    buf.put(MAGIC_HEAD);
    buf.put((byte) 0);
    // Тело
    getSignPayload(buf);
    // Подпись
    buf.put(keyUuid);
    buf.put(sign);
    return buf.array();
  }

  public static SignInfoV0 unpack(ByteBuffer buf, boolean readHeader) {
    buf.order(ByteOrder.BIG_ENDIAN);
    // Заголовок
    if (readHeader) {
      buf.position(buf.position() + 5);
    }
    // Тело
    long date = buf.getLong();
    long size = buf.getLong();
    byte[] digest = new byte[32];
    buf.get(digest);
    // Подпись
    byte[] keyUuid = new byte[16];
    buf.get(keyUuid);
    byte[] sign = new byte[64];
    buf.get(sign);
    return new SignInfoV0(date, size, digest, keyUuid, sign);
  }

  public static SignInfoV0 unpack(ByteBuffer buf) {
    return unpack(buf, true);
  }

  public static SignInfoV0 unpack(byte[] buf, boolean readHeader) {
    return unpack(ByteBuffer.wrap(buf), readHeader);
  }

  public static SignInfoV0 unpack(byte[] buf) {
    return unpack(buf, true);
  }
}
