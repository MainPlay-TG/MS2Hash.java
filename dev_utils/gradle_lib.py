import subprocess
from functools import cached_property
from MainShortcuts2.ex.pathlib_ex import Path
subprocess.run(["java", "-version"])


class Gradle:
  def __init__(self, root: Path):
    self.root = root

  def _read_gradle(self, file: Path, key: str) -> str:
    for line in file.read_lines_iter():
      if "=" in line:
        split = [i.strip() for i in line.split("=", 1)]
        if split[0] == key:
          return eval(split[1])
    raise KeyError(key)

  @cached_property
  def proj_name(self):
    return self._read_gradle(self.root / "settings.gradle", "rootProject.name")

  @cached_property
  def proj_version(self):
    return self._read_gradle(self.root / "build.gradle", "version")

  def run(self, *args, **kw):
    kw.setdefault("check", True)
    kw["args"] = ["bash", "gradlew", *args]
    kw["cwd"] = str(self.root)
    return subprocess.run(**kw)

  def build(self, **kw):
    return self.run("shadowJar", **kw)
