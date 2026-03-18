from pathlib import Path


def find_jar(dir: Path):
  for file in dir.iterdir():
    if file.suffix == ".jar" and file.is_file():
      return file
  raise FileNotFoundError()
