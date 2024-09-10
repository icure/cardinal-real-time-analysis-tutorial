import {doLogin} from "./utils/login.mjs";
import {initCodes} from "./utils/codes.mjs";
import {
	CodeStub,
	DecryptedContact,
	DecryptedService,
	EntitySubscriptionEvent,
	intersection,
	ServiceFilters
} from "@icure/cardinal-sdk";

const main = async () => {
	const sdk = await doLogin()
	await initCodes(sdk)

	const filter = intersection(
		ServiceFilters.byTagAndValueDateForSelf(
			"LOINC",
			{tagCode: "2339-0"}
		),
		ServiceFilters.byTagAndValueDateForSelf(
			"CARDINAL",
			{tagCode: "TO_BE_ANALYZED"}
		)
	)

	const subscription = await sdk.contact.subscribeToServiceCreateOrUpdateEvents(
		filter
	)

	while (!subscription.isClosed) {
		const event = await subscription.waitForEvent(10 * 1000)
		if (event === null) {
			console.log("No event yet")
		} else if (event.$ktClass === EntitySubscriptionEvent.Connected.$ktClass) {
			console.log("Successfully opened connection")
		} else if (event instanceof EntitySubscriptionEvent.EntityNotification) {
			const service = await sdk.contact.decryptService(event.entity)
			console.log(`Received service ${service.id}`)
			const measure = service.content["en"]?.measureValue?.value
			if (!!measure) {
				let inferenceResult: CodeStub
				if (measure < 80) {
					// Snomed code for Hypoglycemia
					inferenceResult = new CodeStub({
						id: "SNOMED|302866003|1",
						type: "SNOMED",
						code: "302866003",
						version: "1"
					})
				} else if (measure > 130) {
					// Snomed code for Hyperglycemia
					inferenceResult = new CodeStub({
						id: "SNOMED|80394007|1",
						type: "SNOMED",
						code: "80394007",
						version: "1"
					})
				} else {
					// Snomed code for normal range
					inferenceResult = new CodeStub({
						id: "SNOMED|260395002|1",
						type: "SNOMED",
						code: "260395002",
						version: "1"
					})
				}
				const analyzedCodeStub = new CodeStub({
					id: "CARDINAL|ANALYZED|1",
					type: "CARDINAL",
					code: "ANALYZED",
					version: "1"
				})
				const updatedService = new DecryptedService({
					...service,
					tags: service.tags
						.filter(it => it.type !== "CARDINAL" || it.code !== "TO_BE_ANALYZED")
						.concat(inferenceResult, analyzedCodeStub)
				})
				if (!!updatedService.contactId) {
					const contact = await sdk.contact.getContact(service.contactId)
					await sdk.contact.modifyContact(
						new DecryptedContact({
							...contact,
							services: contact.services
								.filter(it => it.id !== updatedService.id)
								.concat(updatedService)
						})
					)
					console.log("Successfully updated contact")
				} else {
					console.log("Cannot find parent contact")
				}
			} else {
				console.log(`Service with id ${service.id} does not contain a valid measure.`)
			}
		} else {
			console.log(`Unexpected event: ${event.$ktClass}`)
		}
	}
}

main()