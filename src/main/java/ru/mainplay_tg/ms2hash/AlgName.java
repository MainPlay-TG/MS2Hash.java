package ru.mainplay_tg.ms2hash;

public enum AlgName {
  MD5("M"),
  SHA1("1"),
  SHA256("2"),
  SHA3_256("3"),
  SHA3_512("4"),
  SHA512("5");

  private String v;

  AlgName(String v) {
    this.v = v;
  }

  public String getValue() {
    return v;
  }
}