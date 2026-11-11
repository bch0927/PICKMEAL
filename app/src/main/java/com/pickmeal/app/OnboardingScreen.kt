package com.pickmeal.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke

@Composable
fun PickMealOnboarding(
    initialFavoriteFoods: Set<String> = emptySet(),
    initialAllergies: Set<String> = emptySet(),
    initialBudget: Int = 10000,
    onCancel: (() -> Unit)? = null,
    onComplete: (Set<String>, Set<String>, Int) -> Unit
) {
    // 사용자가 선택한 정보
    val selectedFoods = remember {
        mutableStateListOf<String>().apply {
            addAll(initialFavoriteFoods)
        }
    }

    val selectedAllergies = remember {
        mutableStateListOf<String>().apply {
            addAll(initialAllergies)
        }
    }

    var budget by remember {
        mutableIntStateOf(initialBudget)
    }

    val foodTypes = listOf(
        "한식", "중식", "일식",
        "양식", "분식", "아시아 음식"
    )

    val allergyTypes = listOf(
        "땅콩", "우유", "달걀",
        "밀", "대두", "갑각류",
        "생선", "조개류", "견과류",
        "메밀", "참깨", "기타/미확인"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PICKMEAL",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF27BD89)
        )

        Text(
            text = if (onCancel != null) {
                "내 음식 취향 수정하기"
            } else {
                "당신의 입맛을 알려주세요!"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (onCancel != null) {
                "기존 설정을 확인하고 원하는 항목만 변경하세요."
            } else {
                "처음 한 번만 설정하면 됩니다."
            },
            fontSize = 15.sp
        )

        HorizontalDivider()

        Text(
            text = "1. 좋아하는 음식 종류",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        foodTypes.forEach { food ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = food in selectedFoods,
                    onCheckedChange = { checked ->
                        if (checked) {
                            selectedFoods.add(food)
                        } else {
                            selectedFoods.remove(food)
                        }
                    }
                )
                Text(
                    text = food,
                    modifier = Modifier.padding(
                        top = 12.dp
                    )
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "2. 음식 알레르기",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "해당하는 항목만 선택하세요. " +
                    "알레르기가 없다면 선택하지 않아도 됩니다.",
            fontSize = 13.sp
        )

        allergyTypes.forEach { allergy ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = allergy in selectedAllergies,
                    onCheckedChange = { checked ->
                        if (checked) {
                            selectedAllergies.add(allergy)
                        } else {
                            selectedAllergies.remove(allergy)
                        }
                    }
                )

                Text(
                    text = allergy,
                    modifier = Modifier.padding(
                        top = 12.dp
                    )
                )
            }
        }

        Text(
            text = "※ 알레르기 정보는 참고용입니다. " +
                    "원재료 및 교차접촉 여부는 반드시 " +
                    "음식점에 직접 확인해야 합니다.",
            color = Color(0xFFB45309),
            fontSize = 13.sp
        )

        HorizontalDivider()

        Text(
            text = "3. 한 끼 식사 예산",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        listOf(
            8000, 10000, 12000, 15000, 20000
        ).forEach { price ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = budget == price,
                    onClick = { budget = price }
                )

                Text(
                    text = "%,d원 이하".format(price),
                    modifier = Modifier.padding(
                        top = 12.dp
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (onCancel != null) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1D293B)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF1D293B)
                )
            ) {
                Text(
                    text = "취소하고 돌아가기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Button(
            onClick = {
                onComplete(
                    selectedFoods.toSet(),
                    selectedAllergies.toSet(),
                    budget
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF27BD89),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (onCancel != null) {
                    "변경사항 저장하기"
                } else {
                    "설정 완료하고 시작하기"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
