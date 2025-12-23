package com.xiamo.alt

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xiamo.gui.ComposeScreen
import com.xiamo.gui.titleScreen.TitleScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW


class AltManagerScreen : ComposeScreen(Text.of("Alt Manager")) {

    override fun shouldCloseOnEsc(): Boolean = true

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MinecraftClient.getInstance().setScreen(TitleScreen())
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    @Composable
    override fun renderCompose() {
        var showAddDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var statusMessage by remember { mutableStateOf<String?>(null) }
        var accounts by remember { mutableStateOf(AltManager.accounts.toList()) }
        var msLoginUrl by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        fun refreshAccounts() {
            accounts = AltManager.accounts.toList()
        }

        fun showStatus(message: String) {
            statusMessage = message
            scope.launch {
                delay(3000)
                statusMessage = null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                HeaderSection(
                    currentUsername = AltManager.getCurrentUsername(),
                    onAddClick = { showAddDialog = true },
                    onRestoreClick = {
                        if (AltManager.restoreInitialSession()) {
                            showStatus("已恢复初始账号")
                            refreshAccounts()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val filteredAccounts = accounts.filter {
                    it.username.contains(searchQuery, ignoreCase = true)
                }

                if (filteredAccounts.isEmpty()) {
                    EmptyState(onAddClick = { showAddDialog = true })
                } else {
                    AccountList(
                        accounts = filteredAccounts,
                        currentAccountIndex = AltManager.currentAccountIndex,
                        onLogin = { account ->
                            scope.launch {
                                AltManager.login(account).fold(
                                    onSuccess = { showStatus("已登录: ${it.username}"); refreshAccounts() },
                                    onFailure = { showStatus("登录失败: ${it.message}") }
                                )
                            }
                        },
                        onDelete = { index ->
                            val realIndex = AltManager.accounts.indexOf(filteredAccounts[index])
                            AltManager.removeAccount(realIndex)
                            showStatus("已删除账号")
                            refreshAccounts()
                        },
                        onToggleFavorite = { index ->
                            val realIndex = AltManager.accounts.indexOf(filteredAccounts[index])
                            AltManager.toggleFavorite(realIndex)
                            refreshAccounts()
                        }
                    )
                }
            }


            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }


            if (showAddDialog) {
                AddAccountDialog(
                    onDismiss = { showAddDialog = false },
                    onAddCracked = { username ->
                        AltManager.addCrackedAccount(username).fold(
                            onSuccess = { showStatus("已添加账号: $username"); refreshAccounts() },
                            onFailure = { showStatus("添加失败: ${it.message}") }
                        )
                        showAddDialog = false
                    },
                    onAddCurrent = {
                        AltManager.addFromCurrentSession().fold(
                            onSuccess = { showStatus("已添加当前账号: ${it.username}"); refreshAccounts() },
                            onFailure = { showStatus("添加失败: ${it.message}") }
                        )
                        showAddDialog = false
                    },
                    onDirectLogin = { username ->
                        AltManager.loginCracked(username).fold(
                            onSuccess = { showStatus("已登录: $username"); refreshAccounts() },
                            onFailure = { showStatus("登录失败: ${it.message}") }
                        )
                        showAddDialog = false
                    },
                    onMicrosoftLogin = {
                        scope.launch {
                            showStatus("正在启动微软登录...")
                            MicrosoftAuth.login(
                                onUrl = { url -> msLoginUrl = url },
                                onSuccess = { account ->
                                    AltManager.addMicrosoftAccount(account)
                                    showStatus("已添加微软账号: ${account.username}")
                                    refreshAccounts()
                                    msLoginUrl = null
                                },
                                onError = { error ->
                                    showStatus("微软登录失败: $error")
                                    msLoginUrl = null
                                }
                            )
                        }
                        showAddDialog = false
                    }
                )
            }


            if (msLoginUrl != null) {
                MicrosoftLoginDialog(
                    url = msLoginUrl!!,
                    onDismiss = { msLoginUrl = null }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    currentUsername: String,
    onAddClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "账号管理器",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "当前: $currentUsername",
                fontSize = 9.sp,
                color = Color.Gray
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF2D2D44), CircleShape)
                    .clickable { onRestoreClick() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawRefreshIcon(Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF6C63FF), CircleShape)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawAddIcon(Color.White)
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(color = Color.White, fontSize = 10.sp),
        cursorBrush = SolidColor(Color.White),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D44), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawSearchIcon(Color.Gray)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box {
                    if (query.isEmpty()) {
                        Text("搜索账号...", color = Color.Gray, fontSize = 10.sp)
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            drawPersonIcon(Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("暂无账号", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "点击下方按钮添加账号",
            fontSize = 9.sp,
            color = Color.Gray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("+ 添加账号", fontSize = 10.sp)
        }
    }
}

@Composable
private fun AccountList(
    accounts: List<MinecraftAccount>,
    currentAccountIndex: Int,
    onLogin: (MinecraftAccount) -> Unit,
    onDelete: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(accounts) { index, account ->
            val isCurrentAccount = AltManager.accounts.indexOf(account) == currentAccountIndex
            AccountItem(
                account = account,
                isCurrentAccount = isCurrentAccount,
                onLogin = { onLogin(account) },
                onDelete = { onDelete(index) },
                onToggleFavorite = { onToggleFavorite(index) }
            )
        }
    }
}

@Composable
private fun AccountItem(
    account: MinecraftAccount,
    isCurrentAccount: Boolean,
    onLogin: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isCurrentAccount -> Color(0xFF4CAF50).copy(alpha = 0.3f)
        isHovered -> Color(0xFF3D3D5C)
        else -> Color(0xFF2D2D44)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable { onLogin() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = account.getHeadUrl(32),
            contentDescription = "Player Head",
            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = account.username,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (account.isFavorite) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("★", color = Color(0xFFFFD700), fontSize = 8.sp)
                }

                if (isCurrentAccount) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(shape = RoundedCornerShape(2.dp), color = Color(0xFF4CAF50)) {
                        Text(
                            text = "当前",
                            fontSize = 7.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = when (account.type) {
                        AccountType.MICROSOFT -> Color(0xFF00A4EF)
                        AccountType.SESSION -> Color(0xFFFF9800)
                        AccountType.CRACKED -> Color(0xFF9E9E9E)
                    }
                ) {
                    Text(
                        text = account.type.displayName,
                        fontSize = 7.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                Text(account.uuid.take(8) + "...", fontSize = 8.sp, color = Color.Gray)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (account.isFavorite) "★" else "☆",
                    color = if (account.isFavorite) Color(0xFFFFD700) else Color.Gray,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color(0xFFE57373), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAddCracked: (String) -> Unit,
    onAddCurrent: () -> Unit,
    onDirectLogin: (String) -> Unit,
    onMicrosoftLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2D2D44)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("添加账号", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(modifier = Modifier.height(8.dp))


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                        .padding(2.dp)
                ) {
                    listOf("离线", "微软", "当前", "快速").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) Color(0xFF6C63FF) else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, fontSize = 8.sp, color = if (isSelected) Color.White else Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> CrackedAccountTab(username, { username = it }, onAddCracked)
                    1 -> MicrosoftAccountTab(onMicrosoftLogin)
                    2 -> CurrentAccountTab(onAddCurrent)
                    3 -> DirectLoginTab(username, { username = it }, onDirectLogin)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D3D5C)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("取消", color = Color.Gray, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun CrackedAccountTab(username: String, onUsernameChange: (String) -> Unit, onAdd: (String) -> Unit) {
    Column {
        Text("用户名", color = Color.Gray, fontSize = 8.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = username,
            onValueChange = { if (it.length <= 16) onUsernameChange(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 9.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    if (username.isEmpty()) Text("输入用户名...", color = Color.Gray, fontSize = 9.sp)
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onAdd(username) },
            enabled = username.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text("添加离线账号", fontSize = 9.sp)
        }
    }
}

@Composable
private fun MicrosoftAccountTab(onLogin: () -> Unit) {
    Column {
        Text("使用微软账号登录", color = Color.Gray, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "点击下方按钮将在浏览器中打开微软登录页面",
            color = Color.Gray.copy(alpha = 0.7f),
            fontSize = 8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A4EF)),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text("微软登录", fontSize = 9.sp)
        }
    }
}

@Composable
private fun CurrentAccountTab(onAdd: () -> Unit) {
    Column {
        Text("将当前登录的账号添加到列表中", color = Color.Gray, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://mc-heads.net/avatar/${AltManager.getCurrentUsername()}/16",
                contentDescription = null,
                modifier = Modifier.size(16.dp).clip(RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(AltManager.getCurrentUsername(), color = Color.White, fontSize = 9.sp)
                Text(AltManager.getCurrentUUID()?.take(8) ?: "Unknown", color = Color.Gray, fontSize = 7.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text("添加当前账号", fontSize = 9.sp)
        }
    }
}

@Composable
private fun DirectLoginTab(username: String, onUsernameChange: (String) -> Unit, onLogin: (String) -> Unit) {
    Column {
        Text("直接使用离线账号登录（不保存）", color = Color.Gray, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = username,
            onValueChange = { if (it.length <= 16) onUsernameChange(it) },
            textStyle = TextStyle(color = Color.White, fontSize = 9.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    if (username.isEmpty()) Text("输入用户名...", color = Color.Gray, fontSize = 9.sp)
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onLogin(username) },
            enabled = username.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text("快速登录", fontSize = 9.sp)
        }
    }
}

@Composable
private fun MicrosoftLoginDialog(url: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2D2D44),
            modifier = Modifier.widthIn(max = 280.dp).padding(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("微软登录", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("请在浏览器中完成登录", color = Color.Gray, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("登录链接已复制到剪贴板", color = Color.Gray.copy(alpha = 0.7f), fontSize = 8.sp)
                Spacer(modifier = Modifier.height(8.dp))


                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = url,
                        color = Color(0xFF00A4EF),
                        fontSize = 8.sp,
                        modifier = Modifier.padding(6.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("正在等待登录完成...", color = Color.Yellow, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("取消登录", fontSize = 9.sp)
                }
            }
        }
    }
}

// 自定义图标绘制函数
private fun DrawScope.drawAddIcon(color: Color) {
    val strokeWidth = 1.5.dp.toPx()
    val center = size.width / 2
    val lineLength = size.width * 0.6f
    // 横线
    drawLine(color, Offset(center - lineLength / 2, center), Offset(center + lineLength / 2, center), strokeWidth)
    // 竖线
    drawLine(color, Offset(center, center - lineLength / 2), Offset(center, center + lineLength / 2), strokeWidth)
}

private fun DrawScope.drawRefreshIcon(color: Color) {
    val strokeWidth = 1.5.dp.toPx()
    val radius = size.width * 0.35f
    val center = Offset(size.width / 2, size.height / 2)
    drawArc(color, 30f, 300f, false, topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2), style = Stroke(strokeWidth))
}

private fun DrawScope.drawSearchIcon(color: Color) {
    val strokeWidth = 1.5.dp.toPx()
    val radius = size.width * 0.3f
    val center = Offset(size.width * 0.4f, size.height * 0.4f)
    drawCircle(color, radius, center, style = Stroke(strokeWidth))
    val handleStart = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f)
    val handleEnd = Offset(size.width * 0.85f, size.height * 0.85f)
    drawLine(color, handleStart, handleEnd, strokeWidth)
}

private fun DrawScope.drawPersonIcon(color: Color) {
    val strokeWidth = 2.dp.toPx()
    val headRadius = size.width * 0.2f
    val headCenter = Offset(size.width / 2, size.height * 0.3f)
    drawCircle(color, headRadius, headCenter, style = Stroke(strokeWidth))
    val bodyTop = headCenter.y + headRadius + 3.dp.toPx()
    val bodyPath = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.9f)
        quadraticTo(size.width / 2, bodyTop, size.width * 0.8f, size.height * 0.9f)
    }
    drawPath(bodyPath, color, style = Stroke(strokeWidth))
}
