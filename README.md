# Cardinal Real-Time Communication Tutorial

This repository contains the example code used in the [real-time communication tutorial](TODO).

It shows the use case of a patient-oriented application that publishes medical data and a backend service that has to
perform an analysis whenever a new piece of information is created.

The patient application is simulated by the Publisher. The Publisher logs in as a healthcare party, creates a patient
user, and uses the patient user to simulate the creation of samples by a device that measures the sugar level in blood.
Each one of these samples is also shared of with the healthcare party.

The backend service is simulated by the Subscriber. The Subscriber logs in as a healthcare party and opens a websocket
to listen to all the creation events of the entities shared with them with a specific tag. Whenever a new entity is 
created, the Subscriber assigns a tag to it.

The tutorial code is available in Kotlin, Python, and TypeScript. Below you will find instructions for running the code
in all three languages. For further explanations and examples, check the [Cardinal documentation](TODO).

## Executing the Tutorial in Kotlin

To run the tutorial in Kotlin, clone this repository:

```bash
git clone https://github.com/icure/cardinal-real-time-analysis-tutorial.git
```

Then, open the folder in IntelliJ.
Run the [Subscriber](https://github.com/icure/cardinal-real-time-analysis-tutorial/blob/main/kotlin/src/main/kotlin/com/cardinal/Subscriber.kt),
and then the [Publisher](https://github.com/icure/cardinal-real-time-analysis-tutorial/blob/main/kotlin/src/main/kotlin/com/cardinal/Publisher.kt).

## Executing the Tutorial in Python

To run the tutorial in Python, clone this repository:

```bash
git clone https://github.com/icure/cardinal-real-time-analysis-tutorial.git
cd cardinal-real-time-analysis-tutorial
```

It is recommended to use a virtual environment, to avoid conflicting dependencies. The minimum supported Python version is
3.9.

```bash
cd python
python3 -m venv venv
source venv/bin/activate
```

Then, install the Cardinal SDK from PyPI.

```bash
pip install cardinal-sdk
```

Run the Subscriber.

```bash
python src/subscriber.py
```

Then open a new terminal, navigate to the python directory of this repository, and launch the publisher.

```bash
source venv/bin/activate
python src/publisher.py
```

## Executing the Tutorial in Typescript

To run the tutorial in Typescript, clone this repository:

```bash
git clone https://github.com/icure/cardinal-real-time-analysis-tutorial.git
cd cardinal-real-time-analysis-tutorial
```

The minimum supported Node version is 19. You can install it using [nvm](https://github.com/nvm-sh/nvm).

```bash
nvm install 19
nvm use 19
```

Then, you can navigate to the `typescript` directory and install the required dependencies using [yarn](https://yarnpkg.com/).

```bash
cd typescript
yarn install
```

Run the Subscriber code using yarn:

```bash
yarn ts-node --esm src/subscriber.ts
```

Finally, open another terminal, navigate to the `typescript` and run the Publisher:

```bash
nvm use 19
yarn ts-node --esm src/publisher.ts
```