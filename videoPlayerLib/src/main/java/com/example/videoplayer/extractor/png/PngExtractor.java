/*
 * Copyright 2023 The Android Open Source Project
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
package com.example.videoplayer.extractor.png;

import com.example.videoplayer.common.MimeTypes;
import com.example.videoplayer.common.util.UnstableApi;
import com.example.videoplayer.extractor.Extractor;
import com.example.videoplayer.extractor.ExtractorInput;
import com.example.videoplayer.extractor.ExtractorOutput;
import com.example.videoplayer.extractor.PositionHolder;
import com.example.videoplayer.extractor.SingleSampleExtractor;
import java.io.IOException;

/** Extracts data from the PNG container format. */
@UnstableApi
public final class PngExtractor implements Extractor {

  // See PNG (Portable Network Graphics) Specification, Version 1.2, Section 12.12 and Section 3.1.
  private static final int PNG_FILE_SIGNATURE = 0x8950;
  private static final int PNG_FILE_SIGNATURE_LENGTH = 2;

  private final SingleSampleExtractor imageExtractor;

  /** Creates an instance. */
  public PngExtractor() {
    imageExtractor =
        new SingleSampleExtractor(
            PNG_FILE_SIGNATURE, PNG_FILE_SIGNATURE_LENGTH, MimeTypes.IMAGE_PNG);
  }

  @Override
  public boolean sniff(ExtractorInput input) throws IOException {
    return imageExtractor.sniff(input);
  }

  @Override
  public void init(ExtractorOutput output) {
    imageExtractor.init(output);
  }

  @Override
  public @ReadResult int read(ExtractorInput input, PositionHolder seekPosition)
      throws IOException {
    return imageExtractor.read(input, seekPosition);
  }

  @Override
  public void seek(long position, long timeUs) {
    imageExtractor.seek(position, timeUs);
  }

  @Override
  public void release() {
    // Do nothing.
  }
}
