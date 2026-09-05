"""Attach the jars produced by the verified release build, refusing ambiguous names."""
import os
from pathlib import Path
import subprocess

artifacts = sorted(path for path in Path(".").glob("**/build/libs/*.jar")
                   if "build-logic" not in path.parts)
if not artifacts:
    raise SystemExit("No release artifacts were built")
names = [path.name for path in artifacts]
if len(set(names)) != len(names):
    raise SystemExit("Duplicate release artifact names; fix archiveBaseName before publishing")
subprocess.run(["gh", "release", "upload", os.environ["RELEASE_TAG"],
                *map(str, artifacts), "--clobber"], check=True)
