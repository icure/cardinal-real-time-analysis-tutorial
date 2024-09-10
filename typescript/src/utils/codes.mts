import {CardinalSdk, Code} from "@icure/cardinal-sdk";

export async function initCodes(sdk: CardinalSdk) {
	const codes = await sdk.code.getCodes(["SNOMED|302866003|1", "SNOMED|80394007|1", "SNOMED|260395002|1"])
	if (codes.length === 0) {
		await sdk.code.createCodes(
			[
				new Code({
					id: "SNOMED|302866003|1",
					type: "SNOMED",
					code: "302866003",
					version: "1",
					label: {"en": "Hypoglycemia"}
				}),
				new Code({
					id: "SNOMED|80394007|1",
					type: "SNOMED",
					code: "80394007",
					version: "1",
					label: {"en": "Hyperglycemia"}
				}),
				new Code({
					id: "SNOMED|260395002|1",
					type: "SNOMED",
					code: "260395002",
					version: "1",
					label: {"en": "Normal range"}
				})
			]
		)
	}
}