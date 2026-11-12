
package com.pickmeal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickmeal.app.ui.theme.PICKMEALTheme
import androidx.compose.ui.platform.LocalContext

private val BG = Color(0xFF101827)
private val PANEL = Color(0xFF1D293B)
private val CARD = Color(0xFF27364B)
private val GREEN = Color(0xFF32C48D)
private val WHITE = Color(0xFFF5F7FA)
private val MUTED = Color(0xFFAFBED0)

data class Food(
    val name: String,
    val category: String,
    val emoji: String,
    val price: Int,
    val restaurant: String,
    val rating: Double,
    val reviews: Int,
    val detour: Int
)

// 현재는 시연용 가상 음식점 데이터

private fun menu(
    name: String,
    category: String,
    emoji: String,
    price: Int
): Food {
    return Food(
        name = name,
        category = category,
        emoji = emoji,
        price = price,
        restaurant = "",
        rating = 0.0,
        reviews = 0,
        detour = 0
    )
}

private val foods = listOf(
    // 한식 9종
    menu("제육덮밥", "한식", "🍚", 9500),
    menu("순두부찌개", "한식", "🥘", 9000),
    menu("김치찌개", "한식", "🍲", 9000),
    menu("된장찌개", "한식", "🍲", 9000),
    menu("비빔밥", "한식", "🥗", 10000),
    menu("불고기", "한식", "🥩", 13000),
    menu("삼겹살", "한식", "🥓", 16000),
    menu("국밥", "한식", "🍚", 10000),
    menu("칼국수", "한식", "🍜", 9500),

    // 일식 5종
    menu("돈가스", "일식", "🍱", 11000),
    menu("초밥", "일식", "🍣", 15000),
    menu("라멘", "일식", "🍜", 11000),
    menu("우동", "일식", "🍜", 9000),
    menu("규동", "일식", "🍚", 11000),

    // 중식 5종
    menu("짜장면", "중식", "🍜", 8000),
    menu("짬뽕", "중식", "🍜", 10000),
    menu("마라탕", "중식", "🍲", 13000),
    menu("볶음밥", "중식", "🍚", 9000),
    menu("마파두부", "중식", "🥘", 11000),

    // 양식 5종
    menu("파스타", "양식", "🍝", 13000),
    menu("피자", "양식", "🍕", 15000),
    menu("햄버거", "양식", "🍔", 10000),
    menu("스테이크", "양식", "🥩", 25000),
    menu("리조또", "양식", "🍚", 14000),

    // 아시아 음식 4종
    menu("쌀국수", "아시아", "🍜", 10500),
    menu("팟타이", "아시아", "🍝", 12000),
    menu("카레", "아시아", "🍛", 11000),
    menu("반미", "아시아", "🥖", 9000),

    // 분식 4종
    menu("김밥", "분식", "🍙", 6000),
    menu("떡볶이", "분식", "🍢", 8000),
    menu("라볶이", "분식", "🍜", 8500),
    menu("만두", "분식", "🥟", 8000)
)


private val spicyFoodNames = setOf(
    "제육덮밥",
    "순두부찌개",
    "김치찌개",
    "짬뽕",
    "마라탕",
    "마파두부",
    "떡볶이",
    "라볶이"
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "pickmeal_settings",
            MODE_PRIVATE
        )

        setContent {
            PICKMEALTheme {

                // 앱 실행 시 위치 권한 확인 및 자동 수신
                AutoVehicleLocation()

                var setupComplete by remember {
                    mutableStateOf(
                        prefs.getBoolean(
                            "setup_complete",
                            false
                        )
                    )
                }

                var editingPreferences by remember {
                    mutableStateOf(false)
                }

                if (setupComplete && !editingPreferences) {
                    PickMealApp(
                        onEditPreferences = {
                            editingPreferences = true
                        }
                    )
                } else {
                    PickMealOnboarding(
                        initialFavoriteFoods = prefs.getStringSet(
                            "favorite_foods",
                            emptySet()
                        )?.toSet() ?: emptySet(),

                        initialBudget = prefs.getInt(
                            "budget",
                            10000
                        ),

                        onCancel = if (setupComplete) {
                            { editingPreferences = false }
                        } else {
                            null
                        },

                        onComplete = { favoriteFoods, budget ->
                            prefs.edit()
                                .putStringSet(
                                    "favorite_foods",
                                    favoriteFoods
                                )
                                .putInt("budget", budget)
                                .remove("allergies")
                                .putBoolean("setup_complete", true)
                                .apply()

                            setupComplete = true
                            editingPreferences = false
                        }
                    )
                }
            }
        }
    }
}

    @Composable
    fun PickMealApp(
        onEditPreferences: () -> Unit
    ) {

        // Android 앱 환경에 접근
        val context = LocalContext.current

        // v0.2에서 저장한 설정 파일 열기
        val prefs = remember(context) {
            context.getSharedPreferences(
                "pickmeal_settings",
                android.content.Context.MODE_PRIVATE
            )
        }

        // 선호하는 음식 종류
        val favoriteFoods = remember {
            prefs.getStringSet(
                "favorite_foods",
                emptySet()
            )?.toSet() ?: emptySet()
        }

        // 한 끼 식사 예산
        val userBudget = remember {
            prefs.getInt("budget", 10000)
        }

        // 알레르기 설정 (이 단계에서는 확인용)
        val userAllergies = remember {
            prefs.getStringSet(
                "allergies",
                emptySet()
            )?.toSet() ?: emptySet()
        }

        var mode by remember { mutableStateOf("퇴근길") }
        var page by remember { mutableStateOf("HOME") }
        var index by remember { mutableIntStateOf(0) }
        var recommendationSeed by remember {
            mutableIntStateOf(kotlin.random.Random.nextInt())
        }
        var rejectOpen by remember { mutableStateOf(false) }
        var message by remember {
            mutableStateOf("오늘 저녁은 제가 골라드릴게요!")
        }

        // 거절한 메뉴는 현재 실행 중에 다시 추천하지 않음
        val rejected = remember {
            mutableStateListOf<String>()
        }

        // 오늘만 제외할 음식 종류
        var excludeSpicy by remember { mutableStateOf(false) }
        var priceCeiling by remember {
            mutableStateOf<Int?>(null)
        }

        // 1. 조건에 맞지 않는 메뉴 제외
        // 개인화 추천 점수 계산
        fun calculateScore(food: Food): Int {
            var score = 100

            // 1. 선호 음식 종류면 가산점
            val isFavorite =
                food.category in favoriteFoods ||
                        (
                                food.category == "아시아" &&
                                        "아시아 음식" in favoriteFoods
                                )

            if (isFavorite) {
                score += 30
            }

            // 2. 평소 예산을 초과하면 점수 차감
            // 1,000원 초과할 때마다 5점 감점
            val overBudget =
                (food.price - userBudget).coerceAtLeast(0)

            val pricePenalty = (overBudget / 1000) * 5
            score -= pricePenalty

            return score
        }

        // 거절한 메뉴와 임시 가격 상한을 적용
        val filteredFoods = foods.filter { food ->
            val notRejected = food.name !in rejected

            // "매워서" 거절한 경우의 임시 분류
            val spicyAllowed =
                !excludeSpicy || food.name !in spicyFoodNames

            // 가격 거절 후에는 이전 거절 가격보다 저렴해야 함
            val priceAllowed =
                priceCeiling == null ||
                        food.price <= priceCeiling!!

            notRejected && spicyAllowed && priceAllowed
        }

        // 점수가 높은 메뉴부터 추천
        val availableFoods = filteredFoods.sortedWith(
            compareByDescending<Food> { food ->
                calculateScore(food)
            }.thenBy { food ->
                food.name.hashCode() xor recommendationSeed
            }
        )


        val currentFood = availableFoods.getOrNull(
            index.coerceIn(
                0,
                (availableFoods.size - 1).coerceAtLeast(0)
            )
        )


        fun nextRecommendation(reason: String) {
            // 현재 추천받은 음식의 가격 기억
            val rejectedPrice = currentFood?.price

            // 방금 거절한 음식은 현재 추천 목록에서 제외
            currentFood?.let {
                rejected.add(it.name)
            }

            index = 0
            rejectOpen = false
            page = "RECOMMEND"

            message = when (reason) {
                "SPICY" -> {
                    excludeSpicy = true
                    "오늘은 매운 음식을 제외할게요."
                }

                "PRICE" -> {
                    if (rejectedPrice != null) {
                        // 이전에 거절한 가격보다 더 낮은 상한 유지
                        val newCeiling = rejectedPrice - 1

                        priceCeiling = priceCeiling?.let {
                            minOf(it, newCeiling)
                        } ?: newCeiling
                    }

                    "방금 메뉴보다 저렴한 음식을 찾아볼게요."
                }

                "RECENT" -> {
                    "최근에 드셨군요. 다른 메뉴를 추천할게요."
                }

                else -> {
                    "알겠어요. 다른 메뉴를 골라볼게요."
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BG)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 왼쪽 고정 메뉴 영역
            Column(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
                    .background(PANEL, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "PICKMEAL",
                    color = GREEN,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "오늘 뭐 먹지?",
                    color = WHITE,
                    fontSize = 16.sp
                )

                HorizontalDivider(color = CARD)

                Text(
                    text = "추천 모드",
                    color = MUTED,
                    fontSize = 14.sp
                )

                ModeButton(
                    "🏠 퇴근길",
                    mode == "퇴근길"
                ) {
                    mode = "퇴근길"
                    page = "HOME"
                }

                ModeButton(
                    "🧭 낯선 지역",
                    mode == "낯선 지역"
                ) {
                    mode = "낯선 지역"
                    page = "HOME"
                }

                ModeButton(
                    "🍽 일반 추천",
                    mode == "일반"
                ) {
                    mode = "일반"
                    page = "HOME"
                }

                Spacer(Modifier.height(12.dp))

                HorizontalDivider(
                    color = CARD
                )

                ModeButton(
                    "⚙️ 음식 취향·예산 변경",
                    false
                ) {
                    onEditPreferences()
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = "● PICKMEAL",
                    color = GREEN,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "차량 위치 기반 음식점 검색\nPleos 내비게이션 연동",
                    color = MUTED,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // 오른쪽 메인 콘텐츠
            Column(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .background(PANEL, RoundedCornerShape(18.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (mode) {
                        "퇴근길" -> "퇴근길 모드"
                        "낯선 지역" -> "낯선 지역 모드"
                        else -> "일반 추천 모드"
                    },
                    color = GREEN,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                when (page) {
                    "HOME" -> {
                        HomeScreen(
                            mode = mode,
                            onStart = {
                                rejected.clear()
                                recommendationSeed = kotlin.random.Random.nextInt()
                                excludeSpicy = false
                                priceCeiling = null
                                index = 0
                                rejectOpen = false
                                message = "오늘의 메뉴를 골라봤어요!"
                                page = "RECOMMEND"
                            }
                        )
                    }

                    "RECOMMEND" -> {
                        Text(
                            text = message,
                            color = MUTED,
                            fontSize = 15.sp
                        )

                        if (currentFood == null) {
                            Text(
                                text = "조건에 맞는 메뉴를 모두 확인했어요.",
                                color = WHITE,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "가격 조건이나 제외한 메뉴를 조정하면 " +
                                        "다른 음식을 추천받을 수 있어요.",
                                color = MUTED,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )

                            if (priceCeiling != null) {
                                Text(
                                    text = "현재 가격 제한: " +
                                            "%,d원 미만".format(priceCeiling!! + 1),
                                    color = GREEN,
                                    fontSize = 15.sp
                                )

                                SecondaryButton("가격 제한 해제하기") {
                                    priceCeiling = null
                                    index = 0
                                    rejectOpen = false
                                    message = "가격 제한을 풀고 다시 찾아볼게요."
                                }
                            }

                            ActionButton("처음부터 다시 추천받기") {
                                rejected.clear()
                                excludeSpicy = false
                                priceCeiling = null
                                recommendationSeed =
                                    kotlin.random.Random.nextInt()
                                index = 0
                                rejectOpen = false
                                message = "새로운 메뉴를 추천할게요."
                            }
                        } else {
                            FoodCard(currentFood)

                            if (!rejectOpen) {
                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        ActionButton("좋아요! 선택") {
                                            page = "RESTAURANT"
                                        }
                                    }

                                    Box(Modifier.weight(1f)) {
                                        SecondaryButton("다른 메뉴") {
                                            rejectOpen = true
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "어떤 점이 별로인가요?",
                                    color = WHITE,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        SecondaryButton("매워서") {
                                            nextRecommendation("SPICY")
                                        }
                                    }
                                    Box(Modifier.weight(1f)) {
                                        SecondaryButton("비싸서") {
                                            nextRecommendation("PRICE")
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        SecondaryButton("최근에 먹어서") {
                                            nextRecommendation("RECENT")
                                        }
                                    }
                                    Box(Modifier.weight(1f)) {
                                        SecondaryButton("그냥 별로") {
                                            nextRecommendation("OTHER")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "RESTAURANT" -> {
                        currentFood?.let { food ->
                            KakaoRestaurantScreen(
                                menuName = food.name,
                                onBack = {
                                    page = "RECOMMEND"
                                }
                            )
                        }
                    }

                    "COMPLETE" -> {
                        Text(
                            text = "✅ 식당 선택 완료!",
                            color = GREEN,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = currentFood?.restaurant ?: "",
                            color = WHITE,
                            fontSize = 20.sp
                        )

                        Text(
                            text = "다음 단계에서 Pleos NaviHelper SDK를 연결해 경유지를 실제로 등록할 예정이에요.",
                            color = MUTED,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )

                        ActionButton("다시 추천받기") {
                            page = "HOME"
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(
        mode: String,
        onStart: () -> Unit
    ) {
        Text(
            text = when (mode) {
                "퇴근길" -> "오늘도 수고했어요! 👋"
                "낯선 지역" -> "새로운 곳에 오셨네요! 🧭"
                else -> "오늘은 어떤 메뉴가 좋을까요? 🍽"
            },
            color = WHITE,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = when (mode) {
                "퇴근길" ->
                    "집으로 가는 길에 들르기 좋은 식당을 찾아드릴게요."

                "낯선 지역" ->
                    "이 지역에서 맛있는 한 끼를 찾아드릴게요."

                else ->
                    "먹고 싶은 게 없어도 괜찮아요. 제가 골라드릴게요."
            },
            color = MUTED,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARD, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("🍽️", fontSize = 56.sp)

                Text(
                    text = "오늘 뭐 먹지?",
                    color = WHITE,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "메뉴부터 식당까지 한 번에!",
                    color = MUTED,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        ActionButton("오늘의 메뉴 추천받기", onStart)
    }

    @Composable
    fun FoodCard(food: Food) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARD, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = food.emoji,
                fontSize = 48.sp
            )

            Text(
                text = food.name,
                color = WHITE,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = food.category + " · 예상 가격 " +
                        "%,d원".format(food.price),
                color = MUTED,
                fontSize = 15.sp
            )

            Text(
                text = "이 메뉴는 어떠세요?",
                color = GREEN,
                fontSize = 16.sp
            )
        }
    }

    @Composable
    fun RestaurantCard(food: Food) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CARD, RoundedCornerShape(16.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = food.restaurant,
                color = WHITE,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "⭐ ${food.rating}  ·  리뷰 ${food.reviews}개",
                color = Color(0xFFFFD375),
                fontSize = 17.sp
            )

            HorizontalDivider(color = MUTED.copy(alpha = 0.3f))

            InfoRow("추천 메뉴", food.name)
            InfoRow("예상 가격", "%,d원".format(food.price))
            InfoRow("추가 우회시간", "+${food.detour}분")

            Text(
                text = "※ 시연용 가상 식당 및 평가 정보",
                color = MUTED,
                fontSize = 12.sp
            )
        }
    }

    @Composable
    fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MUTED, fontSize = 15.sp)
            Text(
                value,
                color = WHITE,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    @Composable
    fun ModeButton(
        title: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (selected) GREEN.copy(alpha = 0.18f) else CARD,
                    RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (selected) GREEN else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Text(
                text = title,
                color = if (selected) GREEN else WHITE,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    fun ActionButton(
        title: String,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GREEN,
                contentColor = BG
            )
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun SecondaryButton(
        title: String,
        onClick: () -> Unit
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = WHITE
            )
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
