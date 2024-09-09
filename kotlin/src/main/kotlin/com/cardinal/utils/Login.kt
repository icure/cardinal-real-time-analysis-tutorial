package com.cardinal.utils

import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.utils.RequestStatusException

suspend fun login(): CardinalSdk {
	print("Login: ")
	val username = readln().trim()
	print("Password: ")
	val password = readln().trim()
	return try {
		createSdk(username, password)
	} catch (e: RequestStatusException) {
		if(e.statusCode == 401) {
			println("Invalid username or password, maybe the token expired?")
		} else {
			println("Something went wrong: ${e.message}")
		}
		login()
	} catch (e: Exception) {
		println("Something went wrong: ${e.message}")
		login()
	}
}