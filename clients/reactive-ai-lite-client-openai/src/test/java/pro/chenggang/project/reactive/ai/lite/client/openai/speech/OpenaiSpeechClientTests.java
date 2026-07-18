/*
 *    Copyright 2025-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.reactive.ai.lite.client.openai.speech;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import pro.chenggang.project.reactive.ai.lite.client.openai.OpenaiLlmClientTestApplicationTests;
import pro.chenggang.project.reactive.ai.lite.core.api.ReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechRawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SpeechStreamResponse;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the OpenAI speech client functionality.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public class OpenaiSpeechClientTests extends OpenaiLlmClientTestApplicationTests {

    @Autowired
    ReactiveLlmClient reactiveLlmClient;

    String model = "Qwen3-TTS-12Hz-1.7B-Base-bf16";

    /**
     * Tests a general (non-streaming) execution of the speech API.
     */
    @Test
    void testSpeechGeneralExecute() {
        reactiveLlmClient.speech()
                .model(model)
                .voice("Aiden")
                .inputText("Hello world, this is a test.")
                .responseFormat("pcm")
                .speed(0.3)
                .general()
                .execute()
                .as(StepVerifier::create)
                .consumeNextWith(speechResponse -> {
                    assertThat(speechResponse.getAudioData()).isNotEmpty();
                    playAudioBytes(speechResponse.getAudioData());
                })
                .verifyComplete();
    }

    /**
     * Tests a general (non-streaming) execution of the speech API that returns a raw response.
     */
    @Test
    void testSpeechGeneralExecuteRaw() {
        reactiveLlmClient.speech()
                .model(model)
                .voice("Aiden")
                .inputText("Hello world, this is a test.")
                .responseFormat("pcm")
                .speed(0.3)
                .general()
                .executeRaw()
                .as(StepVerifier::create)
                .consumeNextWith(speechRawResponse -> {
                    DataBuffer dataBuffer = speechRawResponse.getDataChunk();
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    playAudioBytes(bytes);
                })
                .verifyComplete();
    }

    /**
     * Tests a streaming execution of the speech API, concatenating the output chunks.
     */
    @Test
    void testSpeechStreamExecute() {
        reactiveLlmClient.speech()
                .model(model)
                .voice("Aiden")
                .inputText("Hello world, this is a test.")
                .responseFormat("pcm")
                .speed(1.2)
                .stream()
                .execute()
                .map(SpeechStreamResponse::getChunk)
                .collectList()
                .map(this::joinBytesFast)
                .as(StepVerifier::create)
                .consumeNextWith(bytes -> {
                    playAudioBytes(bytes);
                })
                .verifyComplete();
    }

    /**
     * Tests a streaming execution of the speech API returning raw responses.
     */
    @Test
    void testSpeechStreamExecuteRaw() {
        reactiveLlmClient.speech()
                .model(model)
                .voice("Aiden")
                .inputText("Hello world, this is a test.")
                .responseFormat("pcm")
                .speed(1.2)
                .stream()
                .executeRaw()
                .map(SpeechRawResponse::getDataChunk)
                .collectList()
                .flatMap(dataBufferList -> DataBufferUtils.join(Flux.fromIterable(dataBufferList)))
                .as(StepVerifier::create)
                .consumeNextWith(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    playAudioBytes(bytes);
                })
                .verifyComplete();
    }

    /**
     * Plays the audio bytes by loading them into a Clip and playing it.
     *
     * @param audioBytes the audio bytes in PCM format
     * @throws RuntimeException if an error occurs while playing the audio
     */
    public void playAudioBytes(byte[] audioBytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(audioBytes);
             AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis)) {

            // Get a clip resource
            Clip clip = AudioSystem.getClip();

            // Open audio clip and load samples from the audio input stream
            clip.open(audioInputStream);

            // Play the audio
            clip.start();

            // Optional: Keep thread alive just long enough to finish playback
            Thread.sleep(clip.getMicrosecondLength() / 1000);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
            throw new RuntimeException("Error playing audio bytes", e);
        }
    }

    /**
     * Joins a list of byte arrays into a single byte array quickly.
     *
     * @param arrays the list of byte arrays to join
     * @return a single concatenated byte array
     */
    public byte[] joinBytesFast(List<byte[]> arrays) {
        // 1. Calculate total length
        int totalLength = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                totalLength += array.length;
            }
        }

        // 2. Allocate exact memory
        byte[] result = new byte[totalLength];

        // 3. Copy pieces into the result array
        int currentPosition = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                System.arraycopy(array, 0, result, currentPosition, array.length);
                currentPosition += array.length;
            }
        }

        return result;
    }

}
