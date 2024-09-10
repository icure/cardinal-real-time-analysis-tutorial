from cardinal_sdk import CardinalSdk
from cardinal_sdk.model import Code


def init_codes(sdk: CardinalSdk):
	codes = sdk.code.get_codes_blocking(["SNOMED|302866003|1", "SNOMED|80394007|1", "SNOMED|260395002|1"])
	if len(codes) == 0:
		sdk.code.create_codes_blocking([
			Code(
				id="SNOMED|302866003|1",
				type="SNOMED",
				code="302866003",
				version="1",
				label={"en": "Hypoglycemia"}
			),
			Code(
				id="SNOMED|80394007|1",
				type="SNOMED",
				code="80394007",
				version="1",
				label={"en": "Hyperglycemia"}
			),
			Code(
				id="SNOMED|260395002|1",
				type="SNOMED",
				code="260395002",
				version="1",
				label={"en": "Normal range"}
			)
		])
