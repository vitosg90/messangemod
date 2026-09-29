# Chat Bubbles (Fabric, Minecraft 26.2)

## Установка в проект
1. Создай проект из официального шаблона Fabric (сайт Fabric Develop), Minecraft 26.2, Java 25, в шаблоне включи раздельные исходники client/main.
2. Скопируй папку `src/` из этого архива в проект (слей с существующей).
3. В `src/main/resources/fabric.mod.json` в блоке `entrypoints` укажи:
   "client": ["com.example.chatbubbles.ChatBubblesClient"]
   и убедись, что в `id` стоит `chatbubbles` (или поменяй MOD_ID в ChatBubblesClient и папку lang).
4. Запуск: `./gradlew runClient`.

## Управление
- `O` — меню настроек.
- `K` — режим курсора: появляется курсор, ЛКМ по облачку убирает его. Выход: `K` или `Esc`.
- Облачко исчезает само через заданное время (по умолчанию 10 с).
- Клавиши меняются в Настройки → Управление → Облачки чата.
