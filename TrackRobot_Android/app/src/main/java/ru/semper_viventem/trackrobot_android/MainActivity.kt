package ru.semper_viventem.trackrobot_android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.semper_viventem.trackrobot_android.databinding.MainAcitivityBinding

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainViewModel(TrackRobotImpl()) as T
            }
        }
    }

    private val binding: MainAcitivityBinding by lazy { MainAcitivityBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewModel.uiState.observe(this) { uiState ->
            updateText(uiState.ipAddress)
            setButtonsEnabled(uiState.buttonsEnabled)
            setLoadingState(uiState.isLoading)
            setConnectButtonEnabled(uiState.isConnectButtonEnabled)
        }
        viewModel.sideEffects.observe(this) { sideEffect ->
            when (sideEffect) {
                is UiSideEffects.ToastMessage -> {
                    Toast.makeText(this, sideEffect.text, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.ipAddressInput.addTextChangedListener { text ->
            viewModel.onAction(UiAction.OnTextChanged(text?.toString() ?: ""))
        }

        binding.centralButton.setOnClickListener {
            viewModel.onAction(
                UiAction.OnButtonTup(
                    ButtonType.CENTRAL
                )
            )
        }
        binding.topButton.setOnClickListener { viewModel.onAction(UiAction.OnButtonTup(ButtonType.TOP)) }
        binding.bottomButton.setOnClickListener { viewModel.onAction(UiAction.OnButtonTup(ButtonType.BUTTON)) }
        binding.rightButton.setOnClickListener { viewModel.onAction(UiAction.OnButtonTup(ButtonType.RIGHT)) }
        binding.leftButton.setOnClickListener { viewModel.onAction(UiAction.OnButtonTup(ButtonType.LEFT)) }
        binding.connectButton.setOnClickListener { viewModel.onAction(UiAction.OnConnectButtonClick) }

    }

    private fun updateText(text: String) {
        binding.ipAddressInput.setText(text)
        binding.ipAddressInput.setSelection(text.length)
    }

    private fun setConnectButtonEnabled(isEnabled: Boolean) {
        binding.connectButton.isEnabled = isEnabled
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.connectionProgress.isInvisible = !isLoading
        binding.connectButton.isInvisible = isLoading
        binding.ipAddressInput.isEnabled = !isLoading
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        binding.centralButton.isEnabled = isEnabled
        binding.topButton.isEnabled = isEnabled
        binding.leftButton.isEnabled = isEnabled
        binding.rightButton.isEnabled = isEnabled
        binding.bottomButton.isEnabled = isEnabled
    }
}