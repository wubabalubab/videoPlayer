/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.videoplayer.extractor.ts;

import androidx.annotation.Nullable;
import com.example.videoplayer.common.C;
import com.example.videoplayer.common.Format;
import com.example.videoplayer.common.MimeTypes;
import com.example.videoplayer.common.util.Assertions;
import com.example.videoplayer.common.util.ParsableByteArray;
import com.example.videoplayer.common.util.UnstableApi;
import com.example.videoplayer.container.ReorderingBufferQueue;
import com.example.videoplayer.extractor.CeaUtil;
import com.example.videoplayer.extractor.ExtractorOutput;
import com.example.videoplayer.extractor.TrackOutput;
import com.example.videoplayer.extractor.ts.TsPayloadReader.TrackIdGenerator;
import java.util.List;

/** Consumes SEI buffers, outputting contained CEA-608/708 messages to a {@link TrackOutput}. */
@UnstableApi
public final class SeiReader {

  private final List<Format> closedCaptionFormats;
  private final String containerMimeType;
  private final TrackOutput[] outputs;
  private final ReorderingBufferQueue reorderingBufferQueue;

  /**
   * @param closedCaptionFormats A list of formats for the closed caption channels to expose.
   * @param containerMimeType The MIME type of the container holding the SEI buffers.
   */
  public SeiReader(List<Format> closedCaptionFormats, String containerMimeType) {
    this.closedCaptionFormats = closedCaptionFormats;
    this.containerMimeType = containerMimeType;
    outputs = new TrackOutput[closedCaptionFormats.size()];
    reorderingBufferQueue =
        new ReorderingBufferQueue(
            (presentationTimeUs, seiBuffer) ->
                CeaUtil.consume(presentationTimeUs, seiBuffer, outputs));
  }

  public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
    for (int i = 0; i < outputs.length; i++) {
      idGenerator.generateNewId();
      TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_TEXT);
      Format channelFormat = closedCaptionFormats.get(i);
      @Nullable String channelMimeType = channelFormat.sampleMimeType;
      Assertions.checkArgument(
          MimeTypes.APPLICATION_CEA608.equals(channelMimeType)
              || MimeTypes.APPLICATION_CEA708.equals(channelMimeType),
          "Invalid closed caption MIME type provided: " + channelMimeType);
      String formatId = channelFormat.id != null ? channelFormat.id : idGenerator.getFormatId();
      output.format(
          new Format.Builder()
              .setId(formatId)
              .setContainerMimeType(containerMimeType)
              .setSampleMimeType(channelMimeType)
              .setSelectionFlags(channelFormat.selectionFlags)
              .setLanguage(channelFormat.language)
              .setAccessibilityChannel(channelFormat.accessibilityChannel)
              .setInitializationData(channelFormat.initializationData)
              .build());
      outputs[i] = output;
    }
  }

  /**
   * Sets the maximum number of SEI buffers that need to be kept in order to re-order from decode to
   * presentation order.
   */
  public void setReorderingQueueSize(int reorderingQueueSize) {
    reorderingBufferQueue.setMaxSize(reorderingQueueSize);
  }

  public void consume(long pesTimeUs, ParsableByteArray seiBuffer) {
    reorderingBufferQueue.add(pesTimeUs, seiBuffer);
  }

  /**
   * Immediately passes any 'buffered for re-ordering' messages to the {@linkplain TrackOutput
   * outputs} passed to the constructor, using {@link CeaUtil#consume(long, ParsableByteArray,
   * TrackOutput[])}.
   */
  public void flush() {
    reorderingBufferQueue.flush();
  }

  /** Drops any 'buffered for re-ordering' messages. */
  public void clear() {
    reorderingBufferQueue.flush();
  }
}
