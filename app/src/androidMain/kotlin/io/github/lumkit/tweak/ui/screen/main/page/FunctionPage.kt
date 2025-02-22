package io.github.lumkit.tweak.ui.screen.main.page

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.lumkit.tweak.LocalScreenNavigationController
import io.github.lumkit.tweak.R
import io.github.lumkit.tweak.TweakApplication
import io.github.lumkit.tweak.ui.screen.ScreenRoute

@Composable
fun FunctionPage(
    context: Context = LocalContext.current,
    viewModel: FunctionPageViewModel = viewModel { FunctionPageViewModel(context) }
) {
    val navHostController = LocalScreenNavigationController.current

    Box(
        Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        Search(viewModel, navHostController)
        Spacer(modifier = Modifier.size(16.dp))
        Plates(viewModel, navHostController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.Search(
    viewModel: FunctionPageViewModel,
    navHostController: NavHostController
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val search by viewModel.searchTextState.collectAsStateWithLifecycle()
    val plateOrigin by viewModel.plateState.collectAsStateWithLifecycle()

    var modulesSearch by rememberSaveable {
        mutableStateOf<List<FunctionPageViewModel.FunModule>>(
            emptyList()
        )
    }

    LaunchedEffect(search) {
        val list = mutableListOf<FunctionPageViewModel.FunModule>()

        plateOrigin.forEach {
            it.modules.forEach { mod ->
                if (mod.title.lowercase().contains(search.lowercase().trim())) {
                    list.add(mod)
                }
            }
        }

        modulesSearch = list
    }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = search,
                onSearch = {

                },
                onQueryChange = {
                    viewModel.setSearchText(it)
                },
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (expanded) {
                        IconButton(
                            onClick = {
                                viewModel.setSearchText("")
                                expanded = false
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cross),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                placeholder = {
                    Text(text = stringResource(R.string.text_search_functions))
                }
            )
        },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .semantics { traversalIndex = 0f },
        expanded = expanded,
        onExpandedChange = {
            expanded = it
        },
        windowInsets = WindowInsets(top = 0.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(modulesSearch) {
                val enabled = TweakApplication.runtimeStatus >= it.runtimeStatus
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            viewModel.setSearchText("")
                            expanded = false
                            navHostController.navToModule(it)
                        }
                        .alpha(
                            alpha = if (enabled) {
                                1f
                            } else {
                                .5f
                            }
                        ),
                    headlineContent = {
                        Text(
                            text = it.title,
                            textDecoration = if (enabled) {
                                TextDecoration.None
                            } else {
                                TextDecoration.LineThrough
                            }
                        )
                    },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            contentColor = MaterialTheme.colorScheme.outline,
                            color = Color.Transparent
                        ) {
                            Icon(
                                painter = painterResource(it.icon),
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Plates(viewModel: FunctionPageViewModel, navHostController: NavHostController) {

    val plates by viewModel.plateState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
            top = SearchBarDefaults.InputFieldHeight + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(plates) {
            PlateItem(
                it,
                navHostController
            )
        }
    }
}

@Composable
private fun PlateItem(
    funPlate: FunctionPageViewModel.FunPlate,
    navHostController: NavHostController
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(funPlate.title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            funPlate.modules.forEach {
                val enabled = TweakApplication.runtimeStatus >= it.runtimeStatus
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navHostController.navToModule(it)
                    },
                    enabled = enabled,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(16.dp),
                            contentColor = MaterialTheme.colorScheme.outline,
                            color = Color.Transparent
                        ) {

                            Icon(
                                painter = painterResource(it.icon),
                                contentDescription = null,
                            )
                        }
                        Column {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.labelMedium,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false,
                                textDecoration = if (enabled) {
                                    TextDecoration.None
                                } else {
                                    TextDecoration.LineThrough
                                },
                            )
                        }
                    }
                }
            }
            if (funPlate.modules.size % 2 != 0) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

fun NavHostController.navToModule(
    module: FunctionPageViewModel.FunModule
) {
    navigate(
        buildString {
//            if (module.route.startsWith(ScreenRoute.VAB_UPDATE)) {
//                append(module.route.replace("{sharedKey}", ScreenRoute.VAB_UPDATE))
//            } else {
//                append(module.route)
//            }
            append(module.route)
        }
    ) {
        popUpTo(ScreenRoute.MAIN) {
            saveState = true
        }
        restoreState = true
    }
}

fun String.sharedLabel(): String = if (contains("{")) {
    substring(0, lastIndexOf("{") - 1)
} else {
    this
}