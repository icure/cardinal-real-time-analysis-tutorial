package com.cardinal

import com.cardinal.utils.initializeCodes
import com.cardinal.utils.login
import com.icure.cardinal.sdk.filters.ServiceFilters
import com.icure.cardinal.sdk.model.base.CodeStub
import com.icure.cardinal.sdk.subscription.EntitySubscriptionEvent

suspend fun main() {
	val sdk = login()
	initializeCodes(sdk)

	val filter = ServiceFilters.byTagAndValueDateForSelf(
			tagType = "LOINC",
			tagCode = "2339-0"
		)
		.and(
			ServiceFilters.byTagAndValueDateForSelf(
				tagType = "CARDINAL",
				tagCode = "TO_BE_ANALYZED"
			)
		)

	val subscription = sdk.contact.subscribeToServiceCreateOrUpdateEvents(
		filter = filter
	)

	for (event in subscription.eventChannel) {
		when (event) {
			EntitySubscriptionEvent.Connected -> {
				println("Successfully opened connection")
			}
			is EntitySubscriptionEvent.EntityNotification -> {
				val service = sdk.contact.decryptService(event.entity)
				println("Received service ${service.id}")
				val measureOrNull = service.content["en"]?.measureValue?.value
				if (measureOrNull != null) {
					val inferenceResult = when {
						measureOrNull < 80 -> {
							CodeStub( // Snomed code for Hypoglycemia
								id = "SNOMED|302866003|1",
								type = "SNOMED",
								code = "302866003",
								version = "1"
							)
						}
						measureOrNull > 130 -> {
							CodeStub( // Snomed code for Hyperglycemia
								id = "SNOMED|80394007|1",
								type = "SNOMED",
								code = "80394007",
								version = "1"
							)
						}
						else -> {
							CodeStub( // Snomed code for normal range
								id = "SNOMED|260395002|1",
								type = "SNOMED",
								code = "260395002",
								version = "1"
							)
						}
					}
					val analyzedCodeStub = CodeStub(
						id = "CARDINAL|ANALYZED|1",
						type = "CARDINAL",
						code = "ANALYZED",
						version = "1"
					)
					val updatedService = service.copy(
						tags = service.tags.filterNot {
							it.type == "CARDINAL" && it.code == "TO_BE_ANALYZED"
						}.toSet() + setOf(inferenceResult, analyzedCodeStub)
					)
					if (service.contactId != null) {
						val contact = sdk.contact.getContact(service.contactId!!)
						sdk.contact.modifyContact(
							contact.copy(
								services = contact.services.filterNot {
									it.id == updatedService.id
								}.toSet() + setOf(updatedService)
							)
						)
						println("Successfully updated contact")
					} else {
						println("Cannot find parent contact")
					}
				} else {
					println("Service with id ${service.id} does not contain a valid measure.")
				}
			}
			else -> println("Unexpected event: $event")
		}
	}
}