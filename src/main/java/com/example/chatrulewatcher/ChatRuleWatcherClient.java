package com.example.chatrulewatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.regex.Pattern;

/** Client-side chat monitor. Install this mod only on the helper's client. */
public final class ChatRuleWatcherClient implements ClientModInitializer {
    private static final Pattern URL_OR_IP = Pattern.compile("(?i)(https?://|www\\.|discord\\.gg/|t\\.me/|[a-z0-9-]+\\.(com|net|org|ru|gg|me)\\b|\\b(?:\\d{1,3}\\.){3}\\d{1,3}:?\\d{0,5}\\b)");
    private static final String[] INSULTS = {
        "идиот", "кретин", "мразь", "урод", "дебил", "ничтожество", "сука", "тварь",
        "мудак", "гнида", "пидор", "пидорас", "еблан", "чмо", "гондон", "залупа"
    };
    private static final String[] RELATIVES = {
        "мать", "мама", "мамка", "мамку", "мамой", "отец", "папа", "папка", "батя",
        "родители", "родных", "сестра", "брат", "бабушка", "дедушка", "семья"
    };
    private static final String[] POLITICS = {
        "политика", "политический", "выборы", "президент", "война", "национальность",
        "нацист", "фашист", "расист", "нация", "народ", "религия", "террорист"
    };
    private static final String[] SERVER_CRITICISM = {
        "сервер говно", "сервер плохой", "плохой сервер", "админы говно", "администрация говно",
        "админ тупой", "модеры тупые", "модерация говно", "худший сервер",
        "админы бесполезны", "администрация бесполезна"
    };
    private static final String[] IMPERSONATION = {
        "я админ", "я администратор", "я модератор", "я владелец", "я создатель сервера",
        "я из администрации", "я helper", "я staff", "я оператор", "я оп"
    };
    private static final String[] PROVOCATION = {
        "нарушай правила", "обойди правила", "ломай правила", "давайте заспамим",
        "спровоцируй", "получи мут", "сделай так чтобы забанили", "обойди мут", "обойди бан"
    };
    private static final String[] BEGGING = {
        "дай денег", "дайте денег", "дай ресурсы", "дай вещи", "подари", "подарите",
        "дай донат", "купи мне", "выдай мне", "можно бесплатно", "дай ранг", "дай привилегию",
        "скинь денег", "пожалуйста дай"
    };

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (sender == null || message == null) return;
            String raw = message.getString();
            String normalized = normalize(raw);
            String rule = detectRule(raw, normalized);
            if (rule == null) return;

            String playerName = sender.Name();
            String excerpt = raw.replaceAll("\\s+", " ").trim();
            if (excerpt.length() > 100) excerpt = excerpt.substring(0, 97) + "...";
            Text alert = Text.literal("§c[МОДЕРАЦИЯ] §f" + playerName + " §7— §e" + rule
                    + "\n§8Сообщение: §7" + excerpt);
            // Local-only notification: it is displayed to the helper who installed this client mod.
            if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
                net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(alert, false);
            }
        });
    }

    private static String detectRule(String raw, String normalized) {
        if (isCapsViolation(raw)) return "3.2 — Более 70% CAPS";
        if (containsPhrase(normalized, IMPERSONATION)) return "3.7 — Выдавать себя за администрацию";
        if (URL_OR_IP.matcher(raw).find()) return "3.5 — Возможная реклама/ссылка";
        if (containsPhrase(normalized, SERVER_CRITICISM)) return "3.6 — Критика сервера/администрации";
        if (containsPhrase(normalized, PROVOCATION)) return "3.9 — Провокация на нарушение правил";
        if (containsAnyWord(normalized, POLITICS)) return "3.8 — Возможная политическая тема";
        if (containsAnyWord(normalized, RELATIVES)) return "3.3 — Упоминание родных";
        if (containsAnyWord(normalized, INSULTS)) return "3.1 — Возможное оскорбление";
        if (containsPhrase(normalized, BEGGING)) return "3.10 — Попрошайничество";
        return null;
    }

    private static boolean isCapsViolation(String raw) {
        int letters = 0, uppercase = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) uppercase++;
            }
        }
        return letters >= 8 && uppercase * 100.0 / letters > 70.0;
    }

    private static boolean containsAnyWord(String text, String[] words) {
        for (String word : words) if (containsWord(text, word)) return true;
        return false;
    }

    private static boolean containsPhrase(String text, String[] phrases) {
        for (String phrase : phrases) if (text.contains(phrase)) return true;
        return false;
    }

    private static boolean containsWord(String text, String word) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}])", Pattern.UNICODE_CHARACTER_CLASS).matcher(text).find();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("§[0-9a-fk-or]", "").replaceAll("\\s+", " ").trim();
    }
}
