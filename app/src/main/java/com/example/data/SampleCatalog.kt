package com.example.data

import com.example.model.MenuCategory
import com.example.model.MenuProduct

object CafeBusinessInfo {
    const val NAME = "Café Antonio Luna"
    const val TAGLINE = "Specialty Coffee & Comfort Classics in Pasig"
    const val ADDRESS = "15 Justice Ramon Jabson, San Nicolas, Pasig City, Metro Manila 1600, Philippines"
    const val MAPS_URL = "https://maps.app.goo.gl/JEPDSxQgnB5CVgyJ8"
    const val MAPS_COORDS_QUERY = "geo:14.5586,121.0762?q=Cafe+Antonio+Luna+15+Justice+Ramon+Jabson+Pasig"
    val HOURS_LIST = listOf(
        "Monday – Thursday" to "9:00 AM – 12:00 AM",
        "Friday – Saturday" to "9:00 AM – 2:00 AM",
        "Sunday" to "9:00 AM – 9:00 PM"
    )
    const val HOURS_DISCLAIMER =
        "Operating hours reflect public directory listings and may vary on holidays."
    const val CONTACT_NOTE =
        "No public telephone number is listed on Google Maps. Visit us at 15 Justice Ramon Jabson, Pasig City, or reach out via Café Antonio Luna's official social media page."
    const val SAMPLE_MENU_DISCLOSURE =
        "Menu items and PHP prices marked 'Sample' are demonstration placeholders and can be updated anytime in the Admin Dashboard."
}

object SampleCatalog {
    val defaultCategories = listOf(
        MenuCategory(id = "cat_coffee", name = "Coffee", sortOrder = 1L, active = true),
        MenuCategory(id = "cat_non_coffee", name = "Non-Coffee", sortOrder = 2L, active = true),
        MenuCategory(id = "cat_tea", name = "Tea", sortOrder = 3L, active = true),
        MenuCategory(id = "cat_pastries", name = "Pastries", sortOrder = 4L, active = true)
    )

    val defaultProducts = listOf(
        MenuProduct(
            id = "antonio_latte",
            name = "Antonio Latte",
            description = "Signature house espresso pulled over velvety steamed milk with subtle toasted muscovado notes.",
            categoryId = "cat_coffee",
            categoryName = "Coffee",
            pricePhp = 155.0,
            imageKey = "sample_coffee_latte",
            featured = true,
            bestSeller = true,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "pista_latte",
            name = "Pista Latte",
            description = "Roasted pistachio cream folded into double-shot specialty espresso and chilled or steamed milk.",
            categoryId = "cat_coffee",
            categoryName = "Coffee",
            pricePhp = 175.0,
            imageKey = "sample_coffee_latte",
            featured = true,
            bestSeller = true,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "spanish_oat_latte",
            name = "Spanish Oat Latte",
            description = "Balanced condensed milk sweetness with creamy oat milk and a bold medium-roast espresso base.",
            categoryId = "cat_coffee",
            categoryName = "Coffee",
            pricePhp = 165.0,
            imageKey = "hero_cafe_banner",
            featured = false,
            bestSeller = true,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "dark_mocha_espresso",
            name = "Dark Mocha Espresso",
            description = "Rich Belgian-style dark chocolate melted into freshly extracted espresso with cocoa crema.",
            categoryId = "cat_coffee",
            categoryName = "Coffee",
            pricePhp = 160.0,
            imageKey = "sample_coffee_latte",
            featured = false,
            bestSeller = false,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "pasig_americano",
            name = "Pasig House Americano",
            description = "Clean, aromatic double espresso over purified hot or iced water with caramel-nut finish.",
            categoryId = "cat_coffee",
            categoryName = "Coffee",
            pricePhp = 120.0,
            imageKey = "hero_cafe_banner",
            featured = false,
            bestSeller = false,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "tablea_chocolate",
            name = "Heirloom Tablea Tsokolate",
            description = "Traditional roasted Philippine cacao whisked with silky milk for a comforting non-coffee classic.",
            categoryId = "cat_non_coffee",
            categoryName = "Non-Coffee",
            pricePhp = 150.0,
            imageKey = "sample_coffee_latte",
            featured = true,
            bestSeller = false,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "berry_milk_cloud",
            name = "Strawberry Milk Cloud",
            description = "House strawberry reduction layered with chilled milk and light whipped vanilla cream.",
            categoryId = "cat_non_coffee",
            categoryName = "Non-Coffee",
            pricePhp = 165.0,
            imageKey = "sample_matcha_tea",
            featured = false,
            bestSeller = false,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Iced")
        ),
        MenuProduct(
            id = "ceremonial_matcha",
            name = "Ceremonial Matcha Latte",
            description = "Stone-ground first-harvest matcha whisked smooth over fresh milk and subtle cane sweetness.",
            categoryId = "cat_tea",
            categoryName = "Tea",
            pricePhp = 170.0,
            imageKey = "sample_matcha_tea",
            featured = true,
            bestSeller = true,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "honey_earl_grey",
            name = "Wildflower Honey Earl Grey",
            description = "Fragrant bergamot black tea steeped to order with soothing local wildflower honey.",
            categoryId = "cat_tea",
            categoryName = "Tea",
            pricePhp = 135.0,
            imageKey = "sample_matcha_tea",
            featured = false,
            bestSeller = false,
            available = true,
            sampleData = true,
            customizableDrink = true,
            allowedTemps = listOf("Hot", "Iced")
        ),
        MenuProduct(
            id = "butter_croissant",
            name = "Golden Butter Croissant",
            description = "Flaky, honeycomb-layered French butter croissant baked fresh every morning.",
            categoryId = "cat_pastries",
            categoryName = "Pastries",
            pricePhp = 115.0,
            imageKey = "sample_pastry_croissant",
            featured = false,
            bestSeller = true,
            available = true,
            sampleData = true,
            customizableDrink = false,
            allowedTemps = emptyList()
        ),
        MenuProduct(
            id = "pain_au_chocolat",
            name = "Pain au Chocolat",
            description = "Crisp laminated pastry folded around twin batons of bittersweet dark chocolate.",
            categoryId = "cat_pastries",
            categoryName = "Pastries",
            pricePhp = 130.0,
            imageKey = "sample_pastry_croissant",
            featured = true,
            bestSeller = false,
            available = true,
            sampleData = true,
            customizableDrink = false,
            allowedTemps = emptyList()
        )
    )
}
