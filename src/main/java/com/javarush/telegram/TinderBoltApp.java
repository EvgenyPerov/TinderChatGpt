package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static String TELEGRAM_BOT_NAME;
    public static String TELEGRAM_BOT_TOKEN;
    public static String OPEN_AI_TOKEN;
    private ChatGPTService chatGPTService = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> messages = new ArrayList<>();

    private UserInfo myInfo;
    private UserInfo partnerInfo;
    private int counter;

    static {
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            Properties properties = new Properties();
            properties.load(fis);
            TELEGRAM_BOT_NAME = properties.getProperty("telegram.bot.username");
            TELEGRAM_BOT_TOKEN = properties.getProperty("telegram.bot.token");
            OPEN_AI_TOKEN = properties.getProperty("telegram.bot.chatGptToken");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }

    @Override
    public void onUpdateEventReceived(Update update) {

        if (getMessageText().trim().equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoTextMessage("main", loadMessage("main"));
            showMainMenu("Старт", "/start"
                , "генерация Tinder-профля \uD83D\uDE0E", "/profile"
                , "сообщение для знакомства \uD83E\uDD70", "/opener "
                , "переписка от вашего имени \uD83D\uDE08", "/message"
                , "переписка со звездами \uD83D\uDD25", "/date"
                , "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        if (getMessageText().trim().equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoTextMessage("gpt", loadMessage("gpt"));
            return;
        }

        if (getMessageText().trim().equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            sendTextButtonsMessage("Выберите партнера, кого хотите пригласить на свидание:",
                "Ариана Гранде \uD83D\uDD25  (сложность 5/10)", "date_grande",
                "Марго Робби \uD83D\uDD25\uD83D\uDD25  (сложность 7/10)", "date_robbie",
                "Зендея     \uD83D\uDD25\uD83D\uDD25\uD83D\uDD25 (сложность 10/10)", "date_zendaya",
                "Райан Гослинг \uD83D\uDE0E (сложность 7/10)", "date_gosling",
                "Том Харди   \uD83D\uDE0E\uD83D\uDE0E (сложность 10/10)", "date_hardy");
            return;
        }

        if (getMessageText().trim().equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage(loadMessage("message"),
                "Следующее сообщение", "message_next",
                "Пригласить на свидание", "message_date");
            return;
        }

        if (getMessageText().trim().equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            myInfo = new UserInfo();
            counter = 1;
            sendPhotoTextMessage("profile", loadMessage("profile"));
            sendTextMessage("Как вас зовут?");
            return;
        }

        if (getMessageText().trim().equals("/opener")) {
            currentMode = DialogMode.OPENER;
            partnerInfo = new UserInfo();
            counter = 1;
            sendPhotoTextMessage("opener", loadMessage("opener"));
            sendTextMessage("Введите имя");
            return;
        }

        if (!isMessageCommand()) {
            switch (currentMode) {
                case GPT -> {
                    Message pause = sendTextMessage("Подождите немного, чат GPT \uD83E\uDDE0 анализирует данные ...");
                    String response = chatGPTService.sendMessage(loadPrompt("gpt"), getMessageText());
                    updateTextMessage(pause, response);
                }
                case DATE -> {
                    String query = getCallbackQueryButtonKey();
                    if (query.startsWith("date_")) {
                        sendPhotoMessage(query);
                        sendTextMessage("Отлично! Твоя задача - пригласить партнера на свидание ❤\uFE0F. Удачи!");
                        chatGPTService.setPrompt(loadPrompt(query));
                        return;
                    }
                    String response = chatGPTService.addMessage(getMessageText());
                    sendTextMessage(response);
                }
                case MESSAGE -> {
                    String query = getCallbackQueryButtonKey();
                    if (query.startsWith("message_")) {
                        Message pause = sendTextMessage("Подождите немного, чат GPT \uD83E\uDDE0 анализирует данные ...");
                        String response = chatGPTService.sendMessage(loadPrompt(query), String.join("\n\n", messages));
                        updateTextMessage(pause, response);
                    } else {
                        messages.add(getMessageText());
                    }
                }
                case PROFILE -> inputInfoAboutUser(loadPrompt("profile"), myInfo);
                case OPENER -> inputInfoAboutUser(loadPrompt("opener"), partnerInfo);
            }
        }
    }

    private void inputInfoAboutUser(String prompt, UserInfo userInfo) {
        switch (counter) {
            case 1 -> {
                counter = 2;
                userInfo.name = getMessageText();
                sendTextMessage("Сколько лет?");
            }
            case 2 -> {
                counter = 3;
                userInfo.age = getMessageText();
                sendTextMessage("Какое хобби?");
            }
            case 3 -> {
                counter = 4;
                userInfo.hobby = getMessageText();
                sendTextMessage("Укажите профессию");
            }
            case 4 -> {
                counter = 5;
                userInfo.occupation = getMessageText();
                sendTextMessage("Где живете?");
            }
            case 5 -> {
                counter = 6;
                userInfo.city = getMessageText();
                sendTextMessage("Укажите пол");
            }
            case 6 -> {
                counter = 7;
                userInfo.sex = getMessageText();
                sendTextMessage("Укажите привлекательность объекта описания");
            }
            case 7 -> {
                counter = 8;
                userInfo.handsome = getMessageText();
                sendTextMessage("Какой заработок в месяц?");
            }
            case 8 -> {
                counter = 9;
                userInfo.wealth = getMessageText();
                sendTextMessage("Что раздражает в людях?");
            }
            case 9 -> {
                counter = 10;
                userInfo.annoys = getMessageText();
                sendTextMessage("Какая цель этого знакомства?");
            }
            case 10 -> {
                counter = 0;
                userInfo.goals = getMessageText();
                Message pause = sendTextMessage("Подождите немного, чат GPT \uD83E\uDDE0 анализирует данные ...");
                String response = chatGPTService.sendMessage(prompt, userInfo.toString());
                updateTextMessage(pause, response);
            }
        }
    }
}
