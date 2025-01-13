package io.github.lumkit.tweak.ui.screen.main.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.waterfall
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lumkit.tweak.R

@Composable
fun FunctionPage(
    viewModel: FunctionPageViewModel = viewModel { FunctionPageViewModel() }
) {
    Box(
        Modifier.fillMaxSize().
        semantics { isTraversalGroup = true }
    ) {
        Search(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.Search(viewModel: FunctionPageViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val search by viewModel.searchTextState.collectAsStateWithLifecycle()
    val plateOrigin by viewModel.plateState.collectAsStateWithLifecycle()

    var modulesSearch by rememberSaveable { mutableStateOf<List<FunctionPageViewModel.FunModule>>(emptyList()) }

    LaunchedEffect(search) {
        val list = mutableListOf<FunctionPageViewModel.FunModule>()

        plateOrigin.forEach {
            it.modules.forEach {

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
        modifier = Modifier.align(Alignment.TopCenter)
            .semantics { traversalIndex = 0f },
        expanded = expanded,
        onExpandedChange = {
            expanded = it
        },
        windowInsets = WindowInsets(top = 0.dp)
    ) {

    }
}