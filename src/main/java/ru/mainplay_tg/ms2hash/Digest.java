package ru.mainplay_tg.ms2hash;

import java.util.Base64;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Digest {
  private byte[] v;

  public Digest(byte[] v) {
    this.v = v;
  }

  public static Digest fromBase64(String v) {
    return new Digest(Base64.getDecoder().decode(v));
  }

  public static Digest fromHex(String v) throws DecoderException {
    return new Digest(Hex.decodeHex(v));
  }

  public byte[] binary() {
    return v;
  }

  public String base64() {
    return Base64.getEncoder().withoutPadding().encodeToString(v);
  }

  public String hex() {
    return Hex.encodeHexString(v);
  }
}