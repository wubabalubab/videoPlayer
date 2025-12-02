/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.example.videoplayer.exoplayer.drm;

import static com.example.videoplayer.common.util.Assertions.checkNotNull;
import static com.example.videoplayer.exoplayer.drm.DefaultDrmSessionManager.MODE_PLAYBACK;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import com.example.videoplayer.common.MediaItem;
import com.example.videoplayer.common.util.UnstableApi;
import com.example.videoplayer.datasource.DataSource;
import com.example.videoplayer.datasource.DefaultHttpDataSource;
import com.example.videoplayer.exoplayer.upstream.LoadErrorHandlingPolicy;
import com.google.common.primitives.Ints;
import java.util.Map;
import java.util.Objects;

/** Default implementation of {@link DrmSessionManagerProvider}. */
@UnstableApi
public final class DefaultDrmSessionManagerProvider implements DrmSessionManagerProvider {

  private final Object lock;

  @GuardedBy("lock")
  private MediaItem. DrmConfiguration drmConfiguration;

  @GuardedBy("lock")
  private  DrmSessionManager manager;

  @Nullable private DataSource.Factory drmHttpDataSourceFactory;
  @Nullable private String userAgent;
  @Nullable private LoadErrorHandlingPolicy drmLoadErrorHandlingPolicy;

  public DefaultDrmSessionManagerProvider() {
    lock = new Object();
  }

  /**
   * Sets the {@link DataSource.Factory} which is used to create {@link HttpMediaDrmCallback}
   * instances. If {@code null} is passed a {@link DefaultHttpDataSource.Factory} is used.
   *
   * @param drmDataSourceFactory The data source factory or {@code null} to use {@link
   *     DefaultHttpDataSource.Factory}.
   */
  public void setDrmHttpDataSourceFactory(@Nullable DataSource.Factory drmDataSourceFactory) {
    this.drmHttpDataSourceFactory = drmDataSourceFactory;
  }

  /**
   * @deprecated Pass a custom {@link DataSource.Factory} to {@link
   *     #setDrmHttpDataSourceFactory(DataSource.Factory)} which sets the desired user agent on
   *     outgoing requests.
   */
  @Deprecated
  public void setDrmUserAgent(@Nullable String userAgent) {
    this.userAgent = userAgent;
  }

  /**
   * Sets a load error handling policy to pass to {@link
   * DefaultDrmSessionManager.Builder#setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy)}.
   *
   * <p>If {@code null} is passed the setter is not called, so the default {@link
   * LoadErrorHandlingPolicy} defined by {@link DefaultDrmSessionManager.Builder#Builder()} is used
   * instead.
   */
  public void setDrmLoadErrorHandlingPolicy(LoadErrorHandlingPolicy drmLoadErrorHandlingPolicy) {
    this.drmLoadErrorHandlingPolicy = drmLoadErrorHandlingPolicy;
  }

  @Override
  public DrmSessionManager get(MediaItem mediaItem) {
    checkNotNull(mediaItem.localConfiguration);
    @Nullable
    MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
    if (drmConfiguration == null) {
      return DrmSessionManager.DRM_UNSUPPORTED;
    }

    synchronized (lock) {
      if (!Objects.equals(drmConfiguration, this.drmConfiguration)) {
        this.drmConfiguration = drmConfiguration;
        this.manager = createManager(drmConfiguration);
      }
      return checkNotNull(this.manager);
    }
  }

  private DrmSessionManager createManager(MediaItem.DrmConfiguration drmConfiguration) {
    DataSource.Factory dataSourceFactory =
        drmHttpDataSourceFactory != null
            ? drmHttpDataSourceFactory
            : new DefaultHttpDataSource.Factory().setUserAgent(userAgent);
    HttpMediaDrmCallback httpDrmCallback =
        new HttpMediaDrmCallback(
            drmConfiguration.licenseUri == null ? null : drmConfiguration.licenseUri.toString(),
            drmConfiguration.forceDefaultLicenseUri,
            dataSourceFactory);
    for (Map.Entry<String, String> entry : drmConfiguration.licenseRequestHeaders.entrySet()) {
      httpDrmCallback.setKeyRequestProperty(entry.getKey(), entry.getValue());
    }
    DefaultDrmSessionManager.Builder drmSessionManagerBuilder =
        new DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(
                drmConfiguration.scheme, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .setMultiSession(drmConfiguration.multiSession)
            .setPlayClearSamplesWithoutKeys(drmConfiguration.playClearContentWithoutKey)
            .setUseDrmSessionsForClearContent(
                Ints.toArray(drmConfiguration.forcedSessionTrackTypes));
    if (drmLoadErrorHandlingPolicy != null) {
      drmSessionManagerBuilder.setLoadErrorHandlingPolicy(drmLoadErrorHandlingPolicy);
    }
    DefaultDrmSessionManager drmSessionManager = drmSessionManagerBuilder.build(httpDrmCallback);
    drmSessionManager.setMode(MODE_PLAYBACK, drmConfiguration.getKeySetId());
    return drmSessionManager;
  }
}
