/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.videoplayer.exoplayer.mediacodec;

import static com.example.videoplayer.common.util.Assertions.checkState;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.example.videoplayer.common.util.UnstableApi;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;

/** Wrapper class for the platform {@link android.media.LoudnessCodecController}. */
@RequiresApi(35)
@UnstableApi
public final class LoudnessCodecController {

  /** Interface to intercept and modify loudness parameters before applying them to the codec. */
  public interface LoudnessParameterUpdateListener {

    /** The default update listener returning an unmodified set of parameters. */
    LoudnessParameterUpdateListener DEFAULT = bundle -> bundle;

    /**
     * Returns the updated loudness parameters to be applied to the codec.
     *
     * @param parameters The suggested loudness parameters.
     * @return The updated loudness parameters.
     */
    Bundle onLoudnessParameterUpdate(Bundle parameters);
  }

  private final HashSet<MediaCodec> mediaCodecs;
  private final LoudnessParameterUpdateListener updateListener;

  @Nullable private Object loudnessCodecController;

  /** Creates the loudness controller. */
  public LoudnessCodecController() {
    this(LoudnessParameterUpdateListener.DEFAULT);
  }

  /**
   * Creates the loudness controller.
   *
   * @param updateListener The {@link LoudnessParameterUpdateListener} to intercept and modify
   *     parameters.
   */
  public LoudnessCodecController(LoudnessParameterUpdateListener updateListener) {
    this.mediaCodecs = new HashSet<>();
    this.updateListener = updateListener;
  }

  /**
   * Configures the loudness controller with an audio session id.
   *
   * @param audioSessionId The audio session ID.
   */
  public void setAudioSessionId(int audioSessionId) {
    if (loudnessCodecController != null) {
      try {
        Method closeMethod = loudnessCodecController.getClass().getMethod("close");
        closeMethod.invoke(loudnessCodecController);
      } catch (Exception e) {
        // Ignore exceptions
      }
      loudnessCodecController = null;
    }

    // Check if Tiramisu or higher (API 33)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return; // LoudnessCodecController doesn't exist before API 33
    }

    try {
      // Load the LoudnessCodecController class
      Class<?> loudnessCodecControllerClass = Class.forName("android.media.LoudnessCodecController");

      // Create the listener using reflection (to avoid direct reference)
      Class<?> onLoudnessCodecUpdateListenerClass = Class.forName("android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener");

      // Create a proxy for the listener interface
      Object listener = java.lang.reflect.Proxy.newProxyInstance(
          onLoudnessCodecUpdateListenerClass.getClassLoader(),
          new Class<?>[]{onLoudnessCodecUpdateListenerClass},
          (proxy, method, args) -> {
            if ("onLoudnessCodecUpdate".equals(method.getName())) {
              // Extract parameters and call our listener
              MediaCodec codec = (MediaCodec) args[0];
              Bundle parameters = (Bundle) args[1];
              return updateListener.onLoudnessParameterUpdate(parameters);
            }
            return null;
          });

      // Call create method
      Method createMethod = loudnessCodecControllerClass.getMethod(
          "create", int.class, java.util.concurrent.Executor.class, onLoudnessCodecUpdateListenerClass);
      Object newLoudnessCodecController = createMethod.invoke(null, audioSessionId, directExecutor(), listener);

      this.loudnessCodecController = newLoudnessCodecController;

      // Add existing media codecs
      Method addMediaCodecMethod = loudnessCodecControllerClass.getMethod("addMediaCodec", MediaCodec.class);
      for (Iterator<MediaCodec> it = mediaCodecs.iterator(); it.hasNext(); ) {
        MediaCodec codec = it.next();
        boolean registered = (boolean) addMediaCodecMethod.invoke(newLoudnessCodecController, codec);
        if (!registered) {
          it.remove();
        }
      }
    } catch (ClassNotFoundException e) {
      // Ignore - class doesn't exist
    } catch (NoSuchMethodException e) {
      // Ignore - method doesn't exist
    } catch (IllegalAccessException e) {
      // Ignore - access issue
    } catch (InvocationTargetException e) {
      // Ignore - invocation issue
    }
  }

  /**
   * Adds a codec to be configured by the loudness controller.
   *
   * @param mediaCodec A {@link MediaCodec}.
   */
  public void addMediaCodec(MediaCodec mediaCodec) {
    boolean addedToController = true;
    if (loudnessCodecController != null) {
      try {
        Method addMediaCodecMethod = loudnessCodecController.getClass().getMethod("addMediaCodec", MediaCodec.class);
        addedToController = (boolean) addMediaCodecMethod.invoke(loudnessCodecController, mediaCodec);
      } catch (Exception e) {
        addedToController = false;
      }
    }

    if (addedToController) {
      checkState(mediaCodecs.add(mediaCodec));
    }
  }

  /**
   * Removes a codec from being configured by the loudness controller.
   *
   * @param mediaCodec A {@link MediaCodec}.
   */
  public void removeMediaCodec(MediaCodec mediaCodec) {
    boolean removedCodec = mediaCodecs.remove(mediaCodec);
    if (removedCodec && loudnessCodecController != null) {
      try {
        Method removeMediaCodecMethod = loudnessCodecController.getClass().getMethod("removeMediaCodec", MediaCodec.class);
        removeMediaCodecMethod.invoke(loudnessCodecController, mediaCodec);
      } catch (Exception e) {
        // Ignore exceptions
      }
    }
  }

  /** Releases the loudness controller. */
  public void release() {
    mediaCodecs.clear();
    if (loudnessCodecController != null) {
      try {
        Method closeMethod = loudnessCodecController.getClass().getMethod("close");
        closeMethod.invoke(loudnessCodecController);
      } catch (Exception e) {
        // Ignore exceptions
      }
      loudnessCodecController = null;
    }
  }
}
