import {doLogin} from "./utils/login.mjs";
import {
	AccessLevel,
	CodeStub,
	DecryptedContact, DecryptedContent,
	DecryptedPatient, DecryptedService, Measure,
	PatientShareOptions,
	RequestedPermission, SecretIdShareOptions, ServiceFilters,
	ShareMetadataBehaviour,
	User
} from "@icure/cardinal-sdk";
import {v4 as uuid} from 'uuid';
import {createSdk} from "./utils/create_sdk.mjs";
import {random} from "./utils/random.mjs";
import {currentFuzzyDate} from "./utils/date.mjs";
import {readLn} from "./utils/read.mjs";

const main = async () => {
	const sdk = await doLogin()

	const newPatient = new DecryptedPatient({
		id: uuid(),
		firstName: "Edmond",
		lastName: "Dantes",
	})
	const patientWithMetadata = await sdk.patient.withEncryptionMetadata(newPatient)
	const createdPatient = await sdk.patient.createPatient(patientWithMetadata)
	const login = `edmond.dantes.${uuid().substring(0, 6)}@icure.com`
	const patientUser = new User({
		id: uuid(),
		patientId: createdPatient.id,
		login: login,
		email: login
	})
	const createdUser = await sdk.user.createUser(patientUser)
	const loginToken = await sdk.user.getToken(createdUser.id, "login")

	await createSdk(login, loginToken)

	const patient = await sdk.patient.shareWith(
		createdPatient.id,
		createdPatient,
		{
			options: new PatientShareOptions({
				shareSecretIds: new SecretIdShareOptions.AllAvailable({ requireAtLeastOne: true }),
				shareEncryptionKey: ShareMetadataBehaviour.IfAvailable,
				requestedPermissions: RequestedPermission.MaxWrite
			})
		}
	)

	const patientSdk = await createSdk(login, loginToken)

	let createAnother = true
	do {
		const glycemiaValue = random(60, 160)
		const contact = new DecryptedContact({
			id: uuid(),
			openingDate: currentFuzzyDate(),
			services: [
				new DecryptedService({
					id: uuid(),
					content: {
						"en": new DecryptedContent({
							measureValue: new Measure({
								value: glycemiaValue,
								unitCodes: [
									new CodeStub({
										id: "UCUM|mmol/L|1",
										type: "UCUM",
										code: "mmol/L",
										version: "1"
									})
								]
							})
						})
					},
					tags: [
						new CodeStub({
							id: "LOINC|2339-0|1",
							type: "LOINC",
							code: "2339-0",
							version: "1"
						}),
						new CodeStub({
							id: "CARDINAL|TO_BE_ANALYZED|1",
							type: "CARDINAL",
							code: "TO_BE_ANALYZED",
							version: "1"
						})
					]
				})
			]
		})

		const recipientHcp = patient.responsible
		const contactWithEncryptionMetadata = await patientSdk.contact.withEncryptionMetadata(
			contact,
			patient,
			{ delegates: { [recipientHcp]: AccessLevel.Write}}
		)
		await patientSdk.contact.createContact(contactWithEncryptionMetadata)

		createAnother = (await readLn("Sample created, you want to create another? [y/N] ")).trim().toLowerCase() === "y"

	} while (createAnother)

	const show = (await readLn("Do you want to show the results? [y/N] ")).trim().toLowerCase() === "y"
	if (show) {
		const filter = ServiceFilters.byTagAndValueDateForSelf(
			"CARDINAL",
			{ tagCode: "ANALYZED" }
		)
		const serviceIterator = await patientSdk.contact.filterServicesBy(filter)

		while(await serviceIterator.hasNext()) {
			const service = (await serviceIterator.next(1))[0]
			const diagnosisOrNull = service.tags.find( it => it.type == "SNOMED")
			if (diagnosisOrNull !== undefined) {
				const code = await patientSdk.code.getCode(diagnosisOrNull.id)
				console.log(`The diagnosis for sample ${service.id} is ${code.label["en"]}`)
			} else {
				console.log("No diagnosis for this sample")
			}
		}
	}
}

main()