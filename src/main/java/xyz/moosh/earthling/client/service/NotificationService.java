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

package xyz.moosh.earthling.client.service;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xyz.moosh.earthling.client.event.EventBus;
import xyz.moosh.earthling.client.util.ChatUtil;

/**
 * Central point for all player-facing notifications.
 * Modules never call {@code client.player.sendMessage()} directly;
 * they go through here so notification behaviour can be configured in one place.
 *
 * <h3>Notification types</h3>
 * <ul>
 *   <li><b>Chat</b> — prefixed message in local chat.</li>
 *   <li><b>Toast</b> — advancement-style overlay (Phase 6, placeholder for now).</li>
 *   <li><b>Sound</b> — client-side sound cue.</li>
 * </ul>
 */
public class NotificationService extends Service {

    public enum Level { INFO, SUCCESS, WARNING, ERROR }

    public NotificationService(EventBus eventBus) {
        super("notification_service", eventBus);
    }

    // ── Chat ──────────────────────────────────────────────────────────────

    /** Send a prefixed chat notification with a given severity level. */
    public void notify(String message, Level level) {
        String colored = switch (level) {
            case INFO    -> "§7" + message;
            case SUCCESS -> "§a" + message;
            case WARNING -> "§e" + message;
            case ERROR   -> "§c" + message;
        };
        ChatUtil.sendMessage(colored);
    }

    public void info(String message)    { notify(message, Level.INFO); }
    public void success(String message) { notify(message, Level.SUCCESS); }
    public void warn(String message)    { notify(message, Level.WARNING); }
    public void error(String message)   { notify(message, Level.ERROR); }

    // ── Sound ─────────────────────────────────────────────────────────────

    /**
     * Play a subtle UI ping at the player's position.
     * Call this alongside a notification to attract attention without being intrusive.
     */
    public void playPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.MASTER,
                0.5f, // volume
                1.8f, // pitch (high = less intrusive)
                false
        );
    }

    // ── Toast (Phase 6) ───────────────────────────────────────────────────

    /**
     * Show an advancement-style toast.
     * Placeholder — full implementation in Phase 6 when the toast/overlay system is built.
     *
     * @param title   toast title
     * @param subtitle toast subtitle/body
     */
    public void toast(String title, String subtitle) {
        // For now, fall back to a chat message so it isn't silently dropped
        notify("§l" + title + " §r— " + subtitle, Level.INFO);
    }
}
