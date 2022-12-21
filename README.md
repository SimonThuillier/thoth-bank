# thoth bank
Welcome to this proof of concept banking application using [MAIF thoth library](https://github.com/MAIF/thoth).

This is a Spring project initialized with [Spring initializr](https://start.spring.io/) 3.0.0 using maven.

## Installation

### System requirements

- Java 17+
- docker and docker-compose for running Postgre & Kafka

### Configuring the project

Always at the root of the project :
- copy the `.env.dist` file into a new `.env` file
- Choose a location of your liking for a log directory (for example root of the project/logs) and update parameter LOG_FILEPATH accordingly.
- You can keep other parameters the same. The most important are the ports the various services will be running on, the host
  if you want to expose services for cross-testing, and KAFKA_URL which is the url of kafka broker.

### Launching postgre and kafka

Here is a runtime flux overview :

![execution configurations](./docs/thoth-bank.drawio.png)

postgres server and kafka cluster can be launched with docker using the `docker-compose.yml` at the root of the project.
Ensure that network parameters - notably ports match with your `.env` (it is the case with default `.env.dist`) and launch services using
`docker-compose up --build -d`

Use `docker ps` to ensure the four needed containers are running.  

### Execution configuration

This projects contains 2 main entrypoint classes. 

The array above list them all : all paths are exprimed from `src/main/java/com/bts/thoth/bank`. All configuration also have a specific package.

| Class           |                                                          Description |      package       |           path           |
|-----------------|---------------------------------------------------------------------:|:------------------:|:------------------------:|
| DemoApplication |                          run a static test scenario on a new account | com.bts.thoth.bank |  DemoApplication.class   |
| CliApplication  | CLI interactive application allowing to control and consult accounts | com.bts.thoth.bank | BankCliApplication.class |


#### Launching via CLI

TODO

## use

Upon launching the main menu will be displayed on console : 
 
> Type a number to select an action :
> 
> 1 - create new account
> 
> 2 - set account for action / consultation
> 
> 3 - exit app
> 
> Type a choice and enter to validate:

Upon entering a valid number user enter an action process.

### Create new account

It will ask the currency amount for the initial deposit : 
> Enter initial deposit amount : 

Upon enter it will trigger account creation display a validation message 
and enter the newly created account menu.

### set account for action / consultation

It will ask the ID of the account to enter : 
> Enter accountId : 

Upon enter it will enter the requested account menu.

### exit app

Terminate the application. 

### account menu

The account menu allows for depositing, withdrawing, closing 
as well as consulting one account state.

> (Account 123456789 selected)
> 
> Type a number to select an action :
> 
> 1 - deposit
> 
> 2 - withdraw
> 
> 3 - get current state
> 
> 4 - get history
> 
> 5 - close account
> 
> 6 - exit account
> 
>7 - exit app

