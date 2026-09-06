package ua.inventorytype.pnclans.impl.config

import ua.inventorytype.pnclans.api.GiveItemAction

/**
 * Изолированные преобразования старых схем магазина.
 *
 * Этот объект не загружает и не сохраняет файлы. Он получает старую типизированную
 * модель и возвращает новую, поэтому основной ConfigService остаётся коротким.
 * После удаления поддержки старых схем весь файл можно удалить целиком.
 */
internal object LegacyShopMigrator {
    fun migrate(shop: ClanShopConfig, existingVersion: Int, explicitProductRarities: Set<String>): ClanShopConfig {
        val defaults = ClanShopConfig()
        return shop.copy(
            schemaVersion = defaults.schemaVersion,
            title = if (shop.title == "&8Clan shop") defaults.title else shop.title,
            categories = shop.categories.mapValues { (id, category) ->
                val defaultCategory = defaults.categories[id]
                if (existingVersion < 8 && defaultCategory != null && (isGeneratedShopCategory(id, category) || isGeneratedV5ShopCategory(id, category))) {
                    category.copy(slot = defaultCategory.slot, name = defaultCategory.name, lore = defaultCategory.lore)
                } else {
                    category
                }
            },
            products = shop.products.mapValues { (id, product) ->
                val orderedProduct = if (existingVersion < 3 && product.sortOrder == 0) {
                    product.copy(sortOrder = product.slot)
                } else {
                    product
                }
                val replaceRewardSample = existingVersion < 7 && isGeneratedRewardSample(id, orderedProduct)
                val defaultProduct = defaults.products[id]
                val styledProduct = if (existingVersion < 5 && defaultProduct != null && isGeneratedShopProduct(id, orderedProduct)) {
                    orderedProduct.copy(
                        name = defaultProduct.name,
                        lore = defaultProduct.lore,
                        rarity = if (id in explicitProductRarities) orderedProduct.rarity else defaultProduct.rarity
                    )
                } else {
                    orderedProduct
                }
                val rewardedProduct = if (replaceRewardSample && defaultProduct != null) {
                    styledProduct.copy(rewards = defaultProduct.rewards)
                } else {
                    styledProduct
                }
                if (existingVersion < 8 && defaultProduct != null && isGeneratedV5ShopProduct(id, rewardedProduct)) {
                    rewardedProduct.copy(name = defaultProduct.name, lore = defaultProduct.lore)
                } else {
                    rewardedProduct
                }
            },
            display = migrateGeneratedShopDisplay(shop.display, defaults.display)
        )
    }

    fun explicitProductRarityIds(content: String?): Set<String> {
        if (content == null) return emptySet()
        val result = mutableSetOf<String>()
        var insideProducts = false
        var productsIndent = -1
        var productIndent: Int? = null
        var productId: String? = null

        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (!insideProducts) {
                if (trimmed == "products:") {
                    insideProducts = true
                    productsIndent = line.indexOfFirst { char -> !char.isWhitespace() }.coerceAtLeast(0)
                }
                return@forEach
            }
            if (line.isBlank() || trimmed.startsWith('#')) return@forEach

            val indentation = line.indexOfFirst { char -> !char.isWhitespace() }.takeIf { it >= 0 } ?: return@forEach
            if (indentation <= productsIndent) return result
            if (productIndent == null && trimmed.endsWith(':')) productIndent = indentation

            if (indentation == productIndent && trimmed.endsWith(':')) {
                productId = trimmed.removeSuffix(":").trim().trim('"', '\'')
            } else if (productIndent != null && indentation > productIndent && trimmed.startsWith("rarity:")) {
                productId?.let(result::add)
            }
        }
        return result
    }

    fun schemaVersion(content: String?, defaultVersion: Int): Int {
        if (content == null) return defaultVersion
        return Regex("(?m)^schemaVersion\\s*:\\s*(\\d+)")
            .find(content)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: 0
    }

    /** Upgrades only untouched v2 defaults while preserving administrator-owned display text. */
    private fun migrateGeneratedShopDisplay(
        current: ClanShopDisplayConfig,
        defaults: ClanShopDisplayConfig
    ): ClanShopDisplayConfig = current.copy(
        headerName = if (current.headerName in setOf("&#5EFD7D✦ Выберите категорию", "&#5EFD7D✦ Категории магазина", "&#FC7D37✦ &fМагазин клана")) defaults.headerName else current.headerName,
        headerLore = if (current.headerLore in listOf(
                listOf(
                    "&7Категории расположены рядом в верхнем ряду.",
                    "&7Ниже показаны доступные товары."
                ),
                listOf(
                    "",
                    "&#9EFC65 «Навигация»",
                    " &7- &fКатегории находятся в верхнем ряду.",
                    " &7- &fТовары выбранной категории — ниже.",
                    "",
                    "&#FC65DF «Управление»",
                    " &7- &fИспользуйте стрелки и сортировку внизу."
                ),
                listOf(
                    "",
                    "&#9EFC65 «Разделы каталога»",
                    " &7- &fВыберите нужную категорию в ряду ниже.",
                    " &7- &fОткрытый раздел отмечен зелёной галочкой.",
                    "",
                    "&#FC65DF «Каталог»",
                    " &7- &fТовары находятся в отдельной секции ниже."
                ),
                listOf("", "&8Текущий раздел", "&#5EFD7D● &f{category}", "", "&7Категории находятся в ряду ниже.", "&7Выберите раздел и откройте нужный товар.")
            )) defaults.headerLore else current.headerLore,
        backName = if (current.backName in setOf("&#FC3737← Вернуться в штаб", "&#FC3737← Вернуться в штаб клана", "&#FC7D37← &fВ штаб клана")) defaults.backName else current.backName,
        backLore = if (current.backLore in listOf(
                listOf("&7Вернуться в главное меню клана."),
                listOf("", "&#9EFC65 «Навигация»", " &7- &fЗакрыть магазин и вернуться", " &7- &fк основным функциям клана.", "", "&#FF8702➥ &fНажмите, &eЛКМ &fчтобы вернуться"),
                listOf("", "&7Закрыть магазин и вернуться", "&7к основным функциям клана.", "", "&#FF9F1CЛКМ &8— &fвернуться")
            )) defaults.backLore else current.backLore,
        balanceName = if (current.balanceName in setOf("&#FFD700⛁ Балансы", "&#FFD700⛁ Финансовый центр", "&#FFD166● &fДоступные счета")) defaults.balanceName else current.balanceName,
        balanceLore = if (current.balanceLore in listOf(
                listOf(
                    "&7Очки клана: &#FFD700{clan_points}",
                    "&7Vault: &#5EFD7D{vault_balance}",
                    "&7PlayerPoints: &#FC65DF{player_points_balance}"
                ),
                listOf(
                    "",
                    "&#9EFC65 «Баланс клана»",
                    " &7- &fОчки развития: &#FFD700{clan_points}",
                    "",
                    "&#FC65DF «Личные балансы»",
                    " &7- &fVault: &#5EFD7D{vault_balance}",
                    " &7- &fPlayerPoints: &#FC65DF{player_points_balance}",
                    "",
                    "&8Доступность зависит от плагинов сервера."
                ),
                listOf(
                    "",
                    "&#9EFC65 «Казна клана»",
                    " &7- &fКлановые очки: &#FFD700{clan_points}",
                    " &8Списываются с общего баланса клана.",
                    "",
                    "&#FC65DF «Личный счёт»",
                    " &7- &fМонеты: &#5EFD7D{vault_balance}",
                    " &7- &fБонусные очки: &#FC65DF{player_points_balance}",
                    " &8Списываются только с аккаунта игрока."
                ),
                listOf("", "&8Казна клана", " &7Клановые очки: &#FFD166{clan_points}", "", "&8Личный счёт", " &7Монеты: &#5EFD7D{vault_balance}", " &7Бонусные очки: &#C77DFF{player_points_balance}")
            )) defaults.balanceLore else current.balanceLore,
        productSlots = if (current.productSlots in listOf(
                listOf(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34),
                listOf(28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
            )) {
            defaults.productSlots
        } else {
            current.productSlots
        },
        fallbackProductName = if (current.fallbackProductName == "&#FFD700✦ Товар: &f{product}") {
            defaults.fallbackProductName
        } else {
            current.fallbackProductName
        },
        fallbackProductLore = if (current.fallbackProductLore == listOf("", "&#9EFC65 «Категория»", " &7- &f{category}", "", "&#9EFC65 «Количество»", " &7- &f{quantity} шт.", "", "&#FC65DF «Доступные способы оплаты»", "{payment_lines}", "", "&#FFD700 «Требования»", " &7- &fУровень клана: {required_level}", " &7- &fЛимит клана: {clan_limit}", " &7- &fЛимит сервера: {global_limit}", "", "&#FF8702➥ &fНажмите, чтобы выбрать счёт")) {
            defaults.fallbackProductLore
        } else {
            current.fallbackProductLore
        },
        previousPageName = if (current.previousPageName in setOf("&#FC7D37← Предыдущая страница", "&#FC7D37« &fПредыдущие товары", "&#FC7D37← &fНазад")) {
            defaults.previousPageName
        } else {
            current.previousPageName
        },
        nextPageName = if (current.nextPageName in setOf("&#FC7D37Следующая страница →", "&fСледующие товары &#FC7D37»", "&fВперёд &#FC7D37→")) {
            defaults.nextPageName
        } else {
            current.nextPageName
        },
        pageName = if (current.pageName in setOf("&#5EA9FD⌕ Страница {page}/{pages}", "&#FFD700✦ Навигация по каталогу", "&#FFD166✦ &fКаталог &8• &#5EA9FD{page}")) defaults.pageName else current.pageName,
        pageLore = if (current.pageLore in listOf(
                listOf("&7Категория: &f{category}", "&7Найдено товаров: &#5EA9FD{products}"),
                listOf("", "&#9EFC65 «Страница»", " &7- &fСейчас открыта: &#5EA9FD{page} &7из &#5EA9FD{pages}", " &7- &fПоказаны товары: &#5EFD7D{range}", "", "&#FC65DF «Раздел»", " &7- &f{category}", " &7- &fВсего товаров: &#5EA9FD{products}"),
                listOf("", "&7Раздел: &f{category}", "&7Товары: &#5EFD7D{range} &8из &f{products}", "&7Лист: &#5EA9FD{page} &8/ &f{pages}")
            )) defaults.pageLore else current.pageLore,
        sortName = if (current.sortName in setOf("&#FC65DF⇅ Сортировка", "&#FC65DF⇅ &fПорядок товаров")) defaults.sortName else current.sortName,
        sortLore = if (current.sortLore in listOf(
                listOf("", "&#9EFC65 «Текущий режим»", " &7- &f{sort}", "", "&#FF8702➥ &fНажмите, чтобы сменить"),
                listOf("", "&7Сейчас: &f{sort}", "&7Следующий: &#5EA9FD{next_sort}", "", "&#FF9F1CЛКМ &8— &fсменить порядок")
            )) {
            defaults.sortLore
        } else {
            current.sortLore
        },
        emptyCategoryName = if (current.emptyCategoryName == "&#FC3737✘ В категории пока пусто") defaults.emptyCategoryName else current.emptyCategoryName,
        emptyCategoryLore = if (current.emptyCategoryLore == listOf("", "&#9EFC65 «Категория»", " &7- &f{category}", "", "&7Здесь пока нет доступных товаров.", "&7Загляните сюда немного позже.")) defaults.emptyCategoryLore else current.emptyCategoryLore,
        paymentTitle = if (current.paymentTitle in setOf("&#FFD700« Выбор валюты »", "&#FFD166« Оплата покупки »")) defaults.paymentTitle else current.paymentTitle,
        paymentHeaderName = if (current.paymentHeaderName in setOf("&#FFD700✦ Выберите способ оплаты", "&#FFD700✦ Оформление покупки", "&#FFD166✦ &f{product_name}")) {
            defaults.paymentHeaderName
        } else {
            current.paymentHeaderName
        },
        paymentHeaderLore = if (current.paymentHeaderLore in listOf(
                listOf(
                    "&7Товар: &f{product_name}",
                    "&7Количество: &#5EA9FD{quantity}",
                    "",
                    "&7Нажмите на доступную валюту ниже."
                ),
                listOf(
                    "",
                    "&#9EFC65 «Ваш выбор»",
                    " &7- &fТовар: {product_name}",
                    " &7- &fКатегория: &#5EA9FD{category}",
                    " &7- &fКоличество: &#5EA9FD{quantity} шт.",
                    "",
                    "&#FC65DF «Способ оплаты»",
                    " &7- &fВыберите одну из доступных валют ниже."
                ),
                listOf(
                    "",
                    "&#9EFC65 «Ваш выбор»",
                    " &7- &fТовар: {product_name}",
                    " &7- &fКатегория: &#5EA9FD{category}",
                    " &7- &fКоличество: &#5EA9FD{quantity} шт.",
                    "",
                    "&#FC65DF «Откуда списать средства?»",
                    " &7- &fВыберите личный счёт или казну клана."
                ),
                listOf("&8{category}  •  {rarity}", "", "&7Количество: &#5EA9FD{quantity} шт.", "", "&fВыберите счёт, с которого будут списаны средства.")
            )) defaults.paymentHeaderLore else current.paymentHeaderLore,
        paymentOptionName = if (current.paymentOptionName == "{state_icon} &fОплатить: {currency}") {
            defaults.paymentOptionName
        } else {
            current.paymentOptionName
        },
        paymentOptionLore = if (current.paymentOptionLore in listOf(
                listOf("", "&#9EFC65 «Стоимость»", " &7- &fК оплате: &#FFD700{price}", "", "&#FC65DF «Ваш счёт»", " &7- &fСейчас: &#5EA9FD{balance}", " &7- &fПосле покупки: &#5EFD7D{remaining}", "", "&#FFD700 «Статус»", " &7- &f{state}", "", "{action}"),
                listOf("", "&#9EFC65 «Источник списания»", " &7- &f{source}", "", "&#FC65DF «Сумма покупки»", " &7- &fСтоимость: &#FFD700{price}", " &7- &fБаланс счёта: &#5EA9FD{balance}", " &7- &fОстанется: &#5EFD7D{remaining}", "", "&#FFD700 «Статус»", " &7- &f{state}", "", "{action}"),
                listOf("&8{source}", "", "&7Стоимость: &#FFD166{price}", "&7На счету: &#5EA9FD{balance}", "&7Останется: &#5EFD7D{remaining}", "", "{state}", "", "{action}")
            )) defaults.paymentOptionLore else current.paymentOptionLore,
        paymentBackName = if (current.paymentBackName in setOf("&#FC3737← Вернуться к товарам", "&#FC7D37← &fНазад к товарам")) defaults.paymentBackName else current.paymentBackName,
        paymentBackLore = if (current.paymentBackLore in listOf(
                listOf("", "&#9EFC65 «Навигация»", " &7- &fВернуться к выбранной категории", " &7- &fбез сброса страницы и сортировки.", "", "&#FF8702➥ &fНажмите, чтобы вернуться"),
                listOf("", "&7Вернуться в каталог без сброса", "&7категории, страницы и порядка.", "", "&#FF9F1CЛКМ &8— &fвернуться")
            )) defaults.paymentBackLore else current.paymentBackLore
    )

    private fun isGeneratedShopProduct(id: String, product: ClanShopProductConfig): Boolean {
        val generated = when (id) {
            "resource-cache" -> "&#5EFD7D✦ Набор ресурсов" to " &7- &fАлмазы: &#5EA9FD{quantity} шт."
            "combat-cache" -> "&#FC7D37⚔ Боевой запас" to " &7- &fЗолотые яблоки: &#5EA9FD{quantity} шт."
            "weapon-cache" -> "&#FC3737⚔ Оружейный комплект" to " &7- &fАлмазный меч: &#5EA9FD{quantity} шт."
            "legend-cache" -> "&#FC65DF✦ Легендарная награда" to " &7- &fЗвезда Незера: &#5EA9FD{quantity} шт."
            else -> return false
        }
        val extendedLore = listOf(
            "",
            "&#9EFC65 «Категория»",
            " &7- &f{category}",
            "",
            "&#9EFC65 «Содержимое»",
            generated.second,
            "",
            "&#FC65DF «Доступные валюты»",
            "{payment_lines}",
            "",
            "&#FFD700 «Ограничения»",
            " &7- &fУровень клана: {required_level}",
            " &7- &fЛимит клана: {clan_limit}",
            " &7- &fОбщий лимит: {global_limit}",
            "",
            "&#FF8702➥ &fНажмите, чтобы выбрать валюту"
        )
        val schemaTwoLore = listOf(
            "",
            "&#9EFC65 «Содержимое»",
            generated.second,
            "",
            "&#FC65DF «Оплата»",
            "{payment_lines}",
            "",
            "&#FFD700 «Ограничения»",
            " &7- &fУровень клана: {required_level}",
            " &7- &fЛимит клана: {clan_limit}",
            " &7- &fОбщий лимит: {global_limit}",
            "",
            "&#FF8702➥ &fНажмите, чтобы выбрать валюту"
        )
        return product.name == generated.first && product.lore in listOf(schemaTwoLore, extendedLore)
    }

    private fun isGeneratedRewardSample(
        id: String,
        product: ClanShopProductConfig
    ): Boolean {
        val oldReward = when (id) {
            "combat-cache" -> listOf(GiveItemAction("GOLDEN_APPLE", 8))
            "weapon-cache" -> listOf(GiveItemAction("DIAMOND_SWORD", 1))
            else -> return false
        }
        return product.rewards == oldReward && (isGeneratedV5ShopProduct(id, product) || isGeneratedShopProduct(id, product))
    }

    private fun isGeneratedV5ShopProduct(id: String, product: ClanShopProductConfig): Boolean {
        val generated = when (id) {
            "resource-cache" -> Triple("&#5EFD7DАлмазный запас", "&fКомплект из &#5EA9FD{quantity} алмазов&f для участника клана.", false)
            "combat-cache" -> Triple("&#FFD166Боевой паёк", "&fНабор из &#FFD166{quantity} золотых яблок&f для боя.", false)
            "weapon-cache" -> Triple("&#FF6B6BАлмазный меч", "&fНадёжное оружие для клановых сражений.", false)
            "legend-cache" -> Triple("&#C77DFFЗвезда Незера", "&fОсобая награда для развитых кланов.", true)
            else -> return false
        }
        val requirements = mutableListOf(
            "&#A8DADCУсловия",
            " &7Уровень клана: &f{required_level}",
            " &7Лимит клана: &f{clan_limit}"
        )
        if (generated.third) requirements += " &7Лимит сервера: &f{global_limit}"
        val expectedLore = listOf(
            "&8{category}  •  {rarity}",
            "",
            generated.second,
            "",
            "&#FFD166Стоимость",
            "{payment_lines}",
            ""
        ) + requirements + listOf("", "&#FF9F1CЛКМ &8— &fвыбрать способ оплаты")
        return product.name == generated.first && product.lore == expectedLore
    }

    private fun isGeneratedShopCategory(id: String, category: ClanShopCategoryConfig): Boolean {
        val generated = when (id) {
            "all" -> Triple(10, "&#5EA9FD⌕ Все", listOf("&7Показать все товары магазина.", "", "&#FF8702➥ &fНажмите, чтобы открыть"))
            "food" -> Triple(11, "&#FFD700✦ Еда", listOf("&7Боевые припасы и восстановление.", "", "&#FF8702➥ &fНажмите, чтобы открыть"))
            "weapons" -> Triple(12, "&#FC3737⚔ Оружие", listOf("&7Снаряжение для сражений клана.", "", "&#FF8702➥ &fНажмите, чтобы открыть"))
            "resources" -> Triple(13, "&#5EFD7D✦ Ресурсы", listOf("&7Материалы для развития и строительства.", "", "&#FF8702➥ &fНажмите, чтобы открыть"))
            "rare" -> Triple(14, "&#FC65DF✦ Редкое", listOf("&7Ценные и эксклюзивные награды.", "", "&#FF8702➥ &fНажмите, чтобы открыть"))
            else -> return false
        }
        return category.slot == generated.first && category.name == generated.second && category.lore == generated.third
    }

    private fun isGeneratedV5ShopCategory(id: String, category: ClanShopCategoryConfig): Boolean {
        val generated = when (id) {
            "all" -> Triple(11, "&#5EA9FDВсе товары", listOf("&8Полный каталог магазина"))
            "food" -> Triple(12, "&#FFD166Еда", listOf("&8Припасы и восстановление"))
            "weapons" -> Triple(13, "&#FF6B6BОружие", listOf("&8Снаряжение для сражений"))
            "resources" -> Triple(14, "&#5EFD7DРесурсы", listOf("&8Материалы для развития"))
            "rare" -> Triple(15, "&#C77DFFРедкое", listOf("&8Особые награды клана"))
            else -> return false
        }
        return category.slot == generated.first && category.name == generated.second && category.lore == generated.third
    }

}
