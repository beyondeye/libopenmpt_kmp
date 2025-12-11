# GENERAL PROJECT DESCRIPTION
-This is Kotlin Multiplatform project. Supporting Android ,iOS platforms Asm/Js and desktop JVM.
- The project is made of a "shared" KMP module which define kotlin interfaces and connect to 
  native code to play amiga mod music with libopenmpt. On Desktop JVM platform, and android with connect
 to a native library written in C via jni.
- For the ASM/JS platform, the project will use the libopenmpt compiled to wasm/js
- For iOS platform, the project will libopenmpt compiled to arm64/arm64e/x86_64/arm64e/x86_64
- the app module is a simple demo of playing music with libopenmpt with an UI developed with compose multiplatform.
- the "shared" module contains the kotlin interfaces and the native code.
- the libopempt module is a submodule where we compile libopenmpt for the different platforms (currently only for Android)
   There also instructions to compile libopenmpt for wasm/js in the docs directory
# **IMPORTANT NOTE ABOUT RUNNING GRADLE TASKS FOR THE PROJECTS**
If you need to run gradle tasks, for example for checking compilation errors, running tests, and so on,
you must use /opt/android-studio/jbr JAVA JDK, **NOT** the default system jdk
