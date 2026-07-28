package com.anomalyt.column.mod;

import java.lang.reflect.Method;

public class ColumnStatusCommand {
    public static void register() {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback");
            Object event = callbackClass.getField("EVENT").get(null);
            Method registerMethod = event.getClass().getMethod("register", java.util.function.BiConsumer.class);
            registerMethod.invoke(event, (java.util.function.BiConsumer<Object, Object>) (dispatcher, registryAccess) -> {
                try {
                    Class<?> literalBuilderClass = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
                    Method literalMethod = literalBuilderClass.getMethod("literal", String.class);
                    Object literalNode = literalMethod.invoke(null, "columnstatus");
                    Method executesMethod = literalNode.getClass().getMethod("executes", java.util.function.Function.class);
                    Object builtNode = executesMethod.invoke(literalNode, (java.util.function.Function<Object, Integer>) context -> {
                        try {
                            Object source = context.getClass().getMethod("getSource").invoke(context);
                            Method sendFeedback = source.getClass().getMethod("sendFeedback", String.class);
                            sendFeedback.invoke(source, "Column status: running locally");
                        } catch (Exception ignored) {
                        }
                        return 1;
                    });
                    Method registerNodeMethod = dispatcher.getClass().getMethod("register", Object.class);
                    registerNodeMethod.invoke(dispatcher, builtNode);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}
