package ru.semper_viventem.trackrobot_android

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainViewModel(
    private val trackRobot: TrackRobot
) : ViewModel() {

    private val disposable = CompositeDisposable()
    private var textIsChanging: Boolean = false
    val uiState = MutableLiveData(UiState())
    val sideEffects = SingleLiveEvent<UiSideEffects>()

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

    fun onAction(action: UiAction) {
        when (action) {
            is UiAction.OnTextChanged -> changeIpAddress(action.text)
            is UiAction.OnButtonTup -> processTupButtonAction(action.button)
            is UiAction.OnButtonReleased -> processReleaseButtonAction(action.button)
            is UiAction.OnConnectButtonClick -> connectToRobot(uiState.value!!.ipAddress)
        }
    }

    private fun processTupButtonAction(button: ButtonType) {
        when (button) {
            ButtonType.TOP -> trackRobot.moveStraight()
            ButtonType.BUTTON -> trackRobot.moveBack()
            ButtonType.LEFT -> trackRobot.moveLeft()
            ButtonType.RIGHT -> trackRobot.moveRight()
            ButtonType.CENTRAL -> trackRobot.stop()
        }
            .subscribeTillCleared()
    }

    private fun processReleaseButtonAction(button: ButtonType) {
        trackRobot.stop()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeTillCleared()
    }

    private fun connectToRobot(ip: String) {
        trackRobot.connect(ip)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { showProgress() }
            .doOnError { hideProgress() }
            .doOnComplete { hideProgress() }
            .subscribeTillCleared(
                doOnError = {
                    Timber.e(it)
                    updateUiState { copy(isConnected = false) }
                    emmitSideEffect(UiSideEffects.ToastMessage("Connection error"))

                },
                doOnSuccess = {
                    updateUiState { copy(isConnected = true) }
                    emmitSideEffect(UiSideEffects.ToastMessage("Connected"))
                }
            )
    }

    private fun changeIpAddress(text: String) {
        if (!textIsChanging) {
            updateUiState {
                copy(ipAddress = text, isConnectButtonEnabled = StringValidator.isValidIp(text))
            }
        } else {
            textIsChanging = false
        }
    }

    private fun showProgress() {
        updateUiState { copy(isLoading = true) }.apply {  }
    }

    private fun hideProgress() {
        updateUiState { copy(isLoading = false) }
    }

    private fun updateUiState(stateProvider: UiState.() -> UiState) {
        uiState.value = stateProvider.invoke(uiState.value!!)
    }

    private fun emmitSideEffect(sideEffect: UiSideEffects) {
        sideEffects.value = sideEffect
    }

    private fun Completable.subscribeTillCleared(
        doOnError: (e: Throwable) -> Unit = Timber::e,
        doOnSuccess: () -> Unit = { },
    ) {
        disposable.add(this.subscribe(doOnSuccess, doOnError))
    }
}

data class UiState(
    val ipAddress: String = "",
    val buttonsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isConnectButtonEnabled: Boolean = false,
    val isConnected: Boolean = false
)

sealed class UiSideEffects {
    data class ToastMessage(val text: String) : UiSideEffects()
}

sealed class UiAction {
    data class OnTextChanged(val text: String) : UiAction()
    data class OnButtonTup(val button: ButtonType) : UiAction()
    data class OnButtonReleased(val button: ButtonType) : UiAction()
    object OnConnectButtonClick : UiAction()
}

enum class ButtonType {
    LEFT, RIGHT, TOP, BUTTON, CENTRAL
}