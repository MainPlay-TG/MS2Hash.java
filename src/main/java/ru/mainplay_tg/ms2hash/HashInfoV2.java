package ru.mainplay_tg.ms2hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HashInfoV2 {
  public Digest digest;
  public HashAlg alg;
  public Long size;
  public static byte[] MAGIC_HEAD = { 77, 83, 50, 72 }; // MS2H
  public static String FILE_SUFFIX = ".ms2hash";

  public HashInfoV2(Long size, HashAlg alg_code, Digest digest) {
    this.alg = alg_code;
    this.digest = digest;
    this.size = size;
  }

  public byte[] to_v0() throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    result.write(MAGIC_HEAD);
    result.write(0);
    int sizesize = intSizeUnsigned(this.size);
    if (sizesize > 0b1111) {
      throw new IllegalArgumentException("Too big size");
    }
    result.write((this.alg.getCode() << 4) | sizesize);
    result.write(getBytes(this.size, sizesize));
    result.write(this.digest.binary());
    return result.toByteArray();
  }

  public String to_vB() {
    StringBuilder result = new StringBuilder();
    result.append("MS2HB ");
    result.append(alg.getName());
    result.append(" ");
    result.append(size.toString());
    result.append(" ");
    result.append(digest.base64());
    return result.toString();
  }

  public String to_vH() {
    StringBuilder result = new StringBuilder();
    result.append("MS2HH ");
    result.append(alg.getName());
    result.append(" ");
    result.append(size.toString());
    result.append(" ");
    result.append(digest.hex());
    return result.toString();
  }

  // Не проверено
  private static int intSizeUnsigned(long value) {
    if (value == 0) {
      return 1;
    }
    return (Long.SIZE - 1 - Long.numberOfLeadingZeros(value)) / 8 + 1;
  }

  private static byte[] getBytes(long value, int length) {
    byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[length - 1 - i] = (byte) ((value >> (8 * i)) & 0xFF);
    }
    return bytes;
  }
}
