if __name__ != "__main__":
  raise RuntimeError("This script must not be imported")
import json5
import logging
import os
from create_hash import create_hash
from gh_api import GitHubClient
from gradle_lib import Gradle
from jar_finder import find_jar
from MainShortcuts2 import ms
from MainShortcuts2.ex.pathlib_ex import Path
from sys import exit
log = logging.getLogger(__name__)
log.setLevel(logging.DEBUG)
log.addHandler(logging.StreamHandler())
for i in log.handlers:
  i.setLevel(log.level)
# Проверка GitHub Actions
if os.environ.get("GITHUB_ACTIONS") != "true":
  log.fatal("Allowed only on GitHub Actions")
  exit(1)
# Окружение
PROJ_DIR = Path(__file__).parent.parent
os.chdir(PROJ_DIR)
log.info("Project dir: %s", PROJ_DIR)
# GitHub
gh = GitHubClient.from_env()
log.info("GitHub repo: %s", gh.repo)
# Gradle
gradle = Gradle(PROJ_DIR)
proj_name = gradle.proj_name
proj_version = gradle.proj_version
log.info("Project: %s %s", proj_name, proj_version)
# Build
info_file = PROJ_DIR / "src/main/resources/info.json"
log.info("Building...")
info_data = json5.loads(info_file.read_text())
info_data["builded_at"] = ms.utcnow
info_data["name"] = proj_name
info_data["version"] = proj_version
info_file.write_json(info_data, sort_keys=True)
gradle.build()
jar_file = find_jar(PROJ_DIR / "build/libs")
log.info("Build completed: %s", jar_file)
# Release
log.info("Preparing files for release...")
rel_assets = {jar_file.name: jar_file}
rel_assets["info.json"] = info_file
rel_assets["sha256sums.txt"] = create_hash("sha256", rel_assets).encode("utf-8")
log.info("Uploading release...")
release = gh.create_release(
    tag_name=f"v{proj_version}",
    name=f"{proj_name} {proj_version}",
    files=rel_assets,
)
log.info("Release: %s", release.html_url)
# Complete
log.info("\nAll tasks are completed!")
