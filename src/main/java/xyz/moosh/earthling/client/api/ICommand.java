package xyz.moosh.earthling.client.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public interface ICommand {
    String getName();
    LiteralArgumentBuilder<FabricClientCommandSource> build();
}
