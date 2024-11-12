import random
import uuid
from cardinal_sdk.filters import ServiceFilters
from cardinal_sdk.model import DecryptedPatient, User, PatientShareOptions, ShareMetadataBehaviour, \
	RequestedPermission, DecryptedContact, DecryptedService, DecryptedContent, Measure, CodeStub, AccessLevel, \
	SecretIdShareOptionsAllAvailable
from datetime import datetime
from utils.create_sdk import create_sdk
from utils.login import do_login


def publisher():
	sdk = do_login()

	new_patient = DecryptedPatient(
		id=str(uuid.uuid4()),
		first_name="Edmond",
		last_name="Dantes",
	)
	patient_with_metadata = sdk.patient.with_encryption_metadata_blocking(new_patient)
	created_patient = sdk.patient.create_patient_blocking(patient_with_metadata)
	login = f"edmond.dantes.{str(uuid.uuid4())[0:6]}@icure.com"
	patient_user = User(
		id=str(uuid.uuid4()),
		patient_id=created_patient.id,
		login=login,
		email=login
	)
	created_user = sdk.user.create_user_blocking(patient_user)
	login_token = sdk.user.get_token_blocking(created_user.id, "login")

	create_sdk(login, login_token)

	patient = sdk.patient.share_with_blocking(
		delegate_id=created_patient.id,
		patient=created_patient,
		options=PatientShareOptions(
			share_secret_ids=SecretIdShareOptionsAllAvailable(True),
			share_encryption_key=ShareMetadataBehaviour.IfAvailable,
			requested_permissions=RequestedPermission.MaxWrite
		)
	)

	patient_sdk = create_sdk(login, login_token)

	create_another = True
	while create_another:
		glycemia_value = float(random.randint(60, 160))
		contact = DecryptedContact(
			id=str(uuid.uuid4()),
			opening_date=int(datetime.now().strftime("%Y%m%d%H%M%S")),
			services=[
				DecryptedService(
					id=str(uuid.uuid4()),
					content={
						"en": DecryptedContent(
							measure_value=Measure(
								value=glycemia_value,
								unit_codes=[
									CodeStub(
										id="UCUM|mmol/L|1",
										type="UCUM",
										code="mmol/L",
										version="1"
									)
								]
							)
						)
					},
					tags=[
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
					]
				)
			]
		)

		recipient_hcp = patient.responsible
		contact_with_encryption_metadata = patient_sdk.contact.with_encryption_metadata_blocking(
			base=contact,
			patient=patient,
			delegates={recipient_hcp: AccessLevel.Write}
		)
		patient_sdk.contact.create_contact_blocking(contact_with_encryption_metadata)

		create_another = input("Sample created, you want to create another? [y/N] ").strip().lower() == "y"

	show = input("Do you want to show the results? [y/N] ").strip().lower() == "y"
	if show:
		service_filter = ServiceFilters.by_tag_and_value_date_for_self(
			tag_type="CARDINAL",
			tag_code="ANALYZED"
		)
		service_iterator = patient_sdk.contact.filter_services_by_blocking(service_filter)

		while service_iterator.has_next_blocking():
			service = service_iterator.next_blocking(1)[0]
			diagnosis_or_none = next((tag for tag in service.tags if tag.type == "SNOMED"), None)
			if diagnosis_or_none is not None:
				code = patient_sdk.code.get_code_blocking(diagnosis_or_none.id)
				print(f"The diagnosis for sample {service.id} is {code.label['en']}")
			else:
				print("No diagnosis for this sample")


if __name__ == '__main__':
	publisher()
