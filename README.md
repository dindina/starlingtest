# Starling Bank Round-Up Service

This application automatically calculates the round-up amount for your spending transactions in a specified period and transfers it to a designated Starling Bank savings goal.



## 1. Running the Application

### Prerequisites

* **Java Development Kit (JDK):** Make sure you have Java 11 or a later version installed on your system.
* **Maven:** This project uses Maven for dependency management and building. Install Maven if you haven't already.
* **Starling Bank Personal Access Token:** You will need a personal access token from your Starling Bank developer account. You can generate one through the Starling Bank mobile app or the developer portal.
* **Starling Bank Account ID:** You need the ID of the account from which transactions will be read.
* **Starling Bank Savings Goal ID:** You need the ID of the savings goal where the round-up amounts will be transferred.

### Configuration

1.  **Create `application.properties`:** In the `src/main/resources` directory of the project, create a file named `application.properties` if it doesn't already exist.

2.  **Add Configuration Properties:** Add the following properties to the `application.properties` file, replacing the placeholder values with your actual credentials and IDs:

    ```properties
    starling.api.base-url=[https://api-sandbox.starlingbank.com/api/v2](https://api-sandbox.starlingbank.com/api/v2)
    starling.api.key=YOUR_STARLING_PERSONAL_ACCESS_TOKEN
    ```

    **Note:** For testing, the `api-sandbox.starlingbank.com` base URL is used. For real transactions, you might need to adjust this based on Starling Bank's documentation.

### Running with Maven

1.  **Navigate to the Project Directory:** Open your terminal or command prompt and navigate to the root directory of the project where the `pom.xml` file is located.

2.  **Run the Spring Boot Application:** Execute the following Maven command:

    ```bash
    mvn spring-boot:run
    ```

    This command will download the necessary dependencies, build the project, and start the embedded Tomcat server running the application.

### Accessing the Endpoint

Once the application is running, you can trigger the round-up calculation and transfer by sending a `POST` request to the `/roundup/process` endpoint. You will need to provide the `startDate`, `endDate`, and `accountNumber` as query parameters, along with the `goalSavingsId` (which is configured in `application.properties`).

Swagger URL : http://localhost:8080/swagger-ui.html

**Example `curl` request:**

```bash
curl -X POST \
     "http://localhost:8080/round-up/process?startDate=2025-04-14&endDate=2025-04-18&accountNumber=YOUR_ACCOUNT_ID&goalSavingsId=${starling.savings-goal-id}"
     
 working example :    
curl --location --request POST 'http://localhost:8080/round-up/process?startDate=2025-04-10&endDate=2025-04-18&accountNumber=c363b03d-d6c6-4f25-a493-c1161d21496b&goalSavingsId=c3cd2cc4-b5d2-46da-b681-685bc9c39590'
```

3.  Future Enhancements: \
    Error Handling and Resilience : Add more logging \
    
    Provide more specific error responses to the client \
    
    Implement a scheduler (e.g., using Spring's @Scheduled annotation) to automatically run the round-up calculation and transfer at predefined intervals (e.g., daily, weekly) 


4. Testing \
   Write comprehensive unit tests for the RoundUpService to ensure the correctness of the round-up calculation and the transfer logic.


5. Asynchronous Processing \
   Implement asynchronous processing using a message queue (e.g., RabbitMQ, Kafka) to decouple the request handling from the actual API interactions, improving responsiveness and resilience.
