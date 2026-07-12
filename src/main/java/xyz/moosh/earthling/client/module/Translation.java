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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Translation extends Module {

    private final ConfigOption<TranslationLanguage> targetLang;
    private boolean isIgnoring = false;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Translation() {
        super("translation", "Translation", Category.CHAT);
        this.targetLang = config.addEnum("target_lang", "Target Language", TranslationLanguage.ENGLISH_UK);
    }

    @Override
    public void onEnable() {
        eventBus.subscribe(ChatReceiveEvent.class, this::onChat);
    }

    @Override
    public void onDisable() {}

    private void onChat(ChatReceiveEvent event) {
        if (isIgnoring || !isEnabled() || event.isOverlay()) return;

        String fullText = event.getMessage().getString();
        String messageBody = extractBody(fullText);

        // Don't show for very short messages (like "ok" or "1")
        if (messageBody.length() < 2) return;

        // NEW STYLE: Grey, Bold, No Brackets
        MutableComponent icon = Component.literal(" 文")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GRAY) // Grey color
                        .withBold(true)                 // Keep bold
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§bTranslate this message")))
                        .withClickEvent(new ClickEvent.RunCommand("/ert_internal_translate " + messageBody))
                );

        MutableComponent newMsg = Component.empty();
        newMsg.append(event.getMessage());
        newMsg.append(icon);

        event.cancel();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.isIgnoring = true;
            mc.gui.getChat().addMessage(newMsg);
            this.isIgnoring = false;
        }
    }

    private String extractBody(String text) {
        // Look for the last colon (standard chat) or the last arrow (some system formats)
        int lastColon = text.lastIndexOf(':');
        int lastArrow = text.lastIndexOf('>');

        // Pick the one that appears furthest to the right
        int splitIndex = Math.max(lastColon, lastArrow);

        // If we found a separator, take everything after it
        if (splitIndex != -1 && splitIndex < text.length() - 1) {
            return text.substring(splitIndex + 1).trim();
        }

        // If no separator is found (e.g., a system broadcast), use the whole thing
        return text.trim();
    }

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

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
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

    private String parseGoogleResponse(String json) {
        try {
            int start = json.indexOf("\"") + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "§cError parsing translation.";
        }
    }
}