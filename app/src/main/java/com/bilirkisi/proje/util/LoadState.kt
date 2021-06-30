package com.bilirkisi.proje.util

class ErrorMessage {
    companion object {
        var errorMessage: String? = "Bir şeyler ters gitti. HATA !"
    }
}

enum class LoadState {
    SUCCESS, FAILURE, LOADING
}
