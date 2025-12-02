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
package com.example.videoplayer.extractor.ogg;

import static com.example.videoplayer.common.util.Assertions.checkStateNotNull;
import static java.lang.Math.min;

import com.example.videoplayer.common.C;
import com.example.videoplayer.common.ParserException;
import com.example.videoplayer.common.util.ParsableByteArray;
import com.example.videoplayer.common.util.UnstableApi;
import com.example.videoplayer.extractor.Extractor;
import com.example.videoplayer.extractor.ExtractorInput;
import com.example.videoplayer.extractor.ExtractorOutput;
import com.example.videoplayer.extractor.ExtractorsFactory;
import com.example.videoplayer.extractor.PositionHolder;
import com.example.videoplayer.extractor.TrackOutput;
import java.io.IOException;

/** Extracts data from the Ogg container format. */
@UnstableApi
public class OggExtractor implements Extractor {

  /** Factory for {@link OggExtractor} instances. */
  public static final ExtractorsFactory FACTORY = () -> new Extractor[] {new OggExtractor()};

  private static final int MAX_VERIFICATION_BYTES = 8;

  private  ExtractorOutput output;
  private  StreamReader streamReader;
  private boolean streamReaderInitialized;

  @Override
  public boolean sniff(ExtractorInput input) throws IOException {
    try {
      return sniffInternal(input);
    } catch (ParserException e) {
      return false;
    }
  }

  @Override
  public void init(ExtractorOutput output) {
    this.output = output;
  }

  @Override
  public void seek(long position, long timeUs) {
    if (streamReader != null) {
      streamReader.seek(position, timeUs);
    }
  }

  @Override
  public void release() {
    // Do nothing
  }

  @Override
  public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
    checkStateNotNull(output); // Check that init has been called.
    if (streamReader == null) {
      if (!sniffInternal(input)) {
        throw ParserException.createForMalformedContainer(
            "Failed to determine bitstream type", /* cause= */ null);
      }
      input.resetPeekPosition();
    }
    if (!streamReaderInitialized) {
      TrackOutput trackOutput = output.track(0, C.TRACK_TYPE_AUDIO);
      output.endTracks();
      streamReader.init(output, trackOutput);
      streamReaderInitialized = true;
    }
    return streamReader.read(input, seekPosition);
  }

  private boolean sniffInternal(ExtractorInput input) throws IOException {
    OggPageHeader header = new OggPageHeader();
    if (!header.populate(input, true) || (header.type & 0x02) != 0x02) {
      return false;
    }

    int length = min(header.bodySize, MAX_VERIFICATION_BYTES);
    ParsableByteArray scratch = new ParsableByteArray(length);
    input.peekFully(scratch.getData(), 0, length);

    if (FlacReader.verifyBitstreamType(resetPosition(scratch))) {
      streamReader = new FlacReader();
    } else if (VorbisReader.verifyBitstreamType(resetPosition(scratch))) {
      streamReader = new VorbisReader();
    } else if (OpusReader.verifyBitstreamType(resetPosition(scratch))) {
      streamReader = new OpusReader();
    } else {
      return false;
    }
    return true;
  }

  private static ParsableByteArray resetPosition(ParsableByteArray scratch) {
    scratch.setPosition(0);
    return scratch;
  }
}
