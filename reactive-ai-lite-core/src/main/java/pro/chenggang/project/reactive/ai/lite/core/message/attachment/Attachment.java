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
package pro.chenggang.project.reactive.ai.lite.core.message.attachment;

import org.springframework.util.MimeType;
import pro.chenggang.project.reactive.ai.lite.core.message.Message;

/**
 * Represents an attachment to a {@link Message}, enabling the inclusion of non‑textual
 * content such as images, documents, audio, or other media in conversations with AI
 * models. This contract standardises how multimodal data is embedded alongside textual
 * instructions, allowing the model to process richer contexts.
 * <p>
 * Modern large language and vision models can interpret visual and auditory inputs
 * when they are combined with a user‘s text. The {@code Attachment} interface decouples
 * the metadata (MIME type, descriptive name) from the actual payload representation,
 * which is returned as a {@code String} by the {@link #content()} method. This string
 * is <strong>not</strong> intended to be the raw binary content; instead it serves as a
 * pointer or an encoded form that the framework and model provider can understand.
 * Typical implementations include:
 * <ul>
 *   <li><strong>URL‑based attachments</strong> – the content string is a publicly
 *       accessible URL that the AI service can fetch to obtain the media.</li>
 *   <li><strong>Base64‑encoded attachments</strong> – the string contains the
 *       Base64 representation of the raw bytes, directly embedding the data inside
 *       the message payload.</li>
 * </ul>
 * Because the interpretation of the content string varies by implementation, consumers
 * of this interface should consult the concrete class documentation to understand the
 * expected format.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface Attachment {

    /**
     * Returns the <em>Internet Media Type</em> (MIME type) of the attachment's content.
     * <p>
     * The MIME type is critical for AI models because it tells them how to decode and
     * process the raw bytes. For example, an image attachment might have a MIME type of
     * {@code "image/png"}, indicating a Portable Network Graphics file, while a document
     * could be {@code "application/pdf"}. The model uses this information to select the
     * appropriate pre‑processing steps (e.g., image resizing, tokenisation of text within
     * a PDF) before integrating the content into its inference pipeline.
     * </p>
     * <p>
     * The return type is Spring‘s {@link MimeType}, which provides structured access to
     * type and subtype, and can include parameters such as {@code charset}. This allows
     * downstream components to perform content‑type matching without error‑prone string
     * manipulation.
     * </p>
     *
     * @return the MIME type of the attachment; never {@code null}.
     */
    MimeType mimeType();

    /**
     * Returns a human‑readable name for the attachment.
     * <p>
     * The name serves multiple purposes: it gives the AI model contextual hints (e.g.,
     * "screenshot.png" vs. "receipt.pdf"), aids in debugging and logging, and can be
     * used by user interfaces to display attachment lists. Some implementations may
     * default to an empty string if no meaningful filename is available.
     * </p>
     * <p>
     * While the model usually does not rely solely on the name for content interpretation,
     * a descriptive name often accompanies the attachment in the final message structure,
     * helping to maintain a clear and traceable conversation history.
     * </p>
     *
     * @return the attachment name; never {@code null} but may be empty.
     */
    String name();

    /**
     * Returns the content of the attachment as a string.
     * <p>
     * This string is <strong>not</strong> the raw binary content. Instead it serves as
     * an identifier or an encoded form that the AI service can resolve. The exact
     * interpretation depends on the concrete implementation:
     * <ul>
     *   <li>A <em>URL‑based</em> attachment returns a publicly accessible URL. The AI
     *       provider will fetch the resource via HTTP when processing the message.</li>
     *   <li>A <em>Base64‑based</em> attachment returns the binary content encoded as a
     *       Base64 string, which the provider can decode directly without additional
     *       network calls.</li>
     * </ul>
     * This design choice keeps the interface simple and interoperable across different
     * model providers, as many of them accept media content either by reference (URL)
     * or inline (Base64). Implementations must clearly document which scheme they
     * follow so that callers can prepare the data accordingly.
     * </p>
     *
     * @return the string representation of the attachment’s content; never {@code null}.
     */
    String content();

}