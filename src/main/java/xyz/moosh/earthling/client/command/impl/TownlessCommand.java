package xyz.moosh.earthling.client.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.api.ICommand;
import xyz.moosh.earthling.client.model.Resident;
import xyz.moosh.earthling.client.network.EarthMCApi;
import xyz.moosh.earthling.client.util.ChatUtil;
import xyz.moosh.earthling.client.screen.EarthlingConfigScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TownlessCommand implements ICommand {
    private static final int BATCH_SIZE = 100;

    @Override public String getName() { return "townless"; }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("townless")
                .executes(ctx -> { run(); return 1; })
                .then(ClientCommandManager.literal("open_config")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            mc.execute(() -> mc.setScreen(new EarthlingConfigScreen(mc.screen)));
                            return 1;
                        }));
    }

    private void run() {
        EarthMCApi api = EarthlingClient.getInstance().getServiceManager().getEarthMCApi();
        ChatUtil.sendMessage("§7Fetching townless players...");

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String myName = mc.player.getName().getString();

        api.getOnlinePlayers().thenCompose(allNames -> {
            List<CompletableFuture<List<Resident>>> batches = new ArrayList<>();
            for (int i = 0; i < allNames.size(); i += BATCH_SIZE) {
                List<String> batch = allNames.subList(i, Math.min(i + BATCH_SIZE, allNames.size()));
                batches.add(api.getPlayersBatch(new ArrayList<>(batch)));
            }
            return CompletableFuture.allOf(batches.toArray(new CompletableFuture[0]))
                    .thenApply(v -> batches.stream()
                            .flatMap(f -> f.getNow(new ArrayList<>()).stream())
                            .toList());
        }).thenAccept(residents -> mc.execute(() -> {
            List<String> names = residents.stream()
                    .filter(r -> r.town == null || r.town.isBlank())
                    .map(r -> r.name).sorted(String.CASE_INSENSITIVE_ORDER).toList();

            if (names.isEmpty()) {
                ChatUtil.sendMessage("§7No townless players found.");
                return;
            }

            String myTown = residents.stream().filter(r -> r.name.equalsIgnoreCase(myName))
                    .map(r -> r.town).findFirst().orElse("NoTown");

            // Fix 1 & 2: Use Java's internal property instead of Minecraft's Util class
            String chatKey = mc.options.keyChat.getTranslatedKeyMessage().getString().toUpperCase();
            boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
            String osPaste = isMac ? "Command+V" : "Ctrl+V";

            ChatUtil.sendMessage("§bTownless §8(" + names.size() + "/" + residents.size() + "):");
            ChatUtil.sendMessage("§7§oClick a name to copy. Press §f[" + chatKey + "]§7 then §f[" + osPaste + "]§7.");

            MutableComponent list = Component.empty();
            for (int i = 0; i < names.size(); i++) {
                list.append(createPlayerComponent(names.get(i), myTown));
                if (i < names.size() - 1) list.append(Component.literal("§7, "));
            }

            mc.gui.getChat().addMessage(list);

            if (EarthlingClient.getInstance().getTownlessMessage().get().equals(
                    EarthlingClient.getInstance().getTownlessMessage().getDefaultValue())) {

                ClickEvent configClick = new ClickEvent.RunCommand("/ert townless open_config");
                MutableComponent hint = Component.literal("§8[Click to customize your message]")
                        .withStyle(style -> style.withClickEvent(configClick));
                mc.gui.getChat().addMessage(hint);
            }
        }));
    }

    private MutableComponent createPlayerComponent(String name, String town) {
        String rawMsg = EarthlingClient.getInstance().getTownlessMessage().get();
        String formattedMsg = rawMsg.replace("@username", name).replace("@town", town);
        String clipboardContent = "/msg " + name + " " + formattedMsg;

        // Use the Record syntax as verified by your decompile
        ClickEvent click = new ClickEvent.CopyToClipboard(clipboardContent);

        HoverEvent hover = new HoverEvent.ShowText(
                Component.literal("§aClick to copy invite message for " + name + "\n§7After clicking, paste it into chat.")
        );

        return Component.literal("§f" + name).withStyle(style -> style
                .withClickEvent(click)
                .withHoverEvent(hover));
    }
}