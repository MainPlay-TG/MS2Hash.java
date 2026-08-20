package ru.mainplay_tg.ms2hash;

public enum HashAlg {
  MS5(0),
  SHA1(1),
  SHA256(2),
  SHA3_256(3),
  SHA3_512(4),
  SHA512(5);

  private int v;

  HashAlg(int v) {
    this.v = v;
  }

  public int getCode() {
    return v;
  }

  public int getSize() {
    return AlgSize.valueOf(this.toString()).getValue();
  }

  public String getName() {
    return AlgName.valueOf(this.toString()).getValue();
  }
}
