package ru.mainplay_tg.ms2hash;

public enum AlgSize {
  MD5(16),
  SHA1(20),
  SHA256(32),
  SHA3_256(32),
  SHA3_512(64),
  SHA512(64);

  private int v;

  AlgSize(int v) {
    this.v = v;
  }

  public int getValue() {
    return v;
  }
}