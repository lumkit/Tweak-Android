#!/system/bin/sh

install_path="$1"

busybox_install() {
  bf="$install_path/busybox"

  chmod 755 "$bf" || { echo "无法设置权限: $bf" >&2; exit 1; }

  IFS=$'\n'
  for applet in $("$bf" --list); do
    case "$applet" in
    "sh"|"busybox"|"shell"|"swapon"|"swapoff"|"mkswap")
      :
    ;;
    *)
      "$bf" ln -sf busybox "$applet" || { echo "无法创建符号链接: $applet" >&2; exit 1; }
      chmod 755 "$applet" || { echo "无法设置权限: $applet" >&2; exit 1; }
    ;;
    esac
  done

  "$bf" ln -sf busybox busybox_1_30_1 || { echo "无法创建符号链接: busybox_1_30_1" >&2; exit 1; }
}

if [ -n "$install_path" ] && [ -d "$install_path" ]; then
  cd "$install_path" || { echo "无法进入目录: $install_path" >&2; exit 1; }
  if [ ! -f busybox_1_30_1 ]; then
    busybox_install
  else
    echo "BusyBox 已安装: busybox_1_30_1 存在"
  fi
else
  echo "安装路径无效: $install_path" >&2
  exit 1
fi
