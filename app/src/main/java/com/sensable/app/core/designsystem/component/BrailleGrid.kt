package com.sensable.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensable.app.ui.theme.KakaoYellow
import com.sensable.app.ui.theme.SensableTheme

/**
 * 3행 2열 점자 인터페이스 그리드.
 * 버튼 번호: [row=0,col=0]=1, [row=0,col=1]=2, ..., [row=2,col=1]=6
 */
@Composable
fun BrailleGrid(
    onButtonClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { col ->
                    BrailleButton(
                        label = "${row * 2 + col + 1}",
                        onClick = { onButtonClick(row, col) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "점자 그리드")
@Composable
private fun BrailleGridPreview() {
    SensableTheme {
        BrailleGrid(
            onButtonClick = { _, _ -> },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun BrailleButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.aspectRatio(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = KakaoYellow,
            contentColor = Color.Black
        )
    ) {
        Text(text = label, fontSize = 36.sp)
    }
}