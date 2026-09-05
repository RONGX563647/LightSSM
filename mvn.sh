#!/usr/bin/env bash
# LightSSM Maven 启动脚本（Windows / Git Bash）
# boot 目录下 plexus-classworlds 的版本随 Maven 发行版变化，这里自动探测避免写死版本号。
M="${MAVEN_HOME:-E:/dev/maven/apache-maven-3.9.16}"
CW=$(ls "$M"/boot/plexus-classworlds-*.jar 2>/dev/null | head -1)

if [ -z "$CW" ]; then
  echo "未找到 Maven boot jar，请检查 MAVEN_HOME=$M" >&2
  exit 1
fi

exec java -classpath "$CW" \
  "-Dclassworlds.conf=$M/bin/m2.conf" \
  "-Dmaven.home=$M" \
  "-Dmaven.multiModuleProjectDirectory=$(pwd)" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
