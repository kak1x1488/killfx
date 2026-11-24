package com.example.killfx;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class KillFx implements ModInitializer {
    /** Выбранный эффект каждого игрока (в памяти, до перезапуска сервера). */
    private static final Map<UUID, String> CHOICE = new HashMap<>();
    private static final List<Effects.Anim> ANIMS = new ArrayList<>();

    @Override
    public void onInitialize() {
        // Убийство: атакующий - игрок
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
            if (!(victim.getEntityWorld() instanceof ServerWorld world)) return;

            String name = CHOICE.getOrDefault(killer.getUuid(), "random");
            if (name.equals("off")) return;

            Vec3d pos = victim.getEntityPos();
            Effects.Type type = name.equals("random")
                    ? Effects.Type.values()[world.getRandom().nextInt(Effects.Type.values().length)]
                    : Effects.Type.valueOf(name.toUpperCase(Locale.ROOT));

            Effects.play(type, world, pos, ANIMS);
        });

        // Тик анимаций (спирали, волны)
        ServerTickEvents.END_SERVER_TICK.register(server -> ANIMS.removeIf(Effects.Anim::tick));

        // /killfx <эффект>
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            List<String> names = new ArrayList<>(List.of("random", "off"));
            for (Effects.Type t : Effects.Type.values()) names.add(t.name().toLowerCase(Locale.ROOT));

            dispatcher.register(CommandManager.literal("killfx")
                .then(CommandManager.argument("effect", StringArgumentType.word())
                    .suggests((ctx, b) -> CommandSource.suggestMatching(names, b))
                    .executes(ctx -> {
                        String eff = StringArgumentType.getString(ctx, "effect").toLowerCase(Locale.ROOT);
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        if (!names.contains(eff)) {
                            ctx.getSource().sendFeedback(() -> Text.literal("Нет такого эффекта. Доступно: " + names), false);
                            return 0;
                        }
                        CHOICE.put(p.getUuid(), eff);
                        ctx.getSource().sendFeedback(() -> Text.literal("Килл-эффект: " + eff), false);
                        return 1;
                    })));
        });
    }
}
