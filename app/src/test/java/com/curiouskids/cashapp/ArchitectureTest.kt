package com.curiouskids.cashapp

import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureTest {

    @Test
    fun `test ErrorModel structure matches backend spec`() {
        val error = ErrorModel(
            code = "INVALID",
            message = "Something went wrong",
            requestId = "123-abc"
        )
        assertEquals("INVALID", error.code)
        assertEquals("Something went wrong", error.message)
        assertEquals("123-abc", error.requestId)
    }

    @Test
    fun `test Result wrapper states`() {
        val success: Result<String> = Result.Success("Data")
        val error: Result<Nothing> = Result.Error(ErrorModel("ERR", "Msg"))
        val loading: Result<Nothing> = Result.Loading

        assertTrue(success is Result.Success)
        assertTrue(error is Result.Error)
        assertTrue(loading is Result.Loading)
    }
}
