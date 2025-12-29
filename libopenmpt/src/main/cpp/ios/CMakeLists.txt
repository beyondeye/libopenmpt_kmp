cmake_minimum_required(VERSION 3.21)
project(libopenmpt C CXX)

# iOS deployment target
set(CMAKE_OSX_DEPLOYMENT_TARGET "13.0" CACHE STRING "Minimum iOS deployment version")

# C/C++ Standards
set(CMAKE_C_STANDARD 17)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build type defaults to Release
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# Parent source directory
set(OPENMPT_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/..")

# Compiler flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -Wall -fexceptions -frtti")

# Definitions
add_definitions(
    -DLIBOPENMPT_BUILD
    # No external dependencies - no MPT_WITH_* flags
    # This means no MP3, OGG, Vorbis support but all native tracker formats work
)

# Include directories
include_directories(
    ${OPENMPT_SRC_DIR}
    ${OPENMPT_SRC_DIR}/src
    ${OPENMPT_SRC_DIR}/common
)

# Source files (from Android.mk)
set(LIBOPENMPT_SOURCES
    # Common
    ${OPENMPT_SRC_DIR}/common/ComponentManager.cpp
    ${OPENMPT_SRC_DIR}/common/Logging.cpp
    ${OPENMPT_SRC_DIR}/common/mptFileType.cpp
    ${OPENMPT_SRC_DIR}/common/mptPathString.cpp
    ${OPENMPT_SRC_DIR}/common/mptRandom.cpp
    ${OPENMPT_SRC_DIR}/common/mptStringBuffer.cpp
    ${OPENMPT_SRC_DIR}/common/mptTime.cpp
    ${OPENMPT_SRC_DIR}/common/Profiler.cpp
    ${OPENMPT_SRC_DIR}/common/serialization_utils.cpp
    ${OPENMPT_SRC_DIR}/common/version.cpp
    
    # libopenmpt API
    ${OPENMPT_SRC_DIR}/libopenmpt/libopenmpt_c.cpp
    ${OPENMPT_SRC_DIR}/libopenmpt/libopenmpt_cxx.cpp
    ${OPENMPT_SRC_DIR}/libopenmpt/libopenmpt_impl.cpp
    ${OPENMPT_SRC_DIR}/libopenmpt/libopenmpt_ext_impl.cpp
    
    # soundlib core
    ${OPENMPT_SRC_DIR}/soundlib/AudioCriticalSection.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ContainerMMCMP.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ContainerPP20.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ContainerUMX.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ContainerXPK.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Dlsbank.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Fastmix.cpp
    ${OPENMPT_SRC_DIR}/soundlib/InstrumentExtensions.cpp
    ${OPENMPT_SRC_DIR}/soundlib/InstrumentSynth.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ITCompression.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ITTools.cpp
    
    # Module loaders
    ${OPENMPT_SRC_DIR}/soundlib/Load_667.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_669.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_amf.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_ams.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_c67.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_cba.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_dbm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_digi.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_dmf.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_dsm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_dsym.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_dtm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_etx.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_far.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_fc.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_fmt.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_ftm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_gdm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_gmc.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_gt2.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_ice.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_imf.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_ims.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_it.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_itp.cpp
    ${OPENMPT_SRC_DIR}/soundlib/load_j2b.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_kris.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mdl.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_med.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mid.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mo3.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mod.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mt2.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mtm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_mus_km.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_okt.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_plm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_psm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_pt36.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_ptm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_puma.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_rtm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_s3m.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_sfx.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_stk.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_stm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_stp.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_symmod.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_tcb.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_uax.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_ult.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_unic.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_wav.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_xm.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Load_xmf.cpp
    
    # soundlib utilities
    ${OPENMPT_SRC_DIR}/soundlib/Message.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MIDIEvents.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MIDIMacroParser.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MIDIMacros.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MixerLoops.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MixerSettings.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MixFuncTable.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ModChannel.cpp
    ${OPENMPT_SRC_DIR}/soundlib/modcommand.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ModInstrument.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ModSample.cpp
    ${OPENMPT_SRC_DIR}/soundlib/ModSequence.cpp
    ${OPENMPT_SRC_DIR}/soundlib/modsmp_ctrl.cpp
    ${OPENMPT_SRC_DIR}/soundlib/mod_specifications.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MODTools.cpp
    ${OPENMPT_SRC_DIR}/soundlib/MPEGFrame.cpp
    ${OPENMPT_SRC_DIR}/soundlib/OggStream.cpp
    ${OPENMPT_SRC_DIR}/soundlib/OPL.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Paula.cpp
    ${OPENMPT_SRC_DIR}/soundlib/patternContainer.cpp
    ${OPENMPT_SRC_DIR}/soundlib/pattern.cpp
    ${OPENMPT_SRC_DIR}/soundlib/PlaybackTest.cpp
    ${OPENMPT_SRC_DIR}/soundlib/PlayState.cpp
    ${OPENMPT_SRC_DIR}/soundlib/RowVisitor.cpp
    ${OPENMPT_SRC_DIR}/soundlib/S3MTools.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormats.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatBRR.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatFLAC.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatMediaFoundation.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatMP3.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatOpus.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatSFZ.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleFormatVorbis.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SampleIO.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Sndfile.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Snd_flt.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Snd_fx.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Sndmix.cpp
    ${OPENMPT_SRC_DIR}/soundlib/SoundFilePlayConfig.cpp
    ${OPENMPT_SRC_DIR}/soundlib/UMXTools.cpp
    ${OPENMPT_SRC_DIR}/soundlib/UpgradeModule.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Tables.cpp
    ${OPENMPT_SRC_DIR}/soundlib/Tagging.cpp
    ${OPENMPT_SRC_DIR}/soundlib/TinyFFT.cpp
    ${OPENMPT_SRC_DIR}/soundlib/tuningCollection.cpp
    ${OPENMPT_SRC_DIR}/soundlib/tuning.cpp
    ${OPENMPT_SRC_DIR}/soundlib/WAVTools.cpp
    ${OPENMPT_SRC_DIR}/soundlib/WindowedFIR.cpp
    ${OPENMPT_SRC_DIR}/soundlib/XMTools.cpp
    
    # Plugins
    ${OPENMPT_SRC_DIR}/soundlib/plugins/DigiBoosterEcho.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/DMOPlugin.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/DMOUtils.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/Chorus.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/Compressor.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/Distortion.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/Echo.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/Flanger.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/Gargle.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/I3DL2Reverb.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/ParamEq.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/dmo/WavesReverb.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/LFOPlugin.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/PluginManager.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/PlugInterface.cpp
    ${OPENMPT_SRC_DIR}/soundlib/plugins/SymMODEcho.cpp
    
    # DSP
    ${OPENMPT_SRC_DIR}/sounddsp/AGC.cpp
    ${OPENMPT_SRC_DIR}/sounddsp/DSP.cpp
    ${OPENMPT_SRC_DIR}/sounddsp/EQ.cpp
    ${OPENMPT_SRC_DIR}/sounddsp/Reverb.cpp
)

# Create static library
add_library(openmpt STATIC ${LIBOPENMPT_SOURCES})

# Set library properties
set_target_properties(openmpt PROPERTIES
    OUTPUT_NAME "openmpt"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    PUBLIC_HEADER "${OPENMPT_SRC_DIR}/libopenmpt/libopenmpt.h;${OPENMPT_SRC_DIR}/libopenmpt/libopenmpt_config.h"
)

# Install targets
install(TARGETS openmpt
    ARCHIVE DESTINATION lib
    PUBLIC_HEADER DESTINATION include/libopenmpt
)
