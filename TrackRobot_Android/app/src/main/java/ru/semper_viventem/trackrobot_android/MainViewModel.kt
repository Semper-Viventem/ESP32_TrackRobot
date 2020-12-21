package ru.semper_viventem.trackrobot_android

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainViewModel(
    private val trackRobot: TrackRobot
) : ViewModel() {

    private val disposable = CompositeDisposable()
    private var textIsChanging: Boolean = true
    val uiState = MutableLiveData(UiState())
    val sideEffects = SingleLiveEvent<UiSideEffects>()

    init {
        trackRobot.observeConnected()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeTillCleared { isConnected ->
                val message = if (isConnected) "Connected" else "Disconnected"
                emmitSideEffect(UiSideEffects.ToastMessage(message))
                updateUiState { copy(isConnected = isConnected, buttonsEnabled = isConnected) }
            }
    }

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
            .subscribeTillCleared()
    }

    private fun changeIpAddress(text: String) {
        if (!textIsChanging) {
            updateUiState {
                copy(ipAddress = text, isConnectButtonEnabled = StringValidator.isValidIp(text))
            }
            textIsChanging = true
        } else {
            textIsChanging = false
        }
    }

    private fun showProgress() {
        updateUiState { copy(isLoading = true) }.apply { }
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

    private fun <T> Observable<T>.subscribeTillCleared(
        doOnError: (e: Throwable) -> Unit = Timber::e,
        doOnSuccess: (T) -> Unit = { },
    ) {
        disposable.add(this.subscribe(doOnSuccess, doOnError))
    }
}

data class UiState(
    val ipAddress: String = "192.168.88.55",
    val buttonsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isConnectButtonEnabled: Boolean = true,
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