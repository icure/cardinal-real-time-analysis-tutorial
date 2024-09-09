package com.cardinal.utils

import com.icure.cardinal.sdk.CardinalSdk
import com.icure.cardinal.sdk.model.Code

suspend fun initializeCodes(sdk: CardinalSdk) {
	val codes = sdk.code.getCodes(listOf("SNOMED|302866003|1", "SNOMED|80394007|1", "SNOMED|260395002|1"))
	if (codes.isEmpty()) {
		sdk.code.createCodes(
			listOf(
				Code(
					id = "SNOMED|302866003|1",
					type = "SNOMED",
					code = "302866003",
					version = "1",
					label = mapOf("en" to "Hypoglycemia")
				),
				Code(
					id = "SNOMED|80394007|1",
					type = "SNOMED",
					code = "80394007",
					version = "1",
					label = mapOf("en" to "Hyperglycemia")
				),
				Code(
					id = "SNOMED|260395002|1",
					type = "SNOMED",
					code = "260395002",
					version = "1",
					label = mapOf("en" to "Normal range")
				)
			)
		)
	}
}