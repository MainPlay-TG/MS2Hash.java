package ru.mainplay_tg.ms2hash;

public class HashInfoV1 {
  public static class File {
    public Long size;

    public File(Long size) {
      this.size = size;
    }
  }

  public static class Hash {
    public String hex;
    public String type;

    public Hash(String hex, String type) {
      this.hex = hex;
      this.type = type;
    }
  }

  public File file;
  public String format;
  public Hash hash;

  public HashInfoV1(Long size, String hashHex, String hashType) {
    this.file = new File(size);
    this.format = "MainShortcuts2_hash_v1";
    this.hash = new Hash(hashHex, hashType);
  }

  public static HashInfoV1 fromJson(String json) {
    return Util.from_json(json, HashInfoV1.class);
  }

  public Boolean isInvalid(Boolean needSize) {
    if (needSize) {
      if (this.file == null) {
        return true;
      }
      if (this.file.size == null) {
        return true;
      }
    }
    if (this.hash == null) {
      return true;
    }
    if (this.hash.hex == null) {
      return true;
    }
    if (this.hash.type == null) {
      return true;
    }
    return false;
  }

  public Boolean isInvalid() {
    return isInvalid(true);
  }

  public String toJson() {
    return Util.to_json(this);
  }
}
