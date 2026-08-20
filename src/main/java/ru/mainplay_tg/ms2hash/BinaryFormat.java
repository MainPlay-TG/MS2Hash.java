package ru.mainplay_tg.ms2hash;

public enum BinaryFormat {
  BIN0(0),
  ASCII85(65),
  BASE64(66),
  HEX(72);

  private int v;

  BinaryFormat(int v) {
    this.v = v;
  }

  public int getValue() {
    return v;
  }
}