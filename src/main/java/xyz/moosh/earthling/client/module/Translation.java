/*
 * Earthling
 * Copyright (c) 2025 Moosh
 *
 * Earthling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Earthling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Earthling. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package xyz.moosh.earthling.client.module;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.config.ConfigOption;
import xyz.moosh.earthling.client.event.impl.ChatReceiveEvent;
import xyz.moosh.earthling.client.event.impl.ChatSendEvent;
import xyz.moosh.earthling.client.util.ChatUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translation extends Module {

    private final ConfigOption<TranslationLanguage> targetLang;

    /** Guards re-entrancy on the receive side (appending the 文 icon). */
    private boolean isIgnoring = false;

    /**
     * Guards re-entrancy on the send side.
     * If we call connection.sendChat() internally we don't want to
     * intercept that packet again if ChatSendEvent fires for it.
     */
    private boolean isSendingTranslated = false;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Matches a message that ends with an outgoing-translate suffix.
     * <p>
     * Group 1 → the actual message body (everything before the suffix).<br>
     * Group 2 → the raw suffix code, e.g. {@code fr} or {@code zh-cn}.
     * <p>
     * Requires at least one whitespace character before {@code --} so that
     * URLs or other tokens that happen to contain {@code --} are not matched.
     */
    private static final Pattern SUFFIX_PATTERN =
            Pattern.compile("^(.+?)\\s+--(\\S+)$", Pattern.DOTALL);

    public Translation() {
        super("translation", "Translation", Category.CHAT);
        this.targetLang = config.addEnum("target_lang", "Target Language", TranslationLanguage.ENGLISH_UK);
    }

    // -------------------------------------------------------------------------
    // Module lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        eventBus.subscribe(ChatReceiveEvent.class, this::onChat);
        eventBus.subscribe(ChatSendEvent.class, this::onSend);
    }

    @Override
    public void onDisable() {}

    // -------------------------------------------------------------------------
    // Incoming: append 文 icon for click-to-translate
    // -------------------------------------------------------------------------

    private void onChat(ChatReceiveEvent event) {
        if (isIgnoring || !isEnabled() || event.isOverlay()) return;

        String fullText = event.getMessage().getString();
        String messageBody = extractBody(fullText);

        // Skip very short messages like "ok" or single characters
        if (messageBody.length() < 2) return;

        MutableComponent icon = Component.literal(" 文")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GRAY)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§bTranslate this message")))
                        .withClickEvent(new ClickEvent.RunCommand(
                                "/ert_internal_translate " + messageBody))
                );

        MutableComponent newMsg = Component.empty();
        newMsg.append(event.getMessage());
        newMsg.append(icon);

        event.cancel();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            isIgnoring = true;
            mc.gui.getChat().addMessage(newMsg);
            isIgnoring = false;
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing: detect --<suffix> and send translated message
    // -------------------------------------------------------------------------

    private void onSend(ChatSendEvent event) {
        // Not enabled, or we're the ones sending the translated result
        if (!isEnabled() || isSendingTranslated) return;

        String message = event.getMessage();

        // Only intercept plain messages — commands pass through untouched
        if (message.startsWith("/")) return;

        Matcher matcher = SUFFIX_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        String body      = matcher.group(1).trim();
        String rawSuffix = "--" + matcher.group(2); // e.g. "--fr"

        // Shouldn't happen given the pattern, but be safe
        if (body.isEmpty()) return;

        Optional<TranslationLanguage> langOpt = TranslationLanguage.fromSuffix(rawSuffix);

        if (langOpt.isEmpty()) {
            // Unknown suffix — cancel the send and print the help list
            event.cancel();
            showAvailableSuffixes(rawSuffix);
            return;
        }

        // Valid suffix — cancel the original message and send the translated one
        event.cancel();
        translateAndSendOutgoing(body, langOpt.get());
    }

    /**
     * Prints the unknown-suffix error followed by the full suffix list
     * using {@link ChatUtil#sendMessage}.
     */
    private void showAvailableSuffixes(String attempted) {
        ChatUtil.sendMessage("§cUnknown suffix: §f" + attempted);
        ChatUtil.sendMessage("§bAvailable Suffixes:");
        for (TranslationLanguage lang : TranslationLanguage.values()) {
            ChatUtil.sendMessage("§e" + lang.getSuffix() + " §7: §f" + lang);
        }
    }

    /**
     * Translates {@code text} into {@code lang} asynchronously, then sends
     * the result to the server as an unsigned chat packet.
     */
    private void translateAndSendOutgoing(String text, TranslationLanguage lang) {
        CompletableFuture.runAsync(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = String.format(
                        "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s",
                        lang.getCode(), encodedText
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                HttpResponse<String> response =
                        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                String translated = parseGoogleResponse(response.body());

                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.execute(() -> {
                        isSendingTranslated = true;
                        try {
                            // sendChat goes straight to the packet layer and does NOT
                            // re-fire ChatSendEvent, but the flag guards us either way.
                            mc.player.connection.sendChat(translated);
                        } finally {
                            isSendingTranslated = false;
                        }
                    });
                }
            } catch (Exception e) {
                EarthlingClient.LOGGER.error("Outgoing translation failed", e);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.execute(() -> ChatUtil.sendMessage("§cTranslation failed: " + e.getMessage()));
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Click-to-translate (existing /ert_internal_translate handler)
    // -------------------------------------------------------------------------

    public void translateAndSend(String text) {
        String langCode = targetLang.get().getCode();

        CompletableFuture.runAsync(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = String.format(
                        "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s",
                        langCode, encodedText
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                HttpResponse<String> response =
                        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                String translated = parseGoogleResponse(response.body());

                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.execute(() -> mc.player.displayClientMessage(
                            Component.literal("§8[§bTranslation§8] §f" + translated), false
                    ));
                }
            } catch (Exception e) {
                EarthlingClient.LOGGER.error("Translation failed", e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extractBody(String text) {
        int lastColon = text.lastIndexOf(':');
        int lastArrow = text.lastIndexOf('>');
        int splitIndex = Math.max(lastColon, lastArrow);

        if (splitIndex != -1 && splitIndex < text.length() - 1) {
            return text.substring(splitIndex + 1).trim();
        }

        return text.trim();
    }

    private String parseGoogleResponse(String json) {
        try {
            int start = json.indexOf("\"") + 1;
            int end   = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "§cError parsing translation.";
        }
    }
}