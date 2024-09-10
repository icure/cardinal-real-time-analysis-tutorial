from utils.codes import init_codes
from utils.login import do_login
from cardinal_sdk.filters import ServiceFilters, intersection
from cardinal_sdk.model import CodeStub
from cardinal_sdk.subscription import EntitySubscriptionEvent
from datetime import timedelta


def subscriber():
	sdk = do_login()
	init_codes(sdk)

	service_filter = intersection(
		ServiceFilters.by_tag_and_value_date_for_self(
			tag_type="LOINC",
			tag_code="2339-0"
		),
		ServiceFilters.by_tag_and_value_date_for_self(
			tag_type="CARDINAL",
			tag_code="TO_BE_ANALYZED"
		)
	)

	subscription = sdk.contact.subscribe_to_service_create_or_update_events_blocking(
		filter=service_filter
	)

	while subscription.get_close_reason() is None:
		event = subscription.wait_for_event_blocking(timedelta(seconds=10))
		if event is None:
			print("No event yet")
		elif event.type == EntitySubscriptionEvent.Type.Connected:
			print("Successfully opened connection")
		elif event.type == EntitySubscriptionEvent.Type.EntityNotification:
			service = sdk.contact.decrypt_service_blocking(event.entity)
			print(f"Received service {service.id}")

			content = service.content.get("en")
			if content is not None and content.measure_value.value is not None:
				measure = content.measure_value.value
				if measure < 80:
					inference_result = CodeStub(  # Snomed code for Hypoglycemia
						id="SNOMED|302866003|1",
						type="SNOMED",
						code="302866003",
						version="1"
					)
				elif measure > 130:
					inference_result = CodeStub(  # Snomed code for Hyperglycemia
						id="SNOMED|80394007|1",
						type="SNOMED",
						code="80394007",
						version="1"
					)
				else:
					inference_result = CodeStub(  # Snomed code for normal range
						id="SNOMED|260395002|1",
						type="SNOMED",
						code="260395002",
						version="1"
					)
				analyzed_code_stub = CodeStub(
					id="CARDINAL|ANALYZED|1",
					type="CARDINAL",
					code="ANALYZED",
					version="1"
				)
				filtered_tags = [tag for tag in service.tags if not (tag.type == "CARDINAL" and tag.code == "TO_BE_ANALYZED")]
				service.tags = filtered_tags + [inference_result, analyzed_code_stub]
				if service.contact_id is not None:
					contact = sdk.contact.get_contact_blocking(service.contact_id)
					filtered_services = [s for s in contact.services if s.id != service.id]
					contact.services = filtered_services + [service]
					sdk.contact.modify_contact_blocking(contact)
					print("Successfully updated contact")
				else:
					print("Cannot find parent contact")
			else:
				print(f"Service with id {service.id} does not contain a valid measure.")
		else:
			print(f"Unexpected event: {event.type}")


if __name__ == '__main__':
	subscriber()
