from utils.create_sdk import create_sdk
from cardinal_sdk import CardinalSdk


def do_login() -> CardinalSdk:
	username = input("Username: ")
	password = input("Password: ")
	try:
		return create_sdk(username, password)
	except Exception as e:
		print(f"Something went wrong: {e}")
		return do_login()