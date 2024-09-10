import {CardinalSdk} from "@icure/cardinal-sdk";
import {createSdk} from "./create_sdk.mjs";
import {readLn} from "./read.mjs";

export async function doLogin(): Promise<CardinalSdk> {
	const username = await readLn("Login: ")
	const password = await readLn("Password: ")
	try {
		return createSdk(username, password)
	} catch (e) {
		console.error(`Something went wrong: ${(e as Error).message}`)
		return await doLogin()
	}
}