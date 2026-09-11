Feature: Wikipedia search
        As a non logged in user I should be able to do search on Wikipedia.
 
 Scenario: I select a language
        Given I navigate to "https://www.wikipedia.org/"
        And I select "he" option by value from dropdown having id "searchLanguage"
        When I enter "בדיקת תוכנה אוטומטית" into input field having id "searchInput"
        And I click on element having class "pure-button-primary-progressive"
        When I wait for 5 sec
        When I navigate back
        Then I close browser

 Scenario: Search for Selenium in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Selenium" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Selenium"
       And element having id "searchInput" should be present
       And I navigate back
       Then I close browser

 Scenario: Search for Java in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Java" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Java"
       And I navigate back
       Then I close browser

 Scenario: Search for Python in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Python" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Python"
       And I navigate back
       Then I close browser

 Scenario: Search for JavaScript in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "JavaScript" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "JavaScript"
       And I navigate back
       Then I close browser

 Scenario: Search for Cucumber in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Cucumber" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Cucumber"
       And I navigate back
       Then I close browser

 Scenario: Search for Software testing in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Software testing" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Software testing"
       And I navigate back
       Then I close browser

 Scenario: Search for Machine learning in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Machine learning" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Machine learning"
       And I navigate back
       Then I close browser

 Scenario: Search for Artificial intelligence in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Artificial intelligence" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Artificial intelligence"
       And I navigate back
       Then I close browser

 Scenario: Search for Data science in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Data science" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Data science"
       And I navigate back
       Then I close browser

 Scenario: Search for Linux in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Linux" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Linux"
       And I navigate back
       Then I close browser

 Scenario: Search for GitHub in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "GitHub" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "GitHub"
       And I navigate back
       Then I close browser

 Scenario: Search for Docker in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Docker" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Docker"
       And I navigate back
       Then I close browser

 Scenario: Search for Kubernetes in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Kubernetes" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Kubernetes"
       And I navigate back
       Then I close browser

 Scenario: Search for API in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "API" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "API"
       And I navigate back
       Then I close browser

 Scenario: Search for DevOps in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "DevOps" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "DevOps"
       And I navigate back
       Then I close browser

 Scenario: Search for Cloud computing in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Cloud computing" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Cloud computing"
       And I navigate back
       Then I close browser

 Scenario: Search for Cybersecurity in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Cybersecurity" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Cybersecurity"
       And I navigate back
       Then I close browser

 Scenario: Search for HTML in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "HTML" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "HTML"
       And I navigate back
       Then I close browser

 Scenario: Search for CSS in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "CSS" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "CSS"
       And I navigate back
       Then I close browser

 Scenario: Search for Wikipedia in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Wikipedia" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Wikipedia"
       And I navigate back
       Then I close browser

 Scenario: Search for India in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "India" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "India"
       And I navigate back
       Then I close browser

 Scenario: Search for Earth in English
       Given I navigate to "https://www.wikipedia.org/"
       And I select "en" option by value from dropdown having id "searchLanguage"
       When I enter "Earth" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Earth"
       And I navigate back
       Then I close browser

 Scenario: Search for Programming in French
       Given I navigate to "https://www.wikipedia.org/"
       And I select "fr" option by value from dropdown having id "searchLanguage"
       When I enter "Programmation" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Programmation"
       And I navigate back
       Then I close browser

 Scenario: Search for Artificial intelligence in German
       Given I navigate to "https://www.wikipedia.org/"
       And I select "de" option by value from dropdown having id "searchLanguage"
       When I enter "Künstliche Intelligenz" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Künstliche"
       And I navigate back
       Then I close browser

 Scenario: Search for Automation in Spanish
       Given I navigate to "https://www.wikipedia.org/"
       And I select "es" option by value from dropdown having id "searchLanguage"
       When I enter "Automatización" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Automatización"
       And I navigate back
       Then I close browser

 Scenario: Search for Testing in Italian
       Given I navigate to "https://www.wikipedia.org/"
       And I select "it" option by value from dropdown having id "searchLanguage"
       When I enter "Test software" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "Test"
       And I navigate back
       Then I close browser

 Scenario: Search for Software testing in Hebrew
       Given I navigate to "https://www.wikipedia.org/"
       And I select "he" option by value from dropdown having id "searchLanguage"
       When I enter "בדיקת תוכנה" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "בדיקת"
       And I navigate back
       Then I close browser

 Scenario: Search for Automation in Arabic
       Given I navigate to "https://www.wikipedia.org/"
       And I select "ar" option by value from dropdown having id "searchLanguage"
       When I enter "الأتمتة" into input field having id "searchInput"
       And I click on element having class "pure-button-primary-progressive"
       Then I should see page title having partial text as "الأتمتة"
       And I navigate back
       Then I close browser
