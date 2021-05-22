import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Bot {
    private final TelegramBot bot = new TelegramBot("1895532699:AAGm_QCfKz_l5mgjQVgNM0LxCArASAZeBnM");
    private final Set<Long> chatIds = new HashSet<>();
    private long timeSleep = 10_000;
    private String url = "https://www.mvideo.ru/products/igrovaya-konsol-sony-playstation-5-digital-edition-40074203";
    private String tag = "input.add-to-basket-button.c-btn.c-btn_text.o-pay__btn.sel-pdp-button-place-to-cart.sel-button-place-to-cart.sel-product-tile-button-to-cart";

    public Bot() {
        chatIds.add(289599784L);
        setListenerBot();
    }

    /**
     * Слушатель чата. Получает каждое отправленное сообщение и сверяет его с доступными командами.
     * Не работает с пользователями, чей id не был добавлен в chatIds
     */
    private void setListenerBot() {
        bot.setUpdatesListener(new UpdatesListener() {
            @Override
            public int process(List<Update> updates) {

                if (!updates.isEmpty()) {
                    for (int i = 0; i < updates.size(); i++) {
                        Message message = updates.get(i).message();
                        if (message == null) {
                            return -1;
                        }

                        String textReceived = message.text().trim();
                        String[] request = textReceived.split(" ");

                        try {
                            if (request[0].toLowerCase().equals("add")) {
                                addNewUser(request[1]);
                            } else {
                                if (chatIds.contains(getIdChat()))
                                    switch (request[0].toLowerCase()) {
                                        case "time" -> changeTimeSleep(request[1]);
                                        case "url" -> changeURL(request[1]);
                                        case "tag" -> changeTag(request[1]);
                                        case "add" -> addNewUser(request[1]);
                                        case "info" -> writeInfo();
                                        case "help" -> writeHelp();
                                        default -> sendMessage("Unknown command");
                                    }
                            }
                        } catch (Exception e) {
                            sendMessage("Unknown command");
                        }
                    }
                }

                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
        });
    }

    /**
     * Бесконечно проверяет сайт на наличие тега. Если он есть, отправляет сообщение всем пользователям
     */
    public void start() throws InterruptedException, IOException {
        while (true) {
            if (thereIsPS()) sendMessageToAll(String.format("The product is available on this site:%n%n%s", url));
            Thread.sleep(timeSleep);
        }
    }

    /**
     * Дает id пользователя, который написал сообщение
     */
    private Long getIdChat() {
        GetUpdates getUpdates = new GetUpdates().limit(100).offset(0).timeout(0);
        GetUpdatesResponse updatesResponse = bot.execute(getUpdates);
        List<Update> updates = updatesResponse.updates();

        return updates.get(0).message().chat().id();
    }

    /**
     * Добавлние нового пользователя в чат по паролю
     */
    private void addNewUser(String password) {
        if (password.equals("09052000")) {
            chatIds.add(getIdChat());
            sendMessage("You have been added");
        }
    }

    private void writeHelp() {
        sendMessage("time - change the frequency of site verification\n" +
                "url - change the site being checked\n" +
                "tag - change the search tag\n" +
                "info - settings information");
    }

    private void writeInfo() {
        sendMessage(String.format("Time: %d seconds\n\nURL: %s\n\nTag: %s"
                , timeSleep / 1000, url, tag));
    }

    /**
     * Подключается к сайту и ищет нужный тег
     */
    private boolean thereIsPS() throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .referrer("http://www.google.com")
                .get();

        return doc.select(tag).first() != null;
    }

    /**
     * Замена тега для поиска элемента на странице
     */
    private void changeTag(String tag) {
        this.tag = tag;
        sendMessage("The tag has been changed");
    }

    /**
     * Пробует установить соединение и если получится - изменяет url
     */
    private void changeURL(String url) {
        try {
            Jsoup.connect(url)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com")
                    .get();

            this.url = url;
            sendMessageToAll(String.format("The tracked site has been changed to %n%n%s", url));
        } catch (Exception e) {
            sendMessage("Unavailable link");
        }
    }

    /**
     * Отправка сообщения в конкретный чат
     */
    private void sendMessage(Long chatId, String text) {
        bot.execute(new SendMessage(chatId, text));
    }

    private void sendMessage(String text) {
        sendMessage(getIdChat(), text);
    }

    /**
     * Отправка сообщения во все чаты
     */
    private void sendMessageToAll(String text) {
        for (Long chatId : chatIds) sendMessage(chatId, text);
    }

    /**
     * Изменение переодичтости проверки сайта
     */
    private void changeTimeSleep(String time) {
        try {
            long newTime = Long.parseLong(time);
            timeSleep = newTime * 1000;
            sendMessage(String.format("The time was changed to %d seconds", timeSleep / 1000));
        } catch (NumberFormatException e) {
            sendMessage("Invalid time format");
        }
    }
}