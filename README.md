# LoadManager

A Spring-based batch processor that processes customer attempts to load funds into their accounts.

- JDK: 11.0.27
- Gradle: 9.0

## What it does

- Polls an input directory (every ~5 seconds).
- Reads each file line-by-line; each non-empty line is a JSON object representing a load request.
- Skips malformed JSON lines (logged as warnings) and continues.
- Valid requests are evaluated against customer limits:
  - Daily amount limit (5,000$)
  - Weekly amount limit (20,000$)
  - Daily load count limit (3)
- Once the file is processed it will be moved to a backup directory /src/main/resources/backup/.

## Requirements

- JDK 11.0.27 installed and on PATH
- Gradle 9.0
- An input directory with read/write permissions

## Configuration

The application reads properties from source `application.properties`.
- `app.input.directory` (string): Absolute path to the folder to poll for input files.
- `daily.amount.limit.cents` (long): Daily limit per customer, in cents. Default: `500000` ($5,000.00).
- `weekly.amount.limit.cents` (long): Weekly limit per customer, in cents. Default: `2000000` ($20,000.00).
- `daily.load.limit` (long): Max number of accepted loads per customer per day. Default: `3`.

You can override these values based on preferences.

Backup folder:
- Processed files are moved to `src/main/resources/backup` by default.

## Input / Output format

- Input: Newline-delimited JSON (one request per line).
- Request fields:
  - `id` (String)
  - `customer_id` (String)
  - `load_amount` (String)
  - `time` (string)

Example input (each line is one request):
{"id":"1","customer_id":"C1","load_amount":"\$100","time":"2000-01-01T00:00:00Z"}
{"id":"2","customer_id":"C2","load_amount":"\$100","time":"2000-01-01T00:00:00Z"}


## How to run
- Create and populate the input directory configured in application.properties.
Example: app.input.directory=C:/Users/user/Downloads/Input
- Copy your input files to this directory. 
- Start the application:

Using Gradle:
- ./gradlew bootRun

## How to stop
- Stop with Ctrl+C in terminal. Upon prompt, enter "Y" to confirm or press Ctrl+C again to exit.

## Running tests
- ./gradlew test