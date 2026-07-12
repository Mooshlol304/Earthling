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

public enum TranslationLanguage {
    CHINESE("zh-CN", "Chinese (Simplified)"),
    DUTCH("nl", "Dutch"),
    ENGLISH_UK("en", "English (UK)"),
    ENGLISH_US("en", "English (US)"),
    FRENCH("fr", "French"),
    GERMAN("de", "German"),
    ITALIAN("it", "Italian"),
    JAPANESE("ja", "Japanese"),
    KOREAN("ko", "Korean"),
    PORTUGUESE("pt", "Portuguese"),
    RUSSIAN("ru", "Russian"),
    SPANISH("es", "Spanish"),
    TURKISH("tr", "Turkish");

    private final String code;
    private final String label;

    TranslationLanguage(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }

    @Override
    public String toString() { return label; }
}