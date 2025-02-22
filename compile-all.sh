#!/bin/bash

# 设置NDK路径和工作目录
export NDK=~/android-ndk/android-ndk-r25b
export ANDROID_NDK_ROOT=$NDK
export WORK_DIR=~/my_work/all
#export APP_CPP_DIR=$WORK_DIR/app/src/main/cpp
# Kotlin Multiplatform路径
export APP_CPP_DIR=$WORK_DIR/app/src/androidMain/cpp

# 设置ABI和API级别
ABIS=("arm64-v8a" "armeabi-v7a" "x86" "x86_64")
API=21

# 假设OpenSSL、cURL和zlib已经解压在WORK_DIR下
OPENSSL_DIR=$WORK_DIR/openssl-3.3.0
CURL_DIR=$WORK_DIR/curl-8.8.0
ZLIB_DIR=$WORK_DIR/zlib-1.3.1

# 确保 configure 文件有执行权限
chmod +x $OPENSSL_DIR/configure
chmod +x $CURL_DIR/configure
chmod +x $ZLIB_DIR/configure

# 设置环境变量以找到正确的编译工具链
export PATH=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH

# 编译OpenSSL
cd $OPENSSL_DIR
for ABI in "${ABIS[@]}"; do
  case $ABI in
    "arm64-v8a")
      TARGET="android-arm64"
      ;;
    "armeabi-v7a")
      TARGET="android-arm"
      ;;
    "x86")
      TARGET="android-x86"
      ;;
    "x86_64")
      TARGET="android-x86_64"
      ;;
  esac

  ./Configure $TARGET no-tests no-unit-test no-shared -static no-asm -D__ANDROID_API__=$API -fPIC --prefix=$WORK_DIR/openssl/$ABI
  if [ $? -ne 0 ]; then
    echo "Error configuring OpenSSL for $ABI"
    exit 1
  fi

  make clean
  make -j32
  if [ $? -ne 0 ]; then
    echo "Error compiling OpenSSL for $ABI"
    exit 1
  fi

  make install
  if [ $? -ne 0 ]; then
    echo "Error installing OpenSSL for $ABI"
    exit 1
  fi
done

# 编译cURL
cd $CURL_DIR
for ABI in "${ABIS[@]}"; do
  case $ABI in
    "arm64-v8a")
      HOST="aarch64-linux-android"
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      SYSROOT=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot
      CC=$TOOLCHAIN/aarch64-linux-android$API-clang
      ;;
    "armeabi-v7a")
      HOST="arm-linux-androideabi"
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      SYSROOT=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot
      CC=$TOOLCHAIN/armv7a-linux-androideabi$API-clang
      ;;
    "x86")
      HOST="i686-linux-android"
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      SYSROOT=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot
      CC=$TOOLCHAIN/i686-linux-android$API-clang
      ;;
    "x86_64")
      HOST="x86_64-linux-android"
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      SYSROOT=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot
      CC=$TOOLCHAIN/x86_64-linux-android$API-clang
      ;;
  esac

  CFLAGS="-fPIC" ./configure --host=$HOST --with-ssl=$WORK_DIR/openssl/$ABI --prefix=$WORK_DIR/curl/$ABI --with-sysroot=$SYSROOT --disable-shared --enable-static CC=$CC
  if [ $? -ne 0 ]; then
    echo "Error configuring cURL for $ABI"
    exit 1
  fi

  make clean
  make -j32
  if [ $? -ne 0 ]; then
    echo "Error compiling cURL for $ABI"
    exit 1
  fi

  make install
  if [ $? -ne 0 ]; then
    echo "Error installing cURL for $ABI"
    exit 1
  fi
done

# 编译zlib
cd $ZLIB_DIR
for ABI in "${ABIS[@]}"; do
  case $ABI in
    "arm64-v8a")
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      AR=$TOOLCHAIN/llvm-ar
      CC=$TOOLCHAIN/aarch64-linux-android$API-clang
      CXX=$TOOLCHAIN/aarch64-linux-android$API-clang++
      RANLIB=$TOOLCHAIN/llvm-ranlib
      ;;
    "armeabi-v7a")
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      AR=$TOOLCHAIN/llvm-ar
      CC=$TOOLCHAIN/armv7a-linux-androideabi$API-clang
      CXX=$TOOLCHAIN/armv7a-linux-androideabi$API-clang++
      RANLIB=$TOOLCHAIN/llvm-ranlib
      ;;
    "x86")
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      AR=$TOOLCHAIN/llvm-ar
      CC=$TOOLCHAIN/i686-linux-android$API-clang
      CXX=$TOOLCHAIN/i686-linux-android$API-clang++
      RANLIB=$TOOLCHAIN/llvm-ranlib
      ;;
    "x86_64")
      TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
      AR=$TOOLCHAIN/llvm-ar
      CC=$TOOLCHAIN/x86_64-linux-android$API-clang
      CXX=$TOOLCHAIN/x86_64-linux-android$API-clang++
      RANLIB=$TOOLCHAIN/llvm-ranlib
      ;;
  esac

  # 创建一个干净的构建目录
  BUILD_DIR=$WORK_DIR/zlib_build/$ABI
  mkdir -p $BUILD_DIR
  cd $BUILD_DIR

  # 配置zlib编译
  CC=$CC AR=$AR RANLIB=$RANLIB CFLAGS="-fPIC --sysroot=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot" LDFLAGS="--sysroot=$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot" $ZLIB_DIR/configure --prefix=$WORK_DIR/zlib/$ABI --static
  if [ $? -ne 0 ]; then
    echo "Error configuring zlib for $ABI"
    exit 1
  fi

  # 编译zlib
  make clean
  make -j32
  if [ $? -ne 0 ]; then
    echo "Error compiling zlib for $ABI"
    exit 1
  fi

  # 安装zlib
  make install
  if [ $? -ne 0 ]; then
    echo "Error installing zlib for $ABI"
    exit 1
  fi
done

# 复制编译好的文件到指定目录
mkdir -p $APP_CPP_DIR/openssl/include
mkdir -p $APP_CPP_DIR/curl/include
mkdir -p $APP_CPP_DIR/zlib/include

# 复制OpenSSL文件
for ABI in "${ABIS[@]}"; do
  mkdir -p $APP_CPP_DIR/openssl/$ABI/lib
  cp -r $WORK_DIR/openssl/$ABI/include/openssl $APP_CPP_DIR/openssl/include
  cp $WORK_DIR/openssl/$ABI/lib/lib*.a $APP_CPP_DIR/openssl/$ABI/lib
done

# 复制cURL文件
for ABI in "${ABIS[@]}"; do
  mkdir -p $APP_CPP_DIR/curl/$ABI/lib
  cp -r $WORK_DIR/curl/$ABI/include/curl $APP_CPP_DIR/curl/include
  cp $WORK_DIR/curl/$ABI/lib/lib*.a $APP_CPP_DIR/curl/$ABI/lib
done

# 复制zlib文件
for ABI in "${ABIS[@]}"; do
  mkdir -p $APP_CPP_DIR/zlib/$ABI/lib
  cp -r $WORK_DIR/zlib/$ABI/include $APP_CPP_DIR/zlib/
  cp $WORK_DIR/zlib/$ABI/lib/libz.a $APP_CPP_DIR/zlib/$ABI/lib/
done

echo "OpenSSL, cURL and zlib compiled and copied successfully for all ABIs!"
