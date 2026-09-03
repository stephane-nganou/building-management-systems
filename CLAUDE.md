# The shops management system web app

## Overview

This is an Appartment management system web app to allow users to manage their Appartments buildings and customers.
A user can sign in an sign out. Assistants also
A user can create one building or multiple buildins and manage them independently. In each building, they have the ability to add appartments (name/appartment number, rent price, rooms numbers (kitchen, toilets etc.), Information above the person renting) etc.
The user can add expenses related to appartments an the reasons!
A user can generate professional invoices (for rent, cold water) as PDF.
A Owner can also assign one or multiple assistants who will help. The Owner can give fined grained features access to assistant!
A user (the owner) of the shop can view a summary of their incomes and expenses (per building and overall) over a defined period and generate their Profit and Loss statements, which will be useful for tax declaration.

## Development process

When instructed to build a feature:
1. Use your Atlassian tools to read the feature instructions from jira.
2. Develop the feature - do not skip from the feature-dev 7 step process.
3. Thorougly test the feature with unit tests and integration tests and fix any issues.
4. Submit a PR using your github tools

## Technical design

- The entire project should be packaged into Docker containers.  
- The backend should be in backend/ and be a Spring boot project using the latest stable spring boot version, latest stable Java version compatible with the spring boot version, and maven.
- The frontend should be in frontend/  and be an angular project (latest stable version of angular).
- We will use JWT, keycloak and later on, implement Oauth 2.
- The database should use Postgres and be persisted using volumes. For quick e2e test purposes you may use SQLLite. Thing from the beginning about a migration tool (propose what is convenient and why)
- There should be scripts in scripts/ for:  
    ```bash
    # Mac
    scripts/start-mac.sh    # Start
    scripts/stop-mac.sh     # Stop

    # Linux
    scripts/start-linux.sh
    scripts/stop-linux.sh

    # Windows
    scripts/start-windows.ps1
    scripts/stop-windows.ps1
    ```

## Implementation status

@docs/IMPLEMENTATION_STATUS.md