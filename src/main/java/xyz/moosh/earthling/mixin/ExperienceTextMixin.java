package xyz.moosh.earthling.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.moosh.earthling.client.EarthlingClient;
import xyz.moosh.earthling.client.module.ExpOverlay;
import xyz.moosh.earthling.client.util.ExperienceUtils;

@Mixin(ContextualBarRenderer.class)
public interface ExperienceTextMixin {

    @Redirect(
            method = "renderExperienceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private static MutableComponent redirectLevelText(String key, Object[] args) {
        int level = (int) args[0];
        ExpOverlay mod = EarthlingClient.getInstance().getModuleManager().get(ExpOverlay.class);

        if (mod != null && mod.isEnabled() && Minecraft.getInstance().player != null) {
            // Get just the points we have earned in the current level bar
            int progressXp = ExperienceUtils.getXpProgressPoints(Minecraft.getInstance().player);

            // Format: "Level (+Points)" -> e.g., "20 (+200)"
            return Component.literal(level + " (+" + progressXp + ")");
        }

        return Component.translatable(key, args);
    }
}