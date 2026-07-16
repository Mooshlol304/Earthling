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

import java.util.Optional;

public enum TranslationLanguage {
    CHINESE("zh-CN",   "Chinese (Simplified)"),
    DUTCH("nl",        "Dutch"),
    ENGLISH_UK("en",   "English (UK)"),
    FRENCH("fr",       "French"),
    GERMAN("de",       "German"),
    ITALIAN("it",      "Italian"),
    JAPANESE("ja",     "Japanese"),
    KOREAN("ko",       "Korean"),
    PORTUGUESE("pt",   "Portuguese"),
    RUSSIAN("ru",      "Russian"),
    SPANISH("es",      "Spanish"),
    TURKISH("tr",      "Turkish");

    private final String code;
    private final String label;

    TranslationLanguage(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }

    /** The suffix the user types, e.g. {@code --fr} or {@code --zh-cn}. Always lowercase. */
    public String getSuffix() {
        return "--" + code.toLowerCase();
    }

    /**
     * Looks up a language from user-typed input like {@code "--fr"} or {@code "--ZH-CN"}.
     * Comparison is case-insensitive.
     */
    public static Optional<TranslationLanguage> fromSuffix(String input) {
        String normalized = input.toLowerCase();
        for (TranslationLanguage lang : values()) {
            if (lang.getSuffix().equals(normalized)) {
                return Optional.of(lang);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() { return label; }
}