
package com.pickmeal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val restaurantGreen = Color(0xFF32C48D)
private val restaurantWhite = Color(0xFFF5F7FA)
private val restaurantMuted = Color(0xFFAFBED0)
private val restaurantCard = Color(0xFF27364B)

// 실제 카카오 API에서 받은 장소 정보
data class KakaoRestaurant(
    val name: String,
    val address: String,
    val longitude: String,
    val latitude: String,
    val kakaoUrl: String,
    val category: String,
    val distanceMeters: Int?
)

// 추천 순위 계산에 사용하는 데이터
private data class RankedRestaurant(
    val restaurant: KakaoRestaurant,
    val score: Int,
    val reason: String
)

// 메뉴에 어울리는 업종을 판단하는 간단한 규칙
private fun rankRestaurant(
    food: KakaoRestaurant,
    menuName: String
): RankedRestaurant {

    val name = food.name.replace(" ", "").lowercase()
    val category = food.category.replace(" ", "").lowercase()
    val menu = menuName.replace(" ", "").lowercase()

    val keywords = when (menu) {
        // 한식
        "제육덮밥" ->
            listOf("제육", "제육볶음", "제육덮밥")
        "순두부찌개" ->
            listOf("순두부", "두부찌개")
        "김치찌개" ->
            listOf("김치찌개", "김치찜")
        "된장찌개" ->
            listOf("된장찌개", "된장")
        "비빔밥" ->
            listOf("비빔밥")
        "불고기" ->
            listOf("불고기")
        "삼겹살" ->
            listOf("삼겹살", "돼지고기구이", "고깃집")
        "국밥" ->
            listOf("국밥", "돼지국밥", "순대국", "순댓국")
        "칼국수" ->
            listOf("칼국수", "손칼국수")

        // 일식
        "돈가스" ->
            listOf("돈가스", "돈까스", "카츠")
        "초밥" ->
            listOf("초밥", "스시")
        "라멘" ->
            listOf("라멘", "라면전문")
        "우동" ->
            listOf("우동")
        "규동" ->
            listOf("규동", "소고기덮밥")

        // 중식
        "짜장면" ->
            listOf("짜장", "자장면", "중화요리")
        "짬뽕" ->
            listOf("짬뽕", "교동짬뽕", "중화요리")
        "마라탕" ->
            listOf("마라탕", "마라")
        "볶음밥" ->
            listOf("볶음밥")
        "마파두부" ->
            listOf("마파두부", "사천요리")

        // 양식
        "파스타" ->
            listOf("파스타", "이탈리안", "이탈리아")
        "피자" ->
            listOf("피자", "피자리아")
        "햄버거" ->
            listOf("햄버거", "버거")
        "스테이크" ->
            listOf("스테이크", "스테이크하우스")
        "리조또" ->
            listOf("리조또", "리소토", "이탈리안")

        // 아시아
        "쌀국수" ->
            listOf("쌀국수", "베트남음식", "베트남")
        "팟타이" ->
            listOf("팟타이", "태국음식", "타이푸드")
        "카레" ->
            listOf("카레", "커리", "인도음식")
        "반미" ->
            listOf("반미", "베트남샌드위치")

        // 분식
        "김밥" ->
            listOf("김밥", "김밥집")
        "떡볶이" ->
            listOf("떡볶이", "즉석떡볶이")
        "라볶이" ->
            listOf("라볶이", "떡볶이")
        "만두" ->
            listOf("만두", "딤섬")

        else -> listOf(menu)
    }

    val nameMatched = keywords.any { name.contains(it) }
    val categoryMatched = keywords.any {
        category.contains(it)
    }

    var score = 0
    var reason = "검색어와 관련된 음식점"

    when {
        nameMatched -> {
            score += 100
            reason = "식당 이름이 메뉴와 관련되어 있어요"
        }

        categoryMatched -> {
            score += 75
            reason = "업종 분류가 메뉴와 관련되어 있어요"
        }

        else -> {
            score += 20
        }
    }

    // 돈가스 선택 시 도시락 전문점은 후순위
    if (
        (menu.contains("돈가스") || menu.contains("돈까스")) &&
        (name.contains("도시락") || category.contains("도시락"))
    ) {
        score -= 90
        reason = "도시락 업종으로 분류되어 있어요"
    }

    // 가까운 식당에는 소폭 가산점
    val distance = food.distanceMeters

    if (distance != null) {
        val distanceBonus =
            ((3000 - distance).coerceAtLeast(0) / 100)

        score += distanceBonus
    }

    return RankedRestaurant(food, score, reason)
}

// 서버 통신은 메인 스레드 바깥에서 실행
private suspend fun searchKakaoRestaurants(
    keyword: String,
    latitude: Double,
    longitude: Double
): List<KakaoRestaurant> = withContext(Dispatchers.IO) {

    val apiKey = BuildConfig.KAKAO_REST_API_KEY

    require(apiKey.isNotBlank()) {
        "카카오 REST API 키가 없습니다."
    }

    val encodedQuery = URLEncoder.encode(keyword, "UTF-8")

    val apiUrl =
        "https://dapi.kakao.com/v2/local/search/keyword.json" +
                "?query=$encodedQuery" +
                "&category_group_code=FD6" +
                "&x=$longitude" +
                "&y=$latitude" +
                "&radius=3000" +
                "&sort=distance" +
                "&size=15"

    val connection =
        URL(apiUrl).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "Authorization",
            "KakaoAK $apiKey"
        )
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val statusCode = connection.responseCode

        if (statusCode != 200) {
            throw Exception("카카오 API 오류: HTTP $statusCode")
        }

        val response = connection.inputStream
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val documents = JSONObject(response)
            .getJSONArray("documents")

        val result = mutableListOf<KakaoRestaurant>()

        for (i in 0 until documents.length()) {
            val place = documents.getJSONObject(i)

            val roadAddress =
                place.optString("road_address_name")

            val address = roadAddress.ifBlank {
                place.optString("address_name")
            }

            result.add(
                KakaoRestaurant(
                    name = place.optString("place_name"),
                    address = address,
                    longitude = place.optString("x"),
                    latitude = place.optString("y"),
                    kakaoUrl = place.optString("place_url"),
                    category = place.optString("category_name"),
                    distanceMeters = place.optString("distance")
                        .toIntOrNull()
                )
            )
        }

        result
    } finally {
        connection.disconnect()
    }
}

@Composable
fun KakaoRestaurantScreen(
    menuName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val locationPrefs = remember(context) {
        context.getSharedPreferences(
            "pickmeal_location",
            android.content.Context.MODE_PRIVATE
        )
    }

    // 마지막으로 저장한 차량 위치 사용

    var restaurants by remember(menuName) {
        mutableStateOf<List<KakaoRestaurant>>(emptyList())
    }

    var loading by remember(menuName) {
        mutableStateOf(true)
    }

    var errorMessage by remember(menuName) {
        mutableStateOf<String?>(null)
    }

    var currentIndex by remember(menuName) {
        mutableIntStateOf(0)
    }

    var retryCount by remember(menuName) {
        mutableIntStateOf(0)
    }

    var selected by remember(menuName) {
        mutableStateOf(false)
    }

    var navigationStatus by remember(menuName, currentIndex) {
        mutableStateOf("")
    }

    // 메뉴나 좌표가 변경되면 다시 검색
    LaunchedEffect(menuName, retryCount) {
        loading = true
        errorMessage = null
        restaurants = emptyList()
        currentIndex = 0
        selected = false

        // 검색할 때마다 마지막 저장 위치 다시 읽기
        val latitude = locationPrefs
            .getString("latitude", null)
            ?.toDoubleOrNull()

        val longitude = locationPrefs
            .getString("longitude", null)
            ?.toDoubleOrNull()

        if (
            latitude == null ||
            longitude == null ||
            latitude !in -90.0..90.0 ||
            longitude !in -180.0..180.0
        ) {
            errorMessage =
                "차량 위치를 확인할 수 없어요.\n" +
                        "위치 정보를 확인한 후 다시 검색해 주세요."

            loading = false
        } else {
            try {
                restaurants = searchKakaoRestaurants(
                    keyword = menuName,
                    latitude = latitude,
                    longitude = longitude
                )

            } catch (e: Exception) {
                errorMessage = when {
                    e is kotlinx.coroutines.CancellationException ->
                        throw e

                    e.message?.contains("HTTP 401") == true ->
                        "카카오 API 인증에 실패했어요.\n" +
                                "API 키 설정을 확인해 주세요."

                    e.message?.contains("HTTP 403") == true ->
                        "카카오 API 접근이 거부되었어요.\n" +
                                "API 사용 권한을 확인해 주세요."

                    e.message?.contains("HTTP 429") == true ->
                        "요청이 너무 많아요.\n" +
                                "잠시 후 다시 검색해 주세요."

                    e is java.io.IOException ->
                        "인터넷 연결이 불안정하거나\n" +
                                "서버 응답이 지연되고 있어요."

                    else ->
                        "음식점 검색 중 문제가 발생했어요.\n" +
                                "잠시 후 다시 시도해 주세요."
                }
            } finally {
                loading = false
            }
        }
    }

    // 각 식당에 점수를 부여하고 높은 순서대로 정렬
    val rankedRestaurants = remember(restaurants, menuName) {
        restaurants
            .map { rankRestaurant(it, menuName) }
            .sortedWith(
                compareByDescending<RankedRestaurant> {
                    it.score
                }.thenBy {
                    it.restaurant.distanceMeters ?: Int.MAX_VALUE
                }
            )
    }

    val current = rankedRestaurants.getOrNull(currentIndex)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PICKMEAL · 식당 추천",
            color = restaurantGreen,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$menuName 먹으러 어디 갈까요?",
            color = restaurantWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "차량 마지막 수신 위치 기준 · 반경 3km",
            color = restaurantMuted,
            fontSize = 14.sp
        )

        when {
            loading -> {
                CircularProgressIndicator(
                    color = restaurantGreen
                )
                Text("음식점 검색 중...", color = restaurantWhite)
            }

            errorMessage != null -> {
                Text(
                    text = "오류: $errorMessage",
                    color = Color(0xFFFF9999)
                )
            }

            rankedRestaurants.isEmpty() -> {
                Text(
                    text = "주변 3km에서 관련 음식점을 찾지 못했어요.",
                    color = restaurantWhite,
                    fontSize = 18.sp
                )

                Text(
                    text = "다시 검색하거나 다른 메뉴를 선택해 주세요.",
                    color = restaurantMuted,
                    fontSize = 14.sp
                )
            }

            current == null -> {
                Text(
                    text = "추천 가능한 식당을 모두 확인했어요.",
                    color = restaurantWhite
                )

                Button(
                    onClick = {
                        currentIndex = 0
                        selected = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = restaurantGreen
                    )
                ) {
                    Text("처음부터 다시 보기")
                }
            }

            else -> {
                val food = current.restaurant

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            restaurantCard,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(22.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "추천 ${currentIndex + 1} / " +
                                rankedRestaurants.size,
                        color = restaurantGreen,
                        fontSize = 14.sp
                    )

                    Text(
                        text = food.name,
                        color = restaurantWhite,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = food.address,
                        color = restaurantMuted,
                        fontSize = 15.sp
                    )

                    if (food.category.isNotBlank()) {
                        Text(
                            text = food.category,
                            color = restaurantMuted,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(
                        color = restaurantMuted.copy(alpha = 0.3f)
                    )

                    Text(
                        text = current.reason,
                        color = restaurantGreen,
                        fontSize = 15.sp
                    )

                    if (food.distanceMeters != null) {
                        Text(
                            text = "직선거리 약 " +
                                    food.distanceMeters + "m",
                            color = restaurantWhite,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "※ 메뉴 판매 여부·평점·영업 여부는 " +
                                "확인되지 않았습니다.",
                        color = restaurantMuted,
                        fontSize = 12.sp
                    )
                }

                if (selected) {
                    Text(
                        text = "✅ 식당을 선택했어요!",
                        color = restaurantGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "선택한 식당: ${food.name}",
                        color = restaurantWhite
                    )

                    Text(
                        text = "내비게이션 연결은 다음 단계에서 구현합니다.",
                        color = restaurantMuted,
                        fontSize = 14.sp
                    )

                    OutlinedButton(
                        onClick = { selected = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("선택 변경")
                    }
                } else {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            RestaurantNavigationButton(
                                restaurant = food,
                                onStatusChange = { status ->
                                    navigationStatus = status
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = restaurantWhite
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = restaurantMuted
                            )
                        ) {
                            Text(
                                text = "다른 식당",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (
            !loading &&
            (
                    errorMessage != null ||
                            rankedRestaurants.isEmpty()
                    )
        ) {
            Button(
                onClick = {
                    retryCount++
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = restaurantGreen,
                    contentColor = Color(0xFF101827)
                )
            ) {
                Text(
                    text = "다시 검색하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (navigationStatus.isNotBlank()) {
            Text(
                text = navigationStatus,
                color = restaurantGreen,
                fontSize = 15.sp
            )
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = restaurantWhite
            ),
            border = BorderStroke(
                width = 1.dp,
                color = restaurantMuted
            )
        ) {
            Text(
                text = "다른 메뉴 고르기",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}