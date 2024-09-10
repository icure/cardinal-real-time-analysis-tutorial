package com.cardinal

import com.cardinal.utils.createSdk
import com.cardinal.utils.login
import com.icure.cardinal.sdk.crypto.entities.PatientShareOptions
import com.icure.cardinal.sdk.crypto.entities.ShareMetadataBehaviour
import com.icure.cardinal.sdk.filters.ServiceFilters
import com.icure.cardinal.sdk.model.DecryptedContact
import com.icure.cardinal.sdk.model.DecryptedPatient
import com.icure.cardinal.sdk.model.User
import com.icure.cardinal.sdk.model.base.CodeStub
import com.icure.cardinal.sdk.model.embed.AccessLevel
import com.icure.cardinal.sdk.model.embed.DecryptedContent
import com.icure.cardinal.sdk.model.embed.DecryptedService
import com.icure.cardinal.sdk.model.embed.Measure
import com.icure.cardinal.sdk.model.requests.RequestedPermission
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

suspend fun main() {
	val sdk = login()

	val newPatient = DecryptedPatient(
		id = UUID.randomUUID().toString(),
		firstName = "Edmond",
		lastName = "Dantes",
	)
	val patientWithMetadata = sdk.patient.withEncryptionMetadata(newPatient)
	val createdPatient = sdk.patient.createPatient(patientWithMetadata)
	val login = "edmond.dantes.${UUID.randomUUID().toString().substring(0, 6)}@icure.com"
	val patientUser = User(
		id = UUID.randomUUID().toString(),
		patientId = createdPatient.id,
		login = login,
		email = login
	)
	val createdUser = sdk.user.createUser(patientUser)
	val loginToken = sdk.user.getToken(createdUser.id, "login")

	createSdk(login, loginToken)

	val patientSecretIds = sdk.patient.getSecretIdsOf(createdPatient)
	val patientShareResult = sdk.patient.shareWith(
		delegateId = createdPatient.id,
		patient = createdPatient,
		options = PatientShareOptions(
			shareSecretIds = patientSecretIds,
			shareEncryptionKey = ShareMetadataBehaviour.IfAvailable,
			requestedPermissions = RequestedPermission.MaxWrite
		)
	)

	if (patientShareResult.isSuccess) {
		println("Successfully shared patient")
	}

	val patient = patientShareResult.updatedEntityOrThrow()

	val patientSdk = createSdk(login, loginToken)
	
	var createAnother: Boolean
	do {
		val glycemiaValue = Random.nextInt(60, 160).toDouble()
		val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
		val contact = DecryptedContact(
			id = UUID.randomUUID().toString(),
			openingDate = LocalDateTime.now().format(formatter).toLong(),
			services = setOf(
				DecryptedService(
					id = UUID.randomUUID().toString(),
					content = mapOf(
						"en" to DecryptedContent(
							measureValue = Measure(
								value = glycemiaValue,
								unitCodes = setOf(
									CodeStub(
										id = "UCUM|mmol/L|1",
										type="UCUM",
										code="mmol/L",
										version="1"
									)
								)
							)
						)
					),
					tags = setOf(
						CodeStub(
							id="LOINC|2339-0|1",
							type="LOINC",
							code="2339-0",
							version="1"
						),
						CodeStub(
							id="CARDINAL|TO_BE_ANALYZED|1",
							type="CARDINAL",
							code="TO_BE_ANALYZED",
							version="1"
						)
					)
				)
			)
		)

		val recipientHcp = patient.responsible ?: throw IllegalStateException("Patient has no responsible")
		val contactWithEncryptionMetadata = patientSdk.contact.withEncryptionMetadata(
			base = contact,
			patient = patient,
			delegates = mapOf(recipientHcp to AccessLevel.Write)
		)
		patientSdk.contact.createContact(contactWithEncryptionMetadata)

		print("Sample created, you want to create another? [y/N] ")
		createAnother = readln().trim().lowercase() == "y"

	} while (createAnother)

	print("Do you want to show the results? [y/N] ")
	val show = readln().trim().lowercase() == "y"
	if (show) {
		val filter = ServiceFilters.byTagAndValueDateForSelf(
			tagType = "CARDINAL",
			tagCode = "ANALYZED"
		)
		val serviceIterator = patientSdk.contact.filterServicesBy(filter)

		while(serviceIterator.hasNext()) {
			val service = serviceIterator.next(1).first()
			val diagnosisOrNull = service.tags.firstOrNull { it.type == "SNOMED" }
			if (diagnosisOrNull != null) {
				val code = patientSdk.code.getCode(diagnosisOrNull.id!!)
				println("The diagnosis for sample ${service.id} is ${code.label?.getValue("en")}")
			} else {
				println("No diagnosis for this sample")
			}
		}
	}
}